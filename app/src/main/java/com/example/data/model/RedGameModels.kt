package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val avatarTag: String = "RG",
    val totalPoints: Long = 0L,
    val coins: Long = 150L,
    val gamesPlayed: Long = 0L,
    val wins: Long = 0L,
    val level: Long = 1L,
    val dailyClaimedDate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateVault(
    @DocumentId val id: String = "",
    val userId: String = "",
    val cipherText: String = "",
    val iv: String = "",
    val algorithm: String = "AES-256-GCM",
    val integrityHash: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameScore(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameId: String = "",
    val gameTitle: String = "",
    val mode: String = "offline",
    val dimension: String = "2D",
    val score: Long = 0L,
    val pointsEarned: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val avatarTag: String = "RG",
    val totalPoints: Long = 0L,
    val wins: Long = 0L,
    val gamesPlayed: Long = 0L,
    val favoriteGame: String = "Neon Tunnel 3D",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MatchRoom(
    @DocumentId val id: String = "",
    val roomCode: String = "",
    val gameType: String = "TICTACTOE_3X3",
    val hostUid: String = "",
    val hostName: String = "",
    val guestUid: String? = null,
    val guestName: String? = null,
    val memberUids: List<String> = emptyList(),
    val status: String = "waiting",
    val boardState: String = ".........",
    val turnUid: String = "",
    val winnerUid: String? = null,
    val roomSalt: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val cipherText: String = "",
    val iv: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class GameDimension { D3, D2 }
enum class GameConnectivity { OFFLINE, ONLINE }

data class RedGameItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val genre: String,
    val dimension: GameDimension,
    val connectivity: GameConnectivity,
    val rating: String,
    val rewardPtsPerPlay: String,
    val accentHue: Float,
    val isHot: Boolean = false
)

object RedGameCatalog {
    val allGames: List<RedGameItem> = listOf(
        RedGameItem(
            id = "cyber_tunnel_3d",
            title = "Neon Tunnel 3D",
            subtitle = "Melaju di lorong 3D futuristik & hindari penghalang laser",
            genre = "Racing",
            dimension = GameDimension.D3,
            connectivity = GameConnectivity.OFFLINE,
            rating = "4.9",
            rewardPtsPerPlay = "+25~200 Poin",
            accentHue = 350f,
            isHot = true
        ),
        RedGameItem(
            id = "cosmic_cube_3d",
            title = "Cosmic Cube 3D",
            subtitle = "Kendalikan pesawat tempur menembus kubus kristal 3D",
            genre = "Action",
            dimension = GameDimension.D3,
            connectivity = GameConnectivity.OFFLINE,
            rating = "4.8",
            rewardPtsPerPlay = "+25~180 Poin",
            accentHue = 285f,
            isHot = true
        ),
        RedGameItem(
            id = "orbital_breaker_3d",
            title = "Orbital Breaker 3D",
            subtitle = "Hancurkan blok energi 3D dalam perspektif ruang angkasa",
            genre = "Arcade",
            dimension = GameDimension.D3,
            connectivity = GameConnectivity.OFFLINE,
            rating = "4.8",
            rewardPtsPerPlay = "+20~160 Poin",
            accentHue = 195f
        ),
        RedGameItem(
            id = "neon_snake_2d",
            title = "Neon Snake 2D",
            subtitle = "Game klasik REDGAME: makan bintang emas & raih skor tertinggi",
            genre = "Arcade",
            dimension = GameDimension.D2,
            connectivity = GameConnectivity.OFFLINE,
            rating = "4.9",
            rewardPtsPerPlay = "+15~150 Poin",
            accentHue = 345f
        ),
        RedGameItem(
            id = "flappy_bird_2d",
            title = "Cyber Jet 2D",
            subtitle = "Terbang lincah melewati gerbang neon tanpa menabrak",
            genre = "Action",
            dimension = GameDimension.D2,
            connectivity = GameConnectivity.OFFLINE,
            rating = "4.7",
            rewardPtsPerPlay = "+15~140 Poin",
            accentHue = 140f
        ),
        RedGameItem(
            id = "matrix_2048_2d",
            title = "2048 Red Matrix",
            subtitle = "Gabungkan blok energi merah hingga mencapai angka 2048",
            genre = "Arcade",
            dimension = GameDimension.D2,
            connectivity = GameConnectivity.OFFLINE,
            rating = "4.8",
            rewardPtsPerPlay = "+20~200 Poin",
            accentHue = 28f
        ),
        RedGameItem(
            id = "mabar_tictactoe",
            title = "Mabar Arena XO",
            subtitle = "Duel strategi 3x3 real-time dengan chat terenkripsi E2E",
            genre = "Strategy",
            dimension = GameDimension.D2,
            connectivity = GameConnectivity.ONLINE,
            rating = "4.9",
            rewardPtsPerPlay = "+100 Poin Menang",
            accentHue = 355f,
            isHot = true
        ),
        RedGameItem(
            id = "mabar_cyber_duel",
            title = "Mabar Cyber Duel",
            subtitle = "Adu refleks cepat real-time melawan teman & chat live",
            genre = "Action",
            dimension = GameDimension.D3,
            connectivity = GameConnectivity.ONLINE,
            rating = "4.9",
            rewardPtsPerPlay = "+120 Poin Menang",
            accentHue = 215f,
            isHot = true
        )
    )
}
