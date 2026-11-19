package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MascotExpression
import com.example.data.model.MascotPalette
import com.example.data.model.PersonaAvatarEntity
import com.example.data.model.Perspective
import com.example.ui.theme.LocalDialecticSurfaces
import com.example.ui.theme.MonoLabelStyle

@Composable
fun IconicMascotFace(
    expression: MascotExpression,
    fillColor: Color,
    strokeColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isSpeaking: Boolean = false,
    isAnimatingVoice: Boolean = false,
    enableBlink: Boolean = true,
    blinkCycleDurationMs: Int = 3800,
    contentDescriptionText: String = expression.titleZh
) {
    val outlineFallback = MaterialTheme.colorScheme.outlineVariant
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_life")

    val blinkScaleY by if (enableBlink) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = blinkCycleDurationMs.coerceAtLeast(2400)
                    1f at 0
                    1f at (durationMillis - 240)
                    0.12f at (durationMillis - 140)
                    1f at durationMillis
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "blink"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    val speakingBreath by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 640, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_breath"
    )

    val activeScale by animateFloatAsState(
        targetValue = if (isSpeaking) 1.05f else 1f,
        animationSpec = tween(220),
        label = "active_scale"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = contentDescriptionText }
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val minDim = this.size.minDimension
        val headRadius = minDim * (if (isSpeaking) 0.37f else 0.43f) * activeScale
        val strokePx = (minDim * 0.055f).coerceIn(1.5.dp.toPx(), 3.6.dp.toPx())

        // Speaking outer ripple rings
        if (isSpeaking) {
            val ring1Radius = headRadius + minDim * 0.13f * rippleProgress
            val ring1Alpha = (1f - rippleProgress) * 0.55f
            drawCircle(
                color = outlineFallback.copy(alpha = ring1Alpha),
                radius = ring1Radius,
                center = Offset(cx, cy),
                style = Stroke(width = (strokePx * 0.55f).coerceAtLeast(1.dp.toPx()))
            )

            val secondaryPhase = (rippleProgress + 0.5f) % 1f
            val ring2Radius = headRadius + minDim * 0.13f * secondaryPhase
            val ring2Alpha = (1f - secondaryPhase) * 0.35f
            drawCircle(
                color = outlineFallback.copy(alpha = ring2Alpha),
                radius = ring2Radius,
                center = Offset(cx, cy),
                style = Stroke(width = (strokePx * 0.45f).coerceAtLeast(1.dp.toPx()))
            )
        }

        // Subtle contrast halo so pure black or pure white heads stay crisp on dark/light backgrounds
        drawCircle(
            color = outlineFallback.copy(alpha = if (isSpeaking) 0.85f else 0.42f),
            radius = headRadius + strokePx * 0.65f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )

        // Main outer big circle (head)
        drawCircle(
            color = fillColor,
            radius = headRadius,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = strokeColor,
            radius = headRadius,
            center = Offset(cx, cy),
            style = Stroke(width = strokePx)
        )

        // Two identical small circle eyes
        val eyeOffsetX = headRadius * 0.34f
        val eyeOffsetY = -headRadius * 0.10f + if (isSpeaking) -headRadius * 0.02f * speakingBreath else 0f
        val eyeRadius = headRadius * 0.155f
        val leftEyeCenter = Offset(cx - eyeOffsetX, cy + eyeOffsetY)
        val rightEyeCenter = Offset(cx + eyeOffsetX, cy + eyeOffsetY)

        listOf(leftEyeCenter, rightEyeCenter).forEach { eyeCenter ->
            scale(scaleX = 1f, scaleY = blinkScaleY, pivot = eyeCenter) {
                drawCircle(
                    color = strokeColor,
                    radius = eyeRadius,
                    center = eyeCenter,
                    style = Stroke(width = strokePx * 0.92f)
                )
                if (expression == MascotExpression.CURIOUS) {
                    drawCircle(
                        color = strokeColor,
                        radius = eyeRadius * 0.36f,
                        center = eyeCenter
                    )
                } else if (expression == MascotExpression.CONTEMPLATIVE) {
                    drawCircle(
                        color = strokeColor,
                        radius = eyeRadius * 0.24f,
                        center = Offset(eyeCenter.x + eyeRadius * 0.18f, eyeCenter.y - eyeRadius * 0.12f)
                    )
                }
            }
        }

        // Expressive linear brows and mouth lines
        val mouthY = cy + headRadius * 0.36f
        val browY = cy - headRadius * 0.38f
        val fineStroke = Stroke(width = strokePx * 0.86f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val voiceMouthLift = if (isAnimatingVoice || isSpeaking) speakingBreath * headRadius * 0.05f else 0f

        when (expression) {
            MascotExpression.CALM -> {
                val smilePath = Path().apply {
                    moveTo(cx - headRadius * 0.22f, mouthY)
                    quadraticTo(
                        cx,
                        mouthY + headRadius * 0.12f + voiceMouthLift,
                        cx + headRadius * 0.22f,
                        mouthY
                    )
                }
                drawPath(path = smilePath, color = strokeColor, style = fineStroke)
            }

            MascotExpression.CONTEMPLATIVE -> {
                // Raised right eyebrow, level left brow
                drawLine(
                    color = strokeColor,
                    start = Offset(cx - eyeOffsetX - eyeRadius, browY + headRadius * 0.03f),
                    end = Offset(cx - eyeOffsetX + eyeRadius, browY + headRadius * 0.03f),
                    strokeWidth = strokePx * 0.72f,
                    cap = StrokeCap.Round
                )
                val archPath = Path().apply {
                    moveTo(cx + eyeOffsetX - eyeRadius, browY - headRadius * 0.02f)
                    quadraticTo(
                        cx + eyeOffsetX,
                        browY - headRadius * 0.14f,
                        cx + eyeOffsetX + eyeRadius,
                        browY - headRadius * 0.01f
                    )
                }
                drawPath(archPath, color = strokeColor, style = Stroke(width = strokePx * 0.72f, cap = StrokeCap.Round))

                // Off-center thinking mouth line
                drawLine(
                    color = strokeColor,
                    start = Offset(cx - headRadius * 0.05f, mouthY + voiceMouthLift * 0.4f),
                    end = Offset(cx + headRadius * 0.22f, mouthY - headRadius * 0.05f - voiceMouthLift * 0.4f),
                    strokeWidth = strokePx * 0.84f,
                    cap = StrokeCap.Round
                )
            }

            MascotExpression.SKEPTICAL -> {
                // Asymmetric brows
                drawLine(
                    color = strokeColor,
                    start = Offset(cx - eyeOffsetX - eyeRadius, browY - headRadius * 0.02f),
                    end = Offset(cx - eyeOffsetX + eyeRadius, browY + headRadius * 0.06f),
                    strokeWidth = strokePx * 0.74f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = strokeColor,
                    start = Offset(cx + eyeOffsetX - eyeRadius, browY + headRadius * 0.04f),
                    end = Offset(cx + eyeOffsetX + eyeRadius, browY - headRadius * 0.07f),
                    strokeWidth = strokePx * 0.74f,
                    cap = StrokeCap.Round
                )
                // Crisp skeptical zigzag / tilted line mouth
                val skepticalPath = Path().apply {
                    moveTo(cx - headRadius * 0.24f, mouthY + headRadius * 0.03f)
                    lineTo(cx - headRadius * 0.04f, mouthY - headRadius * 0.05f - voiceMouthLift * 0.5f)
                    lineTo(cx + headRadius * 0.12f, mouthY + headRadius * 0.04f + voiceMouthLift * 0.4f)
                    lineTo(cx + headRadius * 0.25f, mouthY - headRadius * 0.03f)
                }
                drawPath(skepticalPath, color = strokeColor, style = fineStroke)
            }

            MascotExpression.GENTLE -> {
                // Soft arched brows
                listOf(leftEyeCenter.x, rightEyeCenter.x).forEach { bx ->
                    val brow = Path().apply {
                        moveTo(bx - eyeRadius * 0.9f, browY + headRadius * 0.03f)
                        quadraticTo(bx, browY - headRadius * 0.08f, bx + eyeRadius * 0.9f, browY + headRadius * 0.03f)
                    }
                    drawPath(brow, color = strokeColor, style = Stroke(width = strokePx * 0.68f, cap = StrokeCap.Round))
                }
                // Warm curved smile
                val gentleSmile = Path().apply {
                    moveTo(cx - headRadius * 0.26f, mouthY - headRadius * 0.03f)
                    quadraticTo(
                        cx,
                        mouthY + headRadius * 0.22f + voiceMouthLift,
                        cx + headRadius * 0.26f,
                        mouthY - headRadius * 0.03f
                    )
                }
                drawPath(gentleSmile, color = strokeColor, style = fineStroke)
                // Delicate cheek ticks
                val tickY = cy + headRadius * 0.16f
                drawLine(
                    color = strokeColor.copy(alpha = 0.7f),
                    start = Offset(cx - headRadius * 0.58f, tickY),
                    end = Offset(cx - headRadius * 0.46f, tickY - headRadius * 0.06f),
                    strokeWidth = strokePx * 0.58f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = strokeColor.copy(alpha = 0.7f),
                    start = Offset(cx + headRadius * 0.46f, tickY),
                    end = Offset(cx + headRadius * 0.58f, tickY - headRadius * 0.06f),
                    strokeWidth = strokePx * 0.58f,
                    cap = StrokeCap.Round
                )
            }

            MascotExpression.STOIC -> {
                drawLine(
                    color = strokeColor,
                    start = Offset(cx - headRadius * 0.24f, mouthY),
                    end = Offset(cx + headRadius * 0.24f, mouthY + voiceMouthLift * 0.35f),
                    strokeWidth = strokePx * 0.88f,
                    cap = StrokeCap.Round
                )
            }

            MascotExpression.CURIOUS -> {
                // Raised curved brows
                listOf(leftEyeCenter.x, rightEyeCenter.x).forEach { bx ->
                    val brow = Path().apply {
                        moveTo(bx - eyeRadius * 0.85f, browY)
                        quadraticTo(bx, browY - headRadius * 0.11f, bx + eyeRadius * 0.85f, browY)
                    }
                    drawPath(brow, color = strokeColor, style = Stroke(width = strokePx * 0.68f, cap = StrokeCap.Round))
                }
                // Small expressive oval or wave mouth
                if (isSpeaking || isAnimatingVoice) {
                    drawOval(
                        color = strokeColor,
                        topLeft = Offset(cx - headRadius * 0.11f, mouthY - headRadius * 0.08f),
                        size = Size(headRadius * 0.22f, headRadius * (0.16f + speakingBreath * 0.08f)),
                        style = Stroke(width = strokePx * 0.82f)
                    )
                } else {
                    val wave = Path().apply {
                        moveTo(cx - headRadius * 0.22f, mouthY)
                        quadraticTo(cx - headRadius * 0.11f, mouthY - headRadius * 0.08f, cx, mouthY)
                        quadraticTo(cx + headRadius * 0.11f, mouthY + headRadius * 0.08f, cx + headRadius * 0.22f, mouthY)
                    }
                    drawPath(wave, color = strokeColor, style = fineStroke)
                }
            }
        }
    }
}

