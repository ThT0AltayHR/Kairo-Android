package com.kairoai.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kairoai.app.ui.Muted
import com.kairoai.app.ui.Panel
import com.kairoai.app.ui.Violet
import com.kairoai.app.ui.kairoColorScheme

class WidgetConfigActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val className = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className.orEmpty()
        val providerDefault = className.substringAfter("KairoWidget", "").substringBefore("Provider").toIntOrNull()?.minus(1)?.coerceIn(0, 19) ?: 0
        val settings = getSharedPreferences("kairo_settings", MODE_PRIVATE)
        val initialPreset = if (settings.contains("widget_preset")) settings.getInt("widget_preset", providerDefault) else providerDefault
        setContent {
            MaterialTheme(colorScheme = kairoColorScheme()) {
                WidgetConfigContent(initialPreset) { styleId ->
                    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        getSharedPreferences("kairo_settings", MODE_PRIVATE).edit().putInt("widget_style_$widgetId", styleId).apply()
                        KairoWidgetProvider.updateWidget(this, widgetId)
                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetConfigContent(initialPreset: Int, onAdd: (Int) -> Unit) {
    var selected by remember { mutableIntStateOf(initialPreset.coerceIn(0, 19)) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Kairo widget'ını seç", color = Color.White, fontSize = 22.sp)
        Text("Koyu tema · 20 farklı görsel dil", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WidgetPresets.all.size) { index ->
                val preset = WidgetPresets.all[index]
                Surface(Modifier.fillMaxWidth().clickable { selected = preset.id }, shape = RoundedCornerShape(16.dp), color = if (selected == preset.id) Color(0xFF211D33) else Panel, border = BorderStroke(1.dp, if (selected == preset.id) Violet else Color.Transparent)) {
                    Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${preset.glyph}   ${preset.name}", color = Color(preset.accent), fontSize = 14.sp)
                        Text(if (selected == preset.id) "SEÇİLDİ" else "ÖNİZLEME", color = Muted, fontSize = 9.sp)
                    }
                }
            }
        }
        Button(onClick = { onAdd(selected) }, modifier = Modifier.fillMaxWidth()) { Text("Bu stili ekle") }
    }
}
