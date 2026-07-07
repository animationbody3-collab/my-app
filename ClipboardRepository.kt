package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ClipboardRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val clipboardDao = database.clipboardDao()

    val allItems: Flow<List<ClipboardItem>> = clipboardDao.getAllItemsFlow()
    val favoriteItems: Flow<List<ClipboardItem>> = clipboardDao.getFavoriteItemsFlow()

    fun searchItems(query: String): Flow<List<ClipboardItem>> {
        return clipboardDao.searchItemsFlow("%$query%")
    }

    /**
     * Safely inserts a clipboard item, handling extremely large texts
     * by writing them to a file if they exceed a size threshold.
     */
    suspend fun addClipboardItem(rawText: String) = withContext(Dispatchers.IO) {
        if (rawText.isBlank()) return@withContext

        // Calculate counts
        val charCount = rawText.length
        val wordCount = rawText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size

        val threshold = 100_000 // 100k characters threshold for file storage
        val item = if (charCount > threshold) {
            try {
                val largeClipsDir = File(context.filesDir, "large_clips")
                if (!largeClipsDir.exists()) {
                    largeClipsDir.mkdirs()
                }
                val fileName = "clip_${UUID.randomUUID()}_${System.currentTimeMillis()}.txt"
                val file = File(largeClipsDir, fileName)
                file.writeText(rawText)

                // Save only a preview of 5,000 characters in Room to prevent DB lag
                val previewText = rawText.take(5000) + "\n\n...[تم حفظ النص كاملاً كملف كبير]..."
                ClipboardItem(
                    text = previewText,
                    wordCount = wordCount,
                    charCount = charCount,
                    isLargeTextStoredInFile = true,
                    filePath = file.absolutePath
                )
            } catch (e: Exception) {
                Log.e("ClipboardRepository", "Failed to write large clipboard to file, saving to DB directly", e)
                ClipboardItem(
                    text = rawText,
                    wordCount = wordCount,
                    charCount = charCount
                )
            }
        } else {
            ClipboardItem(
                text = rawText,
                wordCount = wordCount,
                charCount = charCount
            )
        }

        // Save to Database
        clipboardDao.insertItem(item)

        // Enforce the 5000 clipboard items limit
        pruneDatabaseIfNeeded()
    }

    /**
     * Retrieves the complete text of an item, reading from file if necessary.
     */
    suspend fun getFullText(item: ClipboardItem): String = withContext(Dispatchers.IO) {
        if (item.isLargeTextStoredInFile && item.filePath != null) {
            try {
                val file = File(item.filePath)
                if (file.exists()) {
                    return@withContext file.readText()
                }
            } catch (e: Exception) {
                Log.e("ClipboardRepository", "Failed to read large text from file", e)
            }
        }
        return@withContext item.text
    }

    suspend fun deleteItem(item: ClipboardItem) = withContext(Dispatchers.IO) {
        // Delete large file if exists
        if (item.isLargeTextStoredInFile && item.filePath != null) {
            try {
                val file = File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e("ClipboardRepository", "Failed to delete file for item", e)
            }
        }
        clipboardDao.deleteItem(item)
    }

    suspend fun toggleFavorite(item: ClipboardItem) = withContext(Dispatchers.IO) {
        clipboardDao.updateItem(item.copy(isFavorite = !item.isFavorite))
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        // Clear all files in large_clips
        try {
            val largeClipsDir = File(context.filesDir, "large_clips")
            if (largeClipsDir.exists()) {
                largeClipsDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e("ClipboardRepository", "Failed to clear large clips folder", e)
        }
        clipboardDao.clearAll()
    }

    /**
     * Shows a warning system notification when the clipboard count exceeds 2000.
     */
    private fun showLimitNotification() {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "clipboard_warnings"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "تنبيهات الحافظة",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "تنبيهات الحافظة وتجاوز السعة المحددة"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setContentTitle("تنبيه الحافظة المتقدمة ⚠️")
                .setContentText("لقد تجاوزت حد الـ 2000 عنصر المحفوظ! تم بدء الحذف التلقائي للمحفوظات القديمة غير المثبتة.")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
        } catch (e: Exception) {
            Log.e("ClipboardRepository", "Failed to show notification", e)
        }
    }

    /**
     * Keeps database size under 2000 items. Deletes old files.
     * Pinned items (isFavorite = true) are protected.
     */
    private suspend fun pruneDatabaseIfNeeded() {
        val count = clipboardDao.getItemCount()
        val maxLimit = 2000
        if (count > maxLimit) {
            // Trigger warning notification
            showLimitNotification()

            val toDeleteCount = count - maxLimit
            val oldestNonFavorites = clipboardDao.getOldestNonFavoriteItems(toDeleteCount)
            for (item in oldestNonFavorites) {
                // Delete file if associated
                if (item.isLargeTextStoredInFile && item.filePath != null) {
                    try {
                        val file = File(item.filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        Log.e("ClipboardRepository", "Failed to delete file during pruning", e)
                    }
                }
                clipboardDao.deleteItem(item)
            }
        }
    }
}
