package com.example.ime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.zIndex

/**
 * Character alternative accents map for long-press popups.
 */
val AltCharactersMap = mapOf(
    "a" to listOf("á", "à", "â", "ä", "ã", "å", "æ", "ā"),
    "A" to listOf("Á", "À", "Â", "Ä", "Ã", "Å", "Æ", "Ā"),
    "c" to listOf("ç", "ć", "č"),
    "C" to listOf("Ç", "Ć", "Č"),
    "e" to listOf("é", "è", "ê", "ë", "ē", "ė", "ę"),
    "E" to listOf("É", "È", "Ê", "Ë", "Ē", "Ė", "Ę"),
    "i" to listOf("í", "ì", "î", "ï", "ī", "į"),
    "I" to listOf("Í", "Ì", "Î", "Ï", "Ī", "Į"),
    "n" to listOf("ñ", "ń"),
    "N" to listOf("Ñ", "Ń"),
    "o" to listOf("ó", "ò", "ô", "ö", "õ", "ø", "ō", "œ"),
    "O" to listOf("Ó", "Ò", "Ô", "Ö", "Õ", "Ø", "Ō", "Œ"),
    "s" to listOf("ß", "ś", "š", "$"),
    "S" to listOf("Ś", "Š", "$"),
    "u" to listOf("ú", "ù", "û", "ü", "ū", "ų"),
    "U" to listOf("Ú", "Ù", "Û", "Ü", "Ū", "Ų"),
    "y" to listOf("ý", "ÿ"),
    "Y" to listOf("Ý", "Ÿ"),
    "z" to listOf("ź", "ż", "ž"),
    "Z" to listOf("Ź", "Ż", "Ž"),
    "0" to listOf("°", "⁰", "∅"),
    "1" to listOf("¹", "½", "⅓", "¼"),
    "2" to listOf("²", "⅔"),
    "3" to listOf("³", "¾"),
    "$" to listOf("€", "£", "¥", "Rp", "¢", "₹"),
    "?" to listOf("¿", "‽"),
    "!" to listOf("¡"),
    "%" to listOf("‰")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyButton(
    text: String,
    onKeyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 48.dp,
    fontSize: TextUnit = 18.sp,
    cornerRadius: Dp = 6.dp,
    borderColor: Color = Color.Transparent,
    backgroundColor: Color,
    textColor: Color,
    hintText: String? = null,
    onLongClick: ((String) -> Unit)? = null,
    enableKeyPreview: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showAltPopup by remember { mutableStateOf(false) }

    val altVariants = remember(text) { AltCharactersMap[text] ?: emptyList() }

    val currentBg = if (isPressed || showAltPopup) {
        backgroundColor.copy(alpha = 0.65f)
    } else {
        backgroundColor
    }

    Box(
        modifier = modifier
            .height(keyHeight)
            .padding(horizontal = 1.5.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(currentBg)
            .then(
                if (borderColor != Color.Transparent)
                    Modifier.border(0.8.dp, borderColor, RoundedCornerShape(cornerRadius))
                else Modifier
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onKeyClick(text) },
                onLongClick = {
                    if (altVariants.isNotEmpty()) {
                        showAltPopup = true
                    } else if (onLongClick != null) {
                        onLongClick(text)
                    } else if (hintText != null) {
                        onKeyClick(hintText)
                    }
                }
            )
            .testTag("key_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium
        )

        if (hintText != null) {
            Text(
                text = hintText,
                color = textColor.copy(alpha = 0.42f),
                fontSize = (fontSize.value * 0.52f).coerceIn(9f, 12f).sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 3.dp)
            )
        }

        // 1. Key Preview Magnification Bubble (Shows when key is pressed)
        if (enableKeyPreview && isPressed && !showAltPopup && text.length == 1) {
            Box(
                modifier = Modifier
                    .offset(y = (-58).dp)
                    .zIndex(100f)
                    .size(52.dp, 56.dp)
                    .shadow(6.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .pointerInput(Unit) { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontSize = (fontSize.value * 1.5f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Alt Accents Characters Popup Strip on Long Press
        if (showAltPopup && altVariants.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .offset(y = (-52).dp)
                    .zIndex(110f)
                    .shadow(8.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Include default letter
                    Box(
                        modifier = Modifier
                            .size(36.dp, 42.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(backgroundColor.copy(alpha = 0.8f))
                            .clickable {
                                onKeyClick(text)
                                showAltPopup = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    altVariants.forEach { alt ->
                        Box(
                            modifier = Modifier
                                .size(36.dp, 42.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(backgroundColor.copy(alpha = 0.4f))
                                .clickable {
                                    onKeyClick(alt)
                                    showAltPopup = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = alt,
                                color = textColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
