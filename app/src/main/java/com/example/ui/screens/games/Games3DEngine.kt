package com.example.ui.screens.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RedNeon
import com.example.ui.theme.RedNeonBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class TunnelObject(
    var x: Float,
    var y: Float,
    var z: Float,
    val isCore: Boolean
)

@Composable
fun NeonTunnel3DGame(
    onScoreUpdate: (Long) -> Unit,
    onGameOver: (Long) -> Unit
) {
    var playerX by remember { mutableFloatStateOf(0f) }
    var targetX by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableLongStateOf(0L) }
    var ringPhase by remember { mutableFloatStateOf(0f) }
    var alive by remember { mutableStateOf(true) }
    val objects = remember { mutableStateListOf<TunnelObject>() }

    LaunchedEffect(alive) {
        if (!alive) return@LaunchedEffect
        var lastTime = withFrameNanos { it }
        var spawnTimer = 0f
        while (alive) {
            val now = withFrameNanos { it }
            val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now

            val speed = 18f + (score / 40f).coerceAtMost(24f)
            ringPhase = (ringPhase + dt * 3.5f) % 1f
            playerX += (targetX - playerX) * (dt * 11f).coerceAtMost(1f)

            spawnTimer += dt
            if (spawnTimer >= 0.42f) {
                spawnTimer = 0f
                val lane = (Random.nextInt(-2, 3)) * 0.55f
                val isCore = Random.nextFloat() < 0.35f
                objects.add(TunnelObject(x = lane, y = 0.48f, z = 24f, isCore = isCore))
            }

            val iterator = objects.iterator()
            var gained = false
            while (iterator.hasNext()) {
                val obj = iterator.next()
                obj.z -= speed * dt
                if (obj.z in 1.2f..2.1f && abs(obj.x - playerX) < 0.36f) {
                    if (obj.isCore) {
                        score += 25L
                        gained = true
                        iterator.remove()
                    } else {
                        alive = false
                        onGameOver(score)
                        break
                    }
                } else if (obj.z < 0.6f) {
                    if (!obj.isCore) {
                        score += 5L
                        gained = true
                    }
                    iterator.remove()
                }
            }
            if (gained) onScoreUpdate(score)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("game_canvas_tunnel_3d")
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    targetX = (targetX + dragAmount.x / (size.width * 0.25f)).coerceIn(-1.2f, 1.2f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val dir = if (offset.x < size.width / 2f) -0.45f else 0.45f
                    targetX = (targetX + dir).coerceIn(-1.2f, 1.2f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.36f
            val focal = minOf(w, h) * 0.65f

            // Perspective tunnel rings
            for (i in 0 until 12) {
                val z = ((i + (1f - ringPhase)) * 2f).coerceAtLeast(0.8f)
                val s = focal / z
                val rw = s * 2.8f
                val rh = s * 1.9f
                val alpha = (1f - z / 26f).coerceIn(0.08f, 0.85f)
                drawRoundRect(
                    color = RedNeon.copy(alpha = alpha),
                    topLeft = Offset(cx - rw / 2f, cy - rh / 2f),
                    size = Size(rw, rh),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 3f)
                )
            }

            // Perspective floor rails
            for (lane in -2..2) {
                val lx = lane * 0.55f
                val farX = cx + (lx * focal) / 24f
                val farY = cy + (0.65f * focal) / 24f
                val nearX = cx + (lx * focal) / 1.1f
                val nearY = cy + (0.65f * focal) / 1.1f
                drawLine(
                    color = CyberCyan.copy(alpha = 0.28f),
                    start = Offset(farX, farY),
                    end = Offset(nearX, nearY),
                    strokeWidth = 2.5f
                )
            }

            // Depth-sorted 3D obstacles and gold energy cores
            objects.sortedByDescending { it.z }.forEach { obj ->
                val scale = focal / obj.z.coerceAtLeast(0.7f)
                val sx = cx + obj.x * scale
                val sy = cy + obj.y * scale
                val sizeBox = scale * 0.34f
                if (obj.isCore) {
                    drawCircle(
                        color = GoldCoin,
                        radius = sizeBox * 0.45f,
                        center = Offset(sx, sy)
                    )
                    drawCircle(
                        color = TextPrimary.copy(alpha = 0.7f),
                        radius = sizeBox * 0.2f,
                        center = Offset(sx, sy)
                    )
                } else {
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(RedNeonBright, RedNeon.copy(alpha = 0.45f))),
                        topLeft = Offset(sx - sizeBox / 2f, sy - sizeBox / 2f),
                        size = Size(sizeBox, sizeBox),
                        cornerRadius = CornerRadius(sizeBox * 0.2f, sizeBox * 0.2f)
                    )
                    drawRoundRect(
                        color = TextPrimary.copy(alpha = 0.7f),
                        topLeft = Offset(sx - sizeBox / 2f, sy - sizeBox / 2f),
                        size = Size(sizeBox, sizeBox),
                        cornerRadius = CornerRadius(sizeBox * 0.2f, sizeBox * 0.2f),
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // 3D Player hover-craft
            val shipScale = focal / 1.6f
            val px = cx + playerX * shipScale
            val py = cy + 0.52f * shipScale
            val shipPath = Path().apply {
                moveTo(px, py - 34f)
                lineTo(px + 42f, py + 24f)
                lineTo(px, py + 10f)
                lineTo(px - 42f, py + 24f)
                close()
            }
            drawPath(shipPath, color = CyberCyan)
            drawPath(shipPath, color = TextPrimary, style = Stroke(width = 3f))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        ) {
            Text(
                text = "Geser layar atau ketuk kiri/kanan untuk menghindar & ambil koin emas",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

private data class Cube3D(
    var x: Float,
    var y: Float,
    var z: Float,
    var rot: Float,
    val isReward: Boolean
)

@Composable
fun CosmicCube3DGame(
    onScoreUpdate: (Long) -> Unit,
    onGameOver: (Long) -> Unit
) {
    var shipX by remember { mutableFloatStateOf(0f) }
    var shipY by remember { mutableFloatStateOf(0.25f) }
    var score by remember { mutableLongStateOf(0L) }
    var alive by remember { mutableStateOf(true) }
    val cubes = remember { mutableStateListOf<Cube3D>() }

    LaunchedEffect(alive) {
        if (!alive) return@LaunchedEffect
        var lastTime = withFrameNanos { it }
        var timer = 0f
        while (alive) {
            val now = withFrameNanos { it }
            val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now

            timer += dt
            if (timer >= 0.38f) {
                timer = 0f
                cubes.add(
                    Cube3D(
                        x = Random.nextFloat() * 2.4f - 1.2f,
                        y = Random.nextFloat() * 1.6f - 0.8f,
                        z = 20f,
                        rot = Random.nextFloat() * 6f,
                        isReward = Random.nextFloat() < 0.3f
                    )
                )
            }

            val it = cubes.iterator()
            var updated = false
            while (it.hasNext()) {
                val c = it.next()
                c.z -= (12f + (score / 35f).coerceAtMost(18f)) * dt
                c.rot += dt * 2.4f
                if (c.z in 1.1f..1.85f && abs(c.x - shipX) < 0.34f && abs(c.y - shipY) < 0.34f) {
                    if (c.isReward) {
                        score += 30L
                        updated = true
                        it.remove()
                    } else {
                        alive = false
                        onGameOver(score)
                        break
                    }
                } else if (c.z < 0.5f) {
                    if (!c.isReward) {
                        score += 5L
                        updated = true
                    }
                    it.remove()
                }
            }
            if (updated) onScoreUpdate(score)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("game_canvas_cube_3d")
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    shipX = (shipX + dragAmount.x / (size.width * 0.35f)).coerceIn(-1.2f, 1.2f)
                    shipY = (shipY + dragAmount.y / (size.height * 0.35f)).coerceIn(-0.85f, 0.85f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.48f
            val focal = minOf(size.width, size.height) * 0.62f

            // Project 3D wireframe rotating cubes
            val baseVerts = arrayOf(
                floatArrayOf(-0.22f, -0.22f, -0.22f),
                floatArrayOf(0.22f, -0.22f, -0.22f),
                floatArrayOf(0.22f, 0.22f, -0.22f),
                floatArrayOf(-0.22f, 0.22f, -0.22f),
                floatArrayOf(-0.22f, -0.22f, 0.22f),
                floatArrayOf(0.22f, -0.22f, 0.22f),
                floatArrayOf(0.22f, 0.22f, 0.22f),
                floatArrayOf(-0.22f, 0.22f, 0.22f)
            )
            val edges = arrayOf(
                0 to 1, 1 to 2, 2 to 3, 3 to 0,
                4 to 5, 5 to 6, 6 to 7, 7 to 4,
                0 to 4, 1 to 5, 2 to 6, 3 to 7
            )

            cubes.sortedByDescending { it.z }.forEach { cube ->
                val cosA = cos(cube.rot)
                val sinA = sin(cube.rot)
                val projected = baseVerts.map { v ->
                    // Rotate around Y and X
                    val rx = v[0] * cosA - v[2] * sinA
                    val rz = v[0] * sinA + v[2] * cosA
                    val ry = v[1] * cosA - rz * sinA * 0.5f
                    val tz = (cube.z + rz).coerceAtLeast(0.5f)
                    val s = focal / tz
                    Offset(cx + (cube.x + rx) * s, cy + (cube.y + ry) * s)
                }
                val edgeColor = if (cube.isReward) GoldCoin else CyberPurple
                edges.forEach { (a, b) ->
                    drawLine(
                        color = edgeColor,
                        start = projected[a],
                        end = projected[b],
                        strokeWidth = if (cube.isReward) 4.5f else 3f
                    )
                }
            }

            // Player reticle & craft
            val ps = focal / 1.45f
            val px = cx + shipX * ps
            val py = cy + shipY * ps
            drawCircle(
                color = RedNeon,
                radius = 28f,
                center = Offset(px, py),
                style = Stroke(width = 4f)
            )
            drawCircle(color = CyberCyan, radius = 10f, center = Offset(px, py))
        }

        Text(
            text = "Seret bebas untuk menerbangkan pesawat menembus kubus 3D",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        )
    }
}

private data class Brick3D(val row: Int, val col: Int, var alive: Boolean = true)

@Composable
fun OrbitalBreaker3DGame(
    onScoreUpdate: (Long) -> Unit,
    onGameOver: (Long, Boolean) -> Unit
) {
    var paddleX by remember { mutableFloatStateOf(0.5f) }
    var ballX by remember { mutableFloatStateOf(0.5f) }
    var ballY by remember { mutableFloatStateOf(0.75f) }
    var velX by remember { mutableFloatStateOf(0.45f) }
    var velY by remember { mutableFloatStateOf(-0.62f) }
    var score by remember { mutableLongStateOf(0L) }
    var lives by remember { mutableIntStateOf(3) }
    var running by remember { mutableStateOf(true) }

    val bricks = remember {
        mutableStateListOf<Brick3D>().apply {
            for (r in 0 until 4) {
                for (c in 0 until 5) {
                    add(Brick3D(r, c, true))
                }
            }
        }
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (running) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.04f)
            last = now

            ballX += velX * dt
            ballY += velY * dt

            if (ballX < 0.06f) { ballX = 0.06f; velX = abs(velX) }
            if (ballX > 0.94f) { ballX = 0.94f; velX = -abs(velX) }
            if (ballY < 0.10f) { ballY = 0.10f; velY = abs(velY) }

            // Paddle hit
            if (ballY in 0.83f..0.88f && velY > 0 && abs(ballX - paddleX) < 0.14f) {
                velY = -abs(velY) * 1.02f
                velX = ((ballX - paddleX) * 3.8f).coerceIn(-0.9f, 0.9f)
            }

            // Brick collision
            for (brick in bricks) {
                if (!brick.alive) continue
                val bx = 0.14f + brick.col * 0.18f
                val by = 0.16f + brick.row * 0.08f
                if (abs(ballX - bx) < 0.085f && abs(ballY - by) < 0.038f) {
                    brick.alive = false
                    velY = -velY
                    score += 25L
                    onScoreUpdate(score)
                    break
                }
            }

            if (bricks.none { it.alive }) {
                running = false
                onGameOver(score + 100L, true)
                break
            }

            if (ballY > 0.96f) {
                lives -= 1
                if (lives <= 0) {
                    running = false
                    onGameOver(score, false)
                } else {
                    ballX = paddleX
                    ballY = 0.78f
                    velY = -0.62f
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("game_canvas_breaker_3d")
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    paddleX = (paddleX + dragAmount.x / size.width).coerceIn(0.14f, 0.86f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 3D Perspective arena trapezoid
            fun projectX(nx: Float, ny: Float): Float {
                val depthScale = 0.55f + 0.45f * ny
                return w * 0.5f + (nx - 0.5f) * w * depthScale
            }

            // Arena boundary lines
            val topL = Offset(projectX(0.03f, 0.08f), h * 0.08f)
            val topR = Offset(projectX(0.97f, 0.08f), h * 0.08f)
            val botL = Offset(projectX(0.03f, 0.92f), h * 0.92f)
            val botR = Offset(projectX(0.97f, 0.92f), h * 0.92f)
            drawLine(RedNeon.copy(alpha = 0.5f), topL, botL, strokeWidth = 3f)
            drawLine(RedNeon.copy(alpha = 0.5f), topR, botR, strokeWidth = 3f)
            drawLine(CyberCyan.copy(alpha = 0.5f), topL, topR, strokeWidth = 3f)

            // 3D Bricks
            bricks.forEach { b ->
                if (!b.alive) return@forEach
                val nx = 0.14f + b.col * 0.18f
                val ny = 0.16f + b.row * 0.08f
                val px = projectX(nx, ny)
                val py = h * ny
                val bw = w * 0.11f * (0.6f + 0.4f * ny)
                val bh = h * 0.042f
                val color = when (b.row) {
                    0 -> RedNeon
                    1 -> GoldCoin
                    2 -> CyberPurple
                    else -> CyberCyan
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(px - bw / 2f, py - bh / 2f),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }

            // 3D Ball
            val bx = projectX(ballX, ballY)
            val by = h * ballY
            val br = 12f + 10f * ballY
            drawCircle(color = GoldCoin, radius = br, center = Offset(bx, by))

            // 3D Paddle
            val px = projectX(paddleX, 0.86f)
            val py = h * 0.86f
            val pw = w * 0.24f
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(CyberCyan, TextPrimary, CyberCyan)),
                topLeft = Offset(px - pw / 2f, py - 12f),
                size = Size(pw, 24f),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }

        Text(
            text = "Nyawa: $lives · Geser layar untuk memantulkan bola 3D",
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldSecure,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
