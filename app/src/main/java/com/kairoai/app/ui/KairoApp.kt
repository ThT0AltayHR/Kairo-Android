package com.kairoai.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kairoai.app.data.AssistantUiState
import com.kairoai.app.data.AssistantViewModel
import com.kairoai.app.data.ChatMessage
import com.kairoai.app.overlay.IslandOverlayService
import com.kairoai.app.widget.WidgetPresets

private enum class Page { Home, Workspace, Chat, Activity, Settings }

@Composable
fun KairoApp(vm: AssistantViewModel) {
    val state by vm.ui.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(Page.Home) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(vm::attachWorkspace) }
    val chatFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::addChatAttachment) }
    MaterialTheme(colorScheme = kairoColorScheme()) {
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(fontFamily = KairoSans)) {
        Column(
            Modifier.fillMaxSize().background(Ink)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        ) {
            Header(state.status)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (page) {
                    Page.Home -> HomeScreen(
                        state,
                        onChat = { page = Page.Chat },
                        onSettings = { page = Page.Settings },
                        onOpenConversation = { id -> vm.selectConversation(id); page = Page.Chat }
                    )
                    Page.Workspace -> WorkspaceScreen(state, vm, onChooseFolder = { folderPicker.launch(null) })
                    Page.Chat -> ChatScreen(
                        state,
                        vm,
                        onAttach = { chatFilePicker.launch(arrayOf("*/*")) },
                        onHistory = { historyOpen = true },
                        onNewConversation = vm::startNewConversation
                    )
                    Page.Activity -> ActivityScreen(state)
                    Page.Settings -> SettingsScreen(state, vm)
                }
                if (historyOpen && page == Page.Chat) {
                    ConversationLibraryDialog(
                        conversations = state.conversations,
                        activeId = state.activeConversationId,
                        onDismiss = { historyOpen = false },
                        onSelect = { id -> historyOpen = false; vm.selectConversation(id) },
                        onDelete = vm::deleteConversation,
                        onRename = vm::renameConversation
                    )
                }
            }
            BottomBar(page, onSelect = { page = it })
            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
        }
        }
    }
}

@Composable
private fun Header(status: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Violet, Color(0xFF735FBA)))), contentAlignment = Alignment.Center) {
            Text("K", color = Ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("KAIRO", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, fontSize = 17.sp)
            Text("GEMINI COMPANION", color = Muted, fontSize = 9.sp, letterSpacing = 1.7.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(CircleShape).background(Panel).padding(horizontal = 11.dp, vertical = 7.dp)) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (status.contains("hata", true) || status.contains("kurulamadı", true)) Coral else Mint))
            Spacer(Modifier.width(7.dp)); Text(status.uppercase().take(18), fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Muted)
        }
    }
}

