package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class KeyboardThemeStyle(val displayName: String) {
    LIGHT("Terang Klasik"),
    DARK("Gelap Modern"),
    AMOLED_BLACK("Hitam AMOLED"),
    WARM_PEACH("Warm Peach"),
    FOREST_MINT("Forest Mint"),
    COZY_INDIGO("Cozy Indigo"),
    CYBER_NEON("Cyber Neon"),
    SUNSET_ROSE("Sunset Rose")
}

enum class KeyboardHeightStyle(val displayName: String, val keyHeightDp: Int, val fontSizeSp: Int, val suggestionHeightDp: Int) {
    COMPACT("Pendek (42dp)", 42, 16, 38),
    NORMAL("Sedang / Standar (48dp)", 48, 18, 44),
    TALL("Tinggi (54dp)", 54, 20, 48),
    EXTRA_TALL("Ekstra Tinggi (60dp)", 60, 22, 52)
}

enum class KeyShapeStyle(val displayName: String, val cornerRadiusDp: Int) {
    SQUARE("Minimalis (3dp)", 3),
    ROUNDED("Standar Bulat (8dp)", 8),
    PILL("Kapsul / Membulat (16dp)", 16)
}

data class KeyboardColors(
    val background: Color,
    val keyBackground: Color,
    val keyText: Color,
    val keyBorderColor: Color = Color.Transparent,
    val specialKeyBackground: Color,
    val specialKeyText: Color,
    val actionKeyBackground: Color,
    val actionKeyText: Color,
    val suggestionBackground: Color,
    val suggestionText: Color,
    val textHighlight: Color
)

fun getKeyboardColors(theme: KeyboardThemeStyle): KeyboardColors {
    return when (theme) {
        KeyboardThemeStyle.LIGHT -> KeyboardColors(
            background = Color(0xFFECEFF1),
            keyBackground = Color(0xFFFFFFFF),
            keyText = Color(0xFF263238),
            keyBorderColor = Color(0xFFD3D9DE),
            specialKeyBackground = Color(0xFFCFD8DC),
            specialKeyText = Color(0xFF263238),
            actionKeyBackground = Color(0xFF1E88E5),
            actionKeyText = Color(0xFFFFFFFF),
            suggestionBackground = Color(0xFFF5F7F8),
            suggestionText = Color(0xFF37474F),
            textHighlight = Color(0xFF1E88E5)
        )
        KeyboardThemeStyle.DARK -> KeyboardColors(
            background = Color(0xFF1C1B1F),
            keyBackground = Color(0xFF313033),
            keyText = Color(0xFFE6E1E5),
            keyBorderColor = Color(0xFF49454F),
            specialKeyBackground = Color(0xFF4A4657),
            specialKeyText = Color(0xFFE6E1E5),
            actionKeyBackground = Color(0xFFD0BCFF),
            actionKeyText = Color(0xFF381E72),
            suggestionBackground = Color(0xFF252427),
            suggestionText = Color(0xFFCAC4D0),
            textHighlight = Color(0xFFD0BCFF)
        )
        KeyboardThemeStyle.AMOLED_BLACK -> KeyboardColors(
            background = Color(0xFF000000),
            keyBackground = Color(0xFF121212),
            keyText = Color(0xFFFFFFFF),
            keyBorderColor = Color(0xFF2C2C2C),
            specialKeyBackground = Color(0xFF242424),
            specialKeyText = Color(0xFFE0E0E0),
            actionKeyBackground = Color(0xFF00E676),
            actionKeyText = Color(0xFF000000),
            suggestionBackground = Color(0xFF0A0A0A),
            suggestionText = Color(0xFFB0B0B0),
            textHighlight = Color(0xFF00E676)
        )
        KeyboardThemeStyle.WARM_PEACH -> KeyboardColors(
            background = Color(0xFFFAF0E6),
            keyBackground = Color(0xFFFFF9F2),
            keyText = Color(0xFF5D4037),
            keyBorderColor = Color(0xFFEADBCE),
            specialKeyBackground = Color(0xFFEEDC9A),
            specialKeyText = Color(0xFF5D4037),
            actionKeyBackground = Color(0xFFFF8A65),
            actionKeyText = Color(0xFFFFFFFF),
            suggestionBackground = Color(0xFFFAF3E0),
            suggestionText = Color(0xFF8D6E63),
            textHighlight = Color(0xFFFF8A65)
        )
        KeyboardThemeStyle.FOREST_MINT -> KeyboardColors(
            background = Color(0xFFE8F5E9),
            keyBackground = Color(0xFFFFFFFF),
            keyText = Color(0xFF1B5E20),
            keyBorderColor = Color(0xFFC8E6C9),
            specialKeyBackground = Color(0xFFC8E6C9),
            specialKeyText = Color(0xFF1B5E20),
            actionKeyBackground = Color(0xFF4CAF50),
            actionKeyText = Color(0xFFFFFFFF),
            suggestionBackground = Color(0xFFF1F8E9),
            suggestionText = Color(0xFF2E7D32),
            textHighlight = Color(0xFF4CAF50)
        )
        KeyboardThemeStyle.COZY_INDIGO -> KeyboardColors(
            background = Color(0xFF1A1C29),
            keyBackground = Color(0xFF2A2D43),
            keyText = Color(0xFFE2E8F0),
            keyBorderColor = Color(0xFF3B4060),
            specialKeyBackground = Color(0xFF3F4462),
            specialKeyText = Color(0xFFE2E8F0),
            actionKeyBackground = Color(0xFF6366F1),
            actionKeyText = Color(0xFFFFFFFF),
            suggestionBackground = Color(0xFF212437),
            suggestionText = Color(0xFF94A3B8),
            textHighlight = Color(0xFF6366F1)
        )
        KeyboardThemeStyle.CYBER_NEON -> KeyboardColors(
            background = Color(0xFF0F172A),
            keyBackground = Color(0xFF1E293B),
            keyText = Color(0xFF38BDF8),
            keyBorderColor = Color(0xFF0EA5E9),
            specialKeyBackground = Color(0xFF334155),
            specialKeyText = Color(0xFFF43F5E),
            actionKeyBackground = Color(0xFF06B6D4),
            actionKeyText = Color(0xFF0F172A),
            suggestionBackground = Color(0xFF0B1329),
            suggestionText = Color(0xFF94A3B8),
            textHighlight = Color(0xFF38BDF8)
        )
        KeyboardThemeStyle.SUNSET_ROSE -> KeyboardColors(
            background = Color(0xFFFFF0F5),
            keyBackground = Color(0xFFFFFFFF),
            keyText = Color(0xFF880E4F),
            keyBorderColor = Color(0xFFF8BBD0),
            specialKeyBackground = Color(0xFFFCE4EC),
            specialKeyText = Color(0xFF880E4F),
            actionKeyBackground = Color(0xFFE91E63),
            actionKeyText = Color(0xFFFFFFFF),
            suggestionBackground = Color(0xFFFFF5F8),
            suggestionText = Color(0xFFAD1457),
            textHighlight = Color(0xFFE91E63)
        )
    }
}
