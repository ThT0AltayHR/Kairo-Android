package com.kairoai.app.data

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kairoai.app.overlay.IslandOverlayService
import com.kairoai.app.widget.KairoWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AssistantViewModel(app: Application) : AndroidViewModel(app) {
    private val vault = KeyVault(app)
    private val repository = GeminiRepository()
    private val prefs = app.getSharedPreferences("kairo_settings", 0)
    private val conversationStore = ConversationStore(app)
    private val historyMutex = Mutex()
    private var conversationRecords = conversationStore.readAll()
    private val restoredActiveConversationId = prefs.getString("active_conversation_id", null)
        ?.takeIf { id -> conversationRecords.any { it.id == id } }
    private val restoredConversation = conversationRecords.firstOrNull { it.id == restoredActiveConversationId }
    private val initialConversationId = restoredConversation?.id ?: UUID.randomUUID().toString()
    private val _ui = MutableStateFlow(
        AssistantUiState(
            apiKeySaved = vault.read() != null,
            selectedModel = prefs.getString("selected_model", "") ?: "",
            temperature = prefs.getFloat("temperature", 0.7f),
            maxOutputTokens = prefs.getInt("max_output_tokens", 8192),
            messages = restoredConversation?.messages.orEmpty(),
            activeConversationId = initialConversationId,
            conversations = conversationRecords.map(::summarize),
            overlayEnabled = prefs.getBoolean("overlay_enabled", false),
            islandWidth = prefs.getInt("island_width", 292).toFloat(),
            islandHeight = prefs.getInt("island_height", 54).toFloat(),
            islandTop = prefs.getInt("island_top", 16).toFloat(),
            widgetPreset = prefs.getInt("widget_preset", 0),
            workspaceUri = prefs.getString("workspace_uri", "") ?: "",
            workspaceName = prefs.getString("workspace_name", "") ?: ""
        ).let { initial ->
            if (restoredConversation?.model.isNullOrBlank()) initial
            else initial.copy(selectedModel = restoredConversation!!.model)
        }
    )
    val ui = _ui.asStateFlow()
    private var currentJob: Job? = null
    private var resizeJob: Job? = null
    private var historySaveJob: Job? = null
    private var streamingWidgetPublished = false
    private var streamingOverlayPublished = false

    init {
        if (_ui.value.workspaceUri.isNotBlank()) refreshWorkspace()
    }

    fun startNewConversation() {
        if (_ui.value.busy) cancelResponse()
        historySaveJob?.cancel()
        val previous = _ui.value
        viewModelScope.launch {
            persistConversation(previous)
            val id = UUID.randomUUID().toString()
            prefs.edit().putString("active_conversation_id", id).apply()
            _ui.update {
                it.copy(
                    activeConversationId = id,
                    messages = emptyList(),
                    draftAttachments = emptyList(),
                    editProposal = "",
                    error = null,
                    status = "Yeni sohbet",
                    progress = emptyList()
                )
            }
        }
    }

    fun selectConversation(id: String) {
        if (id == _ui.value.activeConversationId) return
        if (_ui.value.busy) cancelResponse()
        historySaveJob?.cancel()
        val previous = _ui.value
        viewModelScope.launch {
            persistConversation(previous)
            val record = historyMutex.withLock { conversationRecords.firstOrNull { it.id == id } } ?: return@launch
            prefs.edit().putString("active_conversation_id", record.id).apply()
            if (record.model.isNotBlank()) prefs.edit().putString("selected_model", record.model).apply()
            _ui.update {
                it.copy(
                    activeConversationId = record.id,
                    messages = record.messages,
                    selectedModel = record.model.ifBlank { it.selectedModel },
                    draftAttachments = emptyList(),
                    error = null,
                    status = "Sohbet açıldı"
                )
            }
        }
    }

    fun renameConversation(id: String, title: String) {
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.isBlank()) return
        viewModelScope.launch {
            historyMutex.withLock {
                conversationRecords = conversationRecords.map { record ->
                    if (record.id == id) record.copy(title = cleanTitle, updatedAt = System.currentTimeMillis()) else record
                }
                withContext(Dispatchers.IO) { conversationStore.writeAll(conversationRecords) }
            }
            _ui.update { it.copy(conversations = conversationRecords.map(::summarize)) }
        }
    }

    fun deleteConversation(id: String) {
        val isActive = id == _ui.value.activeConversationId
        if (isActive) {
            currentJob?.cancel()
            historySaveJob?.cancel()
        }
        viewModelScope.launch {
            if (!isActive) {
                historySaveJob?.cancel()
                persistConversation(_ui.value)
            }
            val nextRecord = historyMutex.withLock {
                conversationRecords = conversationRecords.filterNot { it.id == id }
                withContext(Dispatchers.IO) { conversationStore.writeAll(conversationRecords) }
                conversationRecords.firstOrNull()
            }
            if (isActive) {
                val nextId = nextRecord?.id ?: UUID.randomUUID().toString()
                prefs.edit().putString("active_conversation_id", nextId).apply()
                if (!nextRecord?.model.isNullOrBlank()) prefs.edit().putString("selected_model", nextRecord!!.model).apply()
                _ui.update {
                    it.copy(
                        activeConversationId = nextId,
                        messages = nextRecord?.messages.orEmpty(),
                        selectedModel = nextRecord?.model?.ifBlank { it.selectedModel } ?: it.selectedModel,
                        draftAttachments = emptyList(),
                        busy = false,
                        error = null,
                        status = if (nextRecord == null) "Sohbet silindi" else "Sohbet açıldı"
                    )
                }
            }
            _ui.update { it.copy(conversations = conversationRecords.map(::summarize)) }
        }
    }

    fun clearConversationHistory() {
        historySaveJob?.cancel()
        currentJob?.cancel()
        viewModelScope.launch {
            historyMutex.withLock {
                conversationRecords = emptyList()
                withContext(Dispatchers.IO) { conversationStore.writeAll(emptyList()) }
            }
            val id = UUID.randomUUID().toString()
            prefs.edit().putString("active_conversation_id", id).apply()
            _ui.update {
                it.copy(
                    conversations = emptyList(),
                    activeConversationId = id,
                    messages = emptyList(),
                    draftAttachments = emptyList(),
                    busy = false,
                    status = "Sohbet geçmişi temizlendi",
                    error = null
                )
            }
        }
    }

    private fun summarize(record: ConversationRecord) = ConversationSummary(
        id = record.id,
        title = record.title,
        model = record.model,
        updatedAt = record.updatedAt,
        messageCount = record.messages.size
    )

    private fun scheduleConversationSave() {
        historySaveJob?.cancel()
        historySaveJob = viewModelScope.launch {
            delay(500)
            persistConversation(_ui.value)
        }
    }

    private suspend fun persistConversation(snapshot: AssistantUiState) {
        if (snapshot.messages.isEmpty() || snapshot.activeConversationId.isBlank()) return
        prefs.edit().putString("active_conversation_id", snapshot.activeConversationId).apply()
        val firstUserMessage = snapshot.messages.firstOrNull { it.role == "user" }
        val suggestedTitle = firstUserMessage?.text?.replace(Regex("\\s+"), " ")?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: firstUserMessage?.attachments?.firstOrNull()?.name
            ?: "Yeni sohbet"
        var storageError: String? = null
        val records = historyMutex.withLock {
            val existing = conversationRecords.firstOrNull { it.id == snapshot.activeConversationId }
            val title = if (existing?.title.isNullOrBlank() || existing?.title == "Yeni sohbet" || existing?.title == "Sohbet") {
                suggestedTitle.take(80)
            } else existing!!.title
            val updated = ConversationRecord(
                id = snapshot.activeConversationId,
                title = title,
                model = snapshot.selectedModel,
                updatedAt = System.currentTimeMillis(),
                messages = snapshot.messages.takeLast(80).map { message ->
                    message.copy(
                        attachments = message.attachments.map { attachment ->
                            attachment.copy(textContent = "", base64Data = "", payloadAvailable = false)
                        }
                    )
                }
            )
            conversationRecords = (conversationRecords.filterNot { it.id == updated.id } + updated)
                .sortedByDescending { it.updatedAt }
                .take(24)
            runCatching { withContext(Dispatchers.IO) { conversationStore.writeAll(conversationRecords) } }
                .onFailure { storageError = it.message ?: "Cihaz depolama hatası" }
            conversationRecords
        }
        _ui.update {
            if (storageError == null) it.copy(conversations = records.map(::summarize))
            else it.copy(error = "Sohbet arşivi kaydedilemedi: $storageError")
        }
    }

    fun saveApiKey(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) { _ui.update { it.copy(error = "API anahtarı boş olamaz.") }; return }
        runCatching { vault.save(trimmed) }.onSuccess {
            _ui.update { it.copy(apiKeySaved = true, error = null, status = "Anahtar cihazda şifreli saklandı") }
            publish("ready", "Gemini anahtarı kaydedildi")
        }.onFailure { err -> _ui.update { it.copy(error = "Anahtar kaydedilemedi: ${err.message}") } }
    }

    fun deleteApiKey() {
        vault.clear()
        _ui.update { it.copy(apiKeySaved = false, models = emptyList(), selectedModel = "", status = "API anahtarı kaldırıldı") }
        publish("offline", "Gemini anahtarı kaldırıldı")
    }

    fun loadModels() {
        val key = vault.read()
        if (key.isNullOrBlank()) { _ui.update { it.copy(error = "Önce Ayarlar'dan Gemini API anahtarını kaydet.") }; return }
        viewModelScope.launch {
            _ui.update { it.copy(status = "Modeller yükleniyor…", error = null) }
            publish("thinking", "Gemini modelleri aranıyor")
            runCatching { repository.listModels(key) }
                .onSuccess { models ->
                    _ui.update { current -> current.copy(models = models, selectedModel = current.selectedModel.takeIf { id -> models.any { it.id == id } } ?: models.firstOrNull()?.id.orEmpty(), status = "${models.size} kullanılabilir model bulundu", error = null) }
                    publish("ready", "${models.size} Gemini modeli hazır")
                }
                .onFailure { err -> _ui.update { it.copy(status = "Bağlantı kurulamadı", error = err.message ?: "Model listesi alınamadı") }; publish("error", "Model listesi alınamadı") }
        }
    }

    fun selectModel(id: String) {
        prefs.edit().putString("selected_model", id).apply()
        _ui.update { it.copy(selectedModel = id) }
        scheduleConversationSave()
    }

    fun setTemperature(value: Float) {
        val safe = value.coerceIn(0f, 1.5f)
        prefs.edit().putFloat("temperature", safe).apply()
        _ui.update { it.copy(temperature = safe) }
    }

    fun setMaxOutputTokens(value: Int) {
        val safe = value.coerceIn(1_024, 16_384)
        prefs.edit().putInt("max_output_tokens", safe).apply()
        _ui.update { it.copy(maxOutputTokens = safe) }
    }

    fun addChatAttachment(uri: Uri) {
        if (_ui.value.draftAttachments.size >= 3) {
            _ui.update { it.copy(error = "Bir mesaja en fazla 3 dosya ekleyebilirsin.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(attachmentLoading = true, error = null) }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    val resolver = app.contentResolver
                    val mime = resolver.getType(uri)?.lowercase().orEmpty()
                    val isText = mime.startsWith("text/") || mime == "application/json"
                    val isImage = mime in setOf("image/png", "image/jpeg", "image/webp")
                    require(isText || isImage || mime == "application/pdf") {
                        "Bu dosya türü desteklenmiyor. PNG/JPEG/WebP, PDF veya metin dosyası seç."
                    }
                    var name = "ek-dosya"
                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) name = cursor.getString(0) ?: name
                    }
                    val bytes = resolver.openInputStream(uri)?.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8_192)
                        var total = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 5 * 1024 * 1024) { "Ek dosyalar 5 MB sınırını aşamaz." }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    } ?: error("Dosya okunamadı.")
                    if (isText) {
                        require(bytes.size <= 256 * 1024) { "Metin dosyaları 256 KB sınırını aşamaz." }
                        ChatAttachment(name, mime, textContent = String(bytes, Charsets.UTF_8), sizeBytes = bytes.size.toLong())
                    } else {
                        ChatAttachment(name, mime, base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP), sizeBytes = bytes.size.toLong())
                    }
                }
            }
            result.onSuccess { attachment ->
                _ui.update { state ->
                    state.copy(
                        draftAttachments = state.draftAttachments + attachment,
                        attachmentLoading = false,
                        status = "Dosya sohbete eklendi",
                        error = null
                    )
                }
            }.onFailure { error ->
                _ui.update { it.copy(attachmentLoading = false, error = error.message ?: "Dosya eklenemedi.") }
            }
        }
    }

    fun removeChatAttachment(name: String) {
        _ui.update { it.copy(draftAttachments = it.draftAttachments.filterNot { attachment -> attachment.name == name }) }
    }

    fun sendMessage(text: String, attachments: List<ChatAttachment> = _ui.value.draftAttachments) {
        val clean = text.trim()
        if ((clean.isEmpty() && attachments.isEmpty()) || _ui.value.busy) return
        val key = vault.read()
        if (key.isNullOrBlank()) { _ui.update { it.copy(error = "Önce Ayarlar'dan Gemini API anahtarını kaydet.") }; return }
        val model = _ui.value.selectedModel.ifBlank { _ui.value.models.firstOrNull()?.id.orEmpty() }
        if (model.isBlank()) { _ui.update { it.copy(error = "Ayarlar'dan Gemini modellerini yükle.") }; return }
        currentJob?.cancel()
        streamingWidgetPublished = false
        streamingOverlayPublished = false
        val user = ChatMessage(System.currentTimeMillis(), "user", clean, attachments)
        val assistantId = user.id + 1
        _ui.update { it.copy(messages = it.messages + user + ChatMessage(assistantId, "assistant", ""), draftAttachments = emptyList(), busy = true, status = "Gemini'ye bağlanıyor", error = null, progress = listOf(ProgressStep("İstek alındı", "Mesaj ve ekler güvenli bağlantı üzerinden gönderiliyor", true), ProgressStep("Yanıt hazırlanıyor", "Canlı yanıt akışı bekleniyor", false))) }
        scheduleConversationSave()
        publish("thinking", "Gemini isteği alındı")
        currentJob = viewModelScope.launch {
            try {
                val generation = _ui.value
                repository.streamReply(key, model, generation.messages.dropLast(1), generation.temperature.toDouble(), generation.maxOutputTokens).collect { chunk ->
                    _ui.update { state ->
                        val items = state.messages.toMutableList()
                        val index = items.indexOfLast { it.id == assistantId }
                        if (index >= 0) items[index] = items[index].copy(text = items[index].text + chunk)
                        state.copy(messages = items, status = "Canlı yanıt geliyor", progress = listOf(ProgressStep("İstek alındı", "Gemini bağlantısı kuruldu", true), ProgressStep("Yanıt akışı", "Metin parça parça görüntüleniyor", false)))
                    }
                    scheduleConversationSave()
                    publish("streaming", "Gemini yanıt üretiyor")
                }
                _ui.update { it.copy(busy = false, status = "Yanıt tamamlandı", progress = listOf(ProgressStep("İstek alındı", "İşlendi", true), ProgressStep("Yanıt tamamlandı", "İçerik hazır", true))) }
                scheduleConversationSave()
                publish("done", "Yanıt tamamlandı")
                delay(6_000)
                if (!_ui.value.busy) publish("ready", "Kairo hazır")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val message = e.message ?: "Bağlantı hatası"
                _ui.update { it.copy(busy = false, status = "Bağlantı hatası", error = message, progress = listOf(ProgressStep("İstek başarısız", message, false))) }
                scheduleConversationSave()
                publish(if (message.contains("Unable to resolve", true) || message.contains("connect", true)) "offline" else "error", "Gemini isteği başarısız")
            }
        }
    }

    fun cancelResponse() {
        currentJob?.cancel()
        _ui.update { it.copy(busy = false, status = "Yanıt durduruldu") }
        scheduleConversationSave()
        publish("ready", "Yanıt durduruldu")
    }

    fun setOverlayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("overlay_enabled", enabled).apply()
        _ui.update { it.copy(overlayEnabled = enabled) }
        publish(if (enabled) "ready" else "idle", if (enabled) "Ada etkin" else "Ada kapatıldı")
    }

    fun setIslandDimensions(width: Float = _ui.value.islandWidth, height: Float = _ui.value.islandHeight, top: Float = _ui.value.islandTop) {
        prefs.edit().putInt("island_width", width.toInt()).putInt("island_height", height.toInt()).putInt("island_top", top.toInt()).apply()
        _ui.update { it.copy(islandWidth = width, islandHeight = height, islandTop = top) }
        resizeJob?.cancel()
        resizeJob = viewModelScope.launch {
            delay(120)
            if (_ui.value.overlayEnabled) getApplication<Application>().startService(Intent(getApplication(), IslandOverlayService::class.java).setAction(IslandOverlayService.ACTION_RESIZE))
        }
    }

    fun selectWidgetPreset(id: Int) {
        prefs.edit().putInt("widget_preset", id).apply()
        _ui.update { it.copy(widgetPreset = id) }
        KairoWidgetProvider.updateAll(getApplication())
    }

    fun attachWorkspace(uri: Uri) {
        val app = getApplication<Application>()
        runCatching { app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        val root = DocumentFile.fromTreeUri(app, uri)
        val name = root?.name ?: "Android projesi"
        prefs.edit().putString("workspace_uri", uri.toString()).putString("workspace_name", name).apply()
        _ui.update { it.copy(workspaceUri = uri.toString(), workspaceName = name, workspaceFiles = emptyList(), selectedFileUri = "", selectedFilePath = "", fileContent = "", editProposal = "", error = null) }
        refreshWorkspace()
    }

    fun refreshWorkspace() {
        val treeUri = _ui.value.workspaceUri.takeIf { it.isNotBlank() }?.let { Uri.parse(it) } ?: return
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                val root = DocumentFile.fromTreeUri(getApplication(), treeUri) ?: return@withContext emptyList()
                val found = mutableListOf<WorkspaceFile>()
                collectTextFiles(root, "", 0, found)
                found.sortedBy { it.path.lowercase() }
            }
            _ui.update { it.copy(workspaceFiles = files) }
        }
    }

    private fun collectTextFiles(dir: DocumentFile, relative: String, depth: Int, output: MutableList<WorkspaceFile>) {
        if (depth > 5 || output.size >= 350) return
        val ignored = setOf(".git", ".gradle", "build", "node_modules", ".idea", "dist")
        val children = runCatching { dir.listFiles().sortedBy { it.name?.lowercase().orEmpty() } }.getOrDefault(emptyList())
        for (child in children) {
            val name = child.name ?: continue
            if (name.startsWith(".") || name in ignored) continue
            val path = if (relative.isBlank()) name else "$relative/$name"
            if (child.isDirectory) {
                collectTextFiles(child, path, depth + 1, output)
            } else {
                val ext = name.substringAfterLast('.', "").lowercase()
                val textExtensions = setOf("kt", "kts", "java", "xml", "json", "gradle", "properties", "md", "txt", "js", "ts", "tsx", "jsx", "html", "css", "scss", "py", "yaml", "yml", "toml", "swift", "sh", "sql", "dart", "go", "rs", "c", "h", "cpp", "hpp")
                val size = child.length()
                if (ext in textExtensions && (size == 0L || size <= 32_768L)) output += WorkspaceFile(child.uri.toString(), path, name, size)
            }
            if (output.size >= 350) break
        }
    }

    fun openWorkspaceFile(file: WorkspaceFile) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(Uri.parse(file.uri))?.let(::readLimited) ?: error("Dosya okunamadı.")
                }
            }
            result.onSuccess { text -> _ui.update { it.copy(selectedFileUri = file.uri, selectedFilePath = file.path, fileContent = text, editProposal = "", error = null) } }
                .onFailure { err -> _ui.update { it.copy(error = err.message ?: "Dosya okunamadı.") } }
        }
    }

    fun setEditInstruction(value: String) { _ui.update { it.copy(editInstruction = value) } }

    fun proposeFileEdit() {
        val current = _ui.value
        val key = vault.read()
        if (key.isNullOrBlank()) { _ui.update { it.copy(error = "Önce Gemini API anahtarını Ayarlar'dan kaydet.") }; return }
        if (current.selectedFileUri.isBlank()) { _ui.update { it.copy(error = "Önce bir metin dosyası aç.") }; return }
        if (current.editInstruction.isBlank()) { _ui.update { it.copy(error = "Yapılacak değişikliği kısaca yaz.") }; return }
        if (current.selectedModel.isBlank()) { _ui.update { it.copy(error = "Önce Gemini modellerini yükle ve bir model seç.") }; return }
        val prompt = """You are editing a user-selected source file. Return only the complete replacement file contents, with no markdown fences and no commentary. Preserve all unrelated behavior and formatting. Do not claim that you have written the file. The app will show the proposed content and wait for explicit user confirmation before saving.

File: ${current.selectedFilePath}
User's requested change:
${current.editInstruction}

Current file contents:
${current.fileContent}
""".trimIndent()
        streamingWidgetPublished = false
        streamingOverlayPublished = false
        _ui.update { it.copy(busy = true, status = "Düzenleme önerisi hazırlanıyor", editProposal = "", error = null, progress = listOf(ProgressStep("Dosya seçildi", current.selectedFilePath, true), ProgressStep("Öneri hazırlanıyor", "Gemini taslağı üretiyor; dosyaya henüz yazılmıyor", false))) }
        publish("thinking", "Dosya için değişiklik taslağı hazırlanıyor")
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            val result = StringBuilder()
            try {
                repository.streamReply(key, current.selectedModel, listOf(ChatMessage(System.currentTimeMillis(), "user", prompt)), current.temperature.toDouble(), current.maxOutputTokens).collect { chunk ->
                    result.append(chunk)
                    _ui.update { it.copy(editProposal = result.toString(), status = "Taslak hazırlanıyor") }
                    publish("streaming", "Gemini değişiklik taslağı hazırlıyor")
                }
                val proposal = stripCodeFence(result.toString())
                _ui.update { it.copy(editProposal = proposal, busy = false, status = "Onay gerekiyor", progress = listOf(ProgressStep("Dosya okundu", current.selectedFilePath, true), ProgressStep("Taslak hazır", "Dosyaya yazılmadan önce onayını bekliyor", true))) }
                publish("approval", "${current.selectedFilePath.substringAfterLast('/')} için taslak hazır; onay gerekiyor")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.update { it.copy(busy = false, editProposal = "", status = "Taslak üretilemedi", error = e.message ?: "Gemini isteği başarısız") }
                publish("error", "Dosya taslağı üretilemedi")
            }
        }
    }

    private fun readLimited(input: java.io.InputStream): String {
        val output = java.io.ByteArrayOutputStream()
        input.use { source ->
            val buffer = ByteArray(4_096)
            var total = 0
            while (true) {
                val read = source.read(buffer)
                if (read < 0) break
                total += read
                require(total <= 32_768) { "Dosya 32 KB sınırını aşıyor." }
                output.write(buffer, 0, read)
            }
        }
        return String(output.toByteArray(), Charsets.UTF_8)
    }

    private fun stripCodeFence(raw: String): String {
        val value = raw.trim()
        if (!value.startsWith("```")) return raw
        val firstLineEnd = value.indexOf('\n')
        if (firstLineEnd < 0 || !value.endsWith("```")) return raw
        return value.substring(firstLineEnd + 1, value.length - 3).trimEnd()
    }

    fun confirmApplyProposal() {
        val uri = _ui.value.selectedFileUri
        val proposal = _ui.value.editProposal
        val expectedContent = _ui.value.fileContent
        if (uri.isBlank() || proposal.isBlank()) return
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val latest = resolver.openInputStream(Uri.parse(uri))?.let(::readLimited) ?: error("Dosya okunamadı.")
                    check(latest == expectedContent) { "Dosya son görüntülemeden sonra değişmiş. Yeniden açıp taslağı tekrar üret." }
                    resolver.openOutputStream(Uri.parse(uri), "wt")?.use { output -> output.write(proposal.toByteArray(Charsets.UTF_8)) } ?: error("Dosya yazılamadı.")
                }
            }
            result.onSuccess {
                _ui.update { it.copy(fileContent = proposal, editProposal = "", status = "Dosya güncellendi", error = null) }
                publish("done", "${_ui.value.selectedFilePath.substringAfterLast('/')} güncellendi")
            }.onFailure { err -> _ui.update { it.copy(error = err.message ?: "Dosya yazılamadı.") }; publish("error", "Dosya yazılamadı") }
        }
    }

    fun discardProposal() { _ui.update { it.copy(editProposal = "", status = "Taslak iptal edildi") }; publish("ready", "Taslak iptal edildi") }

    fun requestDeleteSelected() {
        val current = _ui.value
        if (current.selectedFileUri.isNotBlank()) {
            _ui.update { it.copy(pendingDeleteUri = current.selectedFileUri, pendingDeleteName = current.selectedFilePath) }
            publish("approval", "Silme onayı gerekiyor: ${current.selectedFilePath.substringAfterLast('/')}")
        }
    }

    fun cancelDelete() { _ui.update { it.copy(pendingDeleteUri = "", pendingDeleteName = "") }; publish("ready", "Silme iptal edildi") }

    fun confirmDelete() {
        val uri = _ui.value.pendingDeleteUri
        val name = _ui.value.pendingDeleteName
        if (uri.isBlank()) return
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) { runCatching { DocumentFile.fromSingleUri(getApplication(), Uri.parse(uri))?.delete() == true }.getOrDefault(false) }
            _ui.update { it.copy(pendingDeleteUri = "", pendingDeleteName = "", selectedFileUri = if (deleted) "" else it.selectedFileUri, selectedFilePath = if (deleted) "" else it.selectedFilePath, fileContent = if (deleted) "" else it.fileContent, editProposal = if (deleted) "" else it.editProposal, status = if (deleted) "Dosya silindi" else "Silme başarısız", error = if (deleted) null else "Bu dosya sağlayıcısı silme işlemini desteklemedi.") }
            if (deleted) { refreshWorkspace(); publish("done", "$name silindi") } else publish("error", "Dosya silinemedi")
        }
    }

    private fun publish(kind: String, message: String) {
        val app = getApplication<Application>()
        prefs.edit().putString("live_state", kind).putString("live_message", message).putString("live_model", _ui.value.selectedModel).apply()
        if (kind != "streaming" || !streamingWidgetPublished) {
            KairoWidgetProvider.updateAll(app)
            if (kind == "streaming") streamingWidgetPublished = true
        }
        if (_ui.value.overlayEnabled && (kind != "streaming" || !streamingOverlayPublished)) {
            val update = Intent(app, IslandOverlayService::class.java)
                .setAction(IslandOverlayService.ACTION_UPDATE)
                .putExtra(IslandOverlayService.EXTRA_STATE, kind)
                .putExtra(IslandOverlayService.EXTRA_MESSAGE, message)
            if (kind == "streaming") streamingOverlayPublished = true
            androidx.core.content.ContextCompat.startForegroundService(app, update)
        }
    }
}
