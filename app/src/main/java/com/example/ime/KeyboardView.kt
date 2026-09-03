package com.example.ime

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

enum class KeyboardMode {
    FULL,
    ONE_HANDED_LEFT,
    ONE_HANDED_RIGHT,
    FLOATING
}

sealed class KeyboardLayoutState {
    object QWERTY_LOWER : KeyboardLayoutState()
    object QWERTY_UPPER : KeyboardLayoutState()
    object QWERTY_CAPS_LOCK : KeyboardLayoutState()
    object SYMBOLS : KeyboardLayoutState()
    object EDIT : KeyboardLayoutState()
    object EMOJI : KeyboardLayoutState()
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
    autoCapitalizeNext: Boolean = false,
    currentWord: String = "",
    actionLabel: String = "Enter",
    clipboardText: String? = null,
    isVoiceListening: Boolean = false,
    onVoiceClick: () -> Unit = {},
    inlineSuggestionViews: List<View> = emptyList(),
    onSwitchIme: () -> Unit = {},
    enableKeyPreview: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = getKeyboardColors(activeTheme)
    var layoutState by remember { mutableStateOf<KeyboardLayoutState>(KeyboardLayoutState.QWERTY_LOWER) }
    var keyboardMode by remember { mutableStateOf(KeyboardMode.FULL) }

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

