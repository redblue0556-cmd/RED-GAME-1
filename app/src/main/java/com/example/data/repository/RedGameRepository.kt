package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.GameScore
import com.example.data.model.LeaderboardEntry
import com.example.data.model.MatchRoom
import com.example.data.model.PrivateVault
import com.example.data.model.RedGameItem
import com.example.data.model.UserProfile
import com.example.security.SecurityCryptoEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RedGameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Pengguna wajib masuk dengan Google sebelum mengakses database.")
    }

    private fun sanitizeName(rawName: String?): String {
        val cleaned = (rawName ?: "RedPlayer").trim().replace(Regex("\\s+"), " ")
        return if (cleaned.isBlank()) "RedPlayer" else cleaned.take(40)
    }

    private fun avatarTagFrom(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val tag = when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}"
            parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2)
            parts.size == 1 -> parts[0].take(1)
            else -> "RG"
        }.uppercase(Locale.US)
        return tag.take(4).ifBlank { "RG" }
    }

    private fun <T> wrapStreamError(path: String, uid: String, flow: Flow<T>): Flow<T> =
        flow.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
            }
            throw error
        }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Akses profil lintas pengguna ditolak." }
        val path = "users/$uid"
        emitAll(
            wrapStreamError(
                path,
                uid,
                db.collection("users").document(uid)
                    .snapshots()
                    .map { snap ->
                        if (snap.exists()) {
                            snap.toObject(
                                UserProfile::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        } else null
                    }
            )
        )
    }

    fun observeEncryptedVault(userId: String): Flow<PrivateVault?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Akses brankas enkripsi lintas pengguna ditolak." }
        val path = "users/$uid/private/vault"
        emitAll(
            wrapStreamError(
                path,
                uid,
                db.collection("users").document(uid)
                    .collection("private").document("vault")
                    .snapshots()
                    .map { snap ->
                        if (snap.exists()) {
                            snap.toObject(
                                PrivateVault::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        } else null
                    }
            )
        )
    }

    fun observeUserScores(userId: String): Flow<List<GameScore>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Akses skor lintas pengguna ditolak." }
        val path = "users/$uid/scores"
        emitAll(
            wrapStreamError(
                path,
                uid,
                db.collection("users").document(uid)
                    .collection("scores")
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(
                            GameScore::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                    }
            )
        )
    }

    fun observeGlobalLeaderboard(userId: String): Flow<List<LeaderboardEntry>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "leaderboard"
        emitAll(
            wrapStreamError(
                path,
                uid,
                db.collection("leaderboard")
                    .whereEqualTo("visibility", "public")
                    .limit(100)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(
                            LeaderboardEntry::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.totalPoints }
                    }
            )
        )
    }

    fun observeWaitingRooms(userId: String): Flow<List<MatchRoom>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "match_rooms"
        emitAll(
            wrapStreamError(
                path,
                uid,
                db.collection("match_rooms")
                    .whereEqualTo("status", "waiting")
                    .limit(40)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(
                            MatchRoom::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                    }
            )
        )
    }

    fun observeMyRooms(userId: String): Flow<List<MatchRoom>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "match_rooms"
        emitAll(
            wrapStreamError(
                path,
                uid,
                db.collection("match_rooms")
                    .whereArrayContains("memberUids", uid)
                    .limit(30)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(
                            MatchRoom::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                    }
            )
        )
    }

    fun observeRoom(roomId: String, userId: String): Flow<MatchRoom?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        require(roomId.isNotBlank())
        val path = "match_rooms/$roomId"
        emitAll(
            wrapStreamError(
                path,
                uid,
                db.collection("match_rooms").document(roomId)
                    .snapshots()
                    .map { snap ->
                        if (snap.exists()) {
                            snap.toObject(
                                MatchRoom::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        } else null
                    }
            )
        )
    }

    fun observeRoomMessages(roomId: String, userId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        require(roomId.isNotBlank())
        val path = "match_rooms/$roomId/messages"
        emitAll(
            wrapStreamError(
                path,
                uid,
                db.collection("match_rooms").document(roomId)
                    .collection("messages")
                    .whereArrayContains("memberUids", uid)
                    .limit(100)
                    .snapshots()
                    .map { snap ->
                        snap.toObjects(
                            ChatMessage::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
                    }
            )
        )
    }

    suspend fun ensureUserProfileAndVault(
        userId: String,
        rawDisplayName: String?,
        rawEmail: String?
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val displayName = sanitizeName(rawDisplayName)
            val avatarTag = avatarTagFrom(displayName)

            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "avatarTag" to avatarTag,
                    "totalPoints" to 0L,
                    "coins" to 150L,
                    "gamesPlayed" to 0L,
                    "wins" to 0L,
                    "level" to 1L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(payload).await()
            }

            val lbRef = db.collection("leaderboard").document(uid)
            val lbSnap = lbRef.get().await()
            if (!lbSnap.exists()) {
                val lbPayload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "avatarTag" to avatarTag,
                    "totalPoints" to 0L,
                    "wins" to 0L,
                    "gamesPlayed" to 0L,
                    "favoriteGame" to "Neon Tunnel 3D",
                    "visibility" to "public",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                lbRef.set(lbPayload).await()
            }

            // Always ensure AES-256-GCM encrypted PII vault exists
            val vaultRef = userRef.collection("private").document("vault")
            val vaultSnap = vaultRef.get().await()
            val sensitiveJson = """{"email":"${rawEmail ?: "protected"}","provider":"google.com","securityLevel":"AES-256-GCM+Keystore"}"""
            val encrypted = SecurityCryptoEngine.encryptUserVault(uid, sensitiveJson)
            if (!vaultSnap.exists()) {
                vaultRef.set(
                    mapOf(
                        "userId" to uid,
                        "cipherText" to encrypted.cipherText,
                        "iv" to encrypted.iv,
                        "algorithm" to encrypted.algorithm,
                        "integrityHash" to encrypted.integrityHash,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun refreshEncryptedVault(userId: String, rawEmail: String?): Result<Unit> {
        val path = "users/$userId/private/vault"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val timestampIso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val sensitiveJson = """{"email":"${rawEmail ?: "protected"}","provider":"google.com","lastKeyRotation":"$timestampIso","keystoreHardware":${SecurityCryptoEngine.isHardwareKeystoreActive(uid)}}"""
            val encrypted = SecurityCryptoEngine.encryptUserVault(uid, sensitiveJson)
            val vaultRef = db.collection("users").document(uid).collection("private").document("vault")
            val snap = vaultRef.get().await()
            if (snap.exists()) {
                vaultRef.update(
                    mapOf(
                        "cipherText" to encrypted.cipherText,
                        "iv" to encrypted.iv,
                        "algorithm" to encrypted.algorithm,
                        "integrityHash" to encrypted.integrityHash,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                vaultRef.set(
                    mapOf(
                        "userId" to uid,
                        "cipherText" to encrypted.cipherText,
                        "iv" to encrypted.iv,
                        "algorithm" to encrypted.algorithm,
                        "integrityHash" to encrypted.integrityHash,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimDailyReward(profile: UserProfile): Result<Boolean> {
        val path = "users/${profile.userId}"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            if (profile.dailyClaimedDate == today) {
                return Result.success(false)
            }
            val newCoins = profile.coins + 20L
            val newPoints = profile.totalPoints + 15L
            val newLevel = (newPoints / 250L) + 1L

            db.collection("users").document(uid).update(
                mapOf(
                    "coins" to newCoins,
                    "totalPoints" to newPoints,
                    "level" to newLevel,
                    "dailyClaimedDate" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("leaderboard").document(uid).update(
                mapOf(
                    "totalPoints" to newPoints,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitGameResult(
        profile: UserProfile,
        game: RedGameItem,
        score: Long,
        pointsEarned: Long,
        isWin: Boolean
    ): Result<Unit> {
        val path = "users/${profile.userId}/scores"
        return try {
            val uid = requireUserId()
            check(uid == profile.userId)
            val clampedScore = score.coerceIn(0L, 10_000_000L)
            val clampedPts = pointsEarned.coerceIn(0L, 100_000L)
            val coinsEarned = (clampedPts / 3L).coerceAtLeast(5L)

            val scoreRef = db.collection("users").document(uid).collection("scores").document()
            scoreRef.set(
                mapOf(
                    "userId" to uid,
                    "gameId" to game.id.take(40),
                    "gameTitle" to game.title.take(80),
                    "mode" to if (game.connectivity.name == "ONLINE") "online" else "offline",
                    "dimension" to if (game.dimension.name == "D3") "3D" else "2D",
                    "score" to clampedScore,
                    "pointsEarned" to clampedPts,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val newPoints = (profile.totalPoints + clampedPts).coerceAtMost(100_000_000L)
            val newCoins = (profile.coins + coinsEarned).coerceAtMost(100_000_000L)
            val newGamesPlayed = (profile.gamesPlayed + 1L).coerceAtMost(1_000_000L)
            val newWins = (profile.wins + if (isWin) 1L else 0L).coerceAtMost(1_000_000L)
            val newLevel = ((newPoints / 250L) + 1L).coerceIn(1L, 999L)

            db.collection("users").document(uid).update(
                mapOf(
                    "totalPoints" to newPoints,
                    "coins" to newCoins,
                    "gamesPlayed" to newGamesPlayed,
                    "wins" to newWins,
                    "level" to newLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            db.collection("leaderboard").document(uid).update(
                mapOf(
                    "displayName" to sanitizeName(profile.displayName),
                    "avatarTag" to avatarTagFrom(profile.displayName),
                    "totalPoints" to newPoints,
                    "wins" to newWins,
                    "gamesPlayed" to newGamesPlayed,
                    "favoriteGame" to game.title.take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createMatchRoom(
        hostName: String,
        gameType: String
    ): Result<String> {
        val path = "match_rooms"
        return try {
            val uid = requireUserId()
            val roomRef = db.collection("match_rooms").document()
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val random = SecureRandom()
            val code = (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
            val saltBytes = ByteArray(16).also { random.nextBytes(it) }
            val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
            val initialBoard = if (gameType == "CYBER_DUEL") "0,0" else "........."

            roomRef.set(
                mapOf(
                    "roomCode" to code,
                    "gameType" to gameType,
                    "hostUid" to uid,
                    "hostName" to sanitizeName(hostName),
                    "memberUids" to listOf(uid),
                    "status" to "waiting",
                    "boardState" to initialBoard,
                    "turnUid" to uid,
                    "roomSalt" to saltHex,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(roomRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinMatchRoom(room: MatchRoom, guestName: String): Result<Unit> {
        val path = "match_rooms/${room.id}"
        return try {
            val uid = requireUserId()
            require(room.id.isNotBlank()) { "ID room tidak valid." }
            require(room.hostUid != uid) { "Kamu adalah pembuat room ini." }
            require(room.status == "waiting") { "Room sudah dimulai atau selesai." }

            db.collection("match_rooms").document(room.id).update(
                mapOf(
                    "guestUid" to uid,
                    "guestName" to sanitizeName(guestName),
                    "memberUids" to listOf(room.hostUid, uid),
                    "status" to "playing",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinRoomByCode(roomCodeInput: String, guestName: String): Result<String> {
        val code = roomCodeInput.trim().uppercase(Locale.US)
        val path = "match_rooms"
        return try {
            val uid = requireUserId()
            require(code.length in 4..12) { "Kode room harus 4-12 karakter." }
            val snap = db.collection("match_rooms")
                .whereEqualTo("status", "waiting")
                .limit(40)
                .get()
                .await()
            val match = snap.toObjects(MatchRoom::class.java).firstOrNull { it.roomCode == code }
                ?: return Result.failure(IllegalArgumentException("Kode room '$code' tidak ditemukan atau sudah penuh."))
            if (match.hostUid == uid) {
                return Result.success(match.id)
            }
            joinMatchRoom(match, guestName).getOrThrow()
            Result.success(match.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun makeMatchMove(
        room: MatchRoom,
        newBoardState: String,
        nextTurnUid: String,
        newStatus: String,
        winnerUid: String?
    ): Result<Unit> {
        val path = "match_rooms/${room.id}"
        return try {
            val uid = requireUserId()
            require(room.id.isNotBlank())
            require(uid in room.memberUids) { "Kamu bukan peserta room ini." }
            val updates = mutableMapOf<String, Any>(
                "boardState" to newBoardState.take(200),
                "turnUid" to nextTurnUid,
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (winnerUid != null) {
                updates["winnerUid"] = winnerUid
            }
            db.collection("match_rooms").document(room.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelMatchRoom(room: MatchRoom): Result<Unit> {
        val path = "match_rooms/${room.id}"
        return try {
            val uid = requireUserId()
            require(room.hostUid == uid && room.status == "waiting")
            db.collection("match_rooms").document(room.id).update(
                mapOf(
                    "status" to "finished",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendEncryptedMessage(
        room: MatchRoom,
        senderName: String,
        plaintext: String
    ): Result<Unit> {
        val cleanText = plaintext.trim()
        if (cleanText.isEmpty()) return Result.success(Unit)
        val path = "match_rooms/${room.id}/messages"
        return try {
            val uid = requireUserId()
            require(uid in room.memberUids) { "Hanya peserta room yang dapat mengirim chat." }
            val envelope = SecurityCryptoEngine.encryptRoomChatMessage(
                roomId = room.id,
                roomSalt = room.roomSalt,
                plaintext = cleanText.take(500)
            )
            val msgRef = db.collection("match_rooms").document(room.id)
                .collection("messages").document()
            msgRef.set(
                mapOf(
                    "roomId" to room.id,
                    "senderUid" to uid,
                    "senderName" to sanitizeName(senderName),
                    "cipherText" to envelope.cipherText,
                    "iv" to envelope.iv,
                    "memberUids" to room.memberUids,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
