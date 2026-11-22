package com.example.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.RedDarkSurface
import com.example.ui.theme.RedDeep
import com.example.ui.theme.RedNeon
import com.example.ui.theme.RedNeonBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun NeonSnake2DGame(
    onScoreUpdate: (Long) -> Unit,
    onGameOver: (Long) -> Unit
) {
    val gridSize = 18
    var snake by remember { mutableStateOf(listOf(9 to 9, 8 to 9, 7 to 9)) }
    var dir by remember { mutableStateOf(1 to 0) }
    var nextDir by remember { mutableStateOf(1 to 0) }
    var food by remember { mutableStateOf(13 to 9) }
    var score by remember { mutableLongStateOf(0L) }
    var alive by remember { mutableStateOf(true) }

    fun turn(target: Pair<Int, Int>) {
        if (target.first + dir.first != 0 || target.second + dir.second != 0) {
            nextDir = target
        }
    }

    LaunchedEffect(alive) {
        while (alive) {
            delay((155L - (score / 10L) * 4L).coerceAtLeast(75L))
            dir = nextDir
            val head = snake.first()
            val next = (head.first + dir.first) to (head.second + dir.second)
            if (next.first !in 0 until gridSize || next.second !in 0 until gridSize || next in snake) {
                alive = false
                onGameOver(score)
                break
            }
            val ate = next == food
            snake = if (ate) listOf(next) + snake else listOf(next) + snake.dropLast(1)
            if (ate) {
                score += 15L
                onScoreUpdate(score)
                var candidate: Pair<Int, Int>
                do {
                    candidate = Random.nextInt(gridSize) to Random.nextInt(gridSize)
                } while (candidate in snake)
                food = candidate
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(14.dp)
            .testTag("game_canvas_snake_2d")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        if (abs(drag.x) > abs(drag.y)) {
                            if (drag.x > 10f) turn(1 to 0) else if (drag.x < -10f) turn(-1 to 0)
                        } else {
                            if (drag.y > 10f) turn(0 to 1) else if (drag.y < -10f) turn(0 to -1)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                val cell = size.width / gridSize
                drawRoundRect(
                    color = ObsidianElevated,
                    size = size,
                    cornerRadius = CornerRadius(18f, 18f)
                )
                drawRoundRect(
                    color = RedNeon,
                    size = size,
                    cornerRadius = CornerRadius(18f, 18f),
                    style = Stroke(width = 4f)
                )
                // Food star
                drawCircle(
                    color = GoldCoin,
                    radius = cell * 0.36f,
                    center = Offset((food.first + 0.5f) * cell, (food.second + 0.5f) * cell)
                )
                // Snake body
                snake.forEachIndexed { idx, seg ->
                    val alpha = (1f - idx.toFloat() / (snake.size + 2)).coerceAtLeast(0.35f)
                    drawRoundRect(
                        color = if (idx == 0) RedNeonBright else RedNeon.copy(alpha = alpha),
                        topLeft = Offset(seg.first * cell + 2f, seg.second * cell + 2f),
                        size = Size(cell - 4f, cell - 4f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }

        // Tactile D-Pad
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconButton(
                onClick = { turn(0 to -1) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = RedDarkSurface),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Atas", tint = TextPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                FilledTonalIconButton(
                    onClick = { turn(-1 to 0) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = RedDarkSurface),
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Kiri", tint = TextPrimary)
                }
                FilledTonalIconButton(
                    onClick = { turn(1 to 0) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = RedDarkSurface),
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Kanan", tint = TextPrimary)
                }
            }
            FilledTonalIconButton(
                onClick = { turn(0 to 1) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = RedDarkSurface),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bawah", tint = TextPrimary)
            }
        }
    }
}

private data class LaserGate(var x: Float, val gapY: Float, var passed: Boolean = false)

@Composable
fun CyberJet2DGame(
    onScoreUpdate: (Long) -> Unit,
    onGameOver: (Long) -> Unit
) {
    var jetY by remember { mutableFloatStateOf(0.45f) }
    var velY by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableLongStateOf(0L) }
    var alive by remember { mutableStateOf(true) }
    val gates = remember { mutableStateListOf<LaserGate>() }

    LaunchedEffect(alive) {
        if (!alive) return@LaunchedEffect
        var last = withFrameNanos { it }
        var spawnTimer = 0.8f
        while (alive) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.04f)
            last = now

            velY += 1.65f * dt
            jetY += velY * dt
            if (jetY < 0.04f || jetY > 0.96f) {
                alive = false
                onGameOver(score)
                break
            }

            spawnTimer += dt
            if (spawnTimer >= 1.35f) {
                spawnTimer = 0f
                gates.add(LaserGate(x = 1.1f, gapY = 0.25f + Random.nextFloat() * 0.50f))
            }

            val it = gates.iterator()
            while (it.hasNext()) {
                val g = it.next()
                g.x -= 0.42f * dt
                if (!g.passed && g.x < 0.26f) {
                    g.passed = true
                    score += 15L
                    onScoreUpdate(score)
                }
                if (abs(g.x - 0.26f) < 0.055f && abs(jetY - g.gapY) > 0.135f) {
                    alive = false
                    onGameOver(score)
                    break
                }
                if (g.x < -0.15f) it.remove()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("game_canvas_jet_2d")
            .pointerInput(Unit) {
                detectTapGestures {
                    if (alive) velY = -0.58f
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            gates.forEach { g ->
                val gx = g.x * w
                val gw = w * 0.09f
                val gapHalf = h * 0.145f
                val topH = (g.gapY * h - gapHalf).coerceAtLeast(0f)
                val botY = (g.gapY * h + gapHalf).coerceAtMost(h)
                drawRect(EmeraldSecure, Offset(gx - gw / 2f, 0f), Size(gw, topH))
                drawRect(EmeraldSecure, Offset(gx - gw / 2f, botY), Size(gw, h - botY))
            }
            val jx = w * 0.26f
            val jy = jetY * h
            drawCircle(color = RedNeon, radius = 26f, center = Offset(jx, jy))
            drawCircle(color = GoldCoin, radius = 12f, center = Offset(jx + 10f, jy))
        }
        Text(
            text = "Ketuk layar berulang kali untuk terbang melewati gerbang laser",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        )
    }
}

@Composable
fun Matrix2048Game(
    onScoreUpdate: (Long) -> Unit,
    onGameOver: (Long, Boolean) -> Unit
) {
    var board by remember { mutableStateOf(List(16) { 0 }) }
    var score by remember { mutableLongStateOf(0L) }
    var finished by remember { mutableStateOf(false) }

    fun spawnTile(current: List<Int>): List<Int> {
        val empty = current.indices.filter { current[it] == 0 }
        if (empty.isEmpty()) return current
        val next = current.toMutableList()
        next[empty.random()] = if (Random.nextFloat() < 0.85f) 2 else 4
        return next
    }

    LaunchedEffect(Unit) {
        board = spawnTile(spawnTile(List(16) { 0 }))
    }

    fun compressLine(line: List<Int>): Pair<List<Int>, Long> {
        val nonZero = line.filter { it != 0 }
        val result = mutableListOf<Int>()
        var gained = 0L
        var i = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val merged = nonZero[i] * 2
                result.add(merged)
                gained += merged.toLong()
                i += 2
            } else {
                result.add(nonZero[i])
                i += 1
            }
        }
        while (result.size < 4) result.add(0)
        return result to gained
    }

    fun canMove(b: List<Int>): Boolean {
        if (0 in b) return true
        for (r in 0..3) {
            for (c in 0..3) {
                val v = b[r * 4 + c]
                if (c < 3 && b[r * 4 + c + 1] == v) return true
                if (r < 3 && b[(r + 1) * 4 + c] == v) return true
            }
        }
        return false
    }

    fun move(dx: Int, dy: Int) {
        if (finished) return
        val next = board.toMutableList()
        var gainedTotal = 0L
        if (dx != 0) {
            for (r in 0..3) {
                val row = (0..3).map { c -> board[r * 4 + c] }
                val ordered = if (dx > 0) row.reversed() else row
                val (merged, gained) = compressLine(ordered)
                val finalRow = if (dx > 0) merged.reversed() else merged
                gainedTotal += gained
                for (c in 0..3) next[r * 4 + c] = finalRow[c]
            }
        } else {
            for (c in 0..3) {
                val col = (0..3).map { r -> board[r * 4 + c] }
                val ordered = if (dy > 0) col.reversed() else col
                val (merged, gained) = compressLine(ordered)
                val finalCol = if (dy > 0) merged.reversed() else merged
                gainedTotal += gained
                for (r in 0..3) next[r * 4 + c] = finalCol[r]
            }
        }

        if (next != board) {
            val spawned = spawnTile(next)
            board = spawned
            score += gainedTotal
            onScoreUpdate(score)
            if (2048 in spawned) {
                finished = true
                onGameOver(score, true)
            } else if (!canMove(spawned)) {
                finished = true
                onGameOver(score, score >= 400L)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(16.dp)
            .testTag("game_canvas_2048")
            .pointerInput(Unit) {
                var accumX = 0f
                var accumY = 0f
                var handled = false
                detectDragGestures(
                    onDragStart = { accumX = 0f; accumY = 0f; handled = false },
                    onDrag = { change, drag ->
                        change.consume()
                        accumX += drag.x
                        accumY += drag.y
                        if (!handled && (abs(accumX) > 48f || abs(accumY) > 48f)) {
                            handled = true
                            if (abs(accumX) > abs(accumY)) move(if (accumX > 0) 1 else -1, 0)
                            else move(0, if (accumY > 0) 1 else -1)
                        }
                    }
                )
            }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(ObsidianElevated, RoundedCornerShape(20.dp))
                .border(BorderStroke(1.5.dp, RedNeon), RoundedCornerShape(20.dp))
                .padding(10.dp)
        ) {
            for (r in 0..3) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    for (c in 0..3) {
                        val value = board[r * 4 + c]
                        val tileColor = when (value) {
                            0 -> Color.White.copy(alpha = 0.05f)
                            2, 4 -> RedDarkSurface
                            8, 16 -> RedDeep
                            32, 64 -> RedNeon
                            128, 256 -> GoldAmber
                            else -> GoldCoin
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(tileColor, RoundedCornerShape(12.dp))
                        ) {
                            if (value > 0) {
                                Text(
                                    text = "$value",
                                    fontSize = if (value >= 100) 20.sp else 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (value >= 512) ObsidianBg else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Geser kotak ke atas, bawah, kiri, atau kanan untuk menggabungkan angka",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
