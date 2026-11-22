package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.GameConnectivity
import com.example.data.model.RedGameItem
import com.example.data.model.UiState
import com.example.data.repository.RedGameRepository
import com.example.data.repository.attemptAutoSignIn
import com.example.data.repository.authStateFlow
import com.example.data.repository.signOutUser
import com.example.ui.components.RedGameBackdrop
import com.example.ui.components.RedGameBottomBar
import com.example.ui.components.RedGameHeader
import com.example.ui.components.RedGameTab
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GamesCatalogScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MabarScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RankScreen
import com.example.ui.screens.games.OfflineGameArenaScreen
import com.example.ui.theme.RedGameTheme
import com.example.ui.theme.RedNeon
import com.example.ui.theme.RedNeonBright
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.RedGameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RedGameTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { Firebase.auth }
    val currentUser: FirebaseUser? by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            val credentialManager = CredentialManager.create(context)
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MainRedGameApp(
                currentUserId = user.uid,
                userDisplayName = user.displayName,
                userEmail = user.email
            )
        }
    }
}

@Composable
private fun MainRedGameApp(
    currentUserId: String,
    userDisplayName: String?,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: RedGameViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                RedGameViewModel(
                    repository = RedGameRepository(db),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val vaultState by viewModel.vaultState.collectAsStateWithLifecycle()
    val scoresState by viewModel.scoresState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val waitingRoomsState by viewModel.waitingRoomsState.collectAsStateWithLifecycle()
    val myRoomsState by viewModel.myRoomsState.collectAsStateWithLifecycle()
    val activeRoomState by viewModel.activeRoomState.collectAsStateWithLifecycle()
    val activeRoomId by viewModel.activeRoomId.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    var currentTab by remember { mutableStateOf(RedGameTab.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showFilterBar by remember { mutableStateOf(false) }
    var activeOfflineGame by remember { mutableStateOf<RedGameItem?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2800L)
            viewModel.clearToast()
        }
    }

    fun launchGame(game: RedGameItem) {
        if (game.connectivity == GameConnectivity.ONLINE) {
            currentTab = RedGameTab.MABAR
            val type = if (game.id == "mabar_cyber_duel") "CYBER_DUEL" else "TICTACTOE_3X3"
            viewModel.createRoom(type)
        } else {
            activeOfflineGame = game
        }
    }

    val offlineGame = activeOfflineGame
    if (offlineGame != null) {
        OfflineGameArenaScreen(
            game = offlineGame,
            onExit = { activeOfflineGame = null },
            onScoreSubmitted = { score, pts, won ->
                viewModel.submitOfflineGameScore(offlineGame, score, pts, won)
            }
        )
        return
    }

    if (currentTab != RedGameTab.HOME && activeRoomId == null) {
        BackHandler {
            currentTab = RedGameTab.HOME
        }
    }

    RedGameBackdrop {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxSize()
            ) {
                RedGameHeader(
                    avatarTag = profile?.avatarTag ?: "RG",
                    coins = profile?.coins ?: 150L,
                    totalPoints = profile?.totalPoints ?: 0L,
                    searchQuery = searchQuery,
                    onSearchChange = {
                        searchQuery = it
                        if (currentTab != RedGameTab.HOME && currentTab != RedGameTab.GAMES) {
                            currentTab = RedGameTab.GAMES
                        }
                    },
                    showSearchBar = currentTab == RedGameTab.HOME || currentTab == RedGameTab.GAMES,
                    onToggleFilter = { showFilterBar = !showFilterBar },
                    onProfileClick = { currentTab = RedGameTab.PROFILE }
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (currentTab) {
                        RedGameTab.HOME -> HomeScreen(
                            profile = profile,
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            showFilterBar = showFilterBar,
                            onSelectFilter = { selectedFilter = it },
                            onLaunchGame = ::launchGame,
                            onNavigateTab = { currentTab = it },
                            onClaimDailyReward = viewModel::claimDailyReward
                        )
                        RedGameTab.GAMES -> GamesCatalogScreen(
                            searchQuery = searchQuery,
                            selectedFilter = selectedFilter,
                            onSelectFilter = { selectedFilter = it },
                            onLaunchGame = ::launchGame
                        )
                        RedGameTab.MABAR -> MabarScreen(
                            currentUserId = currentUserId,
                            waitingRoomsState = waitingRoomsState,
                            myRoomsState = myRoomsState,
                            activeRoomState = activeRoomState,
                            activeRoomId = activeRoomId,
                            messagesState = messagesState,
                            decryptMessages = viewModel::decryptChatMessages,
                            onCreateRoom = viewModel::createRoom,
                            onJoinRoom = viewModel::joinRoom,
                            onJoinByCode = viewModel::joinRoomByCode,
                            onOpenRoom = viewModel::openRoom,
                            onCloseRoom = viewModel::closeRoom,
                            onTicTacToeMove = viewModel::makeTicTacToeMove,
                            onCyberDuelTap = viewModel::makeCyberDuelTap,
                            onCancelWaitingRoom = viewModel::cancelWaitingRoom,
                            onClaimMatchReward = viewModel::claimMatchCompletionIfNeeded,
                            onSendMessage = viewModel::sendChatMessage
                        )
                        RedGameTab.RANK -> RankScreen(
                            currentUserId = currentUserId,
                            profile = profile,
                            leaderboardState = leaderboardState
                        )
                        RedGameTab.PROFILE -> ProfileScreen(
                            currentUserId = currentUserId,
                            profile = profile,
                            vaultState = vaultState,
                            scoresState = scoresState,
                            decryptVault = viewModel::decryptVaultJson,
                            onRotateKey = viewModel::rotateEncryptionKey,
                            onSignOut = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }

            // Floating bottom navigation bar
            RedGameBottomBar(
                currentTab = currentTab,
                onSelectTab = { tab ->
                    if (tab != RedGameTab.MABAR && activeRoomId != null) {
                        viewModel.closeRoom()
                    }
                    currentTab = tab
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 620.dp)
            )

            // Floating Toast notification
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = RedNeon,
                    border = BorderStroke(1.dp, RedNeonBright),
                    shadowElevation = 10.dp
                ) {
                    Text(
                        text = toastMessage.orEmpty(),
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
