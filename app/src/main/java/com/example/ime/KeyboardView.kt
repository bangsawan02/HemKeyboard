package com.example.ime

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.ui.theme.KeyShapeStyle
import com.example.ui.theme.KeyboardColors
import com.example.ui.theme.KeyboardHeightStyle
import com.example.ui.theme.KeyboardThemeStyle
import com.example.ui.theme.getKeyboardColors

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
    suggestionBarActionsEnabled: Boolean = true,
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

    val handleSpecialAction = remember(onSpecialPress) {
        { action: String ->
            onSpecialPress(action)
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

    var selectedEmojiCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }

    val handleEmojiToggle = remember {
        {
            layoutState = if (layoutState == KeyboardLayoutState.EMOJI) {
                KeyboardLayoutState.QWERTY_LOWER
            } else {
                KeyboardLayoutState.EMOJI
            }
        }
    }

    val emojiSmileys = remember {
        listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
            "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔",
            "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "😮‍💨",
            "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢",
            "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "🥸",
            "😎", "🤓", "🧐", "😕", "😟", "🙁", "☹️", "😮", "😯", "😲",
            "😳", "🥺", "😦", "😧", "😨", "😰", "😥", "😢", "😭", "😱",
            "😖", "😣", "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠",
            "🤬", "😈", "👿", "💀", "☠️", "💩", "🤡", "👻", "👽", "🤖"
        )
    }

    val emojiGestures = remember {
        listOf(
            "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞",
            "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️",
            "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲",
            "🤝", "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶",
            "👂", "🦻", "👃", "🫀", "🫁", "🧠", "👀", "👁️", "👅", "👄",
            "👶", "🧒", "👦", "👧", "🧑", "👨", "👩", "🧓", "👴", "👵",
            "👨‍💻", "👩‍💻", "🧑‍💻", "👨‍🏫", "👩‍🏫", "🧑‍🔬", "👨‍🎨", "👩‍🎨"
        )
    }

    val emojiStars = remember {
        listOf(
            "⭐", "🌟", "✨", "💫", "🔥", "💯", "🎉", "🎊", "🚀", "💡",
            "❤️", "👍", "👏", "🙌", "😊", "🥳", "😎", "🤩", "🎯", "🏆",
            "⚡", "☀️", "🌈", "🍀", "🍀", "🎁", "🎈", "📱", "🌐", "📌",
            "📍", "🏷️", "📦", "📫", "📬", "📧", "📨", "✉️", "🌐", "🔮"
        )
    }

    val emojiHearts = remember {
        listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
            "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝",
            "💟", "✨", "⭐️", "🌟", "💫", "⚡️", "☄️", "💥", "🔥", "💯",
            "💢", "♨️", "✅", "☑️", "✔️", "❌", "⭕", "🛑", "⛔", "📛",
            "🚫", "❓", "❔", "❕", "❗", "‼️", "⁉️", "➕", "➖", "➗",
            "🟰", "⬆️", "↗️", "➡️", "↘️", "⬇️", "↙️", "⬅️", "↖️", "🔄"
        )
    }

    val emojiAnimals = remember {
        listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨",
            "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤",
            "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛",
            "🦋", "🐌", "🐞", "🐜", "🕷️", "🦂", "🐢", "🐍", "🦎", "🐙",
            "🦑", "🦐", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬", "🐳", "🦈",
            "🐊", "🐅", "🐆", "🦓", "🦍", "🦧", "🐘", "🦛", "🦏", "🐪",
            "🐫", "🦒", "🦘", "🌱", "🌿", "☘️", "🍀", "🎍", "🪴", "🎋",
            "🍃", "🍂", "🍁", "🍄", "🌾", "💐", "🌷", "🌹", "🥀", "🌺",
            "🌸", "🌼", "🌻", "🌞", "🌝", "⭐️", "🌟", "🌙", "🌍", "🌈"
        )
    }

    val emojiFood = remember {
        listOf(
            "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐",
            "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑",
            "🥦", "🥬", "🥒", "🌶️", "🌽", "🥕", "🧄", "🧅", "🥔", "🍠",
            "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞",
            "🧇", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟", "🍕", "🥪",
            "🥙", "🧆", "🌮", "🌯", "🥗", "🥘", "🥫", "🍝", "🍜", "🍲",
            "🍛", "🍣", "🍱", "🥟", "🍤", "🍙", "🍚", "🍦", "🍧", "🍨",
            "🍩", "🍪", "🎂", "🍰", "🧁", "🥧", "🍫", "🍬", "🍭", "🍮",
            "🍯", "🥛", "🍼", "☕️", "🫖", "🍵", "🧃", "🥤", "🧋", "🍺"
        )
    }

    val emojiObjects = remember {
        listOf(
            "⚽", "🏀", "🏈", "⚾️", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱",
            "🏓", "🏸", "🏒", "🏏", "⛳️", "🏹", "🥊", "🥋", "🛹", "🛼",
            "🎽", "🏆", "🥇", "🥈", "🥉", "🎖️", "🎟️", "🎫", "🎪", "🎭",
            "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎺", "🎸",
            "🎲", "🎯", "🎳", "🎮", "🚗", "🚕", "🚙", "🚌", "🏎️", "🚓",
            "🚑", "🚒", "🚐", "🚚", "🚲", "🛵", "🏍️", "🚨", "✈️", "🚀"
        )
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

    val progRow1 = listOf("TAB", "{", "}", "[", "]", "(", ")", "<", ">", "/")
    val progRow2 = listOf("=", "+", "-", "*", "\\", "_", "$", "&", "|", "~")
    val progRow3 = listOf("!", "?", ":", ";", "\"", "'", "%", "^", "#", "@")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(vertical = 4.dp, horizontal = 3.dp)
    ) {
        // 1. Word Suggestion Bar (Komponen UI Bar Saran Kata - 3 Prediksi Kata Teratas)
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
            suggestionBarActionsEnabled = suggestionBarActionsEnabled
        )

        // 2. Baris Angka Keyboard (Dedicated Number Row 1..0) - Langsung di bawah Bar Saran Kata!
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

        // 2. Main Keyboard Layout State
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
                    onSpecialPress = { action -> handleSpecialAction(action) },
                    onBackToAbc = { layoutState = KeyboardLayoutState.QWERTY_LOWER },
                    colors = colors,
                    keyHeight = keyHeight,
                    fontSize = fontSize,
                    cornerRadius = cornerRadius,
                    heightStyle = heightStyle
                )
            }

            KeyboardLayoutState.EMOJI -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(keyHeight * 3 + 10.dp)
                ) {
                    // Category selector bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((keyHeight.value * 0.62f).coerceIn(26f, 34f).dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EmojiCategory.entries.forEach { cat ->
                            val isSelected = selectedEmojiCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                                    .background(
                                        if (isSelected) colors.actionKeyBackground.copy(alpha = 0.35f)
                                        else colors.specialKeyBackground.copy(alpha = 0.6f)
                                    )
                                    .then(
                                        if (isSelected && colors.keyBorderColor != Color.Transparent)
                                            Modifier.border(0.8.dp, colors.actionKeyBackground, RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                                        else Modifier
                                    )
                                    .clickable { selectedEmojiCategory = cat }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = cat.icon, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = cat.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.textHighlight else colors.specialKeyText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Grid of emojis
                    val currentEmojis = when (selectedEmojiCategory) {
                        EmojiCategory.SMILEYS -> emojiSmileys
                        EmojiCategory.GESTURES -> emojiGestures
                        EmojiCategory.HEARTS -> emojiHearts
                        EmojiCategory.STAR -> emojiStars
                        EmojiCategory.ANIMALS -> emojiAnimals
                        EmojiCategory.FOOD -> emojiFood
                        EmojiCategory.OBJECTS -> emojiObjects
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(cornerRadius))
                            .background(colors.keyBackground.copy(alpha = 0.25f))
                            .padding(2.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        items(currentEmojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1.1f)
                                    .padding(1.5.dp)
                                    .clip(RoundedCornerShape(cornerRadius.coerceAtMost(6.dp)))
                                    .background(colors.keyBackground.copy(alpha = 0.55f))
                                    .clickable {
                                        handleSpecialAction("COMMIT:$emoji")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 19.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Row 4 (Bottom Bar - Space, Sym Toggle, Emoji Toggle, Edit Key, Done)
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
                        fontSize = (heightStyle.fontSizeSp - 3).coerceAtLeast(12).sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.specialKeyText
                    )
                }

                // Space
                var dragAmount by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .weight(3.4f)
                        .height(keyHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(colors.keyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent)
                                Modifier.border(0.8.dp, colors.keyBorderColor, RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = { dragAmount = 0f },
                                onDragCancel = { dragAmount = 0f },
                                onHorizontalDrag = { _, dragAmountPx ->
                                    dragAmount += dragAmountPx
                                    if (dragAmount > 30f) {
                                        onSpecialPress("CURSOR_RIGHT")
                                        dragAmount = 0f
                                    } else if (dragAmount < -30f) {
                                        onSpecialPress("CURSOR_LEFT")
                                        dragAmount = 0f
                                    }
                                }
                            )
                        }
                        .clickable { handleSpace() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Spasi",
                        color = colors.keyText.copy(alpha = 0.6f),
                        fontSize = (heightStyle.fontSizeSp - 4).coerceAtLeast(12).sp
                    )
                }

                // Backspace in Emoji Mode
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

// Extension to fill out list padding
fun <T> List<T>.padEnd(size: Int, value: T): List<T> {
    if (this.size >= size) return this
    val result = this.toMutableList()
    while (result.size < size) {
        result.add(value)
    }
    return result
}

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
    suggestionBarActionsEnabled: Boolean = true,
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
        if (suggestionBarActionsEnabled) {
            // [EDIT] Button
            SuggestionBarActionButton(
                text = "EDIT",
                action = "TOGGLE_EDIT", // This will be handled by the click listener below
                onSpecialPress = { onEditToggle() },
                colors = colors,
                height = suggestionHeight,
                cornerRadius = cornerRadius
            )

            Spacer(modifier = Modifier.width(4.dp))

            // [CLIPBOARD] Button
            SuggestionBarActionButton(
                text = "CLIP",
                action = "CLIPBOARD",
                onSpecialPress = onSpecialPress,
                colors = colors,
                height = suggestionHeight,
                cornerRadius = cornerRadius
            )

            Spacer(modifier = Modifier.width(4.dp))

            // [FN] Button
            SuggestionBarActionButton(
                text = "FN",
                action = "FN",
                onSpecialPress = { /* FN */ },
                colors = colors,
                height = suggestionHeight,
                cornerRadius = cornerRadius
            )
            
            Spacer(modifier = Modifier.width(4.dp))

            // [HIDE] Button
            SuggestionBarActionButton(
                text = "HIDE",
                action = "HIDE_KEYBOARD",
                onSpecialPress = onSpecialPress,
                colors = colors,
                height = suggestionHeight,
                cornerRadius = cornerRadius
            )

            Spacer(modifier = Modifier.width(6.dp))
        } else {
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
        }

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditKeyboardView(
    onSpecialPress: (String) -> Unit,
    onBackToAbc: () -> Unit,
    colors: KeyboardColors,
    keyHeight: Dp,
    fontSize: TextUnit,
    cornerRadius: Dp,
    heightStyle: KeyboardHeightStyle
) {
    var activeTab by remember { mutableStateOf("EDIT") } // "EDIT", "CLIP", "FN"
    var isSelectionMode by remember { mutableStateOf(false) }
    var isSelectionLocked by remember { mutableStateOf(false) }

    val activeSelection = isSelectionMode || isSelectionLocked
    val rowHeight = (keyHeight.value * 0.95f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(vertical = 4.dp, horizontal = 3.dp)
    ) {
        // Header Tab Bar (matching Multiling O Keyboard Edit header: "Edit", Clipboard, "Fn", Keyboard Hide)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(colors.suggestionBackground, RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Edit Tab
                Text(
                    text = "Edit",
                    fontSize = 15.sp,
                    fontWeight = if (activeTab == "EDIT") FontWeight.Bold else FontWeight.Medium,
                    color = if (activeTab == "EDIT") colors.textHighlight else colors.keyText.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { activeTab = "EDIT" }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                        .testTag("tab_edit")
                )

                // Clipboard Icon Tab
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Clipboard",
                    tint = if (activeTab == "CLIP") colors.textHighlight else colors.keyText.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable {
                            activeTab = "CLIP"
                            onSpecialPress("PASTE")
                        }
                        .testTag("tab_clipboard")
                )

                // Fn Tab
                Text(
                    text = "Fn",
                    fontSize = 15.sp,
                    fontWeight = if (activeTab == "FN") FontWeight.Bold else FontWeight.Medium,
                    color = if (activeTab == "FN") colors.textHighlight else colors.keyText.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { activeTab = "FN" }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                        .testTag("tab_fn")
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Back to ABC button
                Text(
                    text = "abc",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textHighlight,
                    modifier = Modifier
                        .clickable { onBackToAbc() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("tab_abc")
                )

                // Dismiss Keyboard Icon Tab
                Icon(
                    imageVector = Icons.Default.KeyboardHide,
                    contentDescription = "Tutup Keyboard",
                    tint = colors.keyText.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onSpecialPress("HIDE_KEYBOARD") }
                        .testTag("tab_keyboard_hide")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (activeTab == "FN") {
            // Fn Keys Layout
            val fnRow1 = listOf("F1", "F2", "F3", "F4", "F5", "F6")
            val fnRow2 = listOf("F7", "F8", "F9", "F10", "F11", "F12")
            val fnRow3 = listOf("ESC", "TAB", "DEL", "CTRL", "ALT", "HOME")
            val fnRow4 = listOf("END", "PAGE_UP", "PAGE_DOWN", "INS", "PRTSCR", "ENTER")

            val renderFnRow = @Composable { row: List<String> ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { fnKey ->
                        EditIconButton(
                            text = fnKey,
                            action = fnKey,
                            onSpecialPress = onSpecialPress,
                            colors = colors,
                            height = rowHeight,
                            cornerRadius = cornerRadius,
                            weight = 1f,
                            testTag = "fn_$fnKey"
                        )
                    }
                }
            }

            renderFnRow(fnRow1)
            Spacer(modifier = Modifier.height(4.dp))
            renderFnRow(fnRow2)
            Spacer(modifier = Modifier.height(4.dp))
            renderFnRow(fnRow3)
            Spacer(modifier = Modifier.height(4.dp))
            renderFnRow(fnRow4)
        } else {
            // 5x4 Edit Key Matrix (matching user screenshot)
            // Row 1: Cut, Paste, Up, Copy, Select All
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                EditIconButton(icon = Icons.Default.ContentCut, text = "✂", action = "CUT", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_cut")
                EditIconButton(icon = Icons.Default.ContentPaste, text = "📋", action = "PASTE", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_paste")
                EditIconButton(icon = Icons.Default.ArrowDropUp, text = "▲", action = if (activeSelection) "SELECT_UP" else "ARROW_UP", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_up")
                EditIconButton(icon = Icons.Default.ContentCopy, text = "❐", action = "COPY", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_copy")
                EditIconButton(icon = Icons.Default.SelectAll, text = "⌨", action = "SELECT_ALL", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_select_all")
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Redo, Left, Selection Box Toggle, Right, Undo
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                EditIconButton(icon = Icons.AutoMirrored.Filled.Redo, text = "↷", action = "REDO", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_redo")
                EditIconButton(icon = Icons.Default.ArrowLeft, text = "◄", action = if (activeSelection) "SELECT_LEFT" else "ARROW_LEFT", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_left")

                // Selection Box Toggle Key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(rowHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(if (activeSelection) colors.actionKeyBackground else colors.keyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent || activeSelection)
                                Modifier.border(0.8.dp, if (activeSelection) colors.textHighlight else colors.keyBorderColor, RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .clickable { isSelectionMode = !isSelectionMode }
                        .testTag("edit_select_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CropFree,
                        contentDescription = "Mode Seleksi",
                        tint = if (activeSelection) colors.textHighlight else colors.keyText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                EditIconButton(icon = Icons.Default.ArrowRight, text = "►", action = if (activeSelection) "SELECT_RIGHT" else "ARROW_RIGHT", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_right")
                EditIconButton(icon = Icons.AutoMirrored.Filled.Undo, text = "↶", action = "UNDO", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_undo")
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 3: Page Up, Home, Down, End, Backspace
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                EditIconButton(icon = Icons.Default.VerticalAlignTop, text = "⇞", action = "PAGE_UP", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_page_up")
                EditIconButton(icon = Icons.Default.SkipPrevious, text = "|<", action = if (activeSelection) "SELECT_HOME" else "CURSOR_HOME", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_home")
                EditIconButton(icon = Icons.Default.ArrowDropDown, text = "▼", action = if (activeSelection) "SELECT_DOWN" else "ARROW_DOWN", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_down")
                EditIconButton(icon = Icons.Default.SkipNext, text = ">|", action = if (activeSelection) "SELECT_END" else "CURSOR_END", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_end")
                EditIconButton(icon = Icons.AutoMirrored.Filled.KeyboardBackspace, text = "⌫", action = "BACKSPACE", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, isSpecial = true, testTag = "edit_backspace")
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 4: Lock, Line Up, Space, Line Down, Enter
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // Lock Selection Key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(rowHeight)
                        .padding(horizontal = 1.5.dp)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(if (isSelectionLocked) Color(0xFFFF9800) else colors.keyBackground)
                        .then(
                            if (colors.keyBorderColor != Color.Transparent || isSelectionLocked)
                                Modifier.border(0.8.dp, if (isSelectionLocked) Color(0xFFFF9800) else colors.keyBorderColor, RoundedCornerShape(cornerRadius))
                            else Modifier
                        )
                        .clickable { isSelectionLocked = !isSelectionLocked }
                        .testTag("edit_lock_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelectionLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Kunci Seleksi",
                        tint = if (isSelectionLocked) Color.White else Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                }

                EditIconButton(icon = Icons.Default.KeyboardArrowUp, text = "▲", action = if (activeSelection) "SELECT_UP" else "ARROW_UP", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_move_up")
                EditIconButton(text = "_", action = "SPACE", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_space")
                EditIconButton(icon = Icons.Default.KeyboardArrowDown, text = "▼", action = if (activeSelection) "SELECT_DOWN" else "ARROW_DOWN", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, testTag = "edit_move_down")
                EditIconButton(icon = Icons.AutoMirrored.Filled.KeyboardReturn, text = "↵", action = "ENTER", onSpecialPress = onSpecialPress, colors = colors, height = rowHeight, cornerRadius = cornerRadius, weight = 1f, isAction = true, testTag = "edit_enter")
            }
        }
    }
}

@Composable
fun SuggestionBarActionButton(
    text: String,
    action: String,
    onSpecialPress: (String) -> Unit,
    colors: KeyboardColors,
    height: Dp,
    cornerRadius: Dp
) {
    Box(
        modifier = Modifier
            .height(height)
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.specialKeyBackground.copy(alpha = 0.5f))
            .clickable { onSpecialPress(action) }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colors.specialKeyText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RowScope.EditIconButton(
    icon: ImageVector? = null,
    text: String? = null,
    action: String,
    onSpecialPress: (String) -> Unit,
    colors: KeyboardColors,
    height: Dp,
    cornerRadius: Dp,
    weight: Float = 1f,
    isSpecial: Boolean = false,
    isAction: Boolean = false,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bg = when {
        isAction -> if (isPressed) colors.actionKeyBackground.copy(alpha = 0.75f) else colors.actionKeyBackground
        isSpecial -> if (isPressed) colors.specialKeyBackground.copy(alpha = 0.65f) else colors.specialKeyBackground
        else -> if (isPressed) colors.keyBackground.copy(alpha = 0.65f) else colors.keyBackground
    }

    val fg = when {
        isAction -> Color.White
        isSpecial -> colors.specialKeyText
        else -> colors.keyText
    }

    Box(
        modifier = Modifier
            .weight(weight)
            .height(height)
            .padding(horizontal = 1.5.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(bg)
            .then(
                if (colors.keyBorderColor != Color.Transparent && !isAction)
                    Modifier.border(0.8.dp, colors.keyBorderColor, RoundedCornerShape(cornerRadius))
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null) {
                onSpecialPress(action)
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = action,
                tint = fg,
                modifier = Modifier.size(20.dp)
            )
        } else if (text != null) {
            Text(
                text = text,
                color = fg,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
