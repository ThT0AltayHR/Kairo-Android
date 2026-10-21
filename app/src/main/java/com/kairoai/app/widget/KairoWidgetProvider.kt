package com.kairoai.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kairoai.app.MainActivity
import com.kairoai.app.R

open class KairoWidgetProvider : AppWidgetProvider() {
    open val defaultPresetId: Int get() = 0
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, it, defaultPresetId) }
    }

    companion object {
        private val providers = listOf(
            KairoWidget01Provider::class.java to 0,
            KairoWidget02Provider::class.java to 1,
            KairoWidget03Provider::class.java to 2,
            KairoWidget04Provider::class.java to 3,
            KairoWidget05Provider::class.java to 4,
            KairoWidget06Provider::class.java to 5,
            KairoWidget07Provider::class.java to 6,
            KairoWidget08Provider::class.java to 7,
            KairoWidget09Provider::class.java to 8,
            KairoWidget10Provider::class.java to 9,
            KairoWidget11Provider::class.java to 10,
            KairoWidget12Provider::class.java to 11,
            KairoWidget13Provider::class.java to 12,
            KairoWidget14Provider::class.java to 13,
            KairoWidget15Provider::class.java to 14,
            KairoWidget16Provider::class.java to 15,
            KairoWidget17Provider::class.java to 16,
            KairoWidget18Provider::class.java to 17,
            KairoWidget19Provider::class.java to 18,
            KairoWidget20Provider::class.java to 19,
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            providers.forEach { (providerClass, defaultStyle) ->
                manager.getAppWidgetIds(ComponentName(context, providerClass)).forEach { updateWidget(context, it, defaultStyle) }
            }
        }

        fun updateWidget(context: Context, appWidgetId: Int, defaultPresetId: Int = 0) {
            val prefs = context.getSharedPreferences("kairo_settings", Context.MODE_PRIVATE)
            val presetId = prefs.getInt("widget_style_$appWidgetId", prefs.getInt("widget_preset", defaultPresetId))
            val preset = WidgetPresets.get(presetId)
            val state = prefs.getString("live_state", "idle") ?: "idle"
            val message = prefs.getString("live_message", "Kairo hazır") ?: "Kairo hazır"
            val model = prefs.getString("live_model", "")?.substringAfterLast("/").orEmpty()
            val stateLabel = when (state) {
                "thinking" -> "İSTEK İŞLENİYOR"
                "streaming" -> "CANLI YANIT"
                "done" -> "TAMAMLANDI"
                "approval" -> "ONAY BEKLİYOR"
                "error" -> "HATA OLUŞTU"
                "offline" -> "ÇEVRİMDIŞI"
                else -> "HAZIR"
            }
            val views = RemoteViews(context.packageName, R.layout.kairo_widget)
            views.setInt(R.id.widget_root, "setBackgroundResource", preset.background)
            views.setTextViewText(R.id.widget_glyph, preset.glyph)
            views.setTextColor(R.id.widget_glyph, preset.accent)
            views.setTextViewText(R.id.widget_state, stateLabel)
            views.setTextColor(R.id.widget_state, preset.accent)
            views.setTextViewText(R.id.widget_message, message.take(54))
            views.setTextViewText(R.id.widget_model, model.ifBlank { "GEMINI · API BAĞLANMADI" }.uppercase().take(30))
            val open = PendingIntent.getActivity(context, appWidgetId, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, open)
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        }
    }
}

class KairoWidget01Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 0 }
class KairoWidget02Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 1 }
class KairoWidget03Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 2 }
class KairoWidget04Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 3 }
class KairoWidget05Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 4 }
class KairoWidget06Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 5 }
class KairoWidget07Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 6 }
class KairoWidget08Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 7 }
class KairoWidget09Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 8 }
class KairoWidget10Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 9 }
class KairoWidget11Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 10 }
class KairoWidget12Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 11 }
class KairoWidget13Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 12 }
class KairoWidget14Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 13 }
class KairoWidget15Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 14 }
class KairoWidget16Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 15 }
class KairoWidget17Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 16 }
class KairoWidget18Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 17 }
class KairoWidget19Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 18 }
class KairoWidget20Provider : KairoWidgetProvider() { override val defaultPresetId: Int get() = 19 }
