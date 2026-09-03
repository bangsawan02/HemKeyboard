package com.example.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KeyboardColors

@Composable
fun CodingBarView(
    onSpecialPress: (String) -> Unit,
    colors: KeyboardColors,
    barHeight: Dp,
    cornerRadius: Dp,
    showArrows: Boolean = true,
    showSnippets: Boolean = true,
    modifier: Modifier = Modifier
) {
    val codingSymbols = listOf(
        "TAB" to "TAB",
        "{" to "PAIR:{}",
        "}" to "COMMIT:}",
        "[" to "PAIR:[]",
        "]" to "COMMIT:]",
        "(" to "PAIR:()",
        ")" to "COMMIT:)",
        "<" to "COMMIT:<",
        ">" to "COMMIT:>",
        "=" to "COMMIT:=",
        ";" to "COMMIT:;",
        ":" to "COMMIT::",
        "\"" to "PAIR:\"\"",
        "'" to "PAIR:''",
        "`" to "PAIR:``",
        "$" to "COMMIT:$",
        "_" to "COMMIT:_",
        "|" to "COMMIT:|",
        "&" to "COMMIT:&",
        "!" to "COMMIT:!",
        "?" to "COMMIT:?",
        "->" to "COMMIT:->",
        "=>" to "COMMIT:=>",
        "==" to "COMMIT:==",
        "!=" to "COMMIT:!=",
        "//" to "COMMIT:// "
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
            .background(colors.specialKeyBackground.copy(alpha = 0.5f))
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .testTag("coding_symbols_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Optional quick arrows on the left
        if (showArrows) {
            Box(
                modifier = Modifier
                    .size((barHeight.value - 4).dp)
                    .clip(RoundedCornerShape(cornerRadius.coerceAtMost(4.dp)))
                    .background(colors.keyBackground.copy(alpha = 0.6f))
                    .clickable { onSpecialPress("ARROW_LEFT") }
                    .testTag("coding_arrow_left"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kiri",
                    tint = colors.specialKeyText,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            Box(
                modifier = Modifier
                    .size((barHeight.value - 4).dp)
                    .clip(RoundedCornerShape(cornerRadius.coerceAtMost(4.dp)))
                    .background(colors.keyBackground.copy(alpha = 0.6f))
                    .clickable { onSpecialPress("ARROW_RIGHT") }
                    .testTag("coding_arrow_right"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Kanan",
                    tint = colors.specialKeyText,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
        }

        // Horizontal scrollable coding symbols & snippets
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            codingSymbols.forEach { (label, action) ->
                Box(
                    modifier = Modifier
                        .height((barHeight.value - 4).dp)
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(cornerRadius.coerceAtMost(4.dp)))
                        .background(colors.keyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.6.dp, colors.keyBorderColor, RoundedCornerShape(cornerRadius.coerceAtMost(4.dp)))
                            else Modifier
                        )
                        .clickable { onSpecialPress(action) }
                        .padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = colors.keyText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
