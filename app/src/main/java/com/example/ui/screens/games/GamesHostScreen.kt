package com.example.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.GameDimension
import com.example.data.model.RedGameItem
import com.example.ui.components.NeonCard
import com.example.ui.components.RedGameBackdrop
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.RedNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OfflineGameArenaScreen(
    game: RedGameItem,
    onExit: () -> Unit,
    onScoreSubmitted: (score: Long, pointsEarned: Long, isWin: Boolean) -> Unit
) {
    var currentScore by remember(game.id) { mutableLongStateOf(0L) }
    var isGameOver by remember(game.id) { mutableStateOf(false) }
    var gameWon by remember(game.id) { mutableStateOf(false) }
    var restartSeed by remember(game.id) { mutableIntStateOf(0) }
    var submittedForRound by remember(game.id) { mutableStateOf(false) }

    fun finishRound(finalScore: Long, won: Boolean) {
        currentScore = finalScore
        gameWon = won
        isGameOver = true
        if (!submittedForRound) {
            submittedForRound = true
            val pts = ((finalScore / 5L) + if (won) 40L else 15L).coerceIn(15L, 250L)
            onScoreSubmitted(finalScore, pts, won)
        }
    }

    BackHandler {
        if (!submittedForRound && currentScore > 0L) {
            val pts = (currentScore / 5L + 10L).coerceIn(10L, 200L)
            onScoreSubmitted(currentScore, pts, false)
        }
        onExit()
    }

    RedGameBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            // Top Game HUD
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianElevated.copy(alpha = 0.92f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (!submittedForRound && currentScore > 0L) {
                            val pts = (currentScore / 5L + 10L).coerceIn(10L, 200L)
                            onScoreSubmitted(currentScore, pts, false)
                        }
                        onExit()
                    },
                    modifier = Modifier.testTag("game_exit_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Keluar game",
                        tint = TextPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (game.dimension == GameDimension.D3) "Engine 3D Perspektif · Offline" else "Engine 2D Arcade · Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RedNeon.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, RedNeon)
                ) {
                    Text(
                        text = "SKOR $currentScore",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldCoin,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                key(game.id, restartSeed) {
                    when (game.id) {
                        "cyber_tunnel_3d" -> NeonTunnel3DGame(
                            onScoreUpdate = { currentScore = it },
                            onGameOver = { finishRound(it, it >= 300L) }
                        )
                        "cosmic_cube_3d" -> CosmicCube3DGame(
                            onScoreUpdate = { currentScore = it },
                            onGameOver = { finishRound(it, it >= 250L) }
                        )
                        "orbital_breaker_3d" -> OrbitalBreaker3DGame(
                            onScoreUpdate = { currentScore = it },
                            onGameOver = { score, won -> finishRound(score, won) }
                        )
                        "neon_snake_2d" -> NeonSnake2DGame(
                            onScoreUpdate = { currentScore = it },
                            onGameOver = { finishRound(it, it >= 120L) }
                        )
                        "flappy_bird_2d" -> CyberJet2DGame(
                            onScoreUpdate = { currentScore = it },
                            onGameOver = { finishRound(it, it >= 100L) }
                        )
                        else -> Matrix2048Game(
                            onScoreUpdate = { currentScore = it },
                            onGameOver = { score, won -> finishRound(score, won) }
                        )
                    }
                }

                if (isGameOver) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianElevated.copy(alpha = 0.86f))
                            .padding(24.dp)
                    ) {
                        NeonCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = GoldCoin,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = if (gameWon) "KEMENANGAN!" else "PERMAINAN SELESAI",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Skor Akhir: $currentScore",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = GoldCoin
                                )
                                val earned = ((currentScore / 5L) + if (gameWon) 40L else 15L).coerceIn(15L, 250L)
                                Text(
                                    text = "+$earned Poin & +${(earned / 3L).coerceAtLeast(5L)} Koin tersimpan ke Leaderboard",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(18.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = onExit,
                                        border = BorderStroke(1.dp, RedNeon),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("gameover_exit_button")
                                    ) {
                                        Text("Kembali", color = TextPrimary)
                                    }
                                    Button(
                                        onClick = {
                                            currentScore = 0L
                                            isGameOver = false
                                            submittedForRound = false
                                            restartSeed++
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedNeon),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("gameover_replay_button")
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Main Lagi", color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
