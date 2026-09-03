package com.example.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KeyboardColors
import com.example.util.NativeEmojiHelper

enum class EmojiCategory(val title: String, val icon: String) {
    SMILEYS("Emosi", "😀"),
    GESTURES("Tangan", "👍"),
    HEARTS("Hati", "❤️"),
    STAR("Populer", "⭐"),
    ANIMALS("Alam", "🐶"),
    FOOD("Kuliner", "🍔"),
    OBJECTS("Benda", "⚽")
}

@Composable
fun EmojiKeyboardView(
    colors: KeyboardColors,
    keyHeight: Dp,
    cornerRadius: Dp,
    onEmojiSelected: (String) -> Unit
) {
    var selectedEmojiCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }

    val rawSmileys = remember {
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

    val rawGestures = remember {
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

    val rawStars = remember {
        listOf(
            "⭐", "🌟", "✨", "💫", "🔥", "💯", "🎉", "🎊", "🚀", "💡",
            "❤️", "👍", "👏", "🙌", "😊", "🥳", "😎", "🤩", "🎯", "🏆",
            "⚡", "☀️", "🌈", "🍀", "🎁", "🎈", "📱", "🌐", "📌", "📍"
        )
    }

    val rawHearts = remember {
        listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
            "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝",
            "💟", "✨", "⭐️", "🌟", "💫", "⚡️", "☄️", "💥", "🔥", "💯",
            "💢", "♨️", "✅", "☑️", "✔️", "❌", "⭕", "🛑", "⛔", "📛"
        )
    }

    val rawAnimals = remember {
        listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨",
            "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤",
            "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛",
            "🦋", "🐌", "🐞", "🐜", "🕷️", "🦂", "🐢", "🐍", "🦎", "🐙"
        )
    }

    val rawFood = remember {
        listOf(
            "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐",
            "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑",
            "🥦", "🥬", "🥒", "🌶️", "🌽", "🥕", "🧄", "🧅", "🥔", "🍠",
            "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞",
            "🧇", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟", "🍕", "🥪"
        )
    }

    val rawObjects = remember {
        listOf(
            "⚽", "🏀", "🏈", "⚾️", "🥎", "🎾", "🏐", "🥏", "🎱", "🏓",
            "🏸", "🏒", "🏏", "⛳️", "🏹", "🥊", "🥋", "🛹", "🛼", "🎽",
            "🏆", "🥇", "🥈", "🥉", "🎖️", "🎟️", "🎫", "🎪", "🎭", "🎨",
            "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎺", "🎸", "🎲"
        )
    }

    // Filter using Native Android SDK Paint.hasGlyph to prevent missing glyph / tofu boxes
    val emojiSmileys = remember { NativeEmojiHelper.filterSupportedEmojis(rawSmileys) }
    val emojiGestures = remember { NativeEmojiHelper.filterSupportedEmojis(rawGestures) }
    val emojiStars = remember { NativeEmojiHelper.filterSupportedEmojis(rawStars) }
    val emojiHearts = remember { NativeEmojiHelper.filterSupportedEmojis(rawHearts) }
    val emojiAnimals = remember { NativeEmojiHelper.filterSupportedEmojis(rawAnimals) }
    val emojiFood = remember { NativeEmojiHelper.filterSupportedEmojis(rawFood) }
    val emojiObjects = remember { NativeEmojiHelper.filterSupportedEmojis(rawObjects) }

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
                            onEmojiSelected(emoji)
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
