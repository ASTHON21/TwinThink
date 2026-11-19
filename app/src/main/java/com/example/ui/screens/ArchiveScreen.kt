package com.example.ui.screens

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.export.LocalExportManager
import com.example.data.model.ArchiveFilter
import com.example.data.model.DialecticNodeEntity
import com.example.data.model.DialecticSessionEntity
import com.example.data.model.MascotExpression
import com.example.data.model.PersonaAvatarEntity
import com.example.data.model.Perspective
import com.example.data.model.SessionWithNodes
import com.example.data.model.TuningSettings
import com.example.ui.components.ChatInputComposerBar
import com.example.ui.components.ChatMessageBubbleItem
import com.example.ui.components.MascotWorkshopDialog
import com.example.ui.components.ResidentMascotBar
import com.example.ui.theme.LocalDialecticSurfaces
import com.example.ui.theme.MonoLabelStyle
import com.example.ui.viewmodel.ArchiveMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArchiveScreen(
    sessions: List<SessionWithNodes>,
    selectedSession: SessionWithNodes?,
    metrics: ArchiveMetrics,
    personas: List<PersonaAvatarEntity>,
    activeSpeaker: PersonaAvatarEntity?,
    activePerspective: Perspective,
    tuningSettings: TuningSettings,
    autoSocraticReply: Boolean,
    isAnalyzing: Boolean,
    searchQuery: String,
    activeFilter: ArchiveFilter,
    playingNodeId: Long?,
    playbackProgress: Float,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (ArchiveFilter) -> Unit,
    onResetFilters: () -> Unit,
    onSelectSession: (Long) -> Unit,
    onOpenSessionInDialogue: (Long) -> Unit,
    onNavigateToDialogue: () -> Unit = {},
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
    onSubmitTextInArchive: (String, Long?) -> Unit,
    onOpenVoiceCaptureFromArchive: (Long?) -> Unit,
    onFollowUp: (DialecticNodeEntity) -> Unit,
    onBranchInquiry: (DialecticNodeEntity) -> Unit,
    onToggleAudioPlayback: (DialecticNodeEntity) -> Unit,
    onToggleResolved: (DialecticSessionEntity) -> Unit,
    onDeleteSession: (DialecticSessionEntity) -> Unit,
    onBuildMarkdownExport: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val surfaces = LocalDialecticSurfaces.current
    var showExportDialog by remember { mutableStateOf(false) }
    var sessionPendingDelete by remember { mutableStateOf<DialecticSessionEntity?>(null) }
    var inputText by remember { mutableStateOf("") }
    var showMascotWorkshop by remember { mutableStateOf(false) }
    var editingPersona by remember { mutableStateOf<PersonaAvatarEntity?>(null) }
    val chatListState = rememberLazyListState()

    val markdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            val md = onBuildMarkdownExport()
            val result = LocalExportManager.writeTextToUri(context, uri, md)
            Toast.makeText(
                context,
                if (result.isSuccess) "已成功导出 Markdown 辩证手稿" else "导出失败：${result.exceptionOrNull()?.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
        showExportDialog = false
    }

    val monthLabel = remember {
        SimpleDateFormat("MM.yyyy", Locale.US).format(Date())
    }

    val selectedNodes = selectedSession?.nodes.orEmpty()
    LaunchedEffect(selectedSession?.session?.id, selectedNodes.size) {
        if (selectedNodes.isNotEmpty()) {
            chatListState.animateScrollToItem(selectedNodes.size)
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
            // Top Archive Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.header_archive_title),
                    style = MonoLabelStyle.copy(fontSize = 11.sp, letterSpacing = 1.6.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = {
                            val next = when (activeFilter) {
                                ArchiveFilter.SUBJECT -> ArchiveFilter.OBJECT
                                ArchiveFilter.OBJECT -> ArchiveFilter.ALL
                                else -> ArchiveFilter.SUBJECT
                            }
                            onFilterChange(next)
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = surfaces.surfaceContainer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("archive_swap_filter_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = stringResource(R.string.cd_toggle_perspective),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_swap_subject_object),
                                style = MonoLabelStyle.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                if (sessions.isEmpty()) {
                                    Toast.makeText(context, "暂无可导出的思辨档案", Toast.LENGTH_SHORT).show()
                                } else {
                                    showExportDialog = true
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                            .testTag("archive_export_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = stringResource(R.string.cd_export_archive),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_export),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Resident Iconic Mascot Bar (clicking an avatar head switches into its speaking stage!)
            ResidentMascotBar(
                personas = personas,
                activeSpeakerId = activeSpeaker?.id,
                isSpeakingPulse = isAnalyzing || inputText.isNotEmpty(),
                onSelectSpeaker = { persona ->
                    onSelectSpeaker(persona)
                },
                onEditPersona = { persona ->
                    editingPersona = persona
                    showMascotWorkshop = true
                },
                onAddPersona = {
                    editingPersona = null
                    showMascotWorkshop = true
                }
            )

            // Compact Search & Telemetry Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(surfaces.surfaceContainer)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(15.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.archive_search_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("archive_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除检索",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onSearchQueryChange("") }
                        )
                    }
                }

                Text(
                    text = "$monthLabel · ${metrics.totalCount}记录",
                    style = MonoLabelStyle.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ArchiveFilter.entries.forEach { filter ->
                    val selected = activeFilter == filter
                    val pillText = when (filter) {
                        ArchiveFilter.ALL -> "${filter.labelZh} (${metrics.totalCount})"
                        ArchiveFilter.AUDIO -> "${filter.labelZh} (${metrics.audioCount})"
                        else -> filter.labelZh
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else surfaces.surfaceContainer
                            )
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("filter_pill_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = pillText,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Session Switcher & Chat-Style Content Stream
            if (sessions.isEmpty()) {
                val isGlobalEmpty = metrics.totalCount == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(surfaces.surfaceLow)
                            .padding(24.dp)
                            .testTag(if (isGlobalEmpty) "archive_zero_empty_state" else "archive_filtered_empty_state"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Grain,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = if (isGlobalEmpty) "尚无使用记录" else stringResource(R.string.archive_empty_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (isGlobalEmpty) {
                                "点击上方任意线性形象头像即可进入发声阶段，并直接在下方手动输入或语音开启首条聊天式记录（#001）。"
                            } else {
                                stringResource(R.string.archive_empty_subtitle)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = if (isGlobalEmpty) onNavigateToDialogue else onResetFilters,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.testTag(
                                if (isGlobalEmpty) "empty_archive_start_dialogue_button" else "reset_archive_filters_button"
                            )
                        ) {
                            Text(
                                text = if (isGlobalEmpty) "前往对话页" else stringResource(R.string.archive_reset_filters),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            } else {
                // Horizontal Session Tabs so user can switch between recorded threads cleanly
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.session.id }) { item ->
                        val isCurrent = item.session.id == selectedSession?.session?.id
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCurrent) MaterialTheme.colorScheme.primary else surfaces.surfaceContainer)
                                .clickable { onSelectSession(item.session.id) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("archive_session_chip_${item.session.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${item.session.sessionCode} ${item.session.title}",
                                style = MonoLabelStyle.copy(fontSize = 11.sp),
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 180.dp)
                            )
                            Text(
                                text = "${item.nodes.size}",
                                style = MonoLabelStyle.copy(fontSize = 10.sp),
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Active Archive Record Metadata Header
                selectedSession?.let { active ->
                    val s = active.session
                    val relativeTime = remember(s.updatedAt) {
                        DateUtils.getRelativeTimeSpanString(
                            s.updatedAt,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = relativeTime,
                                style = MonoLabelStyle.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text("·", style = MonoLabelStyle, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = s.conclusionText.ifBlank { "${active.nodes.size}回合" },
                                style = MonoLabelStyle.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (s.isResolved) MaterialTheme.colorScheme.primary else surfaces.surfaceContainer)
                                    .clickable { onToggleResolved(s) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (s.isResolved) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (s.isResolved) "已闭环" else "闭环",
                                    style = MonoLabelStyle.copy(fontSize = 10.sp),
                                    color = if (s.isResolved) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onOpenSessionInDialogue(s.id) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "全屏对话",
                                    style = MonoLabelStyle.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "在对话页打开",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "删除档案",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { sessionPendingDelete = s }
                            )
                        }
                    }

                    // Chat-Style Bubbles Stream for the Selected Record
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("archive_chat_stream"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (active.nodes.isEmpty()) {
                            item(key = "empty_record_hint") {
                                Text(
                                    text = "该记录暂无发言节点。可点击上方形象头进入发声阶段，并在下方直接输入或录音补充。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(surfaces.surfaceLow)
                                        .padding(16.dp)
                                )
                            }
                        } else {
                            itemsIndexed(active.nodes, key = { _, node -> node.id }) { _, node ->
                                ChatMessageBubbleItem(
                                    node = node,
                                    personas = personas,
                                    highContrastInversion = tuningSettings.highContrastInversion,
                                    isPlaying = playingNodeId == node.id,
                                    playbackProgress = if (playingNodeId == node.id) playbackProgress else 0f,
                                    onClickCard = {},
                                    onToggleAudio = { onToggleAudioPlayback(node) },
                                    onFollowUp = { onFollowUp(node) },
                                    onBranchInquiry = { onBranchInquiry(node) }
                                )
                            }
                        }

                        item(key = "archive_analyzing_indicator") {
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
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "客体形象正在记录中回应...",
                                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Sticky Manual Input + Voice Capture Composer right in Archive Screen
            ChatInputComposerBar(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                activeSpeaker = activeSpeaker,
                activePerspective = activePerspective,
                autoSocraticReply = autoSocraticReply,
                hasActiveSession = true,
                placeholderWhenEmptySession = null,
                onToggleAutoSocraticReply = onToggleAutoSocraticReply,
                onCycleSpeaker = onCycleSpeaker,
                onOpenVoiceCapture = {
                    onOpenVoiceCaptureFromArchive(selectedSession?.session?.id)
                },
                onSend = {
                    val textToSend = inputText
                    inputText = ""
                    onSubmitTextInArchive(textToSend, selectedSession?.session?.id)
                },
                onRequireSession = {},
                inputFieldTestTag = "archive_input_field",
                sendButtonTestTag = "archive_send_button",
                voiceButtonTestTag = "archive_voice_button"
            )
        }
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

    if (showExportDialog) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(surfaces.surfaceHigh)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.export_dialog_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showExportDialog = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            markdownLauncher.launch("双生思辨_手稿_$stamp.md")
                        }
                        .padding(12.dp)
                        .testTag("export_markdown_option"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.export_markdown_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.export_markdown_desc),
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            showExportDialog = false
                            LocalExportManager.printDialecticCards(context, sessions)
                        }
                        .padding(12.dp)
                        .testTag("export_print_option"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.export_print_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.export_print_desc),
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                            showExportDialog = false
                            LocalExportManager.shareText(
                                context,
                                "双生思辨 · 本地哲学手稿",
                                onBuildMarkdownExport()
                            )
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "本地文本发送至其他应用",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "调用系统共享面板 · 无需云端服务器",
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    sessionPendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            containerColor = surfaces.surfaceHigh,
            title = {
                Text(
                    text = "抹除该思辨档案？",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "将从本地 SQLite 数据库永久移除「${pending.title}」及其关联的声波录音片段，不可恢复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession(pending)
                        sessionPendingDelete = null
                    }
                ) {
                    Text("确认抹除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) {
                    Text("保留")
                }
            }
        )
    }
}