@Composable
private fun BottomBar(selected: Page, onSelect: (Page) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF0D0F15)).padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        val items = listOf(Triple(Page.Home, "Ana", Icons.Filled.Home), Triple(Page.Workspace, "Proje", Icons.Filled.FolderOpen), Triple(Page.Chat, "Sohbet", Icons.Filled.ChatBubbleOutline), Triple(Page.Activity, "Akış", Icons.Filled.Timeline), Triple(Page.Settings, "Ayarlar", Icons.Filled.Settings))
        items.forEach { (page, label, icon) ->
            Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable { onSelect(page) }.padding(horizontal = 14.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = label, tint = if (page == selected) Violet else Muted, modifier = Modifier.size(21.dp))
                Text(label, color = if (page == selected) Violet else Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: AssistantUiState,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onOpenConversation: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF191629), Color(0xFF10151B), Panel))).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("YARDIMCIN HAZIR", color = Mint, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.6.sp)
                            Spacer(Modifier.height(9.dp))
                            Text("Fikrini söyle.\nBirlikte şekillendirelim.", fontSize = 25.sp, fontWeight = FontWeight.SemiBold, lineHeight = 31.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(if (state.apiKeySaved) "Gemini bağlantısı için hazır" else "Başlamak için Gemini API anahtarını ekle", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(15.dp))
                            Button(onClick = if (state.apiKeySaved) onChat else onSettings, shape = RoundedCornerShape(15.dp)) { Text(if (state.apiKeySaved) "Sohbete geç  →" else "Kurulumu tamamla  →") }
                        }
                        Mascot(Modifier.size(108.dp), mood = when { state.busy -> "thinking"; state.status.contains("tamam", true) -> "done"; else -> "idle" })
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("MODEL", state.selectedModel.substringAfterLast("/", "Seçilmedi").take(19), Violet, Modifier.weight(1f))
                MetricCard("KULLANILABİLİR", "${state.models.size} model", Mint, Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("Canlı akış", "Görünür durum, gizli akıl yürütme değil")
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(Modifier.size(46.dp), mood = if (state.busy) "thinking" else if (state.status.contains("tamam", true)) "done" else "idle")
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(state.status, fontWeight = FontWeight.SemiBold)
                        Text(state.progress.lastOrNull()?.detail ?: "Bir sohbet başlattığında burada güncellenir.", color = Muted, fontSize = 12.sp)
                    }
                    if (state.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Violet)
                }
            }
        }
        item {
            SectionTitle("Hızlı başlat", "Bugün ne üretelim?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("✦  Fikir", onChat, Modifier.weight(1f))
                QuickAction("⌘  Kod", onChat, Modifier.weight(1f))
                QuickAction("◉  Tasarım", onChat, Modifier.weight(1f))
            }
        }
        if (state.conversations.isNotEmpty()) {
            item {
                SectionTitle("Son sohbetler", "Şifreli cihaz arşivinden devam et")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.conversations.take(3).forEach { conversation ->
                        Surface(
                            Modifier.fillMaxWidth().clickable { onOpenConversation(conversation.id) },
                            shape = RoundedCornerShape(17.dp),
                            color = Panel,
                            border = BorderStroke(1.dp, Color(0xFF242632))
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(conversation.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text("${conversation.messageCount} mesaj · ${conversation.model.substringAfterLast('/').ifBlank { "Gemini modeli" }}", color = Muted, fontSize = 10.sp, maxLines = 1)
                                }
                                Text("AÇ  ›", color = Violet, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
        if (state.error != null) item { ErrorCard(state.error) }
        item { Text("KAIRO  •  ANDROID 16 READY", color = Color(0xFF555967), fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(vertical = 12.dp)) }
    }
}

@Composable
private fun ChatScreen(
    state: AssistantUiState,
    vm: AssistantViewModel,
    onAttach: () -> Unit,
    onHistory: () -> Unit,
    onNewConversation: () -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var modelMenu by remember { mutableStateOf(false) }
    var promptLibraryOpen by remember { mutableStateOf(false) }
    var modelSearch by rememberSaveable { mutableStateOf("") }
    val filteredModels = remember(state.models, modelSearch) {
        state.models.filter {
            modelSearch.isBlank() ||
                it.displayName.contains(modelSearch, ignoreCase = true) ||
                it.id.contains(modelSearch, ignoreCase = true) ||
                it.description.contains(modelSearch, ignoreCase = true)
        }
    }
    if (modelMenu) {
        Dialog(onDismissRequest = { modelMenu = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
                shape = RoundedCornerShape(28.dp),
                color = Panel,
                border = BorderStroke(1.dp, Color(0xFF302B42))
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Model kitaplığı", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    Text("${state.models.size} erişilebilir üretim modeli · hesabına göre", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp))
                    OutlinedTextField(
                        value = modelSearch,
                        onValueChange = { modelSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Model ara") },
                        placeholder = { Text("İsim, model kimliği veya açıklama") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    if (state.models.isEmpty()) {
                        Text("Ayarlar'dan API anahtarını kaydet ve modelleri yükle.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 20.dp))
                    } else {
                        LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(filteredModels, key = { it.id }) { model ->
                                val selected = model.id == state.selectedModel
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { vm.selectModel(model.id); modelMenu = false },
                                    shape = RoundedCornerShape(17.dp),
                                    color = if (selected) Color(0xFF242039) else Panel2,
                                    border = BorderStroke(1.dp, if (selected) Violet else Color.Transparent)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(model.displayName, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            if (selected) Text("SEÇİLİ", color = Mint, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Text(model.id, color = Violet, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        if (model.description.isNotBlank()) Text(model.description, color = Muted, fontSize = 10.sp, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                            if (filteredModels.isEmpty()) item { Text("Bu aramayla eşleşen model yok.", color = Muted, modifier = Modifier.padding(12.dp)) }
                        }
                    }
                    TextButton(onClick = { modelMenu = false }, modifier = Modifier.align(Alignment.End)) { Text("Kapat") }
                }
            }
        }
    }
    if (promptLibraryOpen) {
        PromptLibraryDialog(
            onDismiss = { promptLibraryOpen = false },
            onSelect = { prompt -> draft = prompt; promptLibraryOpen = false }
        )
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sohbet", fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                Text("Canlı yanıt akışı · ${state.messages.size / 2} tur", color = Muted, fontSize = 11.sp)
            }
            IconButton(onClick = onHistory) { Icon(Icons.Filled.History, contentDescription = "Sohbet geçmişi", tint = Violet) }
            IconButton(onClick = onNewConversation, enabled = !state.busy) { Icon(Icons.Filled.Add, contentDescription = "Yeni sohbet", tint = Mint) }
            Box {
                AssistChip(onClick = { modelMenu = true }, label = { Text(state.selectedModel.ifBlank { "Model seç" }.substringAfterLast("/"), fontSize = 10.sp, maxLines = 1) })
            }
        }
        if (state.messages.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Mascot(Modifier.size(116.dp))
                Spacer(Modifier.height(12.dp)); Text("Her sohbet yeni bir başlangıç.", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Kod, fikir, tasarım veya merak ettiğin ne varsa yaz.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                AssistChip(onClick = { promptLibraryOpen = true }, label = { Text("✦  12 hazır prompt şablonu", fontSize = 11.sp) })
                Spacer(Modifier.height(7.dp))
                listOf("Bir Android ekranı tasarlamama yardım et", "Bu kodu adım adım açıkla", "Uygulamam için yaratıcı fikirler üret").forEach { suggestion ->
                    AssistChip(onClick = { draft = suggestion }, label = { Text(suggestion, fontSize = 11.sp) })
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.messages, key = { it.id }) { message -> ChatBubble(message, state.busy && message.id == state.messages.lastOrNull()?.id) }
            }
        }
        AnimatedVisibility(state.error != null) { state.error?.let { ErrorCard(it, Modifier.padding(horizontal = 16.dp)) } }
        if (state.draftAttachments.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.draftAttachments.forEach { attachment ->
                        AssistChip(
                            onClick = { vm.removeChatAttachment(attachment.name) },
                            label = { Text("${attachment.name.take(18)}  ×", fontSize = 10.sp, maxLines = 1) }
                        )
                    }
                }
                Text("Seçtiğin ekler bu mesajla Gemini'ye gönderilir; içerikleri sohbet arşivine kaydedilmez.", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(start = 5.dp, top = 1.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = onAttach, enabled = !state.busy && !state.attachmentLoading) {
                if (state.attachmentLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Violet)
                else Icon(Icons.Filled.AttachFile, contentDescription = "Dosya ekle", tint = Violet)
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                placeholder = { Text("Kairo'ya bir şey sor…", color = Muted) },
                shape = RoundedCornerShape(22.dp),
                maxLines = 5,
                enabled = !state.busy
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (state.busy) vm.cancelResponse()
                    else { vm.sendMessage(draft); draft = "" }
                },
                enabled = state.busy || draft.isNotBlank() || state.draftAttachments.isNotEmpty(),
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) { Text(if (state.busy) "■" else "↑", fontSize = if (state.busy) 16.sp else 23.sp) }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, streaming: Boolean) {
    val isUser = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Top) {
        if (!isUser) { Mascot(Modifier.size(30.dp), mood = if (streaming) "thinking" else "idle"); Spacer(Modifier.width(8.dp)) }
        Column(Modifier.fillMaxWidth(.86f).clip(RoundedCornerShape(if (isUser) 21.dp else 19.dp)).background(if (isUser) Color(0xFF252036) else Panel).padding(14.dp)) {
            Text(if (isUser) "SEN" else "KAIRO  ·  GEMINI", color = if (isUser) Violet else Mint, fontSize = 9.sp, letterSpacing = 1.2.sp, fontFamily = FontFamily.Monospace)
            message.attachments.forEach { attachment ->
                Text(
                    if (attachment.payloadAvailable) "📎 ${attachment.name} · ${attachment.sizeBytes / 1024} KB"
                    else "📎 ${attachment.name} · içerik gizlilik için saklanmadı",
                    color = Color(0xFFD6D9E8),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = message.text.ifBlank { if (streaming) "Yanıt hazırlanıyor…" else "" },
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
            if (streaming) { Spacer(Modifier.height(6.dp)); Text("● ● ●   CANLI YANIT", color = Violet, fontFamily = FontFamily.Monospace, fontSize = 8.sp) }
        }
    }
}

@Composable
private fun WorkspaceScreen(state: AssistantUiState, vm: AssistantViewModel, onChooseFolder: () -> Unit) {
    var confirmApply by remember { mutableStateOf(false) }
    var compareOpen by rememberSaveable { mutableStateOf(false) }
    if (state.pendingDeleteUri.isNotBlank()) {
        AlertDialog(
            onDismissRequest = vm::cancelDelete,
            title = { Text("Dosya silinsin mi?") },
            text = { Text("${state.pendingDeleteName} dosyası seçtiğin klasörden kalıcı olarak silinecek. Bu işlem geri alınamaz.") },
            confirmButton = { TextButton(onClick = vm::confirmDelete) { Text("Sil", color = Coral) } },
            dismissButton = { TextButton(onClick = vm::cancelDelete) { Text("Vazgeç") } }
        )
    }
    if (confirmApply) {
        AlertDialog(
            onDismissRequest = { confirmApply = false },
            title = { Text("Taslağı dosyaya yaz?") },
            text = { Text("Gemini'nin ürettiği içerik ${state.selectedFilePath} dosyasının yerine yazılacak. Mevcut içerik değişmişse uygulama yazmayı durdurur.") },
            confirmButton = { TextButton(onClick = { confirmApply = false; vm.confirmApplyProposal() }) { Text("Onayla ve yaz") } },
            dismissButton = { TextButton(onClick = { confirmApply = false }) { Text("İptal") } }
        )
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            Text("Proje alanı", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            Text("Dosyaları seç, taslak iste, sonra kendin onayla.", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Button(onClick = onChooseFolder, modifier = Modifier.fillMaxWidth()) { Text(if (state.workspaceName.isBlank()) "Proje klasörü seç" else "Klasörü değiştir") }
        }
        if (state.workspaceUri.isNotBlank()) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(state.workspaceName.ifBlank { "Proje" }, fontWeight = FontWeight.SemiBold); Text("${state.workspaceFiles.size} desteklenen metin dosyası", color = Muted, fontSize = 11.sp) }
                        TextButton(onClick = vm::refreshWorkspace) { Text("Yenile") }
                    }
                }
            }
            if (state.workspaceFiles.isEmpty()) item { Text("Bu klasörde desteklenen metin dosyası bulunamadı. Gizli/build klasörleri atlanır; kod ve metin dosyaları gösterilir.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp) }
            items(state.workspaceFiles, key = { it.uri }) { file ->
                Surface(Modifier.fillMaxWidth().clickable { vm.openWorkspaceFile(file) }, shape = RoundedCornerShape(15.dp), color = if (file.uri == state.selectedFileUri) Color(0xFF211D33) else Panel, border = BorderStroke(1.dp, if (file.uri == state.selectedFileUri) Violet else Color.Transparent)) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("{}", color = Violet, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(file.name, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text(file.path, color = Muted, fontSize = 9.sp, maxLines = 1) }
                        Text(if (file.sizeBytes > 0) "${file.sizeBytes / 1024} KB" else "—", color = Muted, fontSize = 9.sp)
                    }
                }
            }
        } else {
            item { Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) { Text("Android'in klasör seçicisiyle yalnızca izin verdiğin proje klasörüne erişilir. Kairo dosyayı kendiliğinden değiştirmez veya silmez.", Modifier.padding(16.dp), color = Muted, fontSize = 12.sp, lineHeight = 18.sp) } }
        }
        if (state.selectedFileUri.isNotBlank()) {
            item {
                SectionTitle("Seçili dosya", state.selectedFilePath)
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(state.fileContent.take(1_200).ifBlank { "(Boş dosya)" }, color = Color(0xFFCCCBD8), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 15.sp, maxLines = 9)
                        if (state.fileContent.length > 1_200) Text("… devamı kesildi", color = Muted, fontSize = 9.sp)
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Ne değişsin?", fontWeight = FontWeight.SemiBold)
                        Text("Seçili dosyanın içeriği öneri üretmek için Gemini API'sine gönderilir.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
                        OutlinedTextField(value = state.editInstruction, onValueChange = vm::setEditInstruction, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Örn. bu ekranın hata durumunu iyileştir…") }, minLines = 2, maxLines = 4)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = vm::proposeFileEdit, enabled = !state.busy && state.editInstruction.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (state.busy) "Taslak hazırlanıyor…" else "Gemini'den taslak iste") }
                        if (state.busy) { Spacer(Modifier.height(8.dp)); CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Violet) }
                    }
                }
            }
            if (state.editProposal.isNotBlank()) item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF171A22)), border = BorderStroke(1.dp, if (state.busy) Violet else Mint)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(if (state.busy) "CANLI TASLAK" else "ONAY BEKLİYOR", color = if (state.busy) Violet else Mint, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(state.editProposal.take(2_000), color = Color(0xFFD8D7E3), fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 15.sp, maxLines = 14)
                        if (state.editProposal.length > 2_000) Text("… taslağın kalan kısmı dosyaya yazılmadan önce korunuyor", color = Muted, fontSize = 9.sp)
                        TextButton(onClick = { compareOpen = !compareOpen }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (compareOpen) "Karşılaştırmayı gizle" else "Önce / sonra karşılaştır")
                        }
                        if (compareOpen) {
                            Text("ÖNCE · MEVCUT DOSYA", color = Coral, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 5.dp))
                            Text(state.fileContent.take(3_000), color = Muted, fontFamily = FontFamily.Monospace, fontSize = 9.sp, lineHeight = 13.sp)
                            if (state.fileContent.length > 3_000) Text("… mevcut dosyanın devamı gizlendi", color = Muted, fontSize = 9.sp)
                            Text("SONRA · GEMINI TASLAĞI", color = Mint, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp))
                            Text(state.editProposal.take(3_000), color = Color(0xFFD8D7E3), fontFamily = FontFamily.Monospace, fontSize = 9.sp, lineHeight = 13.sp)
                            if (state.editProposal.length > 3_000) Text("… taslağın devamı dosyaya yazılmadan önce korunuyor", color = Muted, fontSize = 9.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { confirmApply = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("İncele ve değişikliği onayla") }
                        TextButton(onClick = vm::discardProposal, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Taslağı iptal et", color = Muted) }
                    }
                }
            }
            item { TextButton(onClick = vm::requestDeleteSelected, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Seçili dosyayı sil…", color = Coral) } }
        }
        if (state.error != null) item { ErrorCard(state.error) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ActivityScreen(state: AssistantUiState) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Text("Etkinlik", fontSize = 25.sp, fontWeight = FontWeight.SemiBold); Text("Görülebilir ilerleme işaretleri", color = Muted, fontSize = 12.sp) }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Mascot(Modifier.size(54.dp), mood = if (state.busy) "thinking" else if (state.error != null) "error" else "done"); Spacer(Modifier.width(10.dp)); Column { Text(state.status, fontWeight = FontWeight.SemiBold); Text("${state.selectedModel.ifBlank { "Gemini modeli seçilmedi" }}", color = Muted, fontSize = 11.sp) } }
                    Spacer(Modifier.height(16.dp))
                    val steps = state.progress.ifEmpty { listOf(com.kairoai.app.data.ProgressStep("Hazır", "Bir istek gönderdiğinde görev durumu burada görünür.", true)) }
                    steps.forEachIndexed { index, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(19.dp).clip(CircleShape).background(if (step.done) Mint else if (state.busy) Violet else Panel2), contentAlignment = Alignment.Center) { Text(if (step.done) "✓" else "·", fontSize = 11.sp, color = Ink, fontWeight = FontWeight.Bold) }
                                if (index < steps.lastIndex) Box(Modifier.width(1.dp).height(26.dp).background(Color(0xFF303341)))
                            }
                            Spacer(Modifier.width(11.dp)); Column { Text(step.title, fontWeight = FontWeight.Medium, fontSize = 13.sp); Text(step.detail, color = Muted, fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
        item { Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF151421))) { Text("Güvenli gösterim: arayüz bağlantı, yanıt akışı ve tamamlanma gibi durumu gösterir; modelin gizli iç akıl yürütmesini göstermeye çalışmaz.", Modifier.padding(16.dp), color = Muted, fontSize = 12.sp, lineHeight = 18.sp) } }
        if (state.error != null) item { ErrorCard(state.error) }
    }
}