fun PersonaAvatarEntity.parsedExpression(): MascotExpression {
    return MascotExpression.entries.firstOrNull { it.name == expression } ?: MascotExpression.CALM
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResidentMascotBar(
    personas: List<PersonaAvatarEntity>,
    activeSpeakerId: Long?,
    isSpeakingPulse: Boolean,
    onSelectSpeaker: (PersonaAvatarEntity) -> Unit,
    onEditPersona: (PersonaAvatarEntity) -> Unit,
    onAddPersona: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalDialecticSurfaces.current
    val activePersona = personas.firstOrNull { it.id == activeSpeakerId } ?: personas.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaces.surfaceLow.copy(alpha = 0.86f))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
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
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = if (activePersona != null) {
                        "发声阶段 · ${activePersona.name} (${activePersona.parsedExpression().titleZh})"
                    } else {
                        "发声阶段 · 选择形象"
                    },
                    style = MonoLabelStyle.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (activePersona != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onEditPersona(activePersona) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("edit_active_mascot_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑当前形象",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = "调整面相/名色",
                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("resident_mascot_bar"),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(personas, key = { it.id }) { persona ->
                val selected = persona.id == activePersona?.id
                val chipBg = if (selected) surfaces.surfaceHighest else surfaces.surfaceContainer
                val borderColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(chipBg)
                        .border(width = if (selected) 1.2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(99.dp))
                        .combinedClickable(
                            onClick = { onSelectSpeaker(persona) },
                            onLongClick = { onEditPersona(persona) }
                        )
                        .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
                        .testTag("mascot_chip_${persona.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconicMascotFace(
                        expression = persona.parsedExpression(),
                        fillColor = Color(persona.fillColorArgb),
                        strokeColor = Color(persona.strokeColorArgb),
                        size = 36.dp,
                        isSpeaking = selected,
                        isAnimatingVoice = selected && isSpeakingPulse,
                        blinkCycleDurationMs = 3200 + ((persona.id % 5).toInt() * 450),
                        contentDescriptionText = "${persona.name} 形象头像"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = persona.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selected) "发声中" else if (persona.perspective == Perspective.SUBJECT.name) "右侧·主" else "左侧·客",
                            style = MonoLabelStyle.copy(fontSize = 9.sp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            item(key = "add_mascot_cta") {
                Row(
                    modifier = Modifier
                        .height(46.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(surfaces.surfaceContainer)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(99.dp)
                        )
                        .clickable(onClick = onAddPersona)
                        .padding(horizontal = 12.dp)
                        .testTag("add_mascot_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加自定义形象",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "新增形象",
                        style = MonoLabelStyle.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MascotWorkshopDialog(
    editingPersona: PersonaAvatarEntity?,
    defaultPerspective: Perspective,
    onDismiss: () -> Unit,
    onSave: (
        id: Long?,
        name: String,
        expression: MascotExpression,
        fillColorArgb: Long,
        strokeColorArgb: Long,
        perspective: Perspective
    ) -> Unit,
    onDelete: ((PersonaAvatarEntity) -> Unit)? = null
) {
    val surfaces = LocalDialecticSurfaces.current
    var name by remember(editingPersona) {
        mutableStateOf(editingPersona?.name ?: "")
    }
    var expression by remember(editingPersona) {
        mutableStateOf(editingPersona?.parsedExpression() ?: MascotExpression.GENTLE)
    }
    var fillColorArgb by remember(editingPersona) {
        mutableLongStateOf(
            editingPersona?.fillColorArgb
                ?: if (defaultPerspective == Perspective.SUBJECT) MascotPalette.presets[0].fillArgb else MascotPalette.presets[1].fillArgb
        )
    }
    var strokeColorArgb by remember(editingPersona) {
        mutableLongStateOf(
            editingPersona?.strokeColorArgb
                ?: if (defaultPerspective == Perspective.SUBJECT) MascotPalette.presets[0].strokeArgb else MascotPalette.presets[1].strokeArgb
        )
    }
    var perspective by remember(editingPersona) {
        mutableStateOf(
            editingPersona?.perspective?.let { raw -> Perspective.entries.firstOrNull { it.name == raw } }
                ?: defaultPerspective
        )
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(surfaces.surfaceHigh)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingPersona == null) "设计新形象" else "编辑「${editingPersona.name}」",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (editingPersona != null && !editingPersona.isDefault && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_mascot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除形象",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Live Animated Preview
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaces.surfaceContainer)
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconicMascotFace(
                    expression = expression,
                    fillColor = Color(fillColorArgb),
                    strokeColor = Color(strokeColorArgb),
                    size = 86.dp,
                    isSpeaking = true,
                    isAnimatingVoice = true,
                    blinkCycleDurationMs = 2800
                )
                Text(
                    text = "${name.ifBlank { "未命名形象" }} · ${expression.titleZh}",
                    style = MonoLabelStyle.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Custom Name
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 12) name = it },
                label = { Text("形象名称（最多12字）") },
                placeholder = { Text("例如：理性之镜、深夜独白") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mascot_name_input")
            )

            // Linear Expression Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "选择线性面相",
                    style = MonoLabelStyle.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MascotExpression.entries.forEach { item ->
                        val selected = expression == item
                        Column(
                            modifier = Modifier
                                .width(78.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) surfaces.surfaceHighest else surfaces.surfaceContainer)
                                .border(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { expression = item }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("expression_option_${item.name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconicMascotFace(
                                expression = item,
                                fillColor = Color(fillColorArgb),
                                strokeColor = Color(strokeColorArgb),
                                size = 38.dp,
                                enableBlink = false
                            )
                            Text(
                                text = item.titleZh,
                                style = MonoLabelStyle.copy(fontSize = 10.sp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Color Palette & Invert Black/White Swap
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "配色（默认黑白 / 白黑）",
                        style = MonoLabelStyle.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        onClick = {
                            val oldFill = fillColorArgb
                            fillColorArgb = strokeColorArgb
                            strokeColorArgb = oldFill
                        },
                        shape = RoundedCornerShape(99.dp),
                        color = surfaces.surfaceContainer,
                        modifier = Modifier.testTag("swap_mascot_colors_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "反色互换",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "底线反色",
                                style = MonoLabelStyle.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MascotPalette.presets.forEach { preset ->
                        val isSelected = fillColorArgb == preset.fillArgb && strokeColorArgb == preset.strokeArgb
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (isSelected) surfaces.surfaceHighest else surfaces.surfaceContainer)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(99.dp)
                                )
                                .clickable {
                                    fillColorArgb = preset.fillArgb
                                    strokeColorArgb = preset.strokeArgb
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("color_preset_${preset.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset.fillArgb))
                                    .border(2.dp, Color(preset.strokeArgb), CircleShape)
                            )
                            Text(
                                text = preset.labelZh,
                                style = MonoLabelStyle.copy(fontSize = 10.sp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Perspective Alignment (Left vs Right bubble side)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "聊天气泡位置与思辨立场",
                    style = MonoLabelStyle.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Perspective.SUBJECT to "右侧气泡 · 主我感性",
                        Perspective.OBJECT to "左侧气泡 · 客体审视"
                    ).forEach { (item, label) ->
                        val selected = perspective == item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else surfaces.surfaceContainer)
                                .clickable { perspective = item }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MonoLabelStyle.copy(fontSize = 10.sp),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            editingPersona?.id,
                            name.ifBlank { if (perspective == Perspective.SUBJECT) "自叙分身" else "审视镜像" },
                            expression,
                            fillColorArgb,
                            strokeColorArgb,
                            perspective
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("save_mascot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存形象")
                }
            }
        }
    }

    if (showDeleteConfirm && editingPersona != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = surfaces.surfaceHigh,
            title = { Text("移除「${editingPersona.name}」？") },
            text = { Text("历史聊天记录中的该形象署名仍会保留，仅从顶部发声头像栏移除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(editingPersona)
                    }
                ) {
                    Text("确认移除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("保留")
                }
            }
        )
    }
}
