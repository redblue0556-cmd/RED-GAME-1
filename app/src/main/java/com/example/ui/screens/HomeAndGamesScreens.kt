package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GameConnectivity
import com.example.data.model.GameDimension
import com.example.data.model.RedGameCatalog
import com.example.data.model.RedGameItem
import com.example.data.model.UserProfile
import com.example.ui.components.NeonCard
import com.example.ui.components.ProceduralGameCover
import com.example.ui.components.RedGameTab
import com.example.ui.theme.BorderRedGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.RedDarkSurface
import com.example.ui.theme.RedDeep
import com.example.ui.theme.RedNeon
import com.example.ui.theme.RedNeonBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

val GAME_FILTER_CHIPS = listOf("Semua", "3D", "2D", "Offline", "Online", "Racing", "Action", "Arcade")

fun filterGames(query: String, selectedChip: String): List<RedGameItem> {
    val q = query.trim().lowercase()
    return RedGameCatalog.allGames.filter { game ->
        val matchesQuery = q.isEmpty() ||
            game.title.lowercase().contains(q) ||
            game.genre.lowercase().contains(q) ||
            game.subtitle.lowercase().contains(q)
        val matchesChip = when (selectedChip) {
            "Semua" -> true
            "3D" -> game.dimension == GameDimension.D3
            "2D" -> game.dimension == GameDimension.D2
            "Offline" -> game.connectivity == GameConnectivity.OFFLINE
            "Online" -> game.connectivity == GameConnectivity.ONLINE
            else -> game.genre.equals(selectedChip, ignoreCase = true)
        }
        matchesQuery && matchesChip
    }
}

@Composable
fun HomeScreen(
    profile: UserProfile?,
    searchQuery: String,
    selectedFilter: String,
    showFilterBar: Boolean,
    onSelectFilter: (String) -> Unit,
    onLaunchGame: (RedGameItem) -> Unit,
    onNavigateTab: (RedGameTab) -> Unit,
    onClaimDailyReward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val games = remember(searchQuery, selectedFilter) { filterGames(searchQuery, selectedFilter) }
    val heroPool = remember(games) { if (games.isNotEmpty()) games.take(3) else RedGameCatalog.allGames.take(3) }
    var heroIndex by remember { mutableIntStateOf(0) }
    val heroGame = heroPool[heroIndex % heroPool.size]
    val featuredGame = games.firstOrNull { it.isHot && it.id != heroGame.id } ?: games.lastOrNull()

    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        if (showFilterBar) {
            item {
                FilterChipStrip(
                    selected = selectedFilter,
                    onSelect = onSelectFilter
                )
            }
        }

        // Hero Banner
        item {
            HeroShowcaseBanner(
                game = heroGame,
                totalSlides = heroPool.size,
                activeSlide = heroIndex % heroPool.size,
                onSelectSlide = { heroIndex = it },
                onPlayClick = { onLaunchGame(heroGame) }
            )
        }

        // Quick Action Tiles (6 tiles matching REDGAME aesthetic)
        item {
            val todayStr = remember {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            }
            val dailyReady = profile?.dailyClaimedDate != todayStr
            QuickActionTileRow(
                dailyReady = dailyReady,
                onDailyClick = onClaimDailyReward,
                on3DClick = {
                    onSelectFilter("3D")
                    onNavigateTab(RedGameTab.GAMES)
                },
                onOfflineClick = {
                    onSelectFilter("Offline")
                    onNavigateTab(RedGameTab.GAMES)
                },
                onMabarClick = { onNavigateTab(RedGameTab.MABAR) },
                onRankClick = { onNavigateTab(RedGameTab.RANK) },
                onVaultClick = { onNavigateTab(RedGameTab.PROFILE) }
            )
        }

        // Trending Horizontal Rail
        item {
            SectionHeaderRow(
                title = "Game 3D & 2D Populer",
                actionLabel = "Lihat Semua",
                onActionClick = { onNavigateTab(RedGameTab.GAMES) }
            )
            Spacer(Modifier.height(10.dp))
            if (games.isEmpty()) {
                Text(
                    text = "Tidak ada game yang cocok dengan pencarianmu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(games, key = { _, item -> item.id }) { idx, game ->
                        TrendingGameCard(
                            game = game,
                            isTopOne = idx == 0,
                            onPlay = { onLaunchGame(game) },
                            modifier = Modifier.width(164.dp)
                        )
                    }
                }
            }
        }

        // Featured Hot Game Card
        if (featuredGame != null) {
            item {
                SectionHeaderRow(
                    title = "Pilihan Arena Utama",
                    actionLabel = "Semua Game",
                    onActionClick = { onNavigateTab(RedGameTab.GAMES) }
                )
                Spacer(Modifier.height(10.dp))
                FeaturedGameBanner(
                    game = featuredGame,
                    onPlay = { onLaunchGame(featuredGame) }
                )
            }
        }
    }
}

