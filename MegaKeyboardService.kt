package com.example.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.*
import com.example.data.ClipboardItem
import com.example.data.ClipboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class MegaKeyboardService : InputMethodService(), 
    LifecycleOwner, 
    ViewModelStoreOwner, 
    SavedStateRegistryOwner {

    // Lifecycle, SavedState, and ViewModel registries to support ComposeView inside a Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var repository: ClipboardRepository
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // Observable states inside our Service for Jetpack Compose UI
    val activeTheme = mutableStateOf(KeyboardTheme.NEON_NIGHT)
    val customFontFile = mutableStateOf<File?>(null)
    val activeTextStyle = mutableStateOf(UnicodeStylizer.TextStyle.NORMAL)
    val clipboardHistory = mutableStateOf<List<ClipboardItem>>(emptyList())
    
    val currentLanguage = mutableStateOf("en") // "en", "ar", "sym"
    val isShifted = mutableStateOf(false)
    val isClipboardMode = mutableStateOf(false)
    val isStylizerMode = mutableStateOf(false)

    // Translation & Discord Auto-Detect states
    val isTranslationMode = mutableStateOf(false)
    val isDiscordMode = mutableStateOf(false)
    val sourceLang = mutableStateOf("auto")
    val targetLang = mutableStateOf("ar")
    val translationInput = mutableStateOf("")
    val translationResult = mutableStateOf("")
    val discordDetectedText = mutableStateOf("")
    val discordTranslatedText = mutableStateOf("")
    val isTranslating = mutableStateOf(false)

    override fun onCreate() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        savedStateRegistryController.performRestore(null)
        super.onCreate()

        repository = ClipboardRepository(applicationContext)
        loadSettings()

        // 1. Observe clipboard history flow from database
        serviceScope.launch {
            repository.allItems.collectLatest { list ->
                clipboardHistory.value = list
            }
        }

        // 2. Set up system clipboard change listener (as an IME, we have special access)
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val clipText = clip.getItemAt(0).text?.toString()
                if (!clipText.isNullOrBlank()) {
                    serviceScope.launch(Dispatchers.IO) {
                        // Insert into DB. Automatic prune to 2000 items is handled in repository.
                        repository.addClipboardItem(clipText)

                        // If Discord translation is active and text has non-Arabic chars, auto-translate
                        if (isDiscordMode.value && TranslationHelper.hasNonArabicCharacters(clipText)) {
                            serviceScope.launch {
                                discordDetectedText.value = clipText
                                isTranslating.value = true
                                val result = TranslationHelper.translate(clipText, "auto", "ar")
                                discordTranslatedText.value = result
                                isTranslating.value = false
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        // Load settings freshly in case the user edited them in MainActivity
        loadSettings()

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MegaKeyboardService)
            setViewTreeSavedStateRegistryOwner(this@MegaKeyboardService)
            setViewTreeViewModelStoreOwner(this@MegaKeyboardService)
            setContent {
                KeyboardLayout(
                    service = this@MegaKeyboardService,
                    onKeyClick = { key -> handleKeyClick(key) },
                    onBackspace = { handleBackspace() },
                    onSpace = { handleSpace() },
                    onEnter = { handleEnter() },
                    onPasteItem = { item -> handlePasteClipboardItem(item) }
                )
            }
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return composeView
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceJob.cancel()
        store.clear()
        super.onDestroy()
    }

    fun loadSettings() {
        activeTheme.value = KeyboardTheme.getSavedTheme(this)
        activeTextStyle.value = KeyboardTheme.getSavedTextStyle(this)
        
        val fontPath = KeyboardTheme.getSavedFontPath(this)
        customFontFile.value = if (fontPath != null) File(fontPath) else null
    }

    /**
     * Handles typing individual characters. Automatically applies selected style.
     */
    private fun handleKeyClick(key: String) {
        val ic = currentInputConnection ?: return
        val textToCommit = UnicodeStylizer.stylizeText(key, activeTextStyle.value)
        ic.commitText(textToCommit, 1)
        
        // Disable temporary shift if active
        if (isShifted.value) {
            isShifted.value = false
        }
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        ic.deleteSurroundingText(1, 0)
    }

    private fun handleSpace() {
        val ic = currentInputConnection ?: return
        ic.commitText(" ", 1)
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    /**
     * Handles pasting a clipboard item from history, safely retrieving large texts.
     */
    private fun handlePasteClipboardItem(item: ClipboardItem) {
        serviceScope.launch {
            val fullText = repository.getFullText(item)
            val ic = currentInputConnection ?: return@launch
            ic.commitText(fullText, 1)
        }
    }

    /**
     * Toggles language layouts
     */
    fun toggleLanguage() {
        when (currentLanguage.value) {
            "en" -> currentLanguage.value = "ar"
            "ar" -> currentLanguage.value = "sym"
            else -> currentLanguage.value = "en"
        }
        isClipboardMode.value = false
        isStylizerMode.value = false
    }

    fun toggleShift() {
        isShifted.value = !isShifted.value
    }
}
