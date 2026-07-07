package com.example

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.ClipboardItem
import com.example.data.ClipboardRepository
import com.example.keyboard.FontManager
import com.example.keyboard.KeyboardTheme
import com.example.keyboard.UnicodeStylizer
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var repository: ClipboardRepository
    private val isKeyboardEnabledState = MutableStateFlow(false)
    private val isKeyboardSelectedState = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = ClipboardRepository(applicationContext)

        // Monitor activity lifecycle to refresh keyboard configuration status
        lifecycleScope.launch {
            checkKeyboardStatus()
        }

        setContent {
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF7F2FA) // M3 Lavender-grey background
                ) {
                    MainScreen(
                        repository = repository,
                        isKeyboardEnabledFlow = isKeyboardEnabledState,
                        isKeyboardSelectedFlow = isKeyboardSelectedState,
                        onCheckStatus = { checkKeyboardStatus() },
                        onOpenSettings = {
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        },
                        onOpenSwitcher = {
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkKeyboardStatus()
    }

    private fun checkKeyboardStatus() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledList = imm.enabledInputMethodList
        val isEnabled = enabledList.any { it.packageName == packageName }
        isKeyboardEnabledState.value = isEnabled

        val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val isSelected = currentIme != null && currentIme.contains(packageName)
        isKeyboardSelectedState.value = isSelected
    }
}

@Composable
fun MainScreen(
    repository: ClipboardRepository,
    isKeyboardEnabledFlow: MutableStateFlow<Boolean>,
    isKeyboardSelectedFlow: MutableStateFlow<Boolean>,
    onCheckStatus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSwitcher: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("لوحة التحكم", "الحافظة العملاقة", "الثيمات والخطوط")

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "لوحة المفاتيح الذكية",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4)
                    )
                    Text(
                        text = "الاستخدام الشخصي المتطور",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF49454F)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEADDFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color(0xFF21005D),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.border(width = 1.dp, color = Color(0xFFCAC4D0).copy(alpha = 0.5f))
            ) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Keyboard
                                    1 -> Icons.Default.ContentPaste
                                    else -> Icons.Default.Palette
                                },
                                contentDescription = label
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF21005D),
                            selectedTextColor = Color(0xFF21005D),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F),
                            indicatorColor = Color(0xFFEADDFF)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F2FA))
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> OnboardingTab(
                    isKeyboardEnabledFlow = isKeyboardEnabledFlow,
                    isKeyboardSelectedFlow = isKeyboardSelectedFlow,
                    onCheckStatus = onCheckStatus,
                    onOpenSettings = onOpenSettings,
                    onOpenSwitcher = onOpenSwitcher,
                    onTabChange = { selectedTab = it },
                    repository = repository
                )
                1 -> ClipboardTab(repository = repository)
                2 -> ThemesAndFontsTab(repository = repository)
            }
        }
    }
}