@Composable
fun GamesCatalogScreen(
    searchQuery: String,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onLaunchGame: (RedGameItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val games = remember(searchQuery, selectedFilter) { filterGames(searchQuery, selectedFilter) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 680.dp) 3 else 2
        val rows = games.chunked(columns)

        LazyColumn(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Katalog Game 3D & 2D",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Mainkan mode Offline berpoin atau Mabar Online real-time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                FilterChipStrip(selected = selectedFilter, onSelect = onSelectFilter)
            }

            if (rows.isEmpty()) {
                item {
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Game tidak ditemukan",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Coba ubah filter atau kata kunci pencarian.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(rows) { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { game ->
                            TrendingGameCard(
                                game = game,
                                isTopOne = false,
                                onPlay = { onLaunchGame(game) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipStrip(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        GAME_FILTER_CHIPS.forEach { chip ->
            val isSelected = chip == selected
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (isSelected) RedNeon else Color.White.copy(alpha = 0.08f),
                border = if (isSelected) BorderStroke(1.dp, RedNeonBright) else null,
                modifier = Modifier
                    .height(36.dp)
                    .testTag("filter_chip_$chip")
                    .clickable { onSelect(chip) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroShowcaseBanner(
    game: RedGameItem,
    totalSlides: Int,
    activeSlide: Int,
    onSelectSlide: (Int) -> Unit,
    onPlayClick: () -> Unit
) {
    NeonCard(
        cornerRadius = 26.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp)
        ) {
            ProceduralGameCover(
                game = game,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(ObsidianBg.copy(alpha = 0.94f), ObsidianBg.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RedDeep
                ) {
                    Text(
                        text = "${if (game.dimension == GameDimension.D3) "3D ENGINE" else "2D ARCADE"} · ${game.connectivity.name}",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth(0.75f)) {
                    Text(
                        text = game.title.uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                        color = TextPrimary,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = game.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onPlayClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedNeon),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("hero_play_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.play_now), style = MaterialTheme.typography.labelLarge)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until totalSlides) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (i == activeSlide) RedNeon else TextMuted)
                                    .clickable { onSelectSlide(i) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTileRow(
    dailyReady: Boolean,
    onDailyClick: () -> Unit,
    on3DClick: () -> Unit,
    onOfflineClick: () -> Unit,
    onMabarClick: () -> Unit,
    onRankClick: () -> Unit,
    onVaultClick: () -> Unit
) {
    data class TileSpec(
        val label: String,
        val icon: ImageVector,
        val tint: Color,
        val badge: Boolean = false,
        val tag: String,
        val onClick: () -> Unit
    )
    val tiles = listOf(
        TileSpec("Daily\nReward", Icons.Filled.CardGiftcard, GoldCoin, dailyReady, "tile_daily", onDailyClick),
        TileSpec("Arena\n3D", Icons.Filled.ViewInAr, CyberCyan, false, "tile_3d", on3DClick),
        TileSpec("Mode\nOffline", Icons.Filled.WifiOff, RedNeonBright, false, "tile_offline", onOfflineClick),
        TileSpec("Mabar\nOnline", Icons.Filled.Groups, EmeraldSecure, false, "tile_mabar", onMabarClick),
        TileSpec("Global\nRank", Icons.Filled.EmojiEvents, GoldCoin, false, "tile_rank", onRankClick),
        TileSpec("Brankas\nAES", Icons.Filled.Lock, EmeraldSecure, false, "tile_vault", onVaultClick)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        tiles.forEach { tile ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.92f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(ObsidianElevated, ObsidianBg)))
                    .clickable(onClick = tile.onClick)
                    .testTag(tile.tag)
                    .padding(4.dp)
            ) {
                if (tile.badge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(RedNeon)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = tile.icon,
                        contentDescription = tile.label.replace("\n", " "),
                        tint = tile.tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tile.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, lineHeight = 11.sp),
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        TextButton(onClick = onActionClick) {
            Text(
                text = "$actionLabel ›",
                style = MaterialTheme.typography.labelLarge,
                color = RedNeonBright
            )
        }
    }
}

@Composable
fun TrendingGameCard(
    game: RedGameItem,
    isTopOne: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeonCard(
        cornerRadius = 20.dp,
        borderColor = BorderRedGlow.copy(alpha = 0.7f),
        onClick = onPlay,
        modifier = modifier.testTag("game_card_${game.id}")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                ProceduralGameCover(game = game, modifier = Modifier.fillMaxSize())
                Surface(
                    shape = RoundedCornerShape(bottomStart = 10.dp),
                    color = if (game.dimension == GameDimension.D3) RedNeon else CyberCyan,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = if (isTopOne) "NO.1 · 3D" else if (game.dimension == GameDimension.D3) "3D" else "2D",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (game.dimension == GameDimension.D3) TextPrimary else ObsidianBg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = GoldCoin, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(game.rating, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                }
                Text(
                    text = if (game.connectivity == GameConnectivity.ONLINE) "Online" else "Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (game.connectivity == GameConnectivity.ONLINE) EmeraldSecure else TextSecondary
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onPlay,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedNeon),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("play_button_${game.id}")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Main", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun FeaturedGameBanner(
    game: RedGameItem,
    onPlay: () -> Unit
) {
    NeonCard(
        cornerRadius = 22.dp,
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(136.dp)
                    .fillMaxSize()
            ) {
                ProceduralGameCover(game = game, modifier = Modifier.fillMaxSize())
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    color = RedNeon,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "HOT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = game.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = game.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianBg,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = game.rewardPtsPerPlay,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldCoin,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Button(
                        onClick = onPlay,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedNeon),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Main", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
