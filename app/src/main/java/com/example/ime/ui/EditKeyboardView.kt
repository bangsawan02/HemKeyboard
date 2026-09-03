package com.example.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KeyboardColors
import com.example.ui.theme.KeyboardHeightStyle

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
        // Header Tab Bar (Edit, Clipboard, Fn, Keyboard Hide)
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
            // 5x4 Edit Key Matrix
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
