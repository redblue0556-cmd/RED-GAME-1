package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.MatchRoom
import com.example.data.model.UiState
import com.example.ui.components.NeonCard
import com.example.ui.theme.BorderRedGlow
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSecure
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.RedDarkSurface
import com.example.ui.theme.RedNeon
import com.example.ui.theme.RedNeonBright
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DecryptedChatMessage

@Composable
fun MabarScreen(
    currentUserId: String,
    waitingRoomsState: UiState<List<MatchRoom>>,
    myRoomsState: UiState<List<MatchRoom>>,
    activeRoomState: UiState<MatchRoom?>,
    activeRoomId: String?,
    messagesState: UiState<List<ChatMessage>>,
    decryptMessages: (MatchRoom, List<ChatMessage>) -> List<DecryptedChatMessage>,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (MatchRoom) -> Unit,
    onJoinByCode: (String) -> Unit,
    onOpenRoom: (String) -> Unit,
    onCloseRoom: () -> Unit,
    onTicTacToeMove: (MatchRoom, Int) -> Unit,
    onCyberDuelTap: (MatchRoom) -> Unit,
    onCancelWaitingRoom: (MatchRoom) -> Unit,
    onClaimMatchReward: (MatchRoom) -> Unit,
    onSendMessage: (MatchRoom, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeRoomId != null) {
        BackHandler { onCloseRoom() }
        val room = (activeRoomState as? UiState.Success)?.data
        if (room == null) {
            Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
                when (activeRoomState) {
                    is UiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(activeRoomState.message, color = RedNeonBright)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onCloseRoom) { Text("Kembali ke Lobby") }
                    }
                    else -> CircularProgressIndicator(color = RedNeon)
                }
            }
        } else {
            LaunchedEffect(room.status, room.winnerUid) {
                if (room.status == "finished") {
                    onClaimMatchReward(room)
                }
            }
            val rawMsgs = (messagesState as? UiState.Success)?.data.orEmpty()
            val decrypted = remember(room.id, room.roomSalt, rawMsgs) {
                decryptMessages(room, rawMsgs)
            }
            ActiveMabarArena(
                currentUserId = currentUserId,
                room = room,
                messages = decrypted,
                onBack = onCloseRoom,
                onTicTacToeMove = { idx -> onTicTacToeMove(room, idx) },
                onCyberDuelTap = { onCyberDuelTap(room) },
                onCancelRoom = { onCancelWaitingRoom(room) },
                onSendMessage = { txt -> onSendMessage(room, txt) },
                modifier = modifier
            )
        }
        return
    }

    var joinCode by remember { mutableStateOf("") }
    val waitingRooms = (waitingRoomsState as? UiState.Success)?.data.orEmpty()
        .filter { currentUserId !in it.memberUids }
    val myRooms = (myRoomsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text("Mabar Real-Time", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                "Main bersama teman secara langsung dengan chat terenkripsi AES-256-GCM.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Create Room Panel
        item {
            NeonCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Buat Room Baru", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onCreateRoom("TICTACTOE_3X3") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedNeon),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("create_xo_room_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Arena XO", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = { onCreateRoom("CYBER_DUEL") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = ObsidianBg),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("create_duel_room_button")
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Cyber Duel", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it.uppercase().take(8) },
                            placeholder = { Text("Kode Room (mis. ABC123)", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RedNeon,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("join_code_input")
                        )
                        Button(
                            onClick = { if (joinCode.isNotBlank()) onJoinByCode(joinCode) },
                            enabled = joinCode.length >= 4,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RedNeon),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("join_code_button")
                        ) {
                            Text("Masuk")
                        }
                    }
                }
            }
        }

        // My Rooms
        if (myRooms.isNotEmpty()) {
            item {
                Text("Room Saya (${myRooms.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            items(myRooms, key = { it.id }) { room ->
                RoomSummaryRow(
                    room = room,
                    actionLabel = if (room.status == "finished") "Lihat" else "Masuk",
                    onClick = { onOpenRoom(room.id) }
                )
            }
        }

        // Open Waiting Rooms
        item {
            Text("Lobby Terbuka", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }
        when (waitingRoomsState) {
            is UiState.Loading -> item { CircularProgressIndicator(color = RedNeon) }
            is UiState.Error -> item { Text(waitingRoomsState.message, color = RedNeonBright) }
            is UiState.Success -> {
                if (waitingRooms.isEmpty()) {
                    item {
                        NeonCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Belum ada room terbuka dari pemain lain. Buat room baru dan bagikan kode ke temanmu!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(waitingRooms, key = { it.id }) { room ->
                        RoomSummaryRow(
                            room = room,
                            actionLabel = "Gabung",
                            onClick = { onJoinRoom(room) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomSummaryRow(
    room: MatchRoom,
    actionLabel: String,
    onClick: () -> Unit
) {
    NeonCard(
        cornerRadius = 18.dp,
        borderColor = BorderRedGlow.copy(alpha = 0.6f),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_row_${room.roomCode}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RedNeon.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = room.roomCode,
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldCoin,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (room.gameType == "CYBER_DUEL") "Cyber Duel" else "Arena XO",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Host: ${room.hostName} · Status: ${room.status.uppercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedNeon),
                modifier = Modifier.height(48.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun ActiveMabarArena(
    currentUserId: String,
    room: MatchRoom,
    messages: List<DecryptedChatMessage>,
    onBack: () -> Unit,
    onTicTacToeMove: (Int) -> Unit,
    onCyberDuelTap: () -> Unit,
    onCancelRoom: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var chatDraft by remember { mutableStateOf("") }
    var showCipherPreview by remember { mutableStateOf(false) }
    val chatListState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            chatListState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .padding(bottom = 86.dp)
    ) {
        // Arena Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("mabar_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Room ${room.roomCode} · ${if (room.gameType == "CYBER_DUEL") "Cyber Duel" else "Arena XO"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${room.hostName} vs ${room.guestName ?: "Menunggu lawan…"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberCyan
                )
            }
            if (room.status == "waiting" && room.hostUid == currentUserId) {
                OutlinedButton(
                    onClick = onCancelRoom,
                    border = BorderStroke(1.dp, RedNeon),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = RedNeonBright, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tutup", color = TextPrimary, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Match Board Card
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(14.dp)
            ) {
                val statusBanner = when (room.status) {
                    "waiting" -> "Bagikan kode ${room.roomCode} ke teman untuk mulai mabar!"
                    "finished" -> when (room.winnerUid) {
                        "DRAW" -> "Seri! +50 Poin"
                        currentUserId -> "KAMU MENANG! +100 Poin"
                        else -> "Lawan menang · +30 Poin partisipasi"
                    }
                    else -> if (room.gameType == "CYBER_DUEL") {
                        "Tap secepatnya sampai 20 energi!"
                    } else if (room.turnUid == currentUserId) {
                        "Giliran Kamu!"
                    } else {
                        "Menunggu giliran lawan…"
                    }
                }

                Text(
                    text = statusBanner,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (room.status == "finished") GoldCoin else CyberCyan,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))

                if (room.gameType == "CYBER_DUEL") {
                    val parts = room.boardState.split(",")
                    val hostScore = parts.getOrNull(0)?.toIntOrNull() ?: 0
                    val guestScore = parts.getOrNull(1)?.toIntOrNull() ?: 0

                    Text("${room.hostName}: $hostScore / 20", style = MaterialTheme.typography.labelSmall, color = RedNeonBright)
                    LinearProgressIndicator(
                        progress = { (hostScore / 20f).coerceIn(0f, 1f) },
                        color = RedNeon,
                        trackColor = ObsidianBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(99.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("${room.guestName ?: "Lawan"}: $guestScore / 20", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
                    LinearProgressIndicator(
                        progress = { (guestScore / 20f).coerceIn(0f, 1f) },
                        color = CyberCyan,
                        trackColor = ObsidianBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(99.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onCyberDuelTap,
                        enabled = room.status == "playing",
                        colors = ButtonDefaults.buttonColors(containerColor = RedNeon),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("cyber_duel_tap_button")
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("SERANG! (+1 ENERGI)", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    val board = room.boardState.padEnd(9, '.')
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.width(210.dp)
                    ) {
                        for (r in 0..2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (c in 0..2) {
                                    val idx = r * 3 + c
                                    val ch = board[idx]
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ObsidianElevated)
                                            .border(1.dp, BorderRedGlow.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                            .testTag("xo_cell_$idx")
                                            .clickable(
                                                enabled = room.status == "playing" && room.turnUid == currentUserId && ch == '.'
                                            ) { onTicTacToeMove(idx) }
                                    ) {
                                        Text(
                                            text = if (ch == '.') "" else ch.toString(),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (ch == 'X') RedNeonBright else CyberCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Live Encrypted In-Game Chat Panel
        NeonCard(
            borderColor = EmeraldSecure.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Chat Terenkripsi",
                            tint = EmeraldSecure,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Chat Mabar · AES-256-GCM E2E",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldSecure
                        )
                    }
                    Text(
                        text = if (showCipherPreview) "Sembunyikan Cipher" else "Lihat Cipher",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        modifier = Modifier.clickable { showCipherPreview = !showCipherPreview }
                    )
                }

                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    state = chatListState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                "Belum ada pesan. Semua pesan dienkripsi di perangkat sebelum dikirim ke database.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            val isMine = msg.senderUid == currentUserId
                            Column(
                                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isMine) RedDarkSurface else ObsidianElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isMine) RedNeon.copy(alpha = 0.65f) else BorderSubtle
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(
                                            text = "${msg.senderName} · ${msg.timeLabel}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMine) GoldCoin else CyberCyan
                                        )
                                        Text(
                                            text = msg.plaintext,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                        if (showCipherPreview) {
                                            Text(
                                                text = "AES: ${msg.cipherPreview}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = EmeraldSecure
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatDraft,
                        onValueChange = { chatDraft = it },
                        placeholder = { Text("Tulis pesan terenkripsi…", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldSecure,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mabar_chat_input")
                    )
                    Button(
                        onClick = {
                            if (chatDraft.isNotBlank()) {
                                onSendMessage(chatDraft)
                                chatDraft = ""
                            }
                        },
                        enabled = chatDraft.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedNeon),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("mabar_chat_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim")
                    }
                }
            }
        }
    }
}
