package com.tetris.ui.theme

import androidx.compose.ui.graphics.Color
import com.tetris.game.TetrominoType

/**
 * Theme definition for Tetris
 */
data class TetrisTheme(
    val name: String,
    val background: Color,
    val gridBorder: Color,
    val blockBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHighlight: Color,
    val textDanger: Color,
    val overlay: Color,
    val shapeColors: Map<TetrominoType, Color>
)

/**
 * Minimalistic Theme (Classic Black)
 */
val MinimalisticTheme = TetrisTheme(
    name = "Minimalistic",
    background = Color(0xFF000000),
    gridBorder = Color(0xFF282828),
    blockBorder = Color(0xFFFFFFFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF808080),
    textHighlight = Color(0xFFFFD700), // Gold (war grün)
    textDanger = Color(0xFFFFD700),    // Gold (war rot)
    overlay = Color(0xFF000000),
    shapeColors = mapOf(
        TetrominoType.I to Color(0xFF00FFFF), // Cyan
        TetrominoType.O to Color(0xFFFFFF00), // Yellow
        TetrominoType.T to Color(0xFFFF00FF), // Purple
        TetrominoType.S to Color(0xFF00FF00), // Green
        TetrominoType.Z to Color(0xFFFF0000), // Red
        TetrominoType.J to Color(0xFF0000FF), // Blue
        TetrominoType.L to Color(0xFFFFA500)  // Orange
    )
)

/**
 * Tron Theme (Futuristic Neon)
 */
val TronTheme = TetrisTheme(
    name = "Tron",
    background = Color(0xFF0A0E27),
    gridBorder = Color(0xFF00D4FF),
    blockBorder = Color(0xFF00D4FF),
    textPrimary = Color(0xFF00D4FF),
    textSecondary = Color(0xFF4A5F7F),
    textHighlight = Color(0xFF00FFD4),
    textDanger = Color(0xFFFF006E),
    overlay = Color(0xFF0A0E27),
    shapeColors = mapOf(
        TetrominoType.I to Color(0xFF00D4FF),
        TetrominoType.O to Color(0xFFFFD400),
        TetrominoType.T to Color(0xFFFF00D4),
        TetrominoType.S to Color(0xFF00FFD4),
        TetrominoType.Z to Color(0xFFFF006E),
        TetrominoType.J to Color(0xFF006EFF),
        TetrominoType.L to Color(0xFFFF8C00)
    )
)

/**
 * New York Theme (Elegant Gold)
 */
val NewYorkTheme = TetrisTheme(
    name = "New York",
    background = Color(0xFF1A1A1A),
    gridBorder = Color(0xFFD4AF37),
    blockBorder = Color(0xFFD4AF37),
    textPrimary = Color(0xFFD4AF37),
    textSecondary = Color(0xFF8B7355),
    textHighlight = Color(0xFFFFD700),
    textDanger = Color(0xFFDC143C),
    overlay = Color(0xFF1A1A1A),
    shapeColors = mapOf(
        TetrominoType.I to Color(0xFF4FC3F7),
        TetrominoType.O to Color(0xFFFFEB3B),
        TetrominoType.T to Color(0xFFBA68C8),
        TetrominoType.S to Color(0xFF81C784),
        TetrominoType.Z to Color(0xFFE57373),
        TetrominoType.J to Color(0xFF64B5F6),
        TetrominoType.L to Color(0xFFFFB74D)
    )
)

/**
 * Art Deco Theme (Retro Elegance)
 */
val ArtDecoTheme = TetrisTheme(
    name = "Art Deco",
    background = Color(0xFF2B1810),
    gridBorder = Color(0xFFB8860B),
    blockBorder = Color(0xFFD4AF37),
    textPrimary = Color(0xFFD4AF37),
    textSecondary = Color(0xFF8B7355),
    textHighlight = Color(0xFFFFD700),
    textDanger = Color(0xFFDC143C),
    overlay = Color(0xFF2B1810),
    shapeColors = mapOf(
        TetrominoType.I to Color(0xFF5DADE2),
        TetrominoType.O to Color(0xFFF4D03F),
        TetrominoType.T to Color(0xFFAF7AC5),
        TetrominoType.S to Color(0xFF7DCEA0),
        TetrominoType.Z to Color(0xFFEC7063),
        TetrominoType.J to Color(0xFF5499C7),
        TetrominoType.L to Color(0xFFF39C12)
    )
)

/**
 * Available themes
 */
val AllThemes = listOf(
    MinimalisticTheme,
    TronTheme,
    NewYorkTheme,
    ArtDecoTheme
)