    val handleBackToAbc = remember {
        {
            layoutState = KeyboardLayoutState.QWERTY_LOWER
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

    // Main Layout container with One-Handed Mode and Floating Mode styling
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = when (keyboardMode) {
                KeyboardMode.ONE_HANDED_LEFT -> Arrangement.Start
                KeyboardMode.ONE_HANDED_RIGHT -> Arrangement.End
                KeyboardMode.FLOATING -> Arrangement.Center
                KeyboardMode.FULL -> Arrangement.Start
            }
        ) {
            // One Handed Left: Right control strip
            if (keyboardMode == KeyboardMode.ONE_HANDED_LEFT) {
                Column(
                    modifier = Modifier
                        .weight(0.80f)
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    KeyboardMainContent(
                        colors = colors,
                        layoutState = layoutState,
                        keyHeight = keyHeight,
                        fontSize = fontSize,
                        cornerRadius = cornerRadius,
                        suggestionHeight = suggestionHeight,
                        heightStyle = heightStyle,
                        predictions = predictions,
                        currentWord = currentWord,
                        onPredictionClick = onPredictionClick,
                        onSpecialPress = onSpecialPress,
                        handleKeyPress = handleKeyPress,
                        handleBackspace = handleBackspace,
                        handleSpace = handleSpace,
                        handleEnter = handleEnter,
                        handleShiftClick = handleShiftClick,
                        handleSymToggle = handleSymToggle,
                        handleEditToggle = handleEditToggle,
                        handleEmojiToggle = handleEmojiToggle,
                        onBackToAbc = handleBackToAbc,
                        onVoiceClick = onVoiceClick,
                        isVoiceListening = isVoiceListening,
                        clipboardText = clipboardText,
                        inlineSuggestionViews = inlineSuggestionViews,
                        onSwitchIme = onSwitchIme,
                        actionLabel = actionLabel,
                        row1 = row1,
                        row1Hints = row1Hints,
                        row2 = row2,
                        row2Hints = row2Hints,
                        row3 = row3,
                        row3Hints = row3Hints,
                        symRow1 = symRow1,
                        symRow2 = symRow2,
                        symRow3 = symRow3,
                        enableKeyPreview = enableKeyPreview
                    )
                }

                OneHandedSideBar(
                    modifier = Modifier.weight(0.20f),
                    colors = colors,
                    onExpandFull = { keyboardMode = KeyboardMode.FULL },
                    onSwitchSide = { keyboardMode = KeyboardMode.ONE_HANDED_RIGHT },
                    onToggleFloating = { keyboardMode = KeyboardMode.FLOATING }
                )
            } else if (keyboardMode == KeyboardMode.ONE_HANDED_RIGHT) {
                OneHandedSideBar(
                    modifier = Modifier.weight(0.20f),
                    colors = colors,
                    onExpandFull = { keyboardMode = KeyboardMode.FULL },
                    onSwitchSide = { keyboardMode = KeyboardMode.ONE_HANDED_LEFT },
                    onToggleFloating = { keyboardMode = KeyboardMode.FLOATING }
                )

                Column(
                    modifier = Modifier
                        .weight(0.80f)
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    KeyboardMainContent(
                        colors = colors,
                        layoutState = layoutState,
                        keyHeight = keyHeight,
                        fontSize = fontSize,
                        cornerRadius = cornerRadius,
                        suggestionHeight = suggestionHeight,
                        heightStyle = heightStyle,
                        predictions = predictions,
                        currentWord = currentWord,
                        onPredictionClick = onPredictionClick,
                        onSpecialPress = onSpecialPress,
                        handleKeyPress = handleKeyPress,
                        handleBackspace = handleBackspace,
                        handleSpace = handleSpace,
                        handleEnter = handleEnter,
                        handleShiftClick = handleShiftClick,
                        handleSymToggle = handleSymToggle,
                        handleEditToggle = handleEditToggle,
                        handleEmojiToggle = handleEmojiToggle,
                        onBackToAbc = handleBackToAbc,
                        onVoiceClick = onVoiceClick,
                        isVoiceListening = isVoiceListening,
                        clipboardText = clipboardText,
                        inlineSuggestionViews = inlineSuggestionViews,
                        onSwitchIme = onSwitchIme,
                        actionLabel = actionLabel,
                        row1 = row1,
                        row1Hints = row1Hints,
                        row2 = row2,
                        row2Hints = row2Hints,
                        row3 = row3,
                        row3Hints = row3Hints,
                        symRow1 = symRow1,
                        symRow2 = symRow2,
                        symRow3 = symRow3,
                        enableKeyPreview = enableKeyPreview
                    )
                }
            } else if (keyboardMode == KeyboardMode.FLOATING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(vertical = 6.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.background)
                        .border(1.dp, colors.keyBorderColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .background(colors.specialKeyBackground.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Keyboard Melayang (Floating)",
                                fontSize = 11.sp,
                                color = colors.specialKeyText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Layar Penuh",
                                    tint = colors.textHighlight,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { keyboardMode = KeyboardMode.FULL }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        KeyboardMainContent(
                            colors = colors,
                            layoutState = layoutState,
                            keyHeight = (keyHeight.value * 0.9f).dp,
                            fontSize = (fontSize.value * 0.9f).sp,
                            cornerRadius = cornerRadius,
                            suggestionHeight = suggestionHeight,
                            heightStyle = heightStyle,
                            predictions = predictions,
                            currentWord = currentWord,
                            onPredictionClick = onPredictionClick,
                            onSpecialPress = onSpecialPress,
                            handleKeyPress = handleKeyPress,
                            handleBackspace = handleBackspace,
                            handleSpace = handleSpace,
                            handleEnter = handleEnter,
                            handleShiftClick = handleShiftClick,
                            handleSymToggle = handleSymToggle,
                            handleEditToggle = handleEditToggle,
                            handleEmojiToggle = handleEmojiToggle,
                            onBackToAbc = handleBackToAbc,
                            onVoiceClick = onVoiceClick,
                            isVoiceListening = isVoiceListening,
                            clipboardText = clipboardText,
                            inlineSuggestionViews = inlineSuggestionViews,
                            onSwitchIme = onSwitchIme,
                            actionLabel = actionLabel,
                            row1 = row1,
                            row1Hints = row1Hints,
                            row2 = row2,
                            row2Hints = row2Hints,
                            row3 = row3,
                            row3Hints = row3Hints,
                            symRow1 = symRow1,
                            symRow2 = symRow2,
                            symRow3 = symRow3,
                            enableKeyPreview = enableKeyPreview
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 3.dp)
                ) {
                    KeyboardMainContent(
                        colors = colors,
                        layoutState = layoutState,
                        keyHeight = keyHeight,
                        fontSize = fontSize,
                        cornerRadius = cornerRadius,
                        suggestionHeight = suggestionHeight,
                        heightStyle = heightStyle,
                        predictions = predictions,
                        currentWord = currentWord,
                        onPredictionClick = onPredictionClick,
                        onSpecialPress = onSpecialPress,
                        handleKeyPress = handleKeyPress,
                        handleBackspace = handleBackspace,
                        handleSpace = handleSpace,
                        handleEnter = handleEnter,
                        handleShiftClick = handleShiftClick,
                        handleSymToggle = handleSymToggle,
                        handleEditToggle = handleEditToggle,
                        handleEmojiToggle = handleEmojiToggle,
                        onBackToAbc = handleBackToAbc,
                        onVoiceClick = onVoiceClick,
                        isVoiceListening = isVoiceListening,
                        clipboardText = clipboardText,
                        inlineSuggestionViews = inlineSuggestionViews,
                        onSwitchIme = onSwitchIme,
                        onToggleOneHanded = { keyboardMode = KeyboardMode.ONE_HANDED_RIGHT },
                        actionLabel = actionLabel,
                        row1 = row1,
                        row1Hints = row1Hints,
                        row2 = row2,
                        row2Hints = row2Hints,
                        row3 = row3,
                        row3Hints = row3Hints,
                        symRow1 = symRow1,
                        symRow2 = symRow2,
                        symRow3 = symRow3,
                        enableKeyPreview = enableKeyPreview
                    )
                }
            }
        }
    }
}

@Composable
private fun OneHandedSideBar(
    modifier: Modifier = Modifier,
    colors: KeyboardColors,
    onExpandFull: () -> Unit,
    onSwitchSide: () -> Unit,
    onToggleFloating: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(colors.specialKeyBackground.copy(alpha = 0.4f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onExpandFull) {
            Icon(
                imageVector = Icons.Default.Fullscreen,
                contentDescription = "Layar Penuh",
                tint = colors.textHighlight
            )
        }

        IconButton(onClick = onSwitchSide) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Pindah Sisi",
                tint = colors.specialKeyText
            )
        }

