package com.example.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClipboardItem
import com.example.data.ClipboardRepository
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardLayout(
    service: MegaKeyboardService,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onPasteItem: (ClipboardItem) -> Unit
) {
    val theme by service.activeTheme
    val customFont by service.customFontFile
    val textStyle by service.activeTextStyle
    val language by service.currentLanguage
    val shifted by service.isShifted
    val clipboardMode by service.isClipboardMode
    val stylizerMode by service.isStylizerMode
    val history by service.clipboardHistory

    // Load custom font family
    val fontFamily = remember(customFont) {
        if (customFont != null) FontManager.getFontFamily(customFont) else FontFamily.Default
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
            .background(theme.backgroundColor)
            .padding(bottom = 8.dp)
    ) {
        // --- TOOLBAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(theme.backgroundColor.copy(alpha = 0.9f))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left toolbar controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Settings Button (Launches MainActivity)
                IconButton(
                    onClick = {
                        val pm = context.packageManager
                        val intent = pm.getLaunchIntentForPackage(context.packageName)
                        if (intent != null) {
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = theme.accentColor
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Language/Layout Indicator Toggle
                TextButton(
                    onClick = { service.toggleLanguage() },
                    colors = ButtonDefaults.textButtonColors(contentColor = theme.keyTextColor)
                ) {
                    Text(
                        text = when (language) {
                            "en" -> "EN 🌐"
                            "ar" -> "عربي 🌐"
                            else -> "SYM 🌐"
                        },
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = theme.accentColor
                    )
                }
            }

            // Middle state indicator
            Text(
                text = if (textStyle != UnicodeStylizer.TextStyle.NORMAL) {
                    textStyle.displayName.split(" ").first()
                } else {
                    FontManager.getDisplayName(customFont ?: File("default")).take(12)
                },
                fontFamily = fontFamily,
                fontSize = 11.sp,
                color = theme.labelColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Right toolbar controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Translator Toggle
                IconButton(
                    onClick = {
                        service.isTranslationMode.value = !service.isTranslationMode.value
                        service.isClipboardMode.value = false
                        service.isStylizerMode.value = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Translation",
                        tint = if (service.isTranslationMode.value) theme.accentColor else theme.labelColor
                    )
                }

                // Discord Auto Translation Toggle
                IconButton(
                    onClick = {
                        service.isDiscordMode.value = !service.isDiscordMode.value
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Discord Mode",
                        tint = if (service.isDiscordMode.value) Color(0xFF5865F2) else theme.labelColor
                    )
                }

                // Font Stylizer Toggle
                IconButton(
                    onClick = {
                        service.isStylizerMode.value = !service.isStylizerMode.value
                        service.isClipboardMode.value = false
                        service.isTranslationMode.value = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FontDownload,
                        contentDescription = "Fonts",
                        tint = if (stylizerMode) theme.accentColor else theme.labelColor
                    )
                }

                // Clipboard History Drawer Toggle
                IconButton(
                    onClick = {
                        service.isClipboardMode.value = !service.isClipboardMode.value
                        service.isStylizerMode.value = false
                        service.isTranslationMode.value = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Clipboard",
                        tint = if (clipboardMode) theme.accentColor else theme.labelColor
                    )
                }
            }
        }

        Divider(color = theme.keyTextColor.copy(alpha = 0.12f), thickness = 1.dp)

        // Discord Auto-Translate Real-Time Popup Banner
        if (service.isDiscordMode.value && service.discordDetectedText.value.isNotEmpty()) {
            DiscordBanner(
                service = service,
                theme = theme,
                fontFamily = fontFamily
            )
        }

        // --- DYNAMIC CONTENT AREA ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            when {
                clipboardMode -> {
                    ClipboardHistoryPanel(
                        service = service,
                        theme = theme,
                        fontFamily = fontFamily,
                        history = history,
                        onPasteItem = onPasteItem
                    )
                }
                stylizerMode -> {
                    StylizerPanel(
                        service = service,
                        theme = theme,
                        fontFamily = fontFamily
                    )
                }
                service.isTranslationMode.value -> {
                    TranslationPanel(
                        service = service,
                        theme = theme,
                        fontFamily = fontFamily
                    )
                }
                else -> {
                    when (language) {
                        "en" -> EnglishKeyboardLayout(
                            theme = theme,
                            fontFamily = fontFamily,
                            shifted = shifted,
                            onKeyClick = onKeyClick,
                            onBackspace = onBackspace,
                            onSpace = onSpace,
                            onEnter = onEnter,
                            onShiftToggle = { service.toggleShift() }
                        )
                        "ar" -> ArabicKeyboardLayout(
                            theme = theme,
                            fontFamily = fontFamily,
                            onKeyClick = onKeyClick,
                            onBackspace = onBackspace,
                            onSpace = onSpace,
                            onEnter = onEnter
                        )
                        else -> SymbolsKeyboardLayout(
                            theme = theme,
                            fontFamily = fontFamily,
                            onKeyClick = onKeyClick,
                            onBackspace = onBackspace,
                            onSpace = onSpace,
                            onEnter = onEnter
                        )
                    }
                }
            }
        }
    }
}

