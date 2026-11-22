package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.GameScore
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MatchRoom
import com.example.data.model.PrivateVault
import com.example.data.model.RedGameCatalog
import com.example.data.model.RedGameItem
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.RedGameRepository
import com.example.security.SecurityCryptoEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DecryptedChatMessage(
    val id: String,
    val senderUid: String,
    val senderName: String,
    val plaintext: String,
    val cipherPreview: String,
    val timeLabel: String
)

class RedGameViewModel(
    private val repository: RedGameRepository,
    val currentUserId: String,
    private val userDisplayName: String?,
    private val userEmail: String?
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _matchRewardedRoomIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            val res = repository.ensureUserProfileAndVault(currentUserId, userDisplayName, userEmail)
            res.onFailure { e ->
                _toastMessage.value = "Sinkronisasi profil gagal: ${e.localizedMessage ?: "cek koneksi"}"
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(tag: String, fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(tag, fallbackError, rootError)
                emit(UiState.Error(rootError.localizedMessage ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiStateFlow("RedGameVM", "Gagal memuat profil pemain")

    val vaultState: StateFlow<UiState<PrivateVault?>> =
        repository.observeEncryptedVault(currentUserId)
            .toUiStateFlow("RedGameVM", "Gagal memuat brankas enkripsi")

    val scoresState: StateFlow<UiState<List<GameScore>>> =
        repository.observeUserScores(currentUserId)
            .toUiStateFlow("RedGameVM", "Gagal memuat riwayat skor")

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
        repository.observeGlobalLeaderboard(currentUserId)
            .toUiStateFlow("RedGameVM", "Gagal memuat leaderboard global")

    val waitingRoomsState: StateFlow<UiState<List<MatchRoom>>> =
        repository.observeWaitingRooms(currentUserId)
            .toUiStateFlow("RedGameVM", "Gagal memuat lobby mabar")

    val myRoomsState: StateFlow<UiState<List<MatchRoom>>> =
        repository.observeMyRooms(currentUserId)
            .toUiStateFlow("RedGameVM", "Gagal memuat riwayat room mabar")

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeRoomState: StateFlow<UiState<MatchRoom?>> =
        _activeRoomId.flatMapLatest { roomId ->
            if (roomId == null) flowOf(null)
            else repository.observeRoom(roomId, currentUserId)
        }.toUiStateFlow("RedGameVM", "Gagal memuat ruang mabar")

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> =
        _activeRoomId.flatMapLatest { roomId ->
            if (roomId == null) flowOf(emptyList())
            else repository.observeRoomMessages(roomId, currentUserId)
        }.toUiStateFlow("RedGameVM", "Gagal memuat chat terenkripsi")

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun openRoom(roomId: String) {
        _activeRoomId.value = roomId
    }

    fun closeRoom() {
        _activeRoomId.value = null
    }

    fun claimDailyReward() {
        val currentProfile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.claimDailyReward(currentProfile)
                .onSuccess { claimed ->
                    _toastMessage.value = if (claimed) {
                        "Daily Reward diklaim: +20 Koin & +15 Poin!"
                    } else {
                        "Daily Reward hari ini sudah diklaim."
                    }
                }
                .onFailure { e ->
                    _toastMessage.value = e.localizedMessage ?: "Gagal klaim reward"
                }
        }
    }

    fun submitOfflineGameScore(game: RedGameItem, score: Long, pointsEarned: Long, isWin: Boolean) {
        val currentProfile = (profileState.value as? UiState.Success)?.data ?: UserProfile(
            userId = currentUserId,
            displayName = userDisplayName ?: "RedPlayer"
        )
        viewModelScope.launch {
            repository.submitGameResult(currentProfile, game, score, pointsEarned, isWin)
                .onSuccess {
                    _toastMessage.value = "${game.title}: +$pointsEarned Poin ditambahkan ke Leaderboard!"
                }
                .onFailure { e ->
                    _toastMessage.value = "Gagal menyimpan skor: ${e.localizedMessage ?: "cek koneksi"}"
                }
        }
    }

    fun createRoom(gameType: String) {
        val displayName = (profileState.value as? UiState.Success)?.data?.displayName
            ?: userDisplayName ?: "RedPlayer"
        viewModelScope.launch {
            repository.createMatchRoom(displayName, gameType)
                .onSuccess { roomId ->
                    _activeRoomId.value = roomId
                    _toastMessage.value = "Room Mabar dibuat! Bagikan kode room ke teman."
                }
                .onFailure { e ->
                    _toastMessage.value = e.localizedMessage ?: "Gagal membuat room"
                }
        }
    }

    fun joinRoom(room: MatchRoom) {
        if (currentUserId in room.memberUids) {
            _activeRoomId.value = room.id
            return
        }
        val displayName = (profileState.value as? UiState.Success)?.data?.displayName
            ?: userDisplayName ?: "RedPlayer"
        viewModelScope.launch {
            repository.joinMatchRoom(room, displayName)
                .onSuccess {
                    _activeRoomId.value = room.id
                    _toastMessage.value = "Berhasil bergabung ke room ${room.roomCode}!"
                }
                .onFailure { e ->
                    _toastMessage.value = e.localizedMessage ?: "Gagal bergabung ke room"
                }
        }
    }

    fun joinRoomByCode(code: String) {
        val displayName = (profileState.value as? UiState.Success)?.data?.displayName
            ?: userDisplayName ?: "RedPlayer"
        viewModelScope.launch {
            repository.joinRoomByCode(code, displayName)
                .onSuccess { roomId ->
                    _activeRoomId.value = roomId
                    _toastMessage.value = "Berhasil masuk ke room!"
                }
                .onFailure { e ->
                    _toastMessage.value = e.localizedMessage ?: "Kode room tidak ditemukan"
                }
        }
    }

    fun makeTicTacToeMove(room: MatchRoom, index: Int) {
        if (room.status != "playing" || room.turnUid != currentUserId) return
        val board = room.boardState.padEnd(9, '.').toCharArray()
        if (index !in 0..8 || board[index] != '.') return
        val symbol = if (currentUserId == room.hostUid) 'X' else 'O'
        board[index] = symbol
        val newBoard = String(board)

        val winLines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        val hasWon = winLines.any { line -> line.all { board[it] == symbol } }
        val isDraw = !hasWon && board.none { it == '.' }

        val newStatus = if (hasWon || isDraw) "finished" else "playing"
        val winnerUid = when {
            hasWon -> currentUserId
            isDraw -> "DRAW"
            else -> null
        }
        val nextTurn = if (currentUserId == room.hostUid) (room.guestUid ?: room.hostUid) else room.hostUid

        viewModelScope.launch {
            repository.makeMatchMove(room, newBoard, nextTurn, newStatus, winnerUid)
                .onFailure { e ->
                    _toastMessage.value = e.localizedMessage ?: "Langkah gagal"
                }
        }
    }

    fun makeCyberDuelTap(room: MatchRoom) {
        if (room.status != "playing" || currentUserId !in room.memberUids) return
        val parts = room.boardState.split(",")
        var hostScore = parts.getOrNull(0)?.toIntOrNull() ?: 0
        var guestScore = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (currentUserId == room.hostUid) hostScore += 1 else guestScore += 1

        val target = 20
        val newBoard = "$hostScore,$guestScore"
        val winnerUid = when {
            hostScore >= target -> room.hostUid
            guestScore >= target -> room.guestUid ?: currentUserId
            else -> null
        }
        val newStatus = if (winnerUid != null) "finished" else "playing"
        val nextTurn = if (currentUserId == room.hostUid) (room.guestUid ?: room.hostUid) else room.hostUid

        viewModelScope.launch {
            repository.makeMatchMove(room, newBoard, nextTurn, newStatus, winnerUid)
        }
    }

    fun claimMatchCompletionIfNeeded(room: MatchRoom) {
        if (room.status != "finished" || room.id in _matchRewardedRoomIds.value) return
        val isWinner = room.winnerUid == currentUserId
        val isDraw = room.winnerUid == "DRAW"
        if (!isWinner && !isDraw && room.winnerUid == null) return
        _matchRewardedRoomIds.value = _matchRewardedRoomIds.value + room.id

        val game = RedGameCatalog.allGames.firstOrNull {
            if (room.gameType == "CYBER_DUEL") it.id == "mabar_cyber_duel" else it.id == "mabar_tictactoe"
        } ?: RedGameCatalog.allGames.last()

        val pts = when {
            isWinner -> 100L
            isDraw -> 50L
            else -> 30L
        }
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.submitGameResult(profile, game, pts, pts, isWinner)
                .onSuccess {
                    _toastMessage.value = if (isWinner) {
                        "Menang Mabar! +$pts Poin masuk ke Leaderboard!"
                    } else {
                        "Match selesai! +$pts Poin partisipasi."
                    }
                }
        }
    }

    fun cancelWaitingRoom(room: MatchRoom) {
        viewModelScope.launch {
            repository.cancelMatchRoom(room)
                .onSuccess {
                    _activeRoomId.value = null
                    _toastMessage.value = "Room dibatalkan."
                }
        }
    }

    fun sendChatMessage(room: MatchRoom, plaintext: String) {
        val displayName = (profileState.value as? UiState.Success)?.data?.displayName
            ?: userDisplayName ?: "RedPlayer"
        viewModelScope.launch {
            repository.sendEncryptedMessage(room, displayName, plaintext)
                .onFailure { e ->
                    _toastMessage.value = "Gagal kirim chat: ${e.localizedMessage ?: "error"}"
                }
        }
    }

    fun decryptChatMessages(room: MatchRoom, messages: List<ChatMessage>): List<DecryptedChatMessage> {
        return messages.map { msg ->
            val plain = SecurityCryptoEngine.decryptRoomChatMessage(
                roomId = room.id,
                roomSalt = room.roomSalt,
                cipherTextB64 = msg.cipherText,
                ivB64 = msg.iv
            )
            val timeStr = msg.createdAt?.toDate()?.let {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it)
            } ?: ".."
            DecryptedChatMessage(
                id = msg.id,
                senderUid = msg.senderUid,
                senderName = msg.senderName,
                plaintext = plain,
                cipherPreview = msg.cipherText.take(18) + "…",
                timeLabel = timeStr
            )
        }
    }

    fun decryptVaultJson(vault: PrivateVault): String {
        return SecurityCryptoEngine.decryptUserVault(currentUserId, vault.cipherText, vault.iv)
    }

    fun rotateEncryptionKey() {
        viewModelScope.launch {
            repository.refreshEncryptedVault(currentUserId, userEmail)
                .onSuccess {
                    _toastMessage.value = "Kunci AES-256-GCM & hash integritas diperbarui!"
                }
                .onFailure { e ->
                    _toastMessage.value = e.localizedMessage ?: "Gagal memperbarui enkripsi"
                }
        }
    }
}
