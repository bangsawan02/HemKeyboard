package com.example.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ime.ui.CodingBarView
import com.example.ime.ui.EditKeyboardView
import com.example.ime.ui.EmojiKeyboardView
import com.example.ime.ui.KeyButton
import com.example.ime.ui.WordSuggestionBar
import com.example.ui.theme.KeyShapeStyle
import com.example.ui.theme.KeyboardColors
import com.example.ui.theme.KeyboardHeightStyle
import com.example.ui.theme.KeyboardThemeStyle
import com.example.ui.theme.getKeyboardColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

sealed class KeyboardLayoutState {
    object QWERTY_LOWER : KeyboardLayoutState()
    object QWERTY_UPPER : KeyboardLayoutState()
    object QWERTY_CAPS_LOCK : KeyboardLayoutState()
    object SYMBOLS : KeyboardLayoutState()
    object EDIT : KeyboardLayoutState()
    object EMOJI : KeyboardLayoutState()
}

enum class EmojiCategory(val icon: String, val title: String) {
    SMILEYS("😀", "Wajah"),
    GESTURES("👍", "Gestur"),
    HEARTS("❤️", "Simbol"),
    STAR("⭐", "Favorit"),
    ANIMALS("🐱", "Hewan"),
    FOOD("🍔", "Makanan"),
    OBJECTS("⚽", "Objek")
}