// --- ENGLISH KEYBOARD LAYOUT ---
@Composable
fun EnglishKeyboardLayout(
    theme: KeyboardTheme,
    fontFamily: FontFamily,
    shifted: Boolean,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onShiftToggle: () -> Unit
) {
    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Row 1
        Row(modifier = Modifier.fillMaxWidth()) {
            row1.forEach { key ->
                val letter = if (shifted) key.uppercase() else key
                KeyButton(letter, theme, fontFamily, Modifier.weight(1f)) { onKeyClick(letter) }
            }
        }
        // Row 2
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
            row2.forEach { key ->
                val letter = if (shifted) key.uppercase() else key
                KeyButton(letter, theme, fontFamily, Modifier.weight(1f)) { onKeyClick(letter) }
            }
        }
        // Row 3
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Shift Key
            KeyButton(
                text = "⬆",
                theme = theme,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.5f),
                bgColor = if (shifted) theme.accentColor else theme.keyBackgroundColor,
                textColor = if (shifted) theme.backgroundColor else theme.keyTextColor
            ) {
                onShiftToggle()
            }

            row3.forEach { key ->
                val letter = if (shifted) key.uppercase() else key
                KeyButton(letter, theme, fontFamily, Modifier.weight(1f)) { onKeyClick(letter) }
            }

            // Backspace Key
            KeyButton(
                text = "⌫",
                theme = theme,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.5f),
                bgColor = theme.keyBackgroundColor.copy(alpha = 0.7f)
            ) {
                onBackspace()
            }
        }
        // Row 4
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KeyButton(",", theme, fontFamily, Modifier.weight(1.2f)) { onKeyClick(",") }
            KeyButton(" ", theme, fontFamily, Modifier.weight(3.5f)) { onSpace() }
            KeyButton(".", theme, fontFamily, Modifier.weight(1.2f)) { onKeyClick(".") }
            KeyButton(
                text = "⏎",
                theme = theme,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.8f),
                bgColor = theme.accentColor,
                textColor = theme.backgroundColor
            ) {
                onEnter()
            }
        }
    }
}

