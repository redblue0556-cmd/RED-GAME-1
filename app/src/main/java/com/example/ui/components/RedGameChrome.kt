package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RedGameItem
import com.example.ui.theme.BorderRedGlow
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.Exo2FontFamily
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldTextDark
import com.example.ui.theme.ManropeFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.RedDarkSurface
import com.example.ui.theme.RedDeep
import com.example.ui.theme.RedNeon
import com.example.ui.theme.RedNeonBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class RedGameTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    GAMES(R.string.nav_games, Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, "nav_games"),
    MABAR(R.string.nav_mabar, Icons.Filled.Groups, Icons.Outlined.Groups, "nav_mabar"),
    RANK(R.string.nav_rank, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_rank"),
    PROFILE(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

@Composable
fun RedGameBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .drawBehind {
                // Top-left red neon radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(RedNeon.copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.maxDimension * 0.65f
                    ),
                    radius = size.maxDimension * 0.65f,
                    center = Offset(0f, 0f)
                )
                // Bottom-right crimson ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(RedNeon.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = size.maxDimension * 0.6f
                    ),
                    radius = size.maxDimension * 0.6f,
                    center = Offset(size.width, size.height)
                )
                // Diagonal cyber streaks
                drawLine(
                    color = RedNeon.copy(alpha = 0.14f),
                    start = Offset(size.width * 0.28f, 0f),
                    end = Offset(size.width * 0.04f, size.height),
                    strokeWidth = 3f
                )
                drawLine(
                    color = RedNeon.copy(alpha = 0.10f),
                    start = Offset(size.width * 0.85f, 0f),
                    end = Offset(size.width * 0.56f, size.height),
                    strokeWidth = 2.5f
                )
            }
    ) {
        content()
    }
}