@Composable
private fun SettingsScreen(state: AssistantUiState, vm: AssistantViewModel) {
    val context = LocalContext.current
    var keyInput by remember { mutableStateOf("") }
    var clearHistoryConfirm by remember { mutableStateOf(false) }
    if (clearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { clearHistoryConfirm = false },
            title = { Text("Sohbet arşivi temizlensin mi?") },
            text = { Text("Kaydedilmiş tüm sohbetler bu cihazdan silinecek. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = { clearHistoryConfirm = false; vm.clearConversationHistory() }) {
                    Text("Tüm sohbetleri sil", color = Coral)
                }
            },
            dismissButton = { TextButton(onClick = { clearHistoryConfirm = false }) { Text("Vazgeç") } }
        )
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Text("Ayarlar", fontSize = 25.sp, fontWeight = FontWeight.SemiBold); Text("Bağlantı, ada görünümü ve widget stili", color = Muted, fontSize = 12.sp) }
        item {
            SectionTitle("Gemini API", "Modelleri hesabından canlı listele")
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (state.apiKeySaved) "●  Anahtar cihazda şifreli kayıtlı" else "○  Henüz anahtar eklenmedi", color = if (state.apiKeySaved) Mint else Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(value = keyInput, onValueChange = { keyInput = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Gemini API key") }, placeholder = { Text("AIza…") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                        Button(onClick = { vm.saveApiKey(keyInput); keyInput = "" }, modifier = Modifier.weight(1f)) { Text("Cihaza kaydet") }
                        TextButton(onClick = { vm.loadModels() }, modifier = Modifier.weight(1f)) { Text("Modelleri yükle") }
                    }
                    if (state.apiKeySaved) TextButton(onClick = { vm.deleteApiKey() }) { Text("Anahtarı sil", color = Coral) }
                    Text("Anahtar AES-GCM ile Android Keystore kullanılarak cihazda saklanır. Yayınlanan uygulamada güvenli backend proxy tercih et; istemci uygulamalarındaki anahtarlar çıkarılabilir.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
        }
        item {
            SectionTitle("Canlı ada", "Ekran üstü görünüm için izin gerekir")
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp)) {
                    Text(if (state.overlayEnabled) "Ada etkin · ${state.islandWidth.toInt()} × ${state.islandHeight.toInt()} dp" else "Ada kapalı", color = if (state.overlayEnabled) Mint else Muted, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {
                        if (state.overlayEnabled) {
                            context.startService(Intent(context, IslandOverlayService::class.java).setAction(IslandOverlayService.ACTION_STOP))
                            vm.setOverlayEnabled(false)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } else {
                            vm.setOverlayEnabled(true)
                            ContextCompat.startForegroundService(context, Intent(context, IslandOverlayService::class.java).setAction(IslandOverlayService.ACTION_START))
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (state.overlayEnabled) "Üst adayı kapat" else "Üst adayı etkinleştir") }
                    LabeledSlider("Genişlik", state.islandWidth, 220f..370f) { vm.setIslandDimensions(width = it) }
                    LabeledSlider("Yükseklik", state.islandHeight, 44f..82f) { vm.setIslandDimensions(height = it) }
                    LabeledSlider("Üst kenardan konum", state.islandTop, 0f..100f) { vm.setIslandDimensions(top = it) }
                    Text("Üst ada, Android'in standart status bar'ı değil; kullanıcının açık izniyle gösterilen kayan pildir. Bazı üreticiler pil optimizasyonu uygulayabilir.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
        }
        item {
            SectionTitle("Yanıt davranışı", "Seçilen modele göre uygulanır")
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Yaratıcılık", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Temperature", color = Muted, fontSize = 10.sp)
                        }
                        Text(String.format(java.util.Locale.US, "%.1f", state.temperature), color = Violet, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                    Slider(value = state.temperature, onValueChange = vm::setTemperature, valueRange = 0f..1.5f)
                    Text("Yanıt uzunluğu sınırı", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(1_024, 2_048, 4_096, 8_192, 16_384).forEach { limit ->
                            val selected = state.maxOutputTokens == limit
                            Surface(
                                Modifier.weight(1f).clickable { vm.setMaxOutputTokens(limit) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) Color(0xFF28233C) else Panel2,
                                border = BorderStroke(1.dp, if (selected) Violet else Color.Transparent)
                            ) {
                                Text(
                                    "${limit / 1024}k",
                                    Modifier.padding(vertical = 10.dp),
                                    color = if (selected) Violet else Muted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    Text("Modelin kendi limitleri daha düşük olabilir; bu durumda Gemini hata mesajı gösterir. Büyük yanıt sınırları daha fazla kotaya yol açabilir.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 9.dp))
                }
            }
        }
        item {
            SectionTitle("Widget stilleri", "Bir widget eklerken 20 görünümden birini seç")
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(15.dp)) {
                    Text("Seçili: ${WidgetPresets.all.getOrNull(state.widgetPreset)?.name ?: "Aurora"}", color = Mint, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    WidgetPresets.all.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { preset ->
                                Surface(Modifier.weight(1f).padding(vertical = 4.dp).clickable { vm.selectWidgetPreset(preset.id) }, shape = RoundedCornerShape(13.dp), color = if (preset.id == state.widgetPreset) Color(0xFF28233C) else Panel2, border = BorderStroke(1.dp, if (preset.id == state.widgetPreset) Violet else Color.Transparent)) {
                                    Text("${preset.glyph}  ${preset.name}", Modifier.padding(horizontal = 10.dp, vertical = 12.dp), fontSize = 11.sp, color = Color(preset.accent))
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    Text("Android widget'ları animasyonlu GIF gibi sürekli oynatmaz; durum değiştiğinde yenilenir. Akıcı karakter animasyonları uygulamada ve üst adada çalışır.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }
        }
        item {
            SectionTitle("Gizlilik ve arşiv", "Kontrol sende")
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp)) {
                    Text("${state.conversations.size} sohbet cihazda şifreli arşivde", color = Mint, fontSize = 12.sp)
                    Text(
                        "Sohbet metinleri Android Keystore anahtarıyla şifrelenir. Yüklenen dosyaların içeriği arşive yazılmaz; eski ekleri tekrar göndermek için yeniden seçmen gerekir.",
                        color = Muted,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                    TextButton(onClick = { clearHistoryConfirm = true }, enabled = state.conversations.isNotEmpty()) {
                        Text("Sohbet geçmişini temizle", color = Coral)
                    }
                }
            }
        }
        if (state.error != null) item { ErrorCard(state.error) }
        item { Text("Android 16 · Kairo 0.3.0 · Yalnızca Gemini API bağlantısı", color = Color(0xFF555967), fontFamily = FontFamily.Monospace, fontSize = 9.sp, modifier = Modifier.padding(vertical = 10.dp)) }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Column(Modifier.padding(top = 10.dp)) {
        Row { Text(label, Modifier.weight(1f), fontSize = 12.sp); Text(value.toInt().toString(), color = Violet, fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
        Slider(value = value, onValueChange = onValue, valueRange = range)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 2.dp, bottom = 3.dp)) { Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 11.sp) }
}

@Composable
private fun MetricCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(14.dp)) { Text(title, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp); Spacer(Modifier.height(7.dp)); Text(value.ifBlank { "—" }, color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
    }
}

@Composable
private fun QuickAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(15.dp), color = Panel, border = BorderStroke(1.dp, Color(0xFF252833))) { Text(label, Modifier.padding(horizontal = 8.dp, vertical = 13.dp), color = Color(0xFFDAD9E4), fontSize = 11.sp) }
}

@Composable
private fun ErrorCard(message: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2B191C))) { Text(message, Modifier.padding(13.dp), color = Coral, fontSize = 11.sp, lineHeight = 16.sp) }
}
