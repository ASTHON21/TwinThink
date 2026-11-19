package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DialecticNodeEntity
import com.example.data.model.MascotExpression
import com.example.data.model.MascotPalette
import com.example.data.model.PersonaAvatarEntity
import com.example.data.model.Perspective
import com.example.ui.theme.LocalDialecticSurfaces
import com.example.ui.theme.MonoLabelStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatMessageBubbleItem(
    node: DialecticNodeEntity,
    personas: List<PersonaAvatarEntity>,
    highContrastInversion: Boolean,
    isPlaying: Boolean,
    playbackProgress: Float,
    onClickCard: () -> Unit,
    onToggleAudio: () -> Unit,
    onFollowUp: () -> Unit,
    onBranchInquiry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalDialecticSurfaces.current
    val isRightSide = node.perspective == Perspective.SUBJECT.name
    val timeText = remember(node.createdAt) {
        SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(node.createdAt))
    }

    val matchedPersona = remember(node.speakerAvatarId, node.perspective, personas) {
        node.speakerAvatarId?.let { id -> personas.firstOrNull { it.id == id } }
            ?: personas.firstOrNull { it.perspective == node.perspective && it.isDefault }
            ?: personas.firstOrNull { it.perspective == node.perspective }
    }

    val displayName = node.speakerName.ifBlank {
        matchedPersona?.name ?: if (isRightSide) "主我" else "客体"
    }
    val fillColor = matchedPersona?.let { Color(it.fillColorArgb) }
        ?: Color(if (isRightSide) MascotPalette.presets[0].fillArgb else MascotPalette.presets[1].fillArgb)
    val strokeColor = matchedPersona?.let { Color(it.strokeColorArgb) }
        ?: Color(if (isRightSide) MascotPalette.presets[0].strokeArgb else MascotPalette.presets[1].strokeArgb)
    val expression = matchedPersona?.parsedExpression()
        ?: if (isRightSide) MascotExpression.CALM else MascotExpression.SKEPTICAL

    val bubbleShape = if (isRightSide) {
        RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
    }

    val bubbleBackground = when {
        isRightSide -> surfaces.surfaceContainer
        highContrastInversion -> surfaces.surfaceHighest
        else -> surfaces.surfaceHigh
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat_bubble_${node.id}"),
        horizontalArrangement = if (isRightSide) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isRightSide) {
            IconicMascotFace(
                expression = expression,
                fillColor = fillColor,
                strokeColor = strokeColor,
                size = 38.dp,
                isSpeaking = false,
                blinkCycleDurationMs = 3400 + ((node.id % 4).toInt() * 500),
                contentDescriptionText = "$displayName 头像"
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isRightSide) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isRightSide) {
                    Text(
                        text = timeText,
                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.78f)
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = timeText,
                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.78f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clip(bubbleShape)
                    .background(bubbleBackground)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = if (isRightSide) 0.34f else 0.22f
                        ),
                        shape = bubbleShape
                    )
                    .clickable(onClick = onClickCard)
                    .padding(horizontal = 13.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = node.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = if (isRightSide) FontStyle.Normal else FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!node.audioFilePath.isNullOrBlank()) {
                    AudioMemoPlayerRow(
                        isPlaying = isPlaying,
                        progress = playbackProgress,
                        durationSec = node.audioDurationSec.coerceAtLeast(1),
                        waveformCsv = node.waveformCsv,
                        onTogglePlay = onToggleAudio
                    )
                }
            }

            if (!isRightSide) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onFollowUp)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("follow_up_button_${node.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = stringResource(R.string.action_follow_up),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_follow_up),
                            style = MonoLabelStyle.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onBranchInquiry)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("branch_inquiry_button_${node.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallSplit,
                            contentDescription = stringResource(R.string.action_branch),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.action_branch),
                            style = MonoLabelStyle.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        if (isRightSide) {
            Spacer(modifier = Modifier.width(8.dp))
            IconicMascotFace(
                expression = expression,
                fillColor = fillColor,
                strokeColor = strokeColor,
                size = 38.dp,
                isSpeaking = false,
                blinkCycleDurationMs = 3100 + ((node.id % 4).toInt() * 500),
                contentDescriptionText = "$displayName 头像"
            )
        }
    }
}

@Composable
fun ChatInputComposerBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    activeSpeaker: PersonaAvatarEntity?,
    activePerspective: Perspective,
    autoSocraticReply: Boolean,
    hasActiveSession: Boolean,
    placeholderWhenEmptySession: String?,
    onToggleAutoSocraticReply: () -> Unit,
    onCycleSpeaker: () -> Unit,
    onOpenVoiceCapture: () -> Unit,
    onSend: () -> Unit,
    onRequireSession: () -> Unit,
    inputFieldTestTag: String = "dialogue_input_field",
    sendButtonTestTag: String = "send_node_button",
    voiceButtonTestTag: String = "open_voice_capture_button",
    modifier: Modifier = Modifier
) {
    val surfaces = LocalDialecticSurfaces.current
    val speakerName = activeSpeaker?.name ?: activePerspective.labelZh
    val canDirectType = hasActiveSession || placeholderWhenEmptySession == null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            if (activePerspective == Perspective.SUBJECT) MaterialTheme.colorScheme.primary
                            else surfaces.surfaceHighest
                        )
                        .clickable(onClick = onCycleSpeaker)
                        .padding(start = 4.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)
                        .testTag("quick_perspective_pill"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (activeSpeaker != null) {
                        IconicMascotFace(
                            expression = activeSpeaker.parsedExpression(),
                            fillColor = Color(activeSpeaker.fillColorArgb),
                            strokeColor = Color(activeSpeaker.strokeColorArgb),
                            size = 18.dp,
                            enableBlink = false
                        )
                    }
                    Text(
                        text = "$speakerName · 发声",
                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                        color = if (activePerspective == Perspective.SUBJECT) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(surfaces.surfaceContainer)
                        .clickable(onClick = onToggleAutoSocraticReply)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("auto_socratic_toggle")
                ) {
                    Text(
                        text = if (autoSocraticReply) "自动诘问: 开" else "纯手写双声: 关",
                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                        color = if (autoSocraticReply) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Text(
                text = "${inputText.length} / 280",
                style = MonoLabelStyle.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(surfaces.surfaceContainer)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaces.surfaceHigh)
                    .clickable(onClick = onOpenVoiceCapture)
                    .testTag(voiceButtonTestTag),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = stringResource(R.string.cd_voice_capture),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clickable(enabled = !canDirectType) { onRequireSession() },
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = when {
                            !canDirectType -> placeholderWhenEmptySession.orEmpty()
                            activePerspective == Perspective.SUBJECT -> "以「$speakerName」手动输入或点左侧语音..."
                            else -> "以「$speakerName」视角回复..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (canDirectType) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { if (it.length <= 280) onInputTextChange(it) },
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(inputFieldTestTag)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (canDirectType && inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                        else surfaces.surfaceHigh
                    )
                    .clickable(enabled = !canDirectType || inputText.isNotBlank()) {
                        if (!canDirectType) {
                            onRequireSession()
                        } else {
                            onSend()
                        }
                    }
                    .testTag(sendButtonTestTag),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NorthEast,
                    contentDescription = stringResource(R.string.cd_send_node),
                    tint = if (canDirectType && inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
