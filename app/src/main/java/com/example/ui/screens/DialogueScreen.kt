package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DialecticNodeEntity
import com.example.data.model.MascotExpression
import com.example.data.model.MascotPalette
import com.example.data.model.PersonaAvatarEntity
import com.example.data.model.Perspective
import com.example.data.model.SessionWithNodes
import com.example.data.model.TuningSettings
import com.example.ui.components.ChatInputComposerBar
import com.example.ui.components.ChatMessageBubbleItem
import com.example.ui.components.IconicMascotFace
import com.example.ui.components.MascotWorkshopDialog
import com.example.ui.components.ResidentMascotBar
import com.example.ui.components.parsedExpression
import com.example.ui.theme.LocalDialecticSurfaces
import com.example.ui.theme.MonoLabelStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogueScreen(
    activeSession: SessionWithNodes?,
    activePerspective: Perspective,
    personas: List<PersonaAvatarEntity>,
    activeSpeaker: PersonaAvatarEntity?,
    tuningSettings: TuningSettings,
    autoSocraticReply: Boolean,
    isAnalyzing: Boolean,
    playingNodeId: Long?,
    playbackProgress: Float,
    onTogglePerspective: () -> Unit,
    onSelectSpeaker: (PersonaAvatarEntity) -> Unit,
    onCycleSpeaker: () -> Unit,
    onSavePersona: (
        id: Long?,
        name: String,
        expression: MascotExpression,
        fillColorArgb: Long,
        strokeColorArgb: Long,
        perspective: Perspective
    ) -> Unit,
    onDeletePersona: (PersonaAvatarEntity) -> Unit,
    onToggleAutoSocraticReply: () -> Unit,
    onOpenVoiceCapture: () -> Unit,
    onSubmitText: (String) -> Unit,
    onFollowUp: (DialecticNodeEntity) -> Unit,
    onBranchInquiry: (DialecticNodeEntity) -> Unit,
    onToggleAudioPlayback: (DialecticNodeEntity) -> Unit,
    onCreateNewSession: (String, (() -> Unit)?) -> Unit,
    onToggleResolved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalDialecticSurfaces.current
    var inputText by remember { mutableStateOf("") }
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var pendingOpenVoiceAfterCreate by remember { mutableStateOf(false) }
    var newSessionTitle by remember { mutableStateOf("") }
    var focusedNodeId by remember { mutableStateOf<Long?>(null) }

    var showMascotWorkshop by remember { mutableStateOf(false) }
    var editingPersona by remember { mutableStateOf<PersonaAvatarEntity?>(null) }

    val listState = rememberLazyListState()
    val nodes = activeSession?.nodes.orEmpty()

    LaunchedEffect(nodes.size) {
        if (nodes.isNotEmpty()) {
            listState.animateScrollToItem(nodes.size)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
        ) {
            // Top Editorial Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = stringResource(R.string.header_dialogue_title),
                        style = MonoLabelStyle.copy(fontSize = 11.sp, letterSpacing = 1.4.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(surfaces.surfaceContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = stringResource(R.string.badge_local_offline),
                            style = MonoLabelStyle.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            pendingOpenVoiceAfterCreate = false
                            showNewSessionDialog = true
                        },
                        shape = RoundedCornerShape(99.dp),
                        color = surfaces.surfaceContainer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("new_session_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.action_new_session),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_new_session),
                                style = MonoLabelStyle.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = onTogglePerspective,
                        shape = RoundedCornerShape(99.dp),
                        color = surfaces.surfaceContainer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("header_invert_perspective_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = stringResource(R.string.cd_toggle_perspective),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_invert_perspective),
                                style = MonoLabelStyle.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Resident Iconic Mascot Bar (pinned at top of Dialogue screen)
            ResidentMascotBar(
                personas = personas,
                activeSpeakerId = activeSpeaker?.id,
                isSpeakingPulse = isAnalyzing || inputText.isNotEmpty(),
                onSelectSpeaker = onSelectSpeaker,
                onEditPersona = { persona ->
                    editingPersona = persona
                    showMascotWorkshop = true
                },
                onAddPersona = {
                    editingPersona = null
                    showMascotWorkshop = true
                }
            )

            // Active Session Title Strip
            if (activeSession != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activeSession.session.sessionCode} · ${activeSession.session.title}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (activeSession.session.isResolved) MaterialTheme.colorScheme.primary
                                else surfaces.surfaceContainer
                            )
                            .clickable(onClick = onToggleResolved)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("toggle_resolved_badge"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (activeSession.session.isResolved) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (activeSession.session.isResolved) "已闭环" else "标记闭环",
                            style = MonoLabelStyle.copy(fontSize = 10.sp),
                            color = if (activeSession.session.isResolved) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Chat-Style Dialogue Stream or Clean Zero State
            if (activeSession == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaces.surfaceLow)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(24.dp)
                            .testTag("dialogue_zero_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val leftMascot = personas.firstOrNull { it.perspective == Perspective.OBJECT.name }
                            val rightMascot = personas.firstOrNull { it.perspective == Perspective.SUBJECT.name }
                            IconicMascotFace(
                                expression = leftMascot?.parsedExpression() ?: MascotExpression.SKEPTICAL,
                                fillColor = Color(leftMascot?.fillColorArgb ?: MascotPalette.presets[1].fillArgb),
                                strokeColor = Color(leftMascot?.strokeColorArgb ?: MascotPalette.presets[1].strokeArgb),
                                size = 60.dp,
                                isSpeaking = activeSpeaker?.id == leftMascot?.id,
                                blinkCycleDurationMs = 3600
                            )
                            IconicMascotFace(
                                expression = rightMascot?.parsedExpression() ?: MascotExpression.CALM,
                                fillColor = Color(rightMascot?.fillColorArgb ?: MascotPalette.presets[0].fillArgb),
                                strokeColor = Color(rightMascot?.strokeColorArgb ?: MascotPalette.presets[0].strokeArgb),
                                size = 60.dp,
                                isSpeaking = activeSpeaker?.id == rightMascot?.id,
                                blinkCycleDurationMs = 3100
                            )
                        }
                        Text(
                            text = "双生形象已就位，尚无思辨对话",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "点击顶部常驻头像即可切换发声角色或设计新面相；开启新辩题后，下方将以头像+聊天气泡开启自我对话。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                pendingOpenVoiceAfterCreate = false
                                showNewSessionDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("empty_dialogue_new_session_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "开启新辩题",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (nodes.isEmpty()) {
                        item(key = "blank_session_prompt") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(surfaces.surfaceLow)
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "READY · 聊天思辨画布已就绪",
                                    style = MonoLabelStyle.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "辩题已确立。选择上方任意线性形象头像即可作为发声方，在下方手动输入或使用端侧双语语音采集。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    itemsIndexed(nodes, key = { _, node -> node.id }) { index, node ->
                        val isLast = index == nodes.lastIndex
                        val isDimmed = tuningSettings.focusDimming && !isLast && focusedNodeId != node.id
                        val alpha = if (isDimmed) 0.45f else 1f

                        ChatMessageBubbleItem(
                            node = node,
                            personas = personas,
                            highContrastInversion = tuningSettings.highContrastInversion,
                            isPlaying = playingNodeId == node.id,
                            playbackProgress = if (playingNodeId == node.id) playbackProgress else 0f,
                            onClickCard = {
                                focusedNodeId = if (focusedNodeId == node.id) null else node.id
                            },
                            onToggleAudio = { onToggleAudioPlayback(node) },
                            onFollowUp = { onFollowUp(node) },
                            onBranchInquiry = { onBranchInquiry(node) },
                            modifier = Modifier.alpha(alpha)
                        )
                    }

                    item(key = "analyzing_indicator") {
                        AnimatedVisibility(
                            visible = isAnalyzing,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(surfaces.surfaceHigh)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "客体形象正在以「${tuningSettings.socraticStance.titleZh}」立场回应...",
                                    style = MonoLabelStyle.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (activeSession.session.synthesisSummary.isNotBlank() && nodes.isNotEmpty()) {
                        item(key = "synthesis_card") {
                            val session = activeSession.session
                            val tags = session.tagsCsv.split("·").map { it.trim() }.filter { it.isNotEmpty() }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(surfaces.surfaceLowest)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(14.dp)
                                    .testTag("synthesis_card"),
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
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.label_synthesis),
                                            style = MonoLabelStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (session.resonancePercent > 0) {
                                        Text(
                                            text = "共振 ${session.resonancePercent}%",
                                            style = MonoLabelStyle.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Text(
                                    text = session.synthesisSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (tags.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        tags.forEach { tag ->
                                            Text(
                                                text = tag,
                                                style = MonoLabelStyle.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(surfaces.surfaceContainer)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }
            }

            // Bottom Sticky Chat Composer
            ChatInputComposerBar(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                activeSpeaker = activeSpeaker,
                activePerspective = activePerspective,
                autoSocraticReply = autoSocraticReply,
                hasActiveSession = activeSession != null,
                placeholderWhenEmptySession = "请先点击「新辩题」确立命题...",
                onToggleAutoSocraticReply = onToggleAutoSocraticReply,
                onCycleSpeaker = onCycleSpeaker,
                onOpenVoiceCapture = {
                    if (activeSession == null) {
                        pendingOpenVoiceAfterCreate = true
                        showNewSessionDialog = true
                    } else {
                        onOpenVoiceCapture()
                    }
                },
                onSend = {
                    val textToSend = inputText
                    inputText = ""
                    onSubmitText(textToSend)
                },
                onRequireSession = {
                    pendingOpenVoiceAfterCreate = false
                    showNewSessionDialog = true
                }
            )
        }
    }

    if (showNewSessionDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewSessionDialog = false
                pendingOpenVoiceAfterCreate = false
            },
            containerColor = surfaces.surfaceHigh,
            title = {
                Text(
                    text = "开启新的自我辩证流",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                OutlinedTextField(
                    value = newSessionTitle,
                    onValueChange = { newSessionTitle = it },
                    label = { Text("辩题命题（可留空自动提炼）") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_session_title_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newSessionTitle
                        val openVoice = pendingOpenVoiceAfterCreate
                        newSessionTitle = ""
                        pendingOpenVoiceAfterCreate = false
                        showNewSessionDialog = false
                        onCreateNewSession(title) {
                            if (openVoice) onOpenVoiceCapture()
                        }
                    },
                    modifier = Modifier.testTag("confirm_new_session_button")
                ) {
                    Text("确立命题")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNewSessionDialog = false
                        pendingOpenVoiceAfterCreate = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    if (showMascotWorkshop) {
        MascotWorkshopDialog(
            editingPersona = editingPersona,
            defaultPerspective = activePerspective,
            onDismiss = { showMascotWorkshop = false },
            onSave = { id, name, expression, fillArgb, strokeArgb, perspective ->
                onSavePersona(id, name, expression, fillArgb, strokeArgb, perspective)
                showMascotWorkshop = false
            },
            onDelete = { persona ->
                onDeletePersona(persona)
                showMascotWorkshop = false
            }
        )
    }
}
