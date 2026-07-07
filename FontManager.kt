package com.example.keyboard

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.io.FileOutputStream

object FontManager {
    private const val FONTS_DIR = "custom_fonts"

    fun getFontsDir(context: Context): File {
        val dir = File(context.filesDir, FONTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Lists all uploaded custom font files.
     */
    fun listCustomFonts(context: Context): List<File> {
        val dir = getFontsDir(context)
        return dir.listFiles { _, name ->
            name.endsWith(".ttf", ignoreCase = true) || name.endsWith(".otf", ignoreCase = true)
        }?.toList() ?: emptyList()
    }

    /**
     * Copies a font file from the device URI into the app's internal custom_fonts directory.
     */
    fun installFont(context: Context, uri: Uri, originalFileName: String): Boolean {
        try {
            val dir = getFontsDir(context)
            // Make sure the file name is safe and has standard extension
            val extension = if (originalFileName.endsWith(".otf", ignoreCase = true)) ".otf" else ".ttf"
            val cleanName = originalFileName.substringBeforeLast(".")
                .replace("[^a-zA-Z0-9_-]".toRegex(), "_") + extension
            
            val destFile = File(dir, cleanName)
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("FontManager", "Failed to copy font", e)
            return false
        }
    }

    /**
     * Deletes a custom font.
     */
    fun deleteFont(fontFile: File): Boolean {
        return if (fontFile.exists()) {
            fontFile.delete()
        } else {
            false
        }
    }

    /**
     * Generates a Jetpack Compose FontFamily from a local font File.
     */
    fun getFontFamily(fontFile: File?): FontFamily {
        if (fontFile == null || !fontFile.exists()) {
            return FontFamily.Default
        }
        return try {
            FontFamily(Font(fontFile))
        } catch (e: Exception) {
            Log.e("FontManager", "Error loading font ${fontFile.name}, using default", e)
            FontFamily.Default
        }
    }

    /**
     * Formats file name to clean display name (e.g., "my_font_bold.ttf" -> "My Font Bold")
     */
    fun getDisplayName(file: File): String {
        val base = file.nameWithoutExtension
        return base.split("_", "-")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
