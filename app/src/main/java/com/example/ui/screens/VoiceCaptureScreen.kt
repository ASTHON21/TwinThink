package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.audio.AsrEngineStatus
import com.example.data.audio.SherpaCaptureController
import com.example.data.model.DialecticNodeEntity
import com.example.data.model.DictionaryTermEntity
import com.example.data.model.PersonaAvatarEntity
import com.example.data.model.Perspective
import com.example.data.model.TuningSettings
import com.example.ui.components.HairlineWaveformBar
import com.example.ui.components.IconicMascotFace
import com.example.ui.components.VesicaPiscisVisualizer
import com.example.ui.components.parsedExpression
import com.example.ui.theme.LocalDialecticSurfaces
import com.example.ui.theme.MonoLabelStyle
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceCaptureScreen(
    initialPerspective: Perspective,
    previousNode: DialecticNodeEntity?,
    tuningSettings: TuningSettings,
    dictionaryTerms: List<DictionaryTermEntity> = emptyList(),
    activeSpeaker: PersonaAvatarEntity? = null,
    onBack: () -> Unit,
    onCommit: (
        perspective: Perspective,
        transcript: String,
        audioFilePath: String?,
        durationSec: Int,
        waveformCsv: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val surfaces = LocalDialecticSurfaces.current
    val coroutineScope = rememberCoroutineScope()

    var perspective by remember { mutableStateOf(initialPerspective) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionNotice by remember { mutableStateOf<String?>(null) }
    var showContextTurn by remember { mutableStateOf(false) }
    var turnCounter by remember { mutableIntStateOf(1) }
    var isCommitting by remember { mutableStateOf(false) }

    val spectrumBars = remember {
        mutableStateListOf<Float>().apply {
            repeat(36) { add(0.12f) }
        }
    }
    val recordedSamples = remember { mutableListOf<Float>() }

    val captureController = remember { SherpaCaptureController(context) }
    val asrState by captureController.state.collectAsStateWithLifecycle()

    LaunchedEffect(tuningSettings, dictionaryTerms) {
        captureController.updateRuntimeConfig(
            autoPunctuate = tuningSettings.philosophicalPunctuation,
            silenceThresholdSec = tuningSettings.silenceThresholdSec,
            dictionary = dictionaryTerms
        )
    }

    fun finishAndDiscard() {
        if (isCommitting) return
        captureController.stopAndDiscard()
        onBack()
    }

    fun finishAndCommit() {
        if (isCommitting) return
        isCommitting = true
        coroutineScope.launch {
            val result = captureController.stopAndKeep()
            val waveformCsv = if (recordedSamples.isEmpty()) {
                spectrumBars.takeLast(12).joinToString(",") { String.format(Locale.US, "%.2f", it) }
            } else {
                val step = (recordedSamples.size / 12).coerceAtLeast(1)
                recordedSamples.chunked(step).take(12).map { chunk ->
                    chunk.average().toFloat().coerceIn(0.12f, 1f)
                }.joinToString(",") { String.format(Locale.US, "%.2f", it) }
            }
            onCommit(
                perspective,
                result.transcript,
                result.audioFile?.absolutePath,
                result.durationSec,
                waveformCsv
            )
        }
    }

    fun beginCapturePipeline() {
        permissionNotice = null
        captureController.startCapture(
            acousticMode = tuningSettings.acousticMode,
            autoPunctuate = tuningSettings.philosophicalPunctuation,
            silenceThresholdSec = tuningSettings.silenceThresholdSec,
            dictionary = dictionaryTerms,
            onWaveformSample = {},
            onAutoSealTriggered = {
                coroutineScope.launch {
                    finishAndCommit()
                }
            }
        )
    }

    BackHandler {
        finishAndDiscard()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            beginCapturePipeline()
        } else {
            permissionNotice = "未获得麦克风权限，已切换为静默文字速记模式。"
        }
    }

    LaunchedEffect(Unit) {
        if (hasMicPermission) {
            beginCapturePipeline()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Update 36-bar spectrum cleanly on Main thread whenever a new PCM frame arrives
    LaunchedEffect(asrState.elapsedMillis) {
        if (asrState.isCapturing && asrState.elapsedMillis > 0L) {
            recordedSamples.add(asrState.amplitude)
            spectrumBars.removeAt(0)
            spectrumBars.add(asrState.amplitude)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            captureController.releaseOnDispose()
        }
    }

    val isRecordingActive = asrState.isCapturing && !asrState.isPaused
    val timeFormatted = remember(asrState.elapsedMillis) {
        val totalSec = asrState.elapsedMillis / 1000L
        val m = totalSec / 60L
        val s = totalSec % 60L
        val cs = (asrState.elapsedMillis % 1000L) / 10L
        "%02d:%02d.%02d".format(m, s, cs)
    }
    val currentClock = remember {
        SimpleDateFormat("hh:mm a", Locale.US).format(Date())
    }
    val combinedNotice = permissionNotice ?: asrState.statusNotice

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { finishAndDiscard() },
                    modifier = Modifier.testTag("voice_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecordingActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                    Text(
                        text = when {
                            asrState.engineStatus == AsrEngineStatus.LOADING_MODEL -> "模型装载中"
                            isRecordingActive -> stringResource(R.string.voice_status_recording)
                            else -> stringResource(R.string.voice_status_paused)
                        },
                        style = MonoLabelStyle.copy(fontSize = 11.sp, letterSpacing = 1.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = timeFormatted,
                    style = MonoLabelStyle.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Perspective Segmented Switcher
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = asrState.engineStatus.labelZh,
                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (hasMicPermission) asrState.dbfsLabel else "TEXT ONLY",
                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(surfaces.surfaceLowest)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PerspectiveSegmentButton(
                        selected = perspective == Perspective.SUBJECT,
                        label = "主体 · 自我 (EGO)",
                        icon = Icons.Default.Psychology,
                        onClick = {
                            perspective = Perspective.SUBJECT
                            turnCounter++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_perspective_subject")
                    )
                    PerspectiveSegmentButton(
                        selected = perspective == Perspective.OBJECT,
                        label = "客体 · 旁观者",
                        icon = Icons.Default.Visibility,
                        onClick = {
                            perspective = Perspective.OBJECT
                            turnCounter++
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_perspective_object")
                    )
                }
            }

            // Central Vesica Piscis Acoustic Visualizer + Speaking Mascot Head
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeSpeaker != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 14.dp)
                    ) {
                        IconicMascotFace(
                            expression = activeSpeaker.parsedExpression(),
                            fillColor = Color(activeSpeaker.fillColorArgb),
                            strokeColor = Color(activeSpeaker.strokeColorArgb),
                            size = 64.dp,
                            isSpeaking = isRecordingActive,
                            isAnimatingVoice = isRecordingActive && asrState.amplitude > 0.14f,
                            contentDescriptionText = "${activeSpeaker.name} 录音中"
                        )
                        Text(
                            text = "${activeSpeaker.name} · 发声",
                            style = MonoLabelStyle.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                VesicaPiscisVisualizer(
                    activePerspective = perspective,
                    amplitude = asrState.amplitude,
                    isActive = isRecordingActive
                )
            }

            // Permission rationale banner if denied
            if (!hasMicPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(surfaces.surfaceContainer)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.voice_permission_rationale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(stringResource(R.string.voice_permission_grant))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Live Transcription Stream Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaces.surfaceLow)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (asrState.engineStatus == AsrEngineStatus.READY_STREAMING && isRecordingActive)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                        Text(
                            text = "实时心绪捕捉",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "沉寂封存阈值 ${String.format(Locale.US, "%.1f", tuningSettings.silenceThresholdSec)}s",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // VAD silence countdown hairline indicator
                if (asrState.silenceProgress > 0.02f) {
                    LinearProgressIndicator(
                        progress = { asrState.silenceProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .testTag("vad_silence_progress"),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = surfaces.surfaceHigh
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (perspective == Perspective.SUBJECT) "[主体 / EGO / 0$turnCounter]" else "[客体 / WITNESS / 0$turnCounter]",
                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(surfaces.surfaceHighest)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "$currentClock · 16kHz 中英双语流式",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Committed text editor + live streaming partial preview
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 108.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (asrState.committedText.isEmpty() && asrState.partialText.isEmpty()) {
                            Text(
                                text = if (perspective == Perspective.SUBJECT) {
                                    "请开口述说（支持中英双语及混说），端侧引擎将实时转录；或在此轻触键入…"
                                } else {
                                    "以冷静旁观者的视角，用中文或英文说出对前言的审视与诘问…"
                                },
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 19.sp,
                                    lineHeight = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        BasicTextField(
                            value = asrState.committedText,
                            onValueChange = captureController::updateCommittedText,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 19.sp,
                                lineHeight = 28.sp,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp)
                                .testTag("voice_transcript_input")
                        )
                    }

                    if (asrState.partialText.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(surfaces.surfaceContainer)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "流式识别",
                                style = MonoLabelStyle.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(surfaces.surfaceHigh)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                            Text(
                                text = asrState.partialText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("voice_partial_stream_text")
                            )
                        }
                    }
                }

                combinedNotice?.let { notice ->
                    Text(
                        text = notice,
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // 36 hairline spectrum bars
                HairlineWaveformBar(
                    amplitudes = spectrumBars,
                    progress = 0.55f,
                    maxHeight = 26.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Context Reference (Previous Turn)
            if (previousNode != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (showContextTurn) 0.92f else 0.5f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(surfaces.surfaceContainer)
                        .clickable { showContextTurn = !showContextTurn }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "前置辩题回顾 · CONTEXT REFERENCE",
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Icon(
                            imageVector = if (showContextTurn) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    AnimatedVisibility(visible = showContextTurn) {
                        Text(
                            text = previousNode.content,
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Control Dock: 作废 / Pause-Resume Mic / 铭刻
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(surfaces.surfaceContainer)
                            .clickable(enabled = !isCommitting) { finishAndDiscard() }
                            .testTag("voice_discard_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.voice_action_discard),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.voice_action_discard),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(surfaces.surfaceHigh)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
                        .clickable(enabled = !isCommitting) {
                            if (!hasMicPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else if (!asrState.isCapturing && !asrState.isPaused) {
                                beginCapturePipeline()
                            } else if (asrState.isPaused) {
                                captureController.resume()
                            } else {
                                captureController.pause()
                            }
                        }
                        .testTag("voice_pause_resume_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecordingActive) Icons.Default.Mic else Icons.Default.Pause,
                            contentDescription = "录音控制",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(surfaces.surfaceContainer)
                            .clickable(enabled = !isCommitting) { finishAndCommit() }
                            .testTag("voice_commit_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.voice_action_commit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.voice_action_commit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PerspectiveSegmentButton(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalDialecticSurfaces.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else surfaces.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