@Composable
fun RedGameLogo(modifier: Modifier = Modifier.size(46.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Gold crown
        val crown = Path().apply {
            moveTo(w * 0.20f, h * 0.26f)
            lineTo(w * 0.28f, h * 0.06f)
            lineTo(w * 0.39f, h * 0.19f)
            lineTo(w * 0.50f, h * 0.03f)
            lineTo(w * 0.61f, h * 0.19f)
            lineTo(w * 0.72f, h * 0.06f)
            lineTo(w * 0.80f, h * 0.26f)
            close()
        }
        drawPath(crown, color = GoldCoin)

        // Neon red controller body
        val pad = Path().apply {
            moveTo(w * 0.18f, h * 0.42f)
            lineTo(w * 0.82f, h * 0.42f)
            cubicTo(w * 0.94f, h * 0.42f, w * 0.99f, h * 0.72f, w * 0.95f, h * 0.88f)
            cubicTo(w * 0.92f, h * 0.97f, w * 0.84f, h * 0.97f, w * 0.79f, h * 0.90f)
            lineTo(w * 0.70f, h * 0.76f)
            lineTo(w * 0.30f, h * 0.76f)
            lineTo(w * 0.21f, h * 0.90f)
            cubicTo(w * 0.16f, h * 0.97f, w * 0.08f, h * 0.97f, w * 0.05f, h * 0.88f)
            cubicTo(w * 0.01f, h * 0.72f, w * 0.06f, h * 0.42f, w * 0.18f, h * 0.42f)
            close()
        }
        drawPath(pad, color = RedNeon)

        // D-pad
        drawLine(
            color = TextPrimary,
            start = Offset(w * 0.33f, h * 0.52f),
            end = Offset(w * 0.33f, h * 0.68f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = TextPrimary,
            start = Offset(w * 0.25f, h * 0.60f),
            end = Offset(w * 0.41f, h * 0.60f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )

        // Buttons
        drawCircle(TextPrimary, radius = w * 0.045f, center = Offset(w * 0.68f, h * 0.54f))
        drawCircle(TextPrimary, radius = w * 0.045f, center = Offset(w * 0.77f, h * 0.63f))
        drawCircle(TextPrimary, radius = w * 0.045f, center = Offset(w * 0.59f, h * 0.63f))
    }
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    borderColor: Color = BorderRedGlow,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(RedDarkSurface, ObsidianCard)
            ),
            shape = shape
        )
        .border(BorderStroke(1.2.dp, borderColor), shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Column(modifier = baseModifier, content = content)
}

@Composable
fun RedGameHeader(
    avatarTag: String,
    coins: Long,
    totalPoints: Long,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showSearchBar: Boolean,
    onToggleFilter: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_ring")
    val ringAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart),
        label = "ring_spin"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(RedDarkSurface.copy(alpha = 0.96f), ObsidianBg.copy(alpha = 0.92f))
                    )
                )
                .border(
                    width = 1.dp,
                    color = BorderRedGlow.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)
                )
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RedGameLogo(modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = TextPrimary)) { append("RED") }
                            withStyle(SpanStyle(color = RedNeon)) { append("GAME") }
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp),
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = stringResource(R.string.security_badge),
                            tint = EmeraldSecure,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "AES-256 · $totalPoints PTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                letterSpacing = 0.8.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Avatar with animated neon ring + coin badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("header_avatar_button")
                        .clickable(onClick = onProfileClick)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .drawBehind {
                                rotate(ringAngle) {
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            listOf(RedNeon, RedNeonBright, RedDeep, RedNeon)
                                        ),
                                        radius = size.minDimension / 2f,
                                        style = Stroke(width = 4.dp.toPx())
                                    )
                                }
                            }
                            .clip(CircleShape)
                            .background(ObsidianElevated)
                    ) {
                        Text(
                            text = avatarTag.take(2),
                            style = TextStyle(
                                fontFamily = Exo2FontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 7.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Brush.verticalGradient(listOf(GoldCoin, GoldAmber)))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "$coins",
                            style = TextStyle(
                                fontFamily = ManropeFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                color = GoldTextDark
                            )
                        )
                    }
                }
            }

            if (showSearchBar) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.2.dp, BorderRedGlow.copy(alpha = 0.7f), RoundedCornerShape(99.dp))
                            .padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Cari",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                                cursorBrush = SolidColor(RedNeon),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input")
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(RedDarkSurface, ObsidianCard)))
                            .border(1.2.dp, BorderRedGlow.copy(alpha = 0.75f), CircleShape)
                            .testTag("filter_toggle_button")
                            .clickable(onClick = onToggleFilter)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Filter Genre",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RedGameBottomBar(
    currentTab: RedGameTab,
    onSelectTab: (RedGameTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = RedGameTab.entries
    val selectedIndex = tabs.indexOf(currentTab).coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ObsidianBg.copy(alpha = 0.96f))
                .border(1.3.dp, BorderRedGlow.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                .padding(6.dp)
        ) {
            val itemWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
                label = "nav_pill"
            )

            // Sliding glowing neon pill
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .height(54.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(RedNeon.copy(alpha = 0.34f), RedNeon.copy(alpha = 0.08f))
                        )
                    )
                    .border(1.2.dp, RedNeon.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEach { tab ->
                    val selected = tab == currentTab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .testTag(tab.tag)
                            .clickable { onSelectTab(tab) }
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = stringResource(tab.labelRes),
                            tint = if (selected) RedNeonBright else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) RedNeonBright else TextSecondary
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProceduralGameCover(
    game: RedGameItem,
    modifier: Modifier = Modifier
) {
    val primary = Color.hsl(game.accentHue, 0.85f, 0.45f)
    val dark = Color.hsl(game.accentHue, 0.78f, 0.09f)
    val glow = Color.hsl((game.accentHue + 35f) % 360f, 0.95f, 0.62f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(dark, primary.copy(alpha = 0.65f), ObsidianCard),
                start = Offset.Zero,
                end = Offset(w, h)
            )
        )

        when (game.id) {
            "cyber_tunnel_3d" -> {
                // 3D perspective tunnel rings
                val cx = w * 0.55f
                val cy = h * 0.52f
                for (i in 1..6) {
                    val scale = i / 6f
                    val rw = w * 0.78f * scale
                    val rh = h * 0.78f * scale
                    drawRoundRect(
                        color = glow.copy(alpha = 0.25f + scale * 0.55f),
                        topLeft = Offset(cx - rw / 2f, cy - rh / 2f),
                        size = Size(rw, rh),
                        cornerRadius = CornerRadius(12f * scale, 12f * scale),
                        style = Stroke(width = 3f)
                    )
                }
            }
            "cosmic_cube_3d" -> {
                // Isometric wireframe cube
                val cx = w * 0.54f
                val cy = h * 0.52f
                val r = minOf(w, h) * 0.28f
                val front = Path().apply {
                    moveTo(cx - r * 0.7f, cy - r * 0.4f)
                    lineTo(cx + r * 0.3f, cy - r * 0.7f)
                    lineTo(cx + r * 0.7f, cy + r * 0.3f)
                    lineTo(cx - r * 0.3f, cy + r * 0.6f)
                    close()
                }
                drawPath(front, color = CyberPurple.copy(alpha = 0.35f))
                drawPath(front, color = glow, style = Stroke(width = 4f))
                drawCircle(GoldCoin, radius = r * 0.16f, center = Offset(cx, cy))
            }
            "orbital_breaker_3d" -> {
                val rows = 3
                val cols = 4
                val bw = w * 0.16f
                val bh = h * 0.09f
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val x = w * 0.16f + c * (bw + 8f) + r * 6f
                        val y = h * 0.18f + r * (bh + 8f)
                        drawRoundRect(
                            color = if ((r + c) % 2 == 0) CyberCyan else RedNeon,
                            topLeft = Offset(x, y),
                            size = Size(bw, bh),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
                drawCircle(GoldCoin, radius = 10f, center = Offset(w * 0.55f, h * 0.65f))
                drawRoundRect(
                    color = TextPrimary,
                    topLeft = Offset(w * 0.32f, h * 0.80f),
                    size = Size(w * 0.36f, 10f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }
            "neon_snake_2d" -> {
                val segs = listOf(0.25f to 0.65f, 0.38f to 0.65f, 0.51f to 0.65f, 0.51f to 0.48f, 0.65f to 0.48f)
                segs.forEachIndexed { idx, (px, py) ->
                    drawRoundRect(
                        color = RedNeon.copy(alpha = 0.45f + idx * 0.11f),
                        topLeft = Offset(w * px, h * py),
                        size = Size(w * 0.11f, w * 0.11f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
                drawCircle(GoldCoin, radius = w * 0.06f, center = Offset(w * 0.78f, h * 0.30f))
            }
            "flappy_bird_2d" -> {
                drawRect(EmeraldSecure.copy(alpha = 0.75f), topLeft = Offset(w * 0.62f, 0f), size = Size(w * 0.13f, h * 0.35f))
                drawRect(EmeraldSecure.copy(alpha = 0.75f), topLeft = Offset(w * 0.62f, h * 0.62f), size = Size(w * 0.13f, h * 0.38f))
                drawCircle(GoldCoin, radius = w * 0.11f, center = Offset(w * 0.32f, h * 0.48f))
            }
            "matrix_2048_2d" -> {
                val cell = minOf(w, h) * 0.24f
                val sx = w * 0.22f
                val sy = h * 0.22f
                for (r in 0..1) {
                    for (c in 0..1) {
                        drawRoundRect(
                            color = if (r == 1 && c == 1) GoldCoin else RedNeon.copy(alpha = 0.75f),
                            topLeft = Offset(sx + c * (cell + 10f), sy + r * (cell + 10f)),
                            size = Size(cell, cell),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
            }
            else -> {
                // Mabar XO / Cyber Duel sparks
                val r = minOf(w, h) * 0.18f
                drawLine(RedNeonBright, Offset(w * 0.25f, h * 0.32f), Offset(w * 0.45f, h * 0.62f), strokeWidth = 9f, cap = StrokeCap.Round)
                drawLine(RedNeonBright, Offset(w * 0.45f, h * 0.32f), Offset(w * 0.25f, h * 0.62f), strokeWidth = 9f, cap = StrokeCap.Round)
                drawCircle(CyberCyan, radius = r, center = Offset(w * 0.72f, h * 0.48f), style = Stroke(width = 9f))
            }
        }
    }
}
