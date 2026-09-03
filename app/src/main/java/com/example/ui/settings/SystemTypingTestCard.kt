package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ime.KeyboardView
import com.example.ui.theme.KeyShapeStyle
import com.example.ui.theme.KeyboardHeightStyle
import com.example.ui.theme.KeyboardThemeStyle
import com.example.util.NativeInputMethodHelper

@Composable
fun SystemTypingTestCard(
    isSelected: Boolean,
    activeTheme: KeyboardThemeStyle = KeyboardThemeStyle.LIGHT,
    heightStyle: KeyboardHeightStyle = KeyboardHeightStyle.NORMAL,
    shapeStyle: KeyShapeStyle = KeyShapeStyle.ROUNDED,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var testText by remember { mutableStateOf("") }
    var isEmbeddedKeyboardVisible by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Area Uji Coba Ketik",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                AssistChip(
                    onClick = { isEmbeddedKeyboardVisible = !isEmbeddedKeyboardVisible },
                    label = {
                        Text(
                            if (isEmbeddedKeyboardVisible) "Sembunyikan Keyboard" else "Tampilkan Keyboard",
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (isEmbeddedKeyboardVisible) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }

            Text(
                text = "Ketuk kolom teks di bawah ini untuk menguji keyboard langsung di emulator/ponsel Anda.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                placeholder = { Text("Ketuk di sini untuk mulai mengetik...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            isEmbeddedKeyboardVisible = true
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Embedded Live Keyboard for immediate preview testing in AI Studio
            AnimatedVisibility(visible = isEmbeddedKeyboardVisible) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keyboard Hemat (Langsung Aktif):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (testText.isNotEmpty()) {
                            TextButton(
                                onClick = { testText = "" },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Hapus Teks", fontSize = 11.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        KeyboardView(
                            onKeyPress = { char -> testText += char },
                            onSpecialPress = { action ->
                                when {
                                    action == "BACKSPACE" -> if (testText.isNotEmpty()) testText = testText.dropLast(1)
                                    action == "DEL" -> if (testText.isNotEmpty()) testText = testText.drop(1)
                                    action == "SPACE" -> testText += " "
                                    action == "ENTER" -> testText += "\n"
                                    action == "TAB" -> testText += "    "
                                    action == "CUT" -> testText = ""
                                    action == "PASTE" -> testText += " [pasted] "
                                    action == "UNDO" -> if (testText.isNotEmpty()) testText = testText.dropLast(1)
                                    action == "DELETE_WORD" -> testText = testText.substringBeforeLast(" ", "")
                                    action.startsWith("COMMIT:") -> testText += action.removePrefix("COMMIT:")
                                }
                            },
                            predictions = if (testText.isNotEmpty()) {
                                val token = testText.trim().substringAfterLast(" ", testText.trim()).takeLast(6)
                                listOf(token + "an", token, token + "kan", token + "nya")
                            } else {
                                listOf("Saya", "Yang", "Terima kasih", "Bisa")
                            },
                            onPredictionClick = { word ->
                                val before = testText.substringBeforeLast(" ", "")
                                testText = if (before.isNotEmpty()) "$before $word " else "$word "
                            },
                            activeTheme = activeTheme,
                            heightStyle = heightStyle,
                            shapeStyle = shapeStyle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isSelected) {
                OutlinedButton(
                    onClick = { NativeInputMethodHelper.showInputMethodPicker(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gunakan 'Keyboard Hemat' Sebagai Keyboard Utama Sistem", fontSize = 12.sp)
                }
            }
        }
    }
}


