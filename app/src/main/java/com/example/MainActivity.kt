package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.WordEntity
import com.example.ime.KeyboardView
import com.example.ui.theme.KeyShapeStyle
import com.example.ui.theme.KeyboardHeightStyle
import com.example.ui.theme.KeyboardThemeStyle
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.getKeyboardColors
import com.example.viewmodel.DictionaryFilterType
import com.example.viewmodel.KeyboardSettingsViewModel


class MainActivity : ComponentActivity() {

    private val viewModel: KeyboardSettingsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Keyboard Hemat",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    SettingsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh statuses when returning from system settings
        viewModel.checkKeyboardStatus()
        viewModel.loadSettings()
    }
}

@Composable
fun SettingsScreen(
    viewModel: KeyboardSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val activeTheme by viewModel.activeTheme.collectAsState()
    val heightStyle by viewModel.heightStyle.collectAsState()
    val shapeStyle by viewModel.shapeStyle.collectAsState()
    val autocorrectEnabled by viewModel.autocorrectEnabled.collectAsState()
    val predictionEnabled by viewModel.predictionEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val codingBarEnabled by viewModel.codingBarEnabled.collectAsState()
    val cursorArrowsEnabled by viewModel.cursorArrowsEnabled.collectAsState()
    val codeSnippetsEnabled by viewModel.codeSnippetsEnabled.collectAsState()
    val tabUsesSpaces by viewModel.tabUsesSpaces.collectAsStateWithLifecycle()
    
    val vowelOptionalEnabled by viewModel.vowelOptionalEnabled.collectAsStateWithLifecycle()
    val guessMissingLettersEnabled by viewModel.guessMissingLettersEnabled.collectAsStateWithLifecycle()
    val mistypeTolerance by viewModel.mistypeTolerance.collectAsStateWithLifecycle()
    val nextWordPredictionEnabled by viewModel.nextWordPredictionEnabled.collectAsStateWithLifecycle()
    val alwaysPredictEnabled by viewModel.alwaysPredictEnabled.collectAsStateWithLifecycle()
    val predictPasswordsEnabled by viewModel.predictPasswordsEnabled.collectAsStateWithLifecycle()
    val showPasswordEnabled by viewModel.showPasswordEnabled.collectAsStateWithLifecycle()
    val suggestionBarActionsEnabled by viewModel.suggestionBarActionsEnabled.collectAsStateWithLifecycle()

    val wordCount by viewModel.wordCount.collectAsStateWithLifecycle()
    val customWordCount by viewModel.customWordCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val dictionaryWords by viewModel.dictionaryWords.collectAsStateWithLifecycle()

    val isEnabled = viewModel.isKeyboardEnabled.value
    val isSelected = viewModel.isKeyboardSelected.value

    var previewTypedText by remember { mutableStateOf("") }
    var showAddWordDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var newWordInput by remember { mutableStateOf("") }
    var newWordFrequency by remember { mutableIntStateOf(10) }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Keyboard Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Status Aktivasi Keyboard",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusText = if (isEnabled && isSelected) "Aktif & Siap Digunakan" else if (isEnabled) "Diaktifkan tapi Belum Dipilih" else "Belum Diaktifkan"
                    val statusColor = if (isEnabled && isSelected) Color(0xFF2E7D32) else if (isEnabled) Color(0xFFEF6C00) else Color(0xFFC62828)
                    val statusIcon = if (isEnabled && isSelected) Icons.Default.CheckCircle else Icons.Default.Warning

                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = statusText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                if (!isEnabled) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("1. Aktifkan di Setelan Sistem")
                    }
                } else if (!isSelected) {
                    Button(
                        onClick = {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("2. Pilih Sebagai Keyboard Aktif")
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Keyboard Hemat sedang aktif secara global!",
                            color = Color(0xFF2E7D32),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Live Interactive Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pratinjau Langsung Keyboard",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (previewTypedText.isNotEmpty()) {
                        TextButton(onClick = { previewTypedText = "" }, contentPadding = PaddingValues(0.dp)) {
                            Text("Reset", fontSize = 12.sp)
                        }
                    }
                }

                Text(
                    text = "Coba ketik langsung untuk melihat perubahan tinggi, bentuk tombol, dan tema secara real-time.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Display preview output box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (previewTypedText.isEmpty()) "Hasil ketikan pratinjau muncul di sini... (coba tekan tombol Edit)" else previewTypedText,
                        color = if (previewTypedText.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real Live Keyboard Composable
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    KeyboardView(
                        onKeyPress = { char -> previewTypedText += char },
                        onSpecialPress = { action ->
                            when {
                                action == "BACKSPACE" -> if (previewTypedText.isNotEmpty()) previewTypedText = previewTypedText.dropLast(1)
                                action == "SPACE" -> previewTypedText += " "
                                action == "ENTER" -> previewTypedText += "\n"
                                action == "TAB" -> previewTypedText += if (tabUsesSpaces) "    " else "\t"
                                action == "SELECT_ALL" -> { /* Selected */ }
                                action == "COPY" -> { /* Copied */ }
                                action == "CUT" -> previewTypedText = ""
                                action == "PASTE" -> previewTypedText += " [pasted_code] "
                                action == "UNDO" -> if (previewTypedText.isNotEmpty()) previewTypedText = previewTypedText.dropLast(1)
                                action == "REDO" -> previewTypedText += " "
                                action == "DELETE_WORD" -> previewTypedText = previewTypedText.substringBeforeLast(" ", "")
                                action == "DELETE_LINE" -> previewTypedText = previewTypedText.substringBeforeLast("\n", "")
                                action == "ARROW_LEFT" -> { /* Cursor simulation */ }
                                action == "ARROW_RIGHT" -> { /* Cursor simulation */ }
                                action.startsWith("PAIR:") -> previewTypedText += action.removePrefix("PAIR:")
                                action.startsWith("COMMIT:") -> previewTypedText += action.removePrefix("COMMIT:")
                            }
                        },
                        predictions = if (previewTypedText.isNotEmpty()) listOf(previewTypedText.takeLast(6) + "an", previewTypedText.takeLast(6), previewTypedText.takeLast(6) + "kan") else listOf("terima", "kasih", "kembali"),
                        onPredictionClick = { word ->
                            previewTypedText = previewTypedText.substringBeforeLast(" ", "") + (if (previewTypedText.contains(" ")) " " else "") + word + " "
                        },
                        activeTheme = activeTheme,
                        heightStyle = heightStyle,
                        shapeStyle = shapeStyle,
                        codingBarEnabled = codingBarEnabled,
                        cursorArrowsEnabled = cursorArrowsEnabled,
                        codeSnippetsEnabled = codeSnippetsEnabled,
                        suggestionBarActionsEnabled = suggestionBarActionsEnabled
                    )
                }
            }
        }

        // 3. Fast Access & Text Navigation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fitur Akses Cepat & Navigasi Teks",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Akses cepat Salin, Tempel, Potong, Pilih Semua, Undo/Redo, Navigasi Panah, Seleksi Teks, dan Mode Edit khusus.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle 1: Fast Edit Access
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Mode Edit Cepat Keyboard",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Menyediakan tombol Edit khusus untuk navigasi kursor dan penyuntingan teks tingkat lanjut.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = codingBarEnabled,
                        onCheckedChange = { viewModel.updateCodingBar(it) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Toggle 2: Tab uses 4 spaces
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Indentasi TAB (4 Spasi)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tombol TAB akan memasukkan 4 spasi (jika nonaktif: 1 karakter TAB).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = tabUsesSpaces,
                        onCheckedChange = { viewModel.updateTabUsesSpaces(it) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Toggle 3: Selection & Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Navigasi & Seleksi Presisi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Fitur navigasi panah kursor dan seleksi teks persis di baris keyboard.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = codeSnippetsEnabled,
                        onCheckedChange = { viewModel.updateCodeSnippets(it) }
                    )
                }
            }
        }

        // 4. Keyboard Height Adjustment Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Height,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tinggi Keyboard (Ukuran Tombol)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Sesuaikan tinggi tombol keyboard agar pas dengan ukuran jari dan layar Anda.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                KeyboardHeightStyle.entries.forEach { option ->
                    val isSelectedHeight = option == heightStyle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelectedHeight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelectedHeight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.updateHeight(option) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelectedHeight,
                            onClick = { viewModel.updateHeight(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (isSelectedHeight) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height((option.keyHeightDp / 2).dp)
                                .background(
                                    if (isSelectedHeight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            }
        }

        // 4. Key Shape Styling Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RoundedCorner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bentuk & Gaya Tombol",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Pilih sudut lekukan sudut tombol keyboard sesuai selera Anda.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KeyShapeStyle.entries.forEach { shapeOption ->
                        val isSelectedShape = shapeOption == shapeStyle
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelectedShape) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelectedShape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.updateShape(shapeOption) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp, 28.dp)
                                    .clip(RoundedCornerShape(shapeOption.cornerRadiusDp.dp))
                                    .background(
                                        if (isSelectedShape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelectedShape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(shapeOption.cornerRadiusDp.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "A",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelectedShape) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = shapeOption.displayName.substringBefore(" "),
                                fontSize = 12.sp,
                                fontWeight = if (isSelectedShape) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 5. Theme Customization Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pilih Tema Warna Keyboard",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Personalisasikan palet visual tombol dengan tema pilihan.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                KeyboardThemeStyle.entries.forEach { themeOption ->
                    val isSelectedTheme = themeOption == activeTheme
                    val themeColors = getKeyboardColors(themeOption)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelectedTheme) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isSelectedTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.updateTheme(themeOption) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelectedTheme,
                            onClick = { viewModel.updateTheme(themeOption) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = themeOption.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Visual color swatch blocks
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(22.dp, 16.dp).clip(RoundedCornerShape(2.dp)).background(themeColors.background))
                                Box(modifier = Modifier.size(16.dp, 16.dp).clip(RoundedCornerShape(2.dp)).background(themeColors.keyBackground))
                                Box(modifier = Modifier.size(16.dp, 16.dp).clip(RoundedCornerShape(2.dp)).background(themeColors.specialKeyBackground))
                                Box(modifier = Modifier.size(16.dp, 16.dp).clip(RoundedCornerShape(2.dp)).background(themeColors.actionKeyBackground))
                            }
                        }
                    }
                }
            }
        }

        // 6. Autocorrect & Predictions Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Fitur Cerdas & Prediksi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle: Suggestion Bar Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Bar Saran Pro [EDIT][CLIP][HIDE]",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Menambahkan tombol aksi cepat ke bar saran keyboard.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = suggestionBarActionsEnabled,
                        onCheckedChange = { viewModel.updateSuggestionBarActions(it) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Toggle: Prediction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Prediksi Teks Cerdas",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Menampilkan saran kata yang relevan di atas keyboard.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = predictionEnabled,
                        onCheckedChange = { viewModel.updatePrediction(it) }
                    )
                }

                if (predictionEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Next Word Prediction
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text(
                                text = "Prediksi Kata Berikutnya",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Mempelajari pasangan kata untuk menyarankan kata selanjutnya.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = nextWordPredictionEnabled,
                            onCheckedChange = { viewModel.updateNextWordPrediction(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Vowel Optional
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text(
                                text = "Huruf Vokal Opsional",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Prediksi kata meskipun huruf vokal tidak diketik (kbr -> kabar).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = vowelOptionalEnabled,
                            onCheckedChange = { viewModel.updateVowelOptional(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Guess Missing Letters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text(
                                text = "Tebak Huruf Kurang (Beta)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Memperbaiki kata meskipun ada huruf yang kurang (nsary -> necessary).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = guessMissingLettersEnabled,
                            onCheckedChange = { viewModel.updateGuessMissingLetters(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Always Predict
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text(
                                text = "Selalu Prediksi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Abaikan flag 'no prediction' dari aplikasi pihak ketiga.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = alwaysPredictEnabled,
                            onCheckedChange = { viewModel.updateAlwaysPredict(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Predict Passwords
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text(
                                text = "Prediksi di Kolom Kata Sandi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tetap aktifkan prediksi saat mengisi kolom password.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = predictPasswordsEnabled,
                            onCheckedChange = { viewModel.updatePredictPasswords(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Toggle 2: Autocorrect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Koreksi Otomatis (Spasi)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Mengoreksi ejaan kata secara instan saat tombol spasi ditekan.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = autocorrectEnabled,
                        onCheckedChange = { viewModel.updateAutocorrect(it) },
                        enabled = predictionEnabled
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Toggle 3: Haptic Feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Getar Sentuhan (Haptic Feedback)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Memberikan getaran lembut saat setiap tombol ditekan.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = { viewModel.updateHaptic(it) }
                    )
                }
            }
        }

        // 7. Kamus Pengguna Lokal & Prediksi Cerdas (Room Database)
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kamus Pengguna & Prediksi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Database lokal Room untuk mempelajari kata dan nama baru secara otomatis.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = {
                            newWordInput = ""
                            newWordFrequency = 10
                            showAddWordDialog = true
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Kata Baru")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats overview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$wordCount",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Total Kata",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$customWordCount",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Kustom / Nama",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        val estSizeKb = 24.0 + (wordCount * 0.1)
                        val estSizeStr = String.format("%.1f KB", estSizeKb)
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = estSizeStr,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "Ukuran DB",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Cari kata dalam kamus...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus Pencarian", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DictionaryFilterType.entries.forEach { filter ->
                        val isSelectedFilter = filterType == filter
                        FilterChip(
                            selected = isSelectedFilter,
                            onClick = { viewModel.updateFilterType(filter) },
                            label = {
                                Text(
                                    text = filter.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelectedFilter) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Words list view
                if (dictionaryWords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ada kata yang cocok dengan \"$searchQuery\"" else "Kamus kosong",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dictionaryWords.take(15).forEach { wordEntity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = wordEntity.word,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (wordEntity.isUserCustom) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Kustom",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "⚡ ${wordEntity.frequency}x",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteWord(wordEntity.word) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Hapus ${wordEntity.word}",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (dictionaryWords.size > 15) {
                            Text(
                                text = "Menampilkan 15 dari total ${dictionaryWords.size} kata yang tersimpan.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bersihkan Kamus & Setel Ulang")
                }
            }
        }

        // Dialog: Tambah Kata Baru / Nama Kustom
        if (showAddWordDialog) {
            AlertDialog(
                onDismissRequest = { showAddWordDialog = false },
                title = {
                    Text(
                        "Tambah Kata / Nama Kustom",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Kata atau nama ini akan langsung dikenali oleh sistem prediksi keyboard.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = newWordInput,
                            onValueChange = { newWordInput = it },
                            placeholder = { Text("Contoh: Rizky, ChatGPT, Jetpack") },
                            label = { Text("Kata / Nama Baru") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Text(
                            text = "Prioritas / Frekuensi:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5 to "Biasa (5x)", 10 to "Sering (10x)", 25 to "Prioritas (25x)").forEach { (freq, label) ->
                                val isSelectedFreq = newWordFrequency == freq
                                FilterChip(
                                    selected = isSelectedFreq,
                                    onClick = { newWordFrequency = freq },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newWordInput.isNotBlank()) {
                                viewModel.addUserCustomWord(newWordInput.trim(), newWordFrequency)
                                showAddWordDialog = false
                            }
                        },
                        enabled = newWordInput.isNotBlank()
                    ) {
                        Text("Simpan ke Kamus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddWordDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Dialog: Konfirmasi Reset Kamus
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset Kamus ke Bawaan?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Semua kata kustom dan riwayat frekuensi yang dipelajari akan dihapus dan dikembalikan ke daftar kata standar bahasa Indonesia.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearDictionary()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset Sekarang")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // 8. System Typing Test Area Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Area Uji Coba Sistem",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Ketik di bawah untuk menguji keyboard sistem yang sedang aktif di ponsel Anda.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                var testText by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    placeholder = { Text("Mulai mengetik di sini...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (!isSelected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tips: Pastikan 'Keyboard Hemat' aktif dan terpilih untuk membuka keyboard kustom pada kolom sistem.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

