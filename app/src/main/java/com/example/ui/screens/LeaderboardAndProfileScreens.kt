package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GameScore
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PrivateVault
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.security.SecurityCryptoEngine
import com.example.ui.components.NeonCard
import com.example.ui.theme.BorderRedGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.RedDarkSurface
import com.example.ui.theme.RedNeon
import com.example.ui.theme.RedNeonBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun RankScreen(
    currentUserId: String,
    profile: UserProfile?,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text("Leaderboard Global", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                "Kompetisi poin real-time antar pemain RedGame di seluruh dunia.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Player achievements summary
        item {
            val pts = profile?.totalPoints ?: 0L
            val plays = profile?.gamesPlayed ?: 0L
            val wins = profile?.wins ?: 0L
            val badges = listOf(
                Triple("Pemula Arena", "Mainkan 1 game", plays >= 1L),
                Triple("Kolektor 100 Poin", "Kumpulkan 100 poin", pts >= 100L),
                Triple("Juara Mabar", "Raih 1 kemenangan", wins >= 1L),
                Triple("Legenda RedGame", "Capai 500 poin", pts >= 500L)
            )
            NeonCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Capaian Saya", style = MaterialTheme.typography.titleMedium, color = GoldCoin)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        badges.forEach { (title, _, unlocked) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (unlocked) RedNeon.copy(alpha = 0.25f) else ObsidianElevated,
                                border = BorderStroke(1.dp, if (unlocked) RedNeon else BorderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.EmojiEvents,
                                        contentDescription = title,
                                        tint = if (unlocked) GoldCoin else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = title.split(" ").first(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (unlocked) TextPrimary else TextMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        when (leaderboardState) {
            is UiState.Loading -> item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    CircularProgressIndicator(color = RedNeon)
                }
            }
            is UiState.Error -> item {
                Text(leaderboardState.message, color = RedNeonBright)
            }
            is UiState.Success -> {
                val entries = leaderboardState.data
                if (entries.isEmpty()) {
                    item {
                        NeonCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Belum ada skor tercatat. Mainkan game pertamamu untuk naik ke peringkat #1!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    itemsIndexed(entries, key = { _, e -> e.userId }) { idx, entry ->
                        val rank = idx + 1
                        val isMe = entry.userId == currentUserId
                        val rankColor = when (rank) {
                            1 -> GoldCoin
                            2 -> TextPrimary
                            3 -> GoldAmber
                            else -> TextSecondary
                        }
                        NeonCard(
                            cornerRadius = 18.dp,
                            borderColor = if (isMe) CyberCyan else BorderRedGlow.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("leaderboard_row_$rank")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (rank <= 3) rankColor.copy(alpha = 0.2f) else ObsidianElevated)
                                ) {
                                    Text(
                                        text = "#$rank",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = rankColor
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(RedDarkSurface)
                                ) {
                                    Text(
                                        text = entry.avatarTag.take(2),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.displayName + if (isMe) " (Kamu)" else "",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isMe) CyberCyan else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Favorit: ${entry.favoriteGame} · ${entry.wins} Menang",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${entry.totalPoints}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = GoldCoin
                                    )
                                    Text("POIN", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    currentUserId: String,
    profile: UserProfile?,
    vaultState: UiState<PrivateVault?>,
    scoresState: UiState<List<GameScore>>,
    decryptVault: (PrivateVault) -> String,
    onRotateKey: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDecryptedVault by remember { mutableStateOf(false) }
    val hardwareKeystore = remember(currentUserId) { SecurityCryptoEngine.isHardwareKeystoreActive(currentUserId) }
    val recentScores = (scoresState as? UiState.Success)?.data.orEmpty().take(8)

    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            NeonCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(18.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(RedNeon)
                    ) {
                        Text(
                            text = profile?.avatarTag ?: "RG",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.displayName ?: "RedPlayer",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Level ${profile?.level ?: 1} · ${profile?.totalPoints ?: 0} Poin · ${profile?.coins ?: 150} Koin",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GoldCoin
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = EmeraldSecure, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Terverifikasi Google Sign-In", style = MaterialTheme.typography.labelSmall, color = EmeraldSecure)
                        }
                    }
                }
            }
        }

        // Security & Encryption Vault Inspector
        item {
            val vault = (vaultState as? UiState.Success)?.data
            NeonCard(
                borderColor = EmeraldSecure.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = EmeraldSecure)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Brankas Privasi & Enkripsi",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Android Keystore Hardware: ${if (hardwareKeystore) "AKTIF (AES-256-GCM)" else "Perangkat Lunak AES-256-GCM"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldSecure
                    )
                    if (vault != null) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Algoritma: ${vault.algorithm}", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                                Text("IV (Base64): ${vault.iv}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("SHA-256: ${vault.integrityHash.take(28)}…", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "Ciphertext: ${vault.cipherText.take(36)}…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                if (showDecryptedVault) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Terdekripsi Lokal: ${decryptVault(vault)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GoldCoin
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showDecryptedVault = !showDecryptedVault },
                                border = BorderStroke(1.dp, EmeraldSecure),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("toggle_decrypt_vault_button")
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = EmeraldSecure, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (showDecryptedVault) "Tutup" else "Dekripsi", color = TextPrimary, fontSize = 12.sp)
                            }
                            Button(
                                onClick = onRotateKey,
                                colors = ButtonDefaults.buttonColors(containerColor = ObsidianElevated),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("rotate_vault_key_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Rotasi Kunci", color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Text("Menginisialisasi brankas terenkripsi…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }
        }

        // Recent Game Scores
        item {
            Text("Riwayat Skor Terbaru", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        if (recentScores.isEmpty()) {
            item {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Belum ada riwayat permainan. Mainkan game 3D atau 2D untuk mencatat poin pertamamu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentScores, key = { it.id }) { s ->
                NeonCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column {
                            Text("${s.gameTitle} (${s.dimension})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Mode: ${s.mode.uppercase()} · Skor ${s.score}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text("+${s.pointsEarned} PTS", style = MaterialTheme.typography.labelLarge, color = GoldCoin)
                    }
                }
            }
        }

        // Sign out button
        item {
            Button(
                onClick = onSignOut,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedNeon),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sign_out), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
