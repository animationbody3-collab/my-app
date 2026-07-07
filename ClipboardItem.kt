package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String, // Stored preview or full text if small
    val timestamp: Long = System.currentTimeMillis(),
    val wordCount: Int,
    val charCount: Int,
    val isFavorite: Boolean = false,
    val isLargeTextStoredInFile: Boolean = false,
    val filePath: String? = null
)