// --- ARABIC KEYBOARD LAYOUT ---
@Composable
fun ArabicKeyboardLayout(
    theme: KeyboardTheme,
    fontFamily: FontFamily,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit
) {
    val row1 = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د")
    val row2 = listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط")
    val row3 = listOf("ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ", "ذ")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Row 1
        Row(modifier = Modifier.fillMaxWidth()) {
            row1.forEach { key ->
                KeyButton(key, theme, fontFamily, Modifier.weight(1f)) { onKeyClick(key) }
            }
        }
        // Row 2
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            row2.forEach { key ->
                KeyButton(key, theme, fontFamily, Modifier.weight(1f)) { onKeyClick(key) }
            }
        }
        // Row 3
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Backspace on Left for Arabic
            KeyButton(
                text = "⌫",
                theme = theme,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.5f),
                bgColor = theme.keyBackgroundColor.copy(alpha = 0.7f)
            ) {
                onBackspace()
            }

            row3.forEach { key ->
                KeyButton(key, theme, fontFamily, Modifier.weight(1f)) { onKeyClick(key) }
            }
        }
        // Row 4
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KeyButton("،", theme, fontFamily, Modifier.weight(1.2f)) { onKeyClick("،") }
            KeyButton(" ", theme, fontFamily, Modifier.weight(4f)) { onSpace() }
            KeyButton("؟", theme, fontFamily, Modifier.weight(1.2f)) { onKeyClick("؟") }
            KeyButton(
                text = "⏎",
                theme = theme,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.8f),
                bgColor = theme.accentColor,
                textColor = theme.backgroundColor
            ) {
                onEnter()
            }
        }
    }
}

// --- SYMBOLS KEYBOARD LAYOUT ---
@Composable
fun SymbolsKeyboardLayout(
    theme: KeyboardTheme,
    fontFamily: FontFamily,
    onKeyClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit
) {
    val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row2 = listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")")
    val row3 = listOf("!", "\"", "'", ":", ";", "/", "?", "\\")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Row 1
        Row(modifier = Modifier.fillMaxWidth()) {
            row1.forEach { key ->
                KeyButton(key, theme, fontFamily, Modifier.weight(1f)) { onKeyClick(key) }
            }
        }
        // Row 2
        Row(modifier = Modifier.fillMaxWidth()) {
            row2.forEach { key ->
                KeyButton(key, theme, fontFamily, Modifier.weight(1f)) { onKeyClick(key) }
            }
        }
        // Row 3
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.weight(0.5f))
            row3.forEach { key ->
                KeyButton(key, theme, fontFamily, Modifier.weight(1f)) { onKeyClick(key) }
            }
            // Backspace Key
            KeyButton(
                text = "⌫",
                theme = theme,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.5f),
                bgColor = theme.keyBackgroundColor.copy(alpha = 0.7f)
            ) {
                onBackspace()
            }
        }
        // Row 4
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KeyButton("[", theme, fontFamily, Modifier.weight(1.2f)) { onKeyClick("[") }
            KeyButton(" ", theme, fontFamily, Modifier.weight(3.5f)) { onSpace() }
            KeyButton("]", theme, fontFamily, Modifier.weight(1.2f)) { onKeyClick("]") }
            KeyButton(
                text = "⏎",
                theme = theme,
                fontFamily = fontFamily,
                modifier = Modifier.weight(1.8f),
                bgColor = theme.accentColor,
                textColor = theme.backgroundColor
            ) {
                onEnter()
            }
        }
    }
}

