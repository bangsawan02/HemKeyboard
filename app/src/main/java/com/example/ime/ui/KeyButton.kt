package com.example.ime.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    onLongClick: ((String) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentBg = if (isPressed) {
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
                onLongClick = if (onLongClick != null) {
                    { onLongClick(text) }
                } else if (hintText != null) {
                    { onKeyClick(hintText) }
                } else null
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
    }
}
