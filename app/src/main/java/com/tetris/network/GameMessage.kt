package com.tetris.network

import kotlinx.serialization.Serializable

/**
 * Base class for all network messages
 */
@Serializable
sealed class GameMessage {
    /**
     * Player ready to start
     */
    @Serializable
    data class PlayerReady(val playerName: String) : GameMessage()

    /**
     * Game start countdown
     */
    @Serializable
    data class GameStart(val timestamp: Long) : GameMessage()

    /**
     * Piece movement update
     */
    @Serializable
    data class PieceUpdate(
        val x: Int,
        val y: Int,
        val rotation: Int,
        val pieceType: String
    ) : GameMessage()

    /**
     * Piece locked into board
     */
    @Serializable
    data class PieceLocked(
        val timestamp: Long
    ) : GameMessage()

    /**
     * Lines cleared - send garbage to opponent
     */
    @Serializable
    data class LinesCleared(
        val count: Int
    ) : GameMessage()

    /**
     * Receive garbage lines from opponent
     */
    @Serializable
    data class GarbageReceived(
        val count: Int
    ) : GameMessage()

    /**
     * Game stats update
     */
    @Serializable
    data class StatsUpdate(
        val score: Int,
        val level: Int,
        val linesCleared: Int
    ) : GameMessage()

    /**
     * Board state update - sends the entire board grid
     */
    @Serializable
    data class BoardUpdate(
        val board: List<List<Int?>>  // Grid of color values (ARGB as Int, null for empty)
    ) : GameMessage()

    /**
     * Current piece update - sends the currently falling piece
     */
    @Serializable
    data class CurrentPieceUpdate(
        val pieceType: String,          // TetrominoType name (I, O, T, S, Z, J, L)
        val shape: List<List<Int>>,     // Current shape (after rotation)
        val colorInt: Int,              // Color as ARGB Int
        val x: Int,                     // X position
        val y: Int                      // Y position
    ) : GameMessage()

    /**
     * Next piece update - sends the next piece in queue
     */
    @Serializable
    data class NextPieceUpdate(
        val pieceType: String,          // TetrominoType name (I, O, T, S, Z, J, L)
        val colorInt: Int               // Color as ARGB Int
    ) : GameMessage()

    /**
     * Game over notification
     */
    @Serializable
    data class GameOver(
        val score: Int,
        val level: Int,
        val linesCleared: Int
    ) : GameMessage()

    /**
     * Player wants to play again
     */
    @Serializable
    object PlayAgainRequest : GameMessage()

    /**
     * Player left the game (back to menu)
     */
    @Serializable
    object PlayerLeftGame : GameMessage()

    /**
     * Player disconnected
     */
    @Serializable
    object PlayerDisconnected : GameMessage()

    /**
     * Keep alive ping
     */
    @Serializable
    data class Ping(val timestamp: Long) : GameMessage()

    /**
     * Keep alive pong response
     */
    @Serializable
    data class Pong(val timestamp: Long) : GameMessage()
}