// --- BASIC KEY INDIVIDUAL COMPONENT ---
@Composable
fun RowScope.KeyButton(
    text: String,
    theme: KeyboardTheme,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    bgColor: Color = theme.keyBackgroundColor,
    textColor: Color = theme.keyTextColor,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = fontFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

// --- CLIPBOARD HISTORY PANEL (THE MASSIVE CLIPBOARD DRAWER) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardHistoryPanel(
    service: MegaKeyboardService,
    theme: KeyboardTheme,
    fontFamily: FontFamily,
    history: List<ClipboardItem>,
    onPasteItem: (ClipboardItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    
    // Local filter based on search query
    val filteredHistory = remember(history, searchQuery) {
        if (searchQuery.isBlank()) {
            history
        } else {
            history.filter { it.text.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
    ) {
        // Search bar inside keyboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(theme.keyBackgroundColor.copy(alpha = 0.5f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search icon",
                tint = theme.accentColor,
                modifier = Modifier.padding(start = 8.dp)
            )
            
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { 
                    Text(
                        "ابحث في الحافظة (حتى 5000+ نسخ)...", 
                        fontSize = 11.sp, 
                        color = theme.labelColor.copy(alpha = 0.6f),
                        fontFamily = fontFamily
                    ) 
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = theme.keyTextColor,
                    unfocusedTextColor = theme.keyTextColor
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = fontFamily),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = theme.labelColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        if (filteredHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "No items",
                        tint = theme.labelColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "لا توجد نتائج بحث" else "الحافظة فارغة حالياً. أي شيء تنسخه سيحفظ هنا!",
                        color = theme.labelColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontFamily = fontFamily,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredHistory, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPasteItem(item) },
                        colors = CardDefaults.cardColors(
                            containerColor = theme.keyBackgroundColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.text,
                                    fontFamily = fontFamily,
                                    fontSize = 13.sp,
                                    color = theme.keyTextColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${item.wordCount} كلمة | ${item.charCount} حرف",
                                        fontFamily = fontFamily,
                                        fontSize = 10.sp,
                                        color = theme.labelColor.copy(alpha = 0.7f)
                                    )
                                    if (item.isLargeTextStoredInFile) {
                                        Text(
                                            text = "💾 [نص عملاق]",
                                            fontFamily = fontFamily,
                                            fontSize = 9.sp,
                                            color = theme.accentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Star Favorite
                            IconButton(
                                onClick = {
                                    val repo = ClipboardRepository(service.applicationContext)
                                    scope.launch {
                                        repo.toggleFavorite(item)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (item.isFavorite) Color.Red else theme.labelColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Delete copy
                            IconButton(
                                onClick = {
                                    val repo = ClipboardRepository(service.applicationContext)
                                    scope.launch {
                                        repo.deleteItem(item)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = theme.labelColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- STYLIZER PANEL (THE UNICODE STYLED TEXT SELECTOR) ---
@Composable
fun StylizerPanel(
    service: MegaKeyboardService,
    theme: KeyboardTheme,
    fontFamily: FontFamily
) {
    val activeStyle by service.activeTextStyle
    val context = LocalContext.current

    val sampleText = "Hello World"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
    ) {
        Text(
            text = "اختر نمط خط الكتابة التلقائي (Unicode Stylizer):",
            fontFamily = fontFamily,
            fontSize = 12.sp,
            color = theme.accentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            textAlign = TextAlign.Right
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(UnicodeStylizer.TextStyle.values()) { style ->
                val isSelected = style == activeStyle
                val stylizedSample = UnicodeStylizer.stylizeText(sampleText, style)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            service.activeTextStyle.value = style
                            KeyboardTheme.saveTextStyle(context, style)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) theme.accentColor else theme.keyBackgroundColor
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stylizedSample,
                            fontFamily = fontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) theme.backgroundColor else theme.keyTextColor
                        )

                        Text(
                            text = style.displayName,
                            fontFamily = fontFamily,
                            fontSize = 12.sp,
                            color = if (isSelected) theme.backgroundColor.copy(alpha = 0.8f) else theme.labelColor
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationPanel(
    service: MegaKeyboardService,
    theme: KeyboardTheme,
    fontFamily: FontFamily
) {
    var input by service.translationInput
    var result by service.translationResult
    var isTranslating by service.isTranslating
    val sourceLang by service.sourceLang
    var targetLang by service.targetLang
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(4.dp)
    ) {
        // Source and target indicator + quick target languages row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "الترجمة الذكية (Google Translate)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                color = theme.accentColor
            )

            // Select Target Language Indicator
            Text(
                text = "إلى: ${TranslationHelper.SUPPORTED_LANGUAGES.find { it.code == targetLang }?.displayName ?: targetLang}",
                fontSize = 10.sp,
                fontFamily = fontFamily,
                color = theme.labelColor
            )
        }

        // Horizontal list of target languages
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            // Filter auto out of targets
            val targetLanguages = TranslationHelper.SUPPORTED_LANGUAGES.filter { it.code != "auto" }
            items(targetLanguages) { lang ->
                val isSelected = targetLang == lang.code
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) theme.accentColor else theme.keyBackgroundColor)
                        .clickable { targetLang = lang.code }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lang.displayName.split(" ").first(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        color = if (isSelected) theme.backgroundColor else theme.keyTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Input and Output Card
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = theme.keyBackgroundColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                // Input TextField
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        Text(
                            "اكتب هنا للترجمة التلقائية...",
                            fontSize = 12.sp,
                            fontFamily = fontFamily,
                            color = theme.labelColor.copy(alpha = 0.6f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = theme.keyTextColor,
                        unfocusedTextColor = theme.keyTextColor
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = fontFamily),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )

                if (result.isNotEmpty()) {
                    Divider(color = theme.keyTextColor.copy(alpha = 0.1f), thickness = 1.dp)
                    Text(
                        text = result,
                        fontSize = 12.sp,
                        fontFamily = fontFamily,
                        color = theme.accentColor,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Translate Button
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        scope.launch {
                            isTranslating = true
                            result = TranslationHelper.translate(input, sourceLang, targetLang)
                            isTranslating = false
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = theme.backgroundColor)
                } else {
                    Text("ترجم 🌐", fontFamily = fontFamily, fontSize = 12.sp, color = theme.backgroundColor)
                }
            }

            // Write Button (Inserts translation directly to screen)
            Button(
                onClick = {
                    val textToInsert = if (result.isNotEmpty()) result else input
                    if (textToInsert.isNotEmpty()) {
                        val ic = service.currentInputConnection
                        ic?.commitText(textToInsert, 1)
                    }
                },
                modifier = Modifier.weight(1f).height(38.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.keyTextColor.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إدراج النص ✍️", fontFamily = fontFamily, fontSize = 12.sp, color = theme.keyTextColor)
            }

            // Clear Button
            IconButton(
                onClick = {
                    input = ""
                    result = ""
                },
                modifier = Modifier.height(38.dp).width(38.dp).background(theme.keyBackgroundColor, RoundedCornerShape(8.dp))
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = theme.labelColor)
            }
        }
    }
}

@Composable
fun DiscordBanner(
    service: MegaKeyboardService,
    theme: KeyboardTheme,
    fontFamily: FontFamily
) {
    val detected by service.discordDetectedText
    val translated by service.discordTranslatedText
    val isTranslating by service.isTranslating
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .heightIn(max = 90.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF5865F2).copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5865F2)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color.Green)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "كاشف ديسكورد التلقائي نشط دائمًا 🤖",
                        fontSize = 10.sp,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5865F2)
                    )
                }

                IconButton(
                    onClick = {
                        service.discordDetectedText.value = ""
                        service.discordTranslatedText.value = ""
                    },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = theme.labelColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isTranslating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color(0xFF5865F2), strokeWidth = 1.5.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جاري الترجمة الفورية للمحتوى...", fontSize = 10.sp, fontFamily = fontFamily, color = theme.labelColor)
                }
            } else if (detected.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(
                            text = "النص: \"$detected\"",
                            fontSize = 9.sp,
                            fontFamily = fontFamily,
                            color = theme.labelColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "الترجمة: \"$translated\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                            color = theme.keyTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                val ic = service.currentInputConnection
                                ic?.commitText(translated, 1)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFF5865F2), contentColor = Color.White),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("إدراج", fontSize = 9.sp, fontFamily = fontFamily)
                        }

                        TextButton(
                            onClick = {
                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("translation", translated)
                                clipboardManager.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "تم نسخ الترجمة للعربية!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            colors = ButtonDefaults.textButtonColors(containerColor = theme.keyBackgroundColor, contentColor = theme.keyTextColor),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("نسخ", fontSize = 9.sp, fontFamily = fontFamily)
                        }
                    }
                }
            }
        }
    }
}
