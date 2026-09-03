package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ime.KeyboardView
import com.example.ui.theme.KeyShapeStyle
import com.example.ui.theme.KeyboardHeightStyle
import com.example.ui.theme.KeyboardThemeStyle

@Composable
fun KeyboardPreviewCard(
    activeTheme: KeyboardThemeStyle,
    heightStyle: KeyboardHeightStyle,
    shapeStyle: KeyShapeStyle,
    modifier: Modifier = Modifier
) {
    var previewTypedText by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                text = "Coba ketik langsung untuk melihat tata letak, tema, dan responsivitas keyboard secara real-time.",
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
                    text = if (previewTypedText.isEmpty()) "Hasil ketikan pratinjau muncul di sini... (coba ketik atau gunakan tombol Edit)" else previewTypedText,
                    color = if (previewTypedText.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
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
                            action == "TAB" -> previewTypedText += "    "
                            action == "SELECT_ALL" -> { /* Selected */ }
                            action == "COPY" -> { /* Copied */ }
                            action == "CUT" -> previewTypedText = ""
                            action == "PASTE" -> previewTypedText += " [pasted] "
                            action == "UNDO" -> if (previewTypedText.isNotEmpty()) previewTypedText = previewTypedText.dropLast(1)
                            action == "REDO" -> previewTypedText += " "
                            action == "DELETE_WORD" -> previewTypedText = previewTypedText.substringBeforeLast(" ", "")
                            action.startsWith("COMMIT:") -> previewTypedText += action.removePrefix("COMMIT:")
                        }
                    },
                    predictions = if (previewTypedText.isNotEmpty()) listOf(previewTypedText.takeLast(6) + "an", previewTypedText.takeLast(6), previewTypedText.takeLast(6) + "kan") else listOf("Saya", "Yang", "Terima kasih"),
                    onPredictionClick = { word ->
                        previewTypedText = previewTypedText.substringBeforeLast(" ", "") + (if (previewTypedText.contains(" ")) " " else "") + word + " "
                    },
                    activeTheme = activeTheme,
                    heightStyle = heightStyle,
                    shapeStyle = shapeStyle
                )
            }
        }
    }
}
