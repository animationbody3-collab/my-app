package com.example.keyboard

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object TranslationHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    val SUPPORTED_LANGUAGES = listOf(
        Language("auto", "تلقائي (Auto)"),
        Language("ar", "العربية (Arabic)"),
        Language("en", "الإنجليزية (English)"),
        Language("fr", "الفرنسية (French)"),
        Language("es", "الإسبانية (Spanish)"),
        Language("tr", "التركية (Turkish)"),
        Language("de", "الألمانية (German)"),
        Language("ru", "الروسية (Russian)"),
        Language("id", "الإندونيسية (Indonesian)"),
        Language("ur", "الأردية (Urdu)"),
        Language("hi", "الهندية (Hindi)")
    )

    data class Language(val code: String, val displayName: String)

    /**
     * Translates text using the high-performance Google Translate web API.
     */
    suspend fun translate(
        text: String,
        sourceLang: String = "auto",
        targetLang: String = "ar"
    ): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""
        try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encodedText"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "خطأ في الترجمة: استجابة غير صالحة من خادم جوجل"
                }
                val bodyString = response.body?.string() ?: return@withContext "خطأ: استجابة فارغة"
                return@withContext parseTranslationResponse(bodyString)
            }
        } catch (e: Exception) {
            Log.e("TranslationHelper", "Translation failed", e)
            return@withContext "خطأ في الترجمة: تحقق من الاتصال بالإنترنت"
        }
    }

    /**
     * Helper to parse Google Translate nested array JSON using Android's JSONArray.
     */
    private fun parseTranslationResponse(responseBody: String): String {
        return try {
            val jsonArray = JSONArray(responseBody)
            val sentencesArray = jsonArray.optJSONArray(0) ?: return ""
            val sb = StringBuilder()
            for (i in 0 until sentencesArray.length()) {
                val sentence = sentencesArray.optJSONArray(i)
                if (sentence != null) {
                    val translatedSegment = sentence.optString(0)
                    if (translatedSegment != "null" && translatedSegment.isNotEmpty()) {
                        sb.append(translatedSegment)
                    }
                }
            }
            sb.toString()
        } catch (e: Exception) {
            Log.e("TranslationHelper", "Failed to parse translation response", e)
            "خطأ في معالجة الترجمة"
        }
    }

    /**
     * Simple check if text contains English/Latin characters to activate auto-detection
     */
    fun hasNonArabicCharacters(text: String): Boolean {
        for (char in text) {
            // Check for Latin/English alphabets
            if (char in 'a'..'z' || char in 'A'..'Z') {
                return true
            }
        }
        return false
    }
}
