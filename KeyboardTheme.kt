package com.example.keyboard

import android.content.Context
import androidx.compose.ui.graphics.Color

enum class KeyboardTheme(
    val id: String,
    val displayName: String,
    val backgroundColor: Color,
    val keyBackgroundColor: Color,
    val keyTextColor: Color,
    val accentColor: Color,
    val labelColor: Color
) {
    NEON_NIGHT(
        "neon_night",
        "ليل النيون (Neon Night)",
        Color(0xFF0F0B1E), // Deep Violet background
        Color(0xFF1E1535), // Dark purple key
        Color(0xFF00FFFF), // Electric Cyan text
        Color(0xFFFF007F), // Neon Pink accent
        Color(0xFFB0A4E3)
    ),
    COSMIC_DARK(
        "cosmic_dark",
        "الكون المظلم (Cosmic Dark)",
        Color(0xFF0D1117), // Deep space blue black
        Color(0xFF161B22), // Translucent steel
        Color(0xFFC9D1D9), // Soft white/grey
        Color(0xFF58A6FF), // Blue accent
        Color(0xFF8B949E)
    ),
    SUNSET_PINK(
        "sunset_pink",
        "غروب الورد (Sunset Pink)",
        Color(0xFF2D142C), // Deep plum
        Color(0xFF510A32), // Dark wine
        Color(0xFFEE4540), // Warm coral
        Color(0xFFEE4540), // Peach
        Color(0xFFC72C41)
    ),
    EMERALD_GOLD(
        "emerald_gold",
        "الزمرد والذهب (Emerald Gold)",
        Color(0xFF0B2415), // Deep dark green
        Color(0xFF143F24), // Emerald
        Color(0xFFFFD700), // Pure Gold text
        Color(0xFFE5A93C), // Accent gold
        Color(0xFFA1CCA5)
    ),
    CYBERPUNK(
        "cyberpunk",
        "سايبربانك (Cyberpunk)",
        Color(0xFF000000), // Absolute black
        Color(0xFFFCEE09), // Cyberpunk Neon Yellow
        Color(0xFF000000), // Black text on yellow keys
        Color(0xFF00F0FF), // Neon Blue accent
        Color(0xFFFF003C) // Neon Red/Pink
    ),
    COZY_SLATE(
        "cozy_slate",
        "سليت كلاسيك (Cozy Slate)",
        Color(0xFF2F3E46), // Muted dark slate
        Color(0xFF354F52), // Lighter slate
        Color(0xFFCAD2C5), // Off-white key text
        Color(0xFF84A98C), // Sage green accent
        Color(0xFF52796F)
    ),
    CLEAN_LIGHT(
        "clean_light",
        "فاتح ناصع (Clean Light)",
        Color(0xFFF8F9FA), // Clean bright white
        Color(0xFFE9ECEF), // Soft light grey
        Color(0xFF212529), // Charcoal black text
        Color(0xFF0D6EFD), // Royal blue accent
        Color(0xFF6C757D)
    );

    companion object {
        private const val PREFS_NAME = "mega_keyboard_prefs"
        private const val KEY_THEME = "keyboard_theme"
        private const val KEY_FONT_PATH = "custom_font_path"
        private const val KEY_TEXT_STYLE = "selected_text_style"

        fun getSavedTheme(context: Context): KeyboardTheme {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val themeId = prefs.getString(KEY_THEME, NEON_NIGHT.id) ?: NEON_NIGHT.id
            return values().firstOrNull { it.id == themeId } ?: NEON_NIGHT
        }

        fun saveTheme(context: Context, theme: KeyboardTheme) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_THEME, theme.id).apply()
        }

        fun getSavedFontPath(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_FONT_PATH, null)
        }

        fun saveFontPath(context: Context, path: String?) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FONT_PATH, path).apply()
        }

        fun getSavedTextStyle(context: Context): UnicodeStylizer.TextStyle {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val styleName = prefs.getString(KEY_TEXT_STYLE, UnicodeStylizer.TextStyle.NORMAL.name)
            return try {
                UnicodeStylizer.TextStyle.valueOf(styleName ?: UnicodeStylizer.TextStyle.NORMAL.name)
            } catch (e: Exception) {
                UnicodeStylizer.TextStyle.NORMAL
            }
        }

        fun saveTextStyle(context: Context, style: UnicodeStylizer.TextStyle) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_TEXT_STYLE, style.name).apply()
        }
    }
}
