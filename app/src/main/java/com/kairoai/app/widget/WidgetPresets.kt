package com.kairoai.app.widget

import com.kairoai.app.R

data class WidgetPreset(val id: Int, val name: String, val glyph: String, val accent: Int, val background: Int)

object WidgetPresets {
    val all = listOf(
        WidgetPreset(0, "Aurora", "✦", 0xFFB39BFF.toInt(), R.drawable.widget_bg_01),
        WidgetPreset(1, "Neon Dojo", "刀", 0xFFFF9C82.toInt(), R.drawable.widget_bg_02),
        WidgetPreset(2, "Moon Garden", "☾", 0xFF9CCBFF.toInt(), R.drawable.widget_bg_03),
        WidgetPreset(3, "Sakura Circuit", "✿", 0xFFFFA7C9.toInt(), R.drawable.widget_bg_04),
        WidgetPreset(4, "Deep Space", "◉", 0xFF8EA6FF.toInt(), R.drawable.widget_bg_05),
        WidgetPreset(5, "Mint Signal", "⌁", 0xFF85E6C2.toInt(), R.drawable.widget_bg_06),
        WidgetPreset(6, "Ember", "✹", 0xFFFF987F.toInt(), R.drawable.widget_bg_07),
        WidgetPreset(7, "Blue Hour", "⋆", 0xFF89BFFF.toInt(), R.drawable.widget_bg_08),
        WidgetPreset(8, "Lavender Haze", "❋", 0xFFC4A7FF.toInt(), R.drawable.widget_bg_09),
        WidgetPreset(9, "Solar Flare", "☼", 0xFFFFCE84.toInt(), R.drawable.widget_bg_10),
        WidgetPreset(10, "Glacier", "❄", 0xFF8CE7E9.toInt(), R.drawable.widget_bg_11),
        WidgetPreset(11, "Rose Quartz", "◇", 0xFFFFA4B8.toInt(), R.drawable.widget_bg_12),
        WidgetPreset(12, "Cyber Forest", "⌬", 0xFFA4E688.toInt(), R.drawable.widget_bg_13),
        WidgetPreset(13, "Violet Rain", "⟡", 0xFFBE9CFF.toInt(), R.drawable.widget_bg_14),
        WidgetPreset(14, "Quiet Tide", "≈", 0xFF7FD7D0.toInt(), R.drawable.widget_bg_15),
        WidgetPreset(15, "Starforge", "✧", 0xFFFFB978.toInt(), R.drawable.widget_bg_16),
        WidgetPreset(16, "Indigo Pulse", "◎", 0xFF9E9EFF.toInt(), R.drawable.widget_bg_17),
        WidgetPreset(17, "Cloud Study", "☁", 0xFFC2C7E7.toInt(), R.drawable.widget_bg_18),
        WidgetPreset(18, "Crimson Night", "✴", 0xFFFF8295.toInt(), R.drawable.widget_bg_19),
        WidgetPreset(19, "Zero Gravity", "∞", 0xFF9ED7FF.toInt(), R.drawable.widget_bg_20)
    )
    fun get(id: Int) = all.getOrElse(id) { all.first() }
}