        IconButton(onClick = onToggleFloating) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Mode Melayang",
                tint = colors.specialKeyText
            )
        }
    }
}

@Composable
private fun KeyboardMainContent(
    colors: KeyboardColors,
    layoutState: KeyboardLayoutState,
    keyHeight: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    cornerRadius: androidx.compose.ui.unit.Dp,
    suggestionHeight: androidx.compose.ui.unit.Dp,
    heightStyle: KeyboardHeightStyle,
    predictions: List<String>,
    currentWord: String,
    onPredictionClick: (String) -> Unit,
    onSpecialPress: (String) -> Unit,
    handleKeyPress: (String) -> Unit,
    handleBackspace: () -> Unit,
    handleSpace: () -> Unit,
    handleEnter: () -> Unit,
    handleShiftClick: () -> Unit,
    handleSymToggle: () -> Unit,
    handleEditToggle: () -> Unit,
    handleEmojiToggle: () -> Unit,
    onBackToAbc: () -> Unit,
    onVoiceClick: () -> Unit,
    isVoiceListening: Boolean,
    clipboardText: String?,
    inlineSuggestionViews: List<View>,
    onSwitchIme: () -> Unit,
    onToggleOneHanded: (() -> Unit)? = null,
    actionLabel: String,
    row1: List<String>,
    row1Hints: List<String>,
    row2: List<String>,
    row2Hints: List<String>,
    row3: List<String>,
    row3Hints: List<String>,
    symRow1: List<String>,
    symRow2: List<String>,
    symRow3: List<String>,
    enableKeyPreview: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
            onEmojiToggle = handleEmojiToggle,
            onVoiceClick = onVoiceClick,
            isVoiceListening = isVoiceListening,
            clipboardText = clipboardText,
            onClipboardPaste = { text -> onSpecialPress("COMMIT:$text") },
            inlineSuggestionViews = inlineSuggestionViews,
            onSwitchIme = onSwitchIme
        )

        Spacer(modifier = Modifier.height(4.dp))

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
                            textColor = colors.keyText,
                            enableKeyPreview = enableKeyPreview
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
                            textColor = colors.keyText,
                            enableKeyPreview = enableKeyPreview
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Row 3
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val shiftInteraction = remember { MutableInteractionSource() }
                    val isShiftPressed by shiftInteraction.collectIsPressedAsState()

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
                            textColor = colors.keyText,
                            enableKeyPreview = enableKeyPreview
                        )
                    }

                    val backspaceInteraction = remember { MutableInteractionSource() }
                    val isBackspacePressed by backspaceInteraction.collectIsPressedAsState()
                    var backspaceDragAmount by remember { mutableFloatStateOf(0f) }

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
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = { backspaceDragAmount = 0f },
                                    onDragCancel = { backspaceDragAmount = 0f },
                                    onHorizontalDrag = { _, dragAmountPx ->
                                        backspaceDragAmount += dragAmountPx
                                        if (backspaceDragAmount < -45f) {
                                            onSpecialPress("DELETE_WORD")
                                            backspaceDragAmount = 0f
                                        }
                                    }
                                )
                            }
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
                            textColor = colors.keyText,
                            enableKeyPreview = enableKeyPreview
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

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
                            textColor = colors.keyText,
                            enableKeyPreview = enableKeyPreview
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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
                            .clickable { onBackToAbc() },
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
                            textColor = colors.keyText,
                            enableKeyPreview = enableKeyPreview
                        )
                    }

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
                    onBackToAbc = { onBackToAbc() },
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

        // Bottom Navigation & Space Action Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val symInteraction = remember { MutableInteractionSource() }
            val isSymPressed by symInteraction.collectIsPressedAsState()

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

            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .height(keyHeight)
                    .padding(horizontal = 1.5.dp)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(colors.specialKeyBackground)
                    .clickable { onSwitchIme() }
                    .testTag("key_globe_switch"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Ganti Bahasa / Keyboard",
                    tint = colors.specialKeyText,
                    modifier = Modifier.size((heightStyle.keyHeightDp * 0.40).dp.coerceIn(18.dp, 22.dp))
                )
            }

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
                textColor = colors.keyText,
                enableKeyPreview = enableKeyPreview
            )

            var bottomDragAmount by remember { mutableFloatStateOf(0f) }
            val spaceInteraction = remember { MutableInteractionSource() }
            val isSpacePressed by spaceInteraction.collectIsPressedAsState()

            Box(
                modifier = Modifier
                    .weight(3.4f)
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
                textColor = colors.keyText,
                enableKeyPreview = enableKeyPreview
            )

            val enterInteraction = remember { MutableInteractionSource() }
            val isEnterPressed by enterInteraction.collectIsPressedAsState()

            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(keyHeight)
                    .padding(horizontal = 1.5.dp)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(if (isEnterPressed) colors.actionKeyBackground.copy(alpha = 0.75f) else colors.actionKeyBackground)
                    .clickable(interactionSource = enterInteraction, indication = null) { handleEnter() }
                    .testTag("key_enter"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    color = colors.actionKeyText,
                    fontSize = (heightStyle.fontSizeSp - 4).coerceAtLeast(12).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
