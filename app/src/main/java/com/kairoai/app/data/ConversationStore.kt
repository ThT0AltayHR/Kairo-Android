package com.kairoai.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small encrypted local conversation archive.
 *
 * Attachments are intentionally not written to disk. Only their name, MIME type,
 * and size are retained, so a restored chat never silently resends old files.
 */
class ConversationStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "kairo_conversations.enc"))
    private val keyAlias = "kairo_conversations_aes_gcm_v1"

    @Synchronized
    fun readAll(): List<ConversationRecord> = runCatching {
        if (!file.baseFile.exists()) return emptyList()
        val packed = file.openRead().use { it.readBytes() }
        require(packed.size > IV_LENGTH)
        val iv = packed.copyOfRange(0, IV_LENGTH)
        val encrypted = packed.copyOfRange(IV_LENGTH, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, iv))
        val root = JSONObject(String(cipher.doFinal(encrypted), Charsets.UTF_8))
        val rows = root.optJSONArray("conversations") ?: JSONArray()
        buildList {
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val messagesJson = row.optJSONArray("messages") ?: JSONArray()
                val messages = buildList {
                    for (j in 0 until messagesJson.length()) {
                        val messageJson = messagesJson.optJSONObject(j) ?: continue
                        val attachmentJson = messageJson.optJSONArray("attachments") ?: JSONArray()
                        val attachments = buildList {
                            for (k in 0 until attachmentJson.length()) {
                                val attachment = attachmentJson.optJSONObject(k) ?: continue
                                add(
                                    ChatAttachment(
                                        name = attachment.optString("name", "Ek dosya"),
                                        mimeType = attachment.optString("mimeType"),
                                        sizeBytes = attachment.optLong("sizeBytes"),
                                        payloadAvailable = false
                                    )
                                )
                            }
                        }
                        add(
                            ChatMessage(
                                id = messageJson.optLong("id"),
                                role = messageJson.optString("role", "user"),
                                text = messageJson.optString("text"),
                                attachments = attachments
                            )
                        )
                    }
                }
                if (messages.isNotEmpty()) {
                    add(
                        ConversationRecord(
                            id = row.optString("id"),
                            title = row.optString("title", "Sohbet"),
                            model = row.optString("model"),
                            updatedAt = row.optLong("updatedAt"),
                            messages = messages
                        )
                    )
                }
            }
        }.sortedByDescending { it.updatedAt }.take(MAX_CONVERSATIONS)
    }.getOrDefault(emptyList())

    @Synchronized
    fun writeAll(conversations: List<ConversationRecord>) {
        val payload = JSONObject().put(
            "conversations",
            JSONArray().apply {
                conversations.sortedByDescending { it.updatedAt }.take(MAX_CONVERSATIONS).forEach { conversation ->
                    put(
                        JSONObject()
                            .put("id", conversation.id)
                            .put("title", conversation.title.take(MAX_TITLE_LENGTH))
                            .put("model", conversation.model)
                            .put("updatedAt", conversation.updatedAt)
                            .put(
                                "messages",
                                JSONArray().apply {
                                    conversation.messages.takeLast(MAX_MESSAGES).forEach { message ->
                                        put(
                                            JSONObject()
                                                .put("id", message.id)
                                                .put("role", message.role)
                                                .put("text", message.text.take(MAX_MESSAGE_LENGTH))
                                                .put(
                                                    "attachments",
                                                    JSONArray().apply {
                                                        message.attachments.take(3).forEach { attachment ->
                                                            put(
                                                                JSONObject()
                                                                    .put("name", attachment.name.take(180))
                                                                    .put("mimeType", attachment.mimeType.take(120))
                                                                    .put("sizeBytes", attachment.sizeBytes)
                                                            )
                                                        }
                                                    }
                                                )
                                        )
                                    }
                                }
                            )
                    )
                }
            }
        ).toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val packed = cipher.iv + cipher.doFinal(payload)
        var output: java.io.FileOutputStream? = null
        try {
            output = file.startWrite()
            output.write(packed)
            file.finishWrite(output)
        } catch (error: Exception) {
            output?.let(file::failWrite)
            throw error
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_BITS = 128
        private const val MAX_CONVERSATIONS = 24
        private const val MAX_MESSAGES = 80
        private const val MAX_MESSAGE_LENGTH = 12_000
        private const val MAX_TITLE_LENGTH = 80
    }
}