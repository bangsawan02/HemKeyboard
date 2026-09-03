package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.NativeInputMethodHelper

@Composable
fun SystemTypingTestCard(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                    text = "Area Uji Coba Sistem",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!isSelected) {
                    AssistChip(
                        onClick = { NativeInputMethodHelper.showInputMethodPicker(context) },
                        label = { Text("Pilih IME", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            Text(
                text = "Ketik di bawah untuk menguji keyboard sistem yang sedang aktif di ponsel/emulator Anda.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            var testText by remember { mutableStateOf("") }
            OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                placeholder = { Text("Ketuk di sini untuk mulai mengetik...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            if (!isSelected) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { NativeInputMethodHelper.showInputMethodPicker(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih 'Keyboard Hemat' Sebagai Keyboard Utama")
                }
            }
        }
    }
}

