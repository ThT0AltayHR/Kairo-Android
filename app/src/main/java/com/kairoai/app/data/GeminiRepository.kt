package com.kairoai.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GeminiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    suspend fun listModels(apiKey: String): List<GeminiModel> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val found = mutableListOf<GeminiModel>()
        var pageToken: String? = null
        var page = 0
        do {
            val tokenQuery = pageToken?.let { "&pageToken=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000$tokenQuery")
                .header("x-goog-api-key", apiKey)
                .get().build()
            val nextToken = client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw IllegalStateException(readError(raw, response.code))
                val root = JSONObject(raw)
                val arr = root.optJSONArray("models") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val methods = item.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                    val canGenerate = (0 until methods.length()).any { methods.optString(it) == "generateContent" }
                    if (!canGenerate) continue
                    val id = item.optString("name").removePrefix("models/")
                    if (id.isNotBlank()) found += GeminiModel(id, item.optString("displayName", id), item.optString("description"))
                }
                root.optString("nextPageToken").takeIf { it.isNotBlank() }
            }
            pageToken = nextToken
            page++
        } while (pageToken != null && page < 10)
        found.distinctBy { it.id }.sortedBy { it.displayName.lowercase() }
    }

    fun streamReply(
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        temperature: Double = 0.7,
        maxOutputTokens: Int = 8192
    ): Flow<String> = flow {
        val contents = JSONArray()
        history.filter { it.text.isNotBlank() || it.attachments.isNotEmpty() }.takeLast(16).forEach { msg ->
            val parts = JSONArray()
            if (msg.text.isNotBlank()) parts.put(JSONObject().put("text", msg.text))
            msg.attachments.forEach { attachment ->
                if (attachment.textContent.isNotBlank()) {
                    parts.put(JSONObject().put("text", "Attached file: ${attachment.name}\n${attachment.textContent}"))
                } else if (attachment.base64Data.isNotBlank()) {
                    parts.put(JSONObject().put("inline_data", JSONObject()
                        .put("mime_type", attachment.mimeType)
                        .put("data", attachment.base64Data)))
                }
            }
            contents.put(JSONObject()
                .put("role", if (msg.role == "assistant") "model" else "user")
                .put("parts", parts))
        }
        val body = JSONObject().put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", temperature).put("maxOutputTokens", maxOutputTokens))
            .toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val encodedModel = URLEncoder.encode(model, "UTF-8").replace("+", "%20")
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$encodedModel:streamGenerateContent?alt=sse")
            .header("x-goog-api-key", apiKey)
            .post(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorText = response.body?.string().orEmpty()
                throw IllegalStateException(readError(errorText, response.code))
            }
            val source = response.body?.source() ?: throw IllegalStateException("Gemini boş yanıt verdi.")
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isBlank() || payload == "[DONE]") continue
                val parsed = runCatching { JSONObject(payload) }.getOrNull() ?: continue
                val candidates = parsed.optJSONArray("candidates") ?: continue
                val candidate = candidates.optJSONObject(0) ?: continue
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts") ?: JSONArray()
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    if (part.optBoolean("thought", false)) continue
                    val text = part.optString("text").orEmpty()
                    if (text.isNotEmpty()) emit(text)
                }
                if (candidate.optString("finishReason") == "MAX_TOKENS") throw IllegalStateException("Gemini yanıtı token sınırına ulaştı; eksik taslağı dosyaya uygulama.")
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun readError(raw: String, code: Int): String = runCatching {
        JSONObject(raw).optJSONObject("error")?.optString("message")
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Gemini isteği başarısız (HTTP $code)."
}
