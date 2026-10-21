package com.kairoai.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BorderStroke
import androidx.compose.material3.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kairoai.app.data.ConversationSummary
import java.text.DateFormat
import java.util.Date

@Composable
fun ConversationLibraryDialog(
    conversations: List<ConversationSummary>,
    activeId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRename: (String, String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ConversationSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationSummary?>(null) }
    var renameText by remember { mutableStateOf("") }
    val filtered = remember(conversations, query) {
        conversations.filter {
            query.isBlank() ||
                it.title.contains(query, ignoreCase = true) ||
                it.model.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp),
            shape = RoundedCornerShape(28.dp),
            color = Panel,
            border = BorderStroke(1.dp, Color(0xFF302B42))
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Sohbet arşivi", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text("Son 24 sohbet · içerikler cihazda şifreli saklanır", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Sohbetlerde ara") },
                    placeholder = { Text("Başlık veya model") },
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(12.dp))
                if (filtered.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().weight(1f, fill = false).padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Forum, contentDescription = null, tint = Violet, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(if (conversations.isEmpty()) "Henüz kaydedilmiş sohbet yok." else "Eşleşen sohbet bulunamadı.", color = Muted, fontSize = 12.sp)
                        Text("Yeni bir mesaj gönderdiğinde sohbet burada görünür.", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                } else {
                    LazyColumn(
                        Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { conversation ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(conversation.id) },
                                shape = RoundedCornerShape(18.dp),
                                color = if (conversation.id == activeId) Color(0xFF242039) else Panel2,
                                border = BorderStroke(1.dp, if (conversation.id == activeId) Violet else Color.Transparent)
                            ) {
                                Row(
                                    Modifier.padding(start = 13.dp, end = 5.dp, top = 10.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(conversation.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                        Text(
                                            "${conversation.messageCount} mesaj · ${conversation.model.substringAfterLast('/').ifBlank { "Model seçilmedi" }}",
                                            color = Muted,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(conversation.updatedAt)),
                                            color = Muted,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    IconButton(onClick = { renameTarget = conversation; renameText = conversation.title }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Sohbeti yeniden adlandır", tint = Violet, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { deleteTarget = conversation }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Sohbeti sil", tint = Coral, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Kapat") }
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Sohbeti yeniden adlandır") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(80) },
                    singleLine = true,
                    label = { Text("Başlık") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(target.id, renameText)
                    renameTarget = null
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("İptal") } }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Sohbet silinsin mi?") },
            text = { Text("“${target.title}” sohbeti cihazdaki şifreli arşivden kaldırılacak.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) { Text("Sil", color = Coral) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Vazgeç") } }
        )
    }
}