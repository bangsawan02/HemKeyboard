package com.example.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    layoutState: KeyboardLayoutState,
    onEditToggle: () -> Unit,
    onEmojiToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Top 3 Word Predictions formatting logic
    val displayPredictions = remember(predictions, currentWord) {
        if (predictions.isNotEmpty()) {
            val taken = predictions.take(3)
            when (taken.size) {
                1 -> listOf("", taken[0], "")
                2 -> listOf(taken[0], taken[1], "")
                else -> taken
            }
        } else if (currentWord.isNotBlank()) {
            listOf("", currentWord.trim(), "")
        } else {
            listOf("Saya", "Yang", "Terima kasih")
        }
    }

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
        // Default Edit Mode Quick Toggle Button
        Box(
            modifier = Modifier
                .size((suggestionHeight.value - 6).coerceAtLeast(26f).dp)
                .clip(RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                .background(if (layoutState == KeyboardLayoutState.EDIT) colors.actionKeyBackground.copy(alpha = 0.25f) else Color.Transparent)
                .clickable { onEditToggle() }
                .testTag("suggestion_edit_toggle"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Mode Edit",
                tint = if (layoutState == KeyboardLayoutState.EDIT) colors.textHighlight else colors.suggestionText,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(3.dp))

        // Emoji Panel Quick Toggle Button
        Box(
            modifier = Modifier
                .size((suggestionHeight.value - 6).coerceAtLeast(26f).dp)
                .clip(RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                .background(if (layoutState == KeyboardLayoutState.EMOJI) colors.actionKeyBackground.copy(alpha = 0.25f) else Color.Transparent)
                .clickable { onEmojiToggle() }
                .testTag("suggestion_emoji_toggle"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SentimentSatisfiedAlt,
                contentDescription = "Panel Emoji",
                tint = if (layoutState == KeyboardLayoutState.EMOJI) colors.textHighlight else colors.suggestionText,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(3.dp))

        // 3 Word Predictions Items
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            displayPredictions.forEachIndexed { index, suggestion ->
                val isReal = suggestion.isNotEmpty()
                val isCenterPrimary = (index == 1 && isReal)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 2.dp, horizontal = 2.dp)
                        .clip(RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                        .background(
                            if (isCenterPrimary) colors.actionKeyBackground.copy(alpha = 0.22f)
                            else Color.Transparent
                        )
                        .then(
                            if (isCenterPrimary && colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.8.dp, colors.textHighlight.copy(alpha = 0.35f), RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                            else Modifier
                        )
                        .clickable(enabled = isReal) { onPredictionClick(suggestion) }
                        .testTag("suggestion_item_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = suggestion,
                        color = if (isCenterPrimary) colors.textHighlight else colors.suggestionText,
                        fontSize = if (isCenterPrimary) (heightStyle.fontSizeSp - 2).coerceAtLeast(13).sp else (heightStyle.fontSizeSp - 3).coerceAtLeast(11).sp,
                        fontWeight = if (isCenterPrimary) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                if (index < 2) {
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