@Composable
fun KeyboardView(
    onKeyPress: (Char) -> Unit,
    onSpecialPress: (String) -> Unit,
    predictions: List<String>,
    onPredictionClick: (String) -> Unit,
    activeTheme: KeyboardThemeStyle,
    heightStyle: KeyboardHeightStyle = KeyboardHeightStyle.NORMAL,
    shapeStyle: KeyShapeStyle = KeyShapeStyle.ROUNDED,
    codingBarEnabled: Boolean = true,
    cursorArrowsEnabled: Boolean = true,
    codeSnippetsEnabled: Boolean = true,
    autoCapitalizeNext: Boolean = false,
    currentWord: String = "",
    modifier: Modifier = Modifier
) {
    val colors = getKeyboardColors(activeTheme)
    var layoutState by remember { mutableStateOf<KeyboardLayoutState>(KeyboardLayoutState.QWERTY_LOWER) }

    val keyHeight = heightStyle.keyHeightDp.dp
    val fontSize = heightStyle.fontSizeSp.sp
    val cornerRadius = shapeStyle.cornerRadiusDp.dp
    val suggestionHeight = heightStyle.suggestionHeightDp.dp

    LaunchedEffect(autoCapitalizeNext) {
        if (autoCapitalizeNext && layoutState == KeyboardLayoutState.QWERTY_LOWER) {
            layoutState = KeyboardLayoutState.QWERTY_UPPER
        } else if (!autoCapitalizeNext && layoutState == KeyboardLayoutState.QWERTY_UPPER) {
            layoutState = KeyboardLayoutState.QWERTY_LOWER
        }
    }

    val handleKeyPress = remember(onKeyPress) {
        { charText: String ->
            if (charText.isNotEmpty()) {
                onKeyPress(charText.first())
                if (layoutState == KeyboardLayoutState.QWERTY_UPPER) {
                    layoutState = KeyboardLayoutState.QWERTY_LOWER
                }
            }
        }
    }

    val handleBackspace = remember(onSpecialPress) { { onSpecialPress("BACKSPACE") } }
    val handleSpace = remember(onSpecialPress) { { onSpecialPress("SPACE") } }
    val handleEnter = remember(onSpecialPress) { { onSpecialPress("ENTER") } }

    val handleShiftClick = remember {
        {
            layoutState = when (layoutState) {
                KeyboardLayoutState.QWERTY_LOWER -> KeyboardLayoutState.QWERTY_UPPER
                KeyboardLayoutState.QWERTY_UPPER -> KeyboardLayoutState.QWERTY_CAPS_LOCK
                else -> KeyboardLayoutState.QWERTY_LOWER
            }
        }
    }

    val handleSymToggle = remember {
        {
            layoutState = if (layoutState == KeyboardLayoutState.SYMBOLS) {
                KeyboardLayoutState.QWERTY_LOWER
            } else {
                KeyboardLayoutState.SYMBOLS
            }
        }
    }

    val handleEditToggle = remember {
        {
            layoutState = if (layoutState == KeyboardLayoutState.EDIT) {
                KeyboardLayoutState.QWERTY_LOWER
            } else {
                KeyboardLayoutState.EDIT
            }
        }
    }

    val handleEmojiToggle = remember {
        {
            layoutState = if (layoutState == KeyboardLayoutState.EMOJI) {
                KeyboardLayoutState.QWERTY_LOWER
            } else {
                KeyboardLayoutState.EMOJI
            }
        }
    }

    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row1Hints = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row2Hints = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")
    val row3Hints = listOf("*", "\"", "'", ":", ";", "!", "?")

    val symRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val symRow2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
    val symRow3 = listOf("*", "\"", "'", ":", ";", "!", "?")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(vertical = 4.dp, horizontal = 3.dp)
    ) {
        // 1. Word Suggestion Bar (Komponen UI Bar Saran Kata)
        WordSuggestionBar(
            predictions = predictions,
            currentWord = currentWord,
            onPredictionClick = onPredictionClick,
            onSpecialPress = onSpecialPress,
            colors = colors,
            suggestionHeight = suggestionHeight,
            cornerRadius = cornerRadius,
            heightStyle = heightStyle,
            layoutState = layoutState,
            onEditToggle = handleEditToggle,
            onEmojiToggle = handleEmojiToggle
        )

        // 2. Optional Coding & Arrow Keys Bar
        if (codingBarEnabled && layoutState != KeyboardLayoutState.EDIT && layoutState != KeyboardLayoutState.EMOJI) {
            Spacer(modifier = Modifier.height(3.dp))
            CodingBarView(
                onSpecialPress = onSpecialPress,
                colors = colors,
                barHeight = (keyHeight.value * 0.75f).dp,
                cornerRadius = cornerRadius,
                showArrows = cursorArrowsEnabled,
                showSnippets = codeSnippetsEnabled
            )
        }

        // 3. Baris Angka Keyboard (Dedicated Number Row 1..0)
        if (layoutState == KeyboardLayoutState.QWERTY_LOWER || 
            layoutState == KeyboardLayoutState.QWERTY_UPPER || 
            layoutState == KeyboardLayoutState.QWERTY_CAPS_LOCK) {
            
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { numKey ->
                    KeyButton(
                        text = numKey,
                        onKeyClick = handleKeyPress,
                        modifier = Modifier.weight(1f),
                        keyHeight = (keyHeight.value * 0.82f).dp,
                        fontSize = (heightStyle.fontSizeSp - 3).coerceAtLeast(12).sp,
                        cornerRadius = cornerRadius,
                        borderColor = colors.keyBorderColor,
                        backgroundColor = colors.keyBackground,
                        textColor = colors.keyText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 4. Main Keyboard Layout State
        when (layoutState) {
            KeyboardLayoutState.QWERTY_LOWER, KeyboardLayoutState.QWERTY_UPPER, KeyboardLayoutState.QWERTY_CAPS_LOCK -> {
                val isUpper = layoutState != KeyboardLayoutState.QWERTY_LOWER

                // Row 1
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row1.forEachIndexed { index, char ->
                        KeyButton(
                            text = if (isUpper) char.uppercase() else char,
                            hintText = row1Hints.getOrNull(index),
                            onKeyClick = handleKeyPress,
                            modifier = Modifier.weight(1f),
                            keyHeight = keyHeight,
                            fontSize = fontSize,
                            cornerRadius = cornerRadius,
                            borderColor = colors.keyBorderColor,
                            backgroundColor = colors.keyBackground,
                            textColor = colors.keyText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Row 2
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    row2.forEachIndexed { index, char ->
                        KeyButton(
                            text = if (isUpper) char.uppercase() else char,
                            hintText = row2Hints.getOrNull(index),
                            onKeyClick = handleKeyPress,
                            modifier = Modifier.weight(1f),
                            keyHeight = keyHeight,
                            fontSize = fontSize,
                            cornerRadius = cornerRadius,
                            borderColor = colors.keyBorderColor,
                            backgroundColor = colors.keyBackground,
                            textColor = colors.keyText
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Row 3
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val shiftInteraction = remember { MutableInteractionSource() }
                    val isShiftPressed by shiftInteraction.collectIsPressedAsState()

                    // Shift Key
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(keyHeight)
                            .padding(horizontal = 1.5.dp)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(
                                if (isShiftPressed) colors.specialKeyBackground.copy(alpha = 0.65f)
                                else colors.specialKeyBackground
                            )
                            .then(
                                if (colors.keyBorderColor != Color.Transparent)
                                    Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                                else Modifier
                            )
                            .clickable(
                                interactionSource = shiftInteraction,
                                indication = null
                            ) { handleShiftClick() }
                            .testTag("key_shift"),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = if (layoutState == KeyboardLayoutState.QWERTY_CAPS_LOCK) {
                            Icons.Default.KeyboardCapslock
                        } else {
                            Icons.Default.ArrowUpward
                        }
                        val iconColor = if (layoutState != KeyboardLayoutState.QWERTY_LOWER) {
                            colors.textHighlight
                        } else {
                            colors.specialKeyText
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Shift",
                            tint = iconColor,
                            modifier = Modifier.size((heightStyle.keyHeightDp * 0.42).dp.coerceIn(18.dp, 24.dp))
                        )
                    }

                    row3.forEachIndexed { index, char ->
                        KeyButton(
                            text = if (isUpper) char.uppercase() else char,
                            hintText = row3Hints.getOrNull(index),
                            onKeyClick = handleKeyPress,
                            modifier = Modifier.weight(1f),
                            keyHeight = keyHeight,
                            fontSize = fontSize,
                            cornerRadius = cornerRadius,
                            borderColor = colors.keyBorderColor,
                            backgroundColor = colors.keyBackground,
                            textColor = colors.keyText
                        )
                    }

                    // Backspace Key with continuous deletion on hold
                    val backspaceInteraction = remember { MutableInteractionSource() }
                    val isBackspacePressed by backspaceInteraction.collectIsPressedAsState()

                    LaunchedEffect(isBackspacePressed) {
                        if (isBackspacePressed) {
                            delay(400)
                            while (isActive) {
                                handleBackspace()
                                delay(60)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(keyHeight)
                            .padding(horizontal = 1.5.dp)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(
                                if (isBackspacePressed) colors.specialKeyBackground.copy(alpha = 0.65f)
                                else colors.specialKeyBackground
                            )
                            .then(
                                if (colors.keyBorderColor != Color.Transparent)
                                    Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                                else Modifier
                            )
                            .clickable(
                                interactionSource = backspaceInteraction,
                                indication = null
                            ) { handleBackspace() }
                            .testTag("key_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                            contentDescription = "Hapus",
                            tint = colors.specialKeyText,
                            modifier = Modifier.size((heightStyle.keyHeightDp * 0.42).dp.coerceIn(18.dp, 24.dp))
                        )
                    }
                }
            }

            KeyboardLayoutState.SYMBOLS -> {
                // Row 1 (Numbers)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    symRow1.forEach { char ->
                        KeyButton(
                            text = char,
                            onKeyClick = handleKeyPress,
                            modifier = Modifier.weight(1f),
                            keyHeight = keyHeight,
                            fontSize = fontSize,
                            cornerRadius = cornerRadius,
                            borderColor = colors.keyBorderColor,
                            backgroundColor = colors.keyBackground,
                            textColor = colors.keyText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Row 2 (Symbols 1)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    symRow2.forEach { char ->
                        KeyButton(
                            text = char,
                            onKeyClick = handleKeyPress,
                            modifier = Modifier.weight(1f),
                            keyHeight = keyHeight,
                            fontSize = fontSize,
                            cornerRadius = cornerRadius,
                            borderColor = colors.keyBorderColor,
                            backgroundColor = colors.keyBackground,
                            textColor = colors.keyText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Row 3 (Symbols 2 + Backspace)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    // Back to ABC toggle
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(keyHeight)
                            .padding(horizontal = 1.5.dp)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(colors.specialKeyBackground)
                            .then(
                                if (colors.keyBorderColor != Color.Transparent)
                                    Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                                else Modifier
                            )
                            .clickable { layoutState = KeyboardLayoutState.QWERTY_LOWER },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "abc",
                            color = colors.specialKeyText,
                            fontSize = (heightStyle.fontSizeSp - 3).coerceAtLeast(13).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    symRow3.forEach { char ->
                        KeyButton(
                            text = char,
                            onKeyClick = handleKeyPress,
                            modifier = Modifier.weight(1f),
                            keyHeight = keyHeight,
                            fontSize = fontSize,
                            cornerRadius = cornerRadius,
                            borderColor = colors.keyBorderColor,
                            backgroundColor = colors.keyBackground,
                            textColor = colors.keyText
                        )
                    }

                    // Backspace
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(keyHeight)
                            .padding(horizontal = 1.5.dp)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(colors.specialKeyBackground)
                            .then(
                                if (colors.keyBorderColor != Color.Transparent)
                                    Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                                else Modifier
                            )
                            .clickable { handleBackspace() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                            contentDescription = "Hapus",
                            tint = colors.specialKeyText,
                            modifier = Modifier.size((heightStyle.keyHeightDp * 0.42).dp.coerceIn(18.dp, 24.dp))
                        )
                    }
                }
            }

            KeyboardLayoutState.EDIT -> {
                EditKeyboardView(
                    onSpecialPress = { action -> onSpecialPress(action) },
                    onBackToAbc = { layoutState = KeyboardLayoutState.QWERTY_LOWER },
                    colors = colors,
                    keyHeight = keyHeight,
                    fontSize = fontSize,
                    cornerRadius = cornerRadius,
                    heightStyle = heightStyle
                )
            }

            KeyboardLayoutState.EMOJI -> {
                EmojiKeyboardView(
                    colors = colors,
                    keyHeight = keyHeight,
                    cornerRadius = cornerRadius,
                    onEmojiSelected = { emoji -> onSpecialPress("COMMIT:$emoji") }
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 5. Bottom Navigation & Action Bar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (layoutState == KeyboardLayoutState.EMOJI) {
                // ABC Toggle
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(colors.specialKeyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .clickable { layoutState = KeyboardLayoutState.QWERTY_LOWER },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "abc",
                        color = colors.specialKeyText,
                        fontSize = (heightStyle.fontSizeSp - 3).coerceAtLeast(13).sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ?123 Toggle
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(colors.specialKeyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .clickable { layoutState = KeyboardLayoutState.SYMBOLS },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "?123",
                        color = colors.specialKeyText,
                        fontSize = (heightStyle.fontSizeSp - 3).coerceAtLeast(12).sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Edit Toggle
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(colors.specialKeyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .clickable { handleEditToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Edit",
                        color = colors.specialKeyText,
                        fontSize = (heightStyle.fontSizeSp - 3).coerceAtLeast(12).sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Space Key
                Box(
                    modifier = Modifier
                        .weight(3.0f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(colors.keyBackground)
                        .clickable { handleSpace() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Spasi",
                        color = colors.keyText.copy(alpha = 0.6f),
                        fontSize = (heightStyle.fontSizeSp - 4).coerceAtLeast(12).sp
                    )
                }

                // Backspace
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(colors.specialKeyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .clickable { handleBackspace() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                        contentDescription = "Hapus",
                        tint = colors.specialKeyText,
                        modifier = Modifier.size((heightStyle.keyHeightDp * 0.42).dp.coerceIn(18.dp, 24.dp))
                    )
                }

                // Enter
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(colors.actionKeyBackground)
                        .clickable { handleEnter() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Enter",
                        color = colors.actionKeyText,
                        fontSize = (heightStyle.fontSizeSp - 4).coerceAtLeast(12).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                val symInteraction = remember { MutableInteractionSource() }
                val isSymPressed by symInteraction.collectIsPressedAsState()

                // Sym Toggle
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(if (isSymPressed) colors.specialKeyBackground.copy(alpha = 0.65f) else colors.specialKeyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .clickable(interactionSource = symInteraction, indication = null) { handleSymToggle() }
                        .testTag("key_symbols_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (layoutState == KeyboardLayoutState.SYMBOLS) "abc" else "?123",
                        color = colors.specialKeyText,
                        fontSize = (heightStyle.fontSizeSp - 3).coerceAtLeast(13).sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val emojiInteraction = remember { MutableInteractionSource() }
                val isEmojiPressed by emojiInteraction.collectIsPressedAsState()

                // Emoji Toggle Button
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(if (isEmojiPressed) colors.specialKeyBackground.copy(alpha = 0.65f) else colors.specialKeyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .clickable(interactionSource = emojiInteraction, indication = null) { handleEmojiToggle() }
                        .testTag("key_emoji_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SentimentSatisfiedAlt,
                        contentDescription = "Emoji",
                        tint = colors.specialKeyText,
                        modifier = Modifier.size((heightStyle.keyHeightDp * 0.42).dp.coerceIn(18.dp, 24.dp))
                    )
                }

                val editInteraction = remember { MutableInteractionSource() }
                val isEditPressed by editInteraction.collectIsPressedAsState()

                // Edit Mode Toggle Button
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(
                            if (layoutState == KeyboardLayoutState.EDIT) colors.actionKeyBackground.copy(alpha = 0.35f)
                            else if (isEditPressed) colors.specialKeyBackground.copy(alpha = 0.65f)
                            else colors.specialKeyBackground
                        )
                        .then(
                            if (colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.8.dp, colors.keyBorderColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .clickable(interactionSource = editInteraction, indication = null) { handleEditToggle() }
                        .testTag("key_edit_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Edit",
                        fontSize = (heightStyle.fontSizeSp - 3).coerceAtLeast(12).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (layoutState == KeyboardLayoutState.EDIT) colors.textHighlight else colors.specialKeyText
                    )
                }

                // Comma
                KeyButton(
                    text = ",",
                    hintText = ";",
                    onKeyClick = handleKeyPress,
                    modifier = Modifier.weight(0.8f),
                    keyHeight = keyHeight,
                    fontSize = fontSize,
                    cornerRadius = cornerRadius,
                    borderColor = colors.keyBorderColor,
                    backgroundColor = colors.keyBackground,
                    textColor = colors.keyText
                )

                // Space with drag cursor navigation & double-tap period
                var bottomDragAmount by remember { mutableFloatStateOf(0f) }
                val spaceInteraction = remember { MutableInteractionSource() }
                val isSpacePressed by spaceInteraction.collectIsPressedAsState()

                Box(
                    modifier = Modifier
                        .weight(3.5f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(if (isSpacePressed) colors.keyBackground.copy(alpha = 0.65f) else colors.keyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.8.dp, colors.keyBorderColor, RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = { bottomDragAmount = 0f },
                                onDragCancel = { bottomDragAmount = 0f },
                                onHorizontalDrag = { _, dragAmountPx ->
                                    bottomDragAmount += dragAmountPx
                                    if (bottomDragAmount > 30f) {
                                        onSpecialPress("CURSOR_RIGHT")
                                        bottomDragAmount = 0f
                                    } else if (bottomDragAmount < -30f) {
                                        onSpecialPress("CURSOR_LEFT")
                                        bottomDragAmount = 0f
                                    }
                                }
                            )
                        }
                        .clickable(interactionSource = spaceInteraction, indication = null) { handleSpace() }
                        .testTag("key_space"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Spasi",
                        color = colors.keyText.copy(alpha = 0.6f),
                        fontSize = (heightStyle.fontSizeSp - 4).coerceAtLeast(12).sp
                    )
                }

                // Period
                KeyButton(
                    text = ".",
                    hintText = "!",
                    onKeyClick = handleKeyPress,
                    modifier = Modifier.weight(0.8f),
                    keyHeight = keyHeight,
                    fontSize = fontSize,
                    cornerRadius = cornerRadius,
                    borderColor = colors.keyBorderColor,
                    backgroundColor = colors.keyBackground,
                    textColor = colors.keyText
                )

                val enterInteraction = remember { MutableInteractionSource() }
                val isEnterPressed by enterInteraction.collectIsPressedAsState()

                // Enter / Action
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(if (isEnterPressed) colors.actionKeyBackground.copy(alpha = 0.75f) else colors.actionKeyBackground)
                        .clickable(interactionSource = enterInteraction, indication = null) { handleEnter() }
                        .testTag("key_enter"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Enter",
                        color = colors.actionKeyText,
                        fontSize = (heightStyle.fontSizeSp - 4).coerceAtLeast(12).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
