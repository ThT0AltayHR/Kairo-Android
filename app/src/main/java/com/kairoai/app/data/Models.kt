package com.kairoai.app.data

data class GeminiModel(val id: String, val displayName: String, val description: String = "")
data class ChatAttachment(
    val name: String,
    val mimeType: String,
    val textContent: String = "",
    val base64Data: String = "",
    val sizeBytes: Long = 0L,
    val payloadAvailable: Boolean = true
)
data class ChatMessage(
    val id: Long,
    val role: String,
    val text: String,
    val attachments: List<ChatAttachment> = emptyList()
)
data class ProgressStep(val title: String, val detail: String, val done: Boolean)
data class WorkspaceFile(val uri: String, val path: String, val name: String, val sizeBytes: Long)
data class ConversationRecord(
    val id: String,
    val title: String,
    val model: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>
)
data class ConversationSummary(
    val id: String,
    val title: String,
    val model: String,
    val updatedAt: Long,
    val messageCount: Int
)

data class AssistantUiState(
    val apiKeySaved: Boolean = false,
    val models: List<GeminiModel> = emptyList(),
    val selectedModel: String = "",
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 8192,
    val messages: List<ChatMessage> = emptyList(),
    val activeConversationId: String = "",
    val conversations: List<ConversationSummary> = emptyList(),
    val draftAttachments: List<ChatAttachment> = emptyList(),
    val attachmentLoading: Boolean = false,
    val busy: Boolean = false,
    val status: String = "Hazır",
    val error: String? = null,
    val progress: List<ProgressStep> = emptyList(),
    val overlayEnabled: Boolean = false,
    val islandWidth: Float = 292f,
    val islandHeight: Float = 54f,
    val islandTop: Float = 16f,
    val widgetPreset: Int = 0,
    val workspaceUri: String = "",
    val workspaceName: String = "",
    val workspaceFiles: List<WorkspaceFile> = emptyList(),
    val selectedFileUri: String = "",
    val selectedFilePath: String = "",
    val fileContent: String = "",
    val editInstruction: String = "",
    val editProposal: String = "",
    val pendingDeleteUri: String = "",
    val pendingDeleteName: String = ""
)
