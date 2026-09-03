package com.example.ime.ui

import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ime.KeyboardLayoutState
import com.example.ui.theme.KeyboardColors
import com.example.ui.theme.KeyboardHeightStyle

@Composable
fun WordSuggestionBar(
    predictions: List<String>,
    currentWord: String = "",
    onPredictionClick: (String) -> Unit,
    onSpecialPress: (String) -> Unit,
    colors: KeyboardColors,
    suggestionHeight: Dp,
    cornerRadius: Dp,
    heightStyle: KeyboardHeightStyle,
    onVoiceClick: () -> Unit = {},
    isVoiceListening: Boolean = false,
    clipboardText: String? = null,
    onClipboardPaste: (String) -> Unit = {},
    inlineSuggestionViews: List<View> = emptyList(),
    modifier: Modifier = Modifier
) {
    // 4 Word Predictions formatting logic
    val displayPredictions = remember(predictions, currentWord) {
        if (predictions.isNotEmpty()) {
            predictions.take(4)
        } else if (currentWord.isNotBlank()) {
            listOf(currentWord.trim())
        } else {
            listOf("Saya", "Yang", "Terima kasih", "Bisa")
        }
    }

    // Voice pulsating animation
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(suggestionHeight)
            .clip(RoundedCornerShape(cornerRadius.coerceAtMost(8.dp)))
            .background(colors.suggestionBackground)
            .padding(horizontal = 4.dp)
            .testTag("word_suggestion_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Native Voice Typing Mic Button
        Box(
            modifier = Modifier
                .size((suggestionHeight.value - 6).coerceAtLeast(26f).dp)
                .clip(CircleShape)
                .background(
                    if (isVoiceListening) Color(0xFFE53935)
                    else Color.Transparent
                )
                .then(if (isVoiceListening) Modifier.scale(pulseScale) else Modifier)
                .clickable { onVoiceClick() }
                .testTag("suggestion_voice_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVoiceListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Ketik Suara (Voice Typing)",
                tint = if (isVoiceListening) Color.White else colors.suggestionText,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 2. Autofill Inline Suggestions (Android 11+ Password Manager / OTP / Credentials)
        if (inlineSuggestionViews.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                inlineSuggestionViews.forEach { view ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.9f)
                            .clip(RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                            .border(0.8.dp, colors.actionKeyBackground, RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = {
                                (view.parent as? ViewGroup)?.removeView(view)
                                view
                            },
                            modifier = Modifier.wrapContentSize()
                        )
                    }
                }
            }
        } else if (!clipboardText.isNullOrBlank() && currentWord.isEmpty()) {
            // 3. Live Clipboard Instant Paste Chip
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp, horizontal = 2.dp)
                    .clip(RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                    .background(colors.actionKeyBackground.copy(alpha = 0.18f))
                    .border(0.8.dp, colors.textHighlight.copy(alpha = 0.4f), RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                    .clickable { onClipboardPaste(clipboardText) }
                    .padding(horizontal = 8.dp)
                    .testTag("clipboard_paste_chip"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Tempel",
                        tint = colors.textHighlight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tempel: \"${clipboardText.take(20)}\"",
                        color = colors.textHighlight,
                        fontSize = (heightStyle.fontSizeSp - 3).coerceAtLeast(11).sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // 4. 4 Word Predictions Items
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                displayPredictions.forEachIndexed { index, suggestion ->
                    val isReal = suggestion.isNotEmpty()
                    val isPrimary = (index == 0 && isReal) // First suggestion is primary highlight

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 2.dp, horizontal = 1.5.dp)
                            .clip(RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                            .background(
                                if (isPrimary) colors.actionKeyBackground.copy(alpha = 0.22f)
                                else Color.Transparent
                            )
                            .then(
                                if (isPrimary && colors.keyBorderColor != Color.Transparent)
                                    Modifier.border(0.8.dp, colors.textHighlight.copy(alpha = 0.35f), RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                                else Modifier
                            )
                            .clickable(enabled = isReal) { onPredictionClick(suggestion) }
                            .testTag("suggestion_item_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = suggestion,
                            color = if (isPrimary) colors.textHighlight else colors.suggestionText,
                            fontSize = if (isPrimary) (heightStyle.fontSizeSp - 3).coerceAtLeast(12).sp else (heightStyle.fontSizeSp - 4).coerceAtLeast(11).sp,
                            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (index < displayPredictions.size - 1) {
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight(0.45f)
                                .background(colors.suggestionText.copy(alpha = 0.15f))
                        )
                    }
                }
            }
        }
    }
}
