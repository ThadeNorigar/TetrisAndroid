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
     * Game over notification
     */
    @Serializable
    data class GameOver(
        val score: Int,
        val level: Int,
        val linesCleared: Int
    ) : GameMessage()

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
