package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.DialogueScreen
import com.example.ui.screens.TuningScreen
import com.example.ui.screens.VoiceCaptureScreen
import com.example.ui.theme.DialecticTheme
import com.example.ui.theme.LocalDialecticSurfaces
import com.example.ui.viewmodel.DialecticViewModel

private object Routes {
    const val DIALOGUE = "dialogue"
    const val ARCHIVE = "archive"
    const val TUNING = "tuning"
    const val VOICE_CAPTURE = "voice_capture"
}

private data class BottomTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialecticAppRoot(
                onUpdateSystemBars = { isDark ->
                    val style = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
            )
        }
    }
}

@Composable
fun DialecticAppRoot(
    onUpdateSystemBars: (isDark: Boolean) -> Unit,
    viewModel: DialecticViewModel = viewModel()
) {
    val tuningSettings by viewModel.tuningSettings.collectAsStateWithLifecycle()
    val activePerspective by viewModel.activePerspective.collectAsStateWithLifecycle()

    DialecticTheme(
        themeMode = tuningSettings.visualThemeMode,
        activePerspective = activePerspective
    ) {
        val surfaces = LocalDialecticSurfaces.current
        LaunchedEffect(surfaces.isDark) {
            onUpdateSystemBars(surfaces.isDark)
        }

        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: Routes.DIALOGUE
        val showBottomBar = currentRoute != Routes.VOICE_CAPTURE

        val tabs = listOf(
            BottomTab(
                route = Routes.DIALOGUE,
                labelResId = R.string.nav_dialogue,
                selectedIcon = Icons.Filled.ChatBubble,
                unselectedIcon = Icons.Outlined.ChatBubbleOutline
            ),
            BottomTab(
                route = Routes.ARCHIVE,
                labelResId = R.string.nav_archive,
                selectedIcon = Icons.Filled.HistoryEdu,
                unselectedIcon = Icons.Outlined.HistoryEdu
            ),
            BottomTab(
                route = Routes.TUNING,
                labelResId = R.string.nav_tuning,
                selectedIcon = Icons.Filled.GraphicEq,
                unselectedIcon = Icons.Outlined.GraphicEq
            )
        )

        val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
        val selectedArchiveSession by viewModel.selectedArchiveSession.collectAsStateWithLifecycle()
        val personas by viewModel.personas.collectAsStateWithLifecycle()
        val activeSpeaker by viewModel.activeSpeaker.collectAsStateWithLifecycle()
        val autoSocraticReply by viewModel.autoSocraticReply.collectAsStateWithLifecycle()
        val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
        val playingNodeId by viewModel.playingNodeId.collectAsStateWithLifecycle()
        val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
        val filteredSessions by viewModel.filteredArchiveSessions.collectAsStateWithLifecycle()
        val archiveMetrics by viewModel.archiveMetrics.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val archiveFilter by viewModel.archiveFilter.collectAsStateWithLifecycle()
        val dictionaryTerms by viewModel.dictionaryTerms.collectAsStateWithLifecycle()

        var pendingVoiceTargetSessionId by remember { mutableStateOf<Long?>(null) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        tonalElevation = 0.dp
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != tab.route) {
                                        navController.navigate(tab.route) {
                                            popUpTo(Routes.DIALOGUE) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelResId)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelResId),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = surfaces.surfaceHigh
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.DIALOGUE
                ) {
                    composable(Routes.DIALOGUE) {
                        DialogueScreen(
                            activeSession = activeSession,
                            activePerspective = activePerspective,
                            personas = personas,
                            activeSpeaker = activeSpeaker,
                            tuningSettings = tuningSettings,
                            autoSocraticReply = autoSocraticReply,
                            isAnalyzing = isAnalyzing,
                            playingNodeId = playingNodeId,
                            playbackProgress = playbackProgress,
                            onTogglePerspective = viewModel::togglePerspective,
                            onSelectSpeaker = viewModel::selectSpeaker,
                            onCycleSpeaker = viewModel::cycleSpeaker,
                            onSavePersona = viewModel::savePersona,
                            onDeletePersona = viewModel::deletePersona,
                            onToggleAutoSocraticReply = viewModel::toggleAutoSocraticReply,
                            onOpenVoiceCapture = {
                                viewModel.stopPlayback()
                                pendingVoiceTargetSessionId = activeSession?.session?.id
                                navController.navigate(Routes.VOICE_CAPTURE)
                            },
                            onSubmitText = { text -> viewModel.submitDialogueTurn(text) },
                            onFollowUp = viewModel::triggerFollowUp,
                            onBranchInquiry = viewModel::triggerBranchInquiry,
                            onToggleAudioPlayback = viewModel::toggleAudioPlayback,
                            onCreateNewSession = viewModel::createNewSession,
                            onToggleResolved = {
                                activeSession?.session?.let(viewModel::toggleSessionResolved)
                            }
                        )
                    }

                    composable(Routes.ARCHIVE) {
                        ArchiveScreen(
                            sessions = filteredSessions,
                            selectedSession = selectedArchiveSession,
                            metrics = archiveMetrics,
                            personas = personas,
                            activeSpeaker = activeSpeaker,
                            activePerspective = activePerspective,
                            tuningSettings = tuningSettings,
                            autoSocraticReply = autoSocraticReply,
                            isAnalyzing = isAnalyzing,
                            searchQuery = searchQuery,
                            activeFilter = archiveFilter,
                            playingNodeId = playingNodeId,
                            playbackProgress = playbackProgress,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onFilterChange = viewModel::setArchiveFilter,
                            onResetFilters = viewModel::resetArchiveFilters,
                            onSelectSession = viewModel::selectArchiveSession,
                            onOpenSessionInDialogue = { sessionId ->
                                viewModel.selectSession(sessionId)
                                navController.navigate(Routes.DIALOGUE) {
                                    popUpTo(Routes.DIALOGUE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToDialogue = {
                                navController.navigate(Routes.DIALOGUE) {
                                    popUpTo(Routes.DIALOGUE) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onSelectSpeaker = viewModel::selectSpeaker,
                            onCycleSpeaker = viewModel::cycleSpeaker,
                            onSavePersona = viewModel::savePersona,
                            onDeletePersona = viewModel::deletePersona,
                            onToggleAutoSocraticReply = viewModel::toggleAutoSocraticReply,
                            onSubmitTextInArchive = { text, sessionId ->
                                viewModel.submitDialogueTurn(text, targetSessionId = sessionId)
                            },
                            onOpenVoiceCaptureFromArchive = { sessionId ->
                                viewModel.stopPlayback()
                                pendingVoiceTargetSessionId = sessionId
                                navController.navigate(Routes.VOICE_CAPTURE)
                            },
                            onFollowUp = viewModel::triggerFollowUp,
                            onBranchInquiry = viewModel::triggerBranchInquiry,
                            onToggleAudioPlayback = viewModel::toggleAudioPlayback,
                            onToggleResolved = viewModel::toggleSessionResolved,
                            onDeleteSession = viewModel::deleteSession,
                            onBuildMarkdownExport = viewModel::buildMarkdownExport
                        )
                    }

                    composable(Routes.TUNING) {
                        TuningScreen(
                            settings = tuningSettings,
                            dictionaryTerms = dictionaryTerms,
                            onTogglePreviewPerspective = viewModel::togglePerspective,
                            onSaveSettings = viewModel::saveTuningSettings,
                            onAddTerm = viewModel::addDictionaryTerm,
                            onRemoveTerm = viewModel::removeDictionaryTerm,
                            onResetToOrigin = viewModel::resetTuningToOrigin,
                            onBuildJsonExport = viewModel::buildJsonExport
                        )
                    }

                    composable(Routes.VOICE_CAPTURE) {
                        val targetContextSession = selectedArchiveSession?.takeIf {
                            pendingVoiceTargetSessionId == it.session.id
                        } ?: activeSession
                        VoiceCaptureScreen(
                            initialPerspective = activePerspective,
                            previousNode = targetContextSession?.nodes?.lastOrNull(),
                            tuningSettings = tuningSettings,
                            dictionaryTerms = dictionaryTerms,
                            activeSpeaker = activeSpeaker,
                            onBack = { navController.popBackStack() },
                            onCommit = { perspective, transcript, audioPath, durationSec, waveformCsv ->
                                viewModel.setPerspective(perspective)
                                viewModel.commitVoiceCapture(
                                    perspective = perspective,
                                    transcript = transcript,
                                    audioFilePath = audioPath,
                                    durationSec = durationSec,
                                    waveformCsv = waveformCsv,
                                    targetSessionId = pendingVoiceTargetSessionId
                                )
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