// ================= TAB 1: ONBOARDING & SETUP CONTROL =================
@Composable
fun OnboardingTab(
    isKeyboardEnabledFlow: MutableStateFlow<Boolean>,
    isKeyboardSelectedFlow: MutableStateFlow<Boolean>,
    onCheckStatus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSwitcher: () -> Unit,
    onTabChange: (Int) -> Unit,
    repository: ClipboardRepository
) {
    val isEnabled by isKeyboardEnabledFlow.collectAsStateWithLifecycle()
    val isSelected by isKeyboardSelectedFlow.collectAsStateWithLifecycle()
    val items by repository.allItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val itemsCount = items.size

    var testInputText by remember { mutableStateOf("") }
    var isShowingAddDialogLocal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // --- BENTO GRID DASHBOARD ---
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Bento Card 1: Giant Purple Card (Span 6)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTabChange(1) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF6750A4)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Clipboard",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color.White.copy(alpha = 0.3f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isEnabled && isSelected) "نشط" else "غير مفعّل",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Column {
                            Text(
                                text = "${itemsCount} / 5000+",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                style = LocalTextStyle.current.copy(lineHeight = 44.sp)
                            )
                            Text(
                                text = "عنصر في الحافظة الذكية",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "تم تحسين المحرك لمعالجة نصوص تتجاوز المليار كلمة دون تأخير.",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.75f),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // Row of Bento Cards (Span 3 & Span 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Fonts Card (Left)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable { onTabChange(2) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.FontDownload,
                                contentDescription = "Fonts",
                                tint = Color(0xFF21005D),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "الخطوط",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF21005D)
                                )
                                Text(
                                    text = "رفع خط خارجي (TTF)",
                                    fontSize = 10.sp,
                                    color = Color(0xFF49454F)
                                )
                            }
                        }
                    }

                    // Themes Card (Right)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable { onTabChange(2) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Themes",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "المظاهر",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1B20)
                                )
                                Text(
                                    text = "142 سمة مخصصة",
                                    fontSize = 10.sp,
                                    color = Color(0xFF49454F)
                                )
                            }
                        }
                    }
                }

                // Row of Bento Cards 3 & 4 (Span 4 & Span 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Performance Card (Span 4 - 65% width)
                    Card(
                        modifier = Modifier
                            .weight(0.65f)
                            .height(100.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF7FF)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFE6E0E9))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "وضع المعالجة الفائق",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1B20)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                // Progress Bar
                                LinearProgressIndicator(
                                    progress = { 0.85f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFF6750A4),
                                    trackColor = Color(0xFFE6E0E9)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "جاهز لنسخ 1,000,000,000 كلمة",
                                    fontSize = 9.sp,
                                    color = Color(0xFF49454F)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFD0BCFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Performance",
                                    tint = Color(0xFF21005D),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Add Button Card (Span 2 - 35% width)
                    Card(
                        modifier = Modifier
                            .weight(0.35f)
                            .height(100.dp)
                            .clickable { isShowingAddDialogLocal = true },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEF8)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Add text",
                                    tint = Color(0xFF21005D),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("إضافة سريعة", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF21005D))
                            }
                        }
                    }
                }

                // Security Bento Card (Span 6)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "تشفير وحماية البيانات الشخصية",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                        }
                        
                        // Styled mockup switch
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6750A4))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .align(Alignment.CenterStart)
                            )
                        }
                    }
                }
            }
        }

        // --- ACTIVATION WIZARD CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "خطوات التفعيل والتشغيل 🛠️",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )

                    // Step 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Status",
                            tint = if (isEnabled) Color(0xFF2E7D32) else Color(0xFF49454F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الخطوة 1: تفعيل الكيبورد في الإعدادات", color = Color(0xFF1D1B20), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("قم بتفعيل كيبورد الحافظة العملاقة في إعدادات الإدخال واللغات الخاصة بالنظام.", color = Color(0xFF49454F), fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEnabled) Color(0xFFEADDFF) else Color(0xFF6750A4),
                            contentColor = if (isEnabled) Color(0xFF21005D) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isEnabled) "تم التفعيل بنجاح ✓" else "تفعيل الكيبورد الآن",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE6E0E9), thickness = 1.dp)

                    // Step 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Status",
                            tint = if (isSelected) Color(0xFF2E7D32) else Color(0xFF49454F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الخطوة 2: اختيار الكيبورد كلوحة افتراضية", color = Color(0xFF1D1B20), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("قم بتحويل لوحة المفاتيح النشطة حالياً إلى كيبورد الحافظة العملاقة لتتمكن من استخدامه.", color = Color(0xFF49454F), fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onOpenSwitcher,
                        enabled = isEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFFEADDFF) else Color(0xFF6750A4),
                            contentColor = if (isSelected) Color(0xFF21005D) else Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isSelected) "الكيبورد نشط وافتراضي الآن ✓" else "تبديل إلى كيبورد الحافظة العملاقة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onCheckStatus,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF6750A4))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تحديث حالة التفعيل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // --- KEYBOARD TEST AREA ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF7FF)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFE6E0E9))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "منطقة تجربة الكيبورد والحافظة ✍️",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                    Text(
                        text = "اضغط على مربع النص أدناه لتجربة سرعة الكتابة، نسخ النصوص الطويلة، وتجربة الخطوط والزخارف التلقائية وميزة الحافظة!",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F),
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = testInputText,
                        onValueChange = { testInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("اكتب هنا للتجربة وافتح الكيبورد...", color = Color(0xFF49454F), fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedLabelColor = Color(0xFF6750A4),
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (testInputText.isNotEmpty()) {
                        Button(
                            onClick = { testInputText = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8DEF8), contentColor = Color(0xFF21005D)),
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("مسح النص", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Manual dialog on OnboardingTab
    if (isShowingAddDialogLocal) {
        AlertDialog(
            onDismissRequest = { isShowingAddDialogLocal = false },
            title = { Text("إضافة نص يدوي للحافظة ✍️", color = Color(0xFF1D1B20), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = testInputText,
                    onValueChange = { testInputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    placeholder = { Text("اكتب أو الصق نصاً هنا...", color = Color(0xFF49454F)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedContainerColor = Color(0xFFF7F2FA),
                        unfocusedContainerColor = Color(0xFFF7F2FA),
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (testInputText.isNotBlank()) {
                            scope.launch {
                                repository.addClipboardItem(testInputText)
                                testInputText = ""
                                isShowingAddDialogLocal = false
                                Toast.makeText(context, "تم الحفظ بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حفظ وتخزين", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isShowingAddDialogLocal = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB3261E))
                ) {
                    Text("إلغاء")
                }
            },
            containerColor = Color.White
        )
    }
}

// ================= TAB 2: MEGA CLIPBOARD MANAGER =================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardTab(repository: ClipboardRepository) {
    val items by repository.allItems.collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { it.text.contains(searchQuery, ignoreCase = true) }
        }
    }

    var manualTextEntry by remember { mutableStateOf("") }
    var isShowingAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab Header & Add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("الحافظة العملاقة 📋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                Text("تخزين حتى 5000 نسخة مع حماية النصوص وحفظها", fontSize = 12.sp, color = Color(0xFF49454F))
            }

            FloatingActionButton(
                onClick = { isShowingAddDialog = true },
                containerColor = Color(0xFFEADDFF),
                contentColor = Color(0xFF21005D),
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add manually")
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("بحث في الحافظة...", color = Color(0xFF49454F)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF6750A4)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF49454F))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6750A4),
                unfocusedBorderColor = Color(0xFFCAC4D0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color(0xFF1D1B20),
                unfocusedTextColor = Color(0xFF1D1B20)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Clear All Stats card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "عدد العناصر المحفوظة: ${filteredItems.size} عنصر",
                color = Color(0xFF6750A4),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            if (items.isNotEmpty()) {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearAll()
                            Toast.makeText(context, "تم تفريغ الحافظة بالكامل!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB3261E))
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Clear all", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تفريغ الحافظة")
                }
            }
        }

        // List display
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = "Empty",
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "لا توجد نتائج مطابقة لبحثك" else "الحافظة فارغة حالياً!\nقم بنسخ أي نصوص طويلة في جهازك، وسنقوم بحفظها هنا تلقائياً حتى 5000 نسخة.",
                        color = Color(0xFF49454F),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (item.isFavorite) Color(0xFF6750A4) else Color(0xFFCAC4D0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Pin",
                                        tint = if (item.isFavorite) Color(0xFFFFB400) else Color(0xFF49454F),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable {
                                                scope.launch { repository.toggleFavorite(item) }
                                            }
                                    )
                                    
                                    Text(
                                        text = "${item.wordCount} كلمة | ${item.charCount} حرف",
                                        fontSize = 11.sp,
                                        color = Color(0xFF49454F)
                                    )

                                    if (item.isLargeTextStoredInFile) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFE8DEF8))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                "💾 نص عملاق (مخزن كملف)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF21005D)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Quick Copy button to system
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val fullText = repository.getFullText(item)
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(android.content.ClipData.newPlainText("MegaCopy", fullText))
                                                Toast.makeText(context, "تم النسخ بنجاح!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF6750A4), modifier = Modifier.size(16.dp))
                                    }

                                    // Delete button
                                    IconButton(
                                        onClick = {
                                            scope.launch { repository.deleteItem(item) }
                                            Toast.makeText(context, "تم حذف المذكرة من الحافظة", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFB3261E), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.text,
                                color = Color(0xFF1D1B20),
                                fontSize = 13.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Add manual text Dialog
    if (isShowingAddDialog) {
        AlertDialog(
            onDismissRequest = { isShowingAddDialog = false },
            title = { Text("إضافة نص يدوي للحافظة ✍️", color = Color(0xFF1D1B20), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = manualTextEntry,
                    onValueChange = { manualTextEntry = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    placeholder = { Text("اكتب أو الصق نصاً هنا...", color = Color(0xFF49454F)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedContainerColor = Color(0xFFF7F2FA),
                        unfocusedContainerColor = Color(0xFFF7F2FA),
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualTextEntry.isNotBlank()) {
                            scope.launch {
                                repository.addClipboardItem(manualTextEntry)
                                manualTextEntry = ""
                                isShowingAddDialog = false
                                Toast.makeText(context, "تم الحفظ بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حفظ وتخزين", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingAddDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB3261E))) {
                    Text("إلغاء")
                }
            },
            containerColor = Color.White
        )
    }
}

// ================= TAB 3: THEMES & FONTS UPLOADER =================
@Composable
fun ThemesAndFontsTab(repository: ClipboardRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTheme by remember { mutableStateOf(KeyboardTheme.NEON_NIGHT) }
    var uploadedFonts by remember { mutableStateOf<List<File>>(emptyList()) }
    var activeFontPath by remember { mutableStateOf<String?>(null) }

    // Read initial preferences and uploaded fonts
    LaunchedEffect(Unit) {
        activeTheme = KeyboardTheme.getSavedTheme(context)
        activeFontPath = KeyboardTheme.getSavedFontPath(context)
        uploadedFonts = FontManager.listCustomFonts(context)
    }

    // Dynamic Font Picker Contract
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            var fileName = "custom_font.ttf"
            
            // Query original filename
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            if (fileName.endsWith(".ttf", ignoreCase = true) || fileName.endsWith(".otf", ignoreCase = true)) {
                val success = FontManager.installFont(context, uri, fileName)
                if (success) {
                    Toast.makeText(context, "تم تثبيت الخط المخصص بنجاح! 🎉", Toast.LENGTH_LONG).show()
                    uploadedFonts = FontManager.listCustomFonts(context)
                } else {
                    Toast.makeText(context, "خطأ أثناء تثبيت ملف الخط", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "الملف غير صالح. يدعم فقط صيغ TTF و OTF!", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Tab Section Header
        item {
            Column {
                Text("الأشكال والثيمات والخطوط 🎨", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                Text("خصص كيبوردك بالثيمات الرائعة وارفع خطوطك الخاصة المفضلة", fontSize = 12.sp, color = Color(0xFF49454F))
            }
        }

        // --- SECTION 1: FONTS UPLOADER ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("رفع وتخصيص خطوط الكيبورد 🖋️", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                    Text(
                        "ارفع خطك العربي أو الإنجليزي المفضل بصيغة (.ttf أو .otf) وسنقوم بتطبيقه على جميع مفاتيح الكيبورد في جهازك فوراً!",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F),
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { fontPickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("رفع وتثبيت ملف خط جديد (.ttf / .otf)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    HorizontalDivider(color = Color(0xFFE6E0E9), thickness = 1.dp)

                    Text("قائمة خطوطك المخصصة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))

                    if (uploadedFonts.isEmpty()) {
                        Text(
                            "لا توجد خطوط مخصصة حالياً. الكيبورد يستخدم خط النظام الافتراضي.",
                            fontSize = 11.sp,
                            color = Color(0xFF49454F),
                            fontFamily = FontFamily.Default
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Default System Font Option
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeFontPath = null
                                        KeyboardTheme.saveFontPath(context, null)
                                        Toast.makeText(context, "تم تطبيق خط النظام الافتراضي", Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (activeFontPath == null) Color(0xFFEADDFF) else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (activeFontPath == null) Color(0xFF6750A4) else Color(0xFFCAC4D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (activeFontPath == null) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Selected",
                                        tint = if (activeFontPath == null) Color(0xFF6750A4) else Color(0xFF49454F),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("خط النظام الافتراضي (Default)", color = Color(0xFF1D1B20), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Dynamic Installed Fonts List
                            uploadedFonts.forEach { fontFile ->
                                val isSelected = activeFontPath == fontFile.absolutePath
                                val displayName = FontManager.getDisplayName(fontFile)
                                val customFontFamily = remember(fontFile) { FontManager.getFontFamily(fontFile) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeFontPath = fontFile.absolutePath
                                            KeyboardTheme.saveFontPath(context, fontFile.absolutePath)
                                            Toast.makeText(context, "تم تطبيق خط: $displayName", Toast.LENGTH_SHORT).show()
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFEADDFF) else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = "Selected",
                                                tint = if (isSelected) Color(0xFF6750A4) else Color(0xFF49454F),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = displayName,
                                                    color = Color(0xFF1D1B20),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "تجربة الخط المخصص (أبجد هوز - ABC)",
                                                    color = Color(0xFF49454F),
                                                    fontSize = 12.sp,
                                                    fontFamily = customFontFamily
                                                )
                                            }
                                        }

                                        // Delete dynamic font button
                                        IconButton(
                                            onClick = {
                                                if (isSelected) {
                                                    activeFontPath = null
                                                    KeyboardTheme.saveFontPath(context, null)
                                                }
                                                FontManager.deleteFont(fontFile)
                                                uploadedFonts = FontManager.listCustomFonts(context)
                                                Toast.makeText(context, "تم حذف ملف الخط", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete font", tint = Color(0xFFB3261E), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 2: THEMES CHOOSER ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("اختر ثيم لوحة المفاتيح 🎨", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                    Text("غير شكل وألوان الكيبورد فوراً بالاختيار من بين 7 ثيمات واضحة وقوية!", fontSize = 12.sp, color = Color(0xFF49454F))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KeyboardTheme.values().forEach { theme ->
                            val isSelected = theme == activeTheme
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTheme = theme
                                        KeyboardTheme.saveTheme(context, theme)
                                        Toast.makeText(context, "تم تفعيل ثيم: ${theme.displayName}", Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFEADDFF) else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Selected",
                                            tint = if (isSelected) Color(0xFF6750A4) else Color(0xFF49454F),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(theme.displayName, color = Color(0xFF1D1B20), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Palette preview bubbles
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(theme.backgroundColor).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(theme.keyBackgroundColor).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(theme.keyTextColor).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape))
                                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(theme.accentColor).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
