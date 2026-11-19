package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Perspective
import com.example.ui.theme.LocalDialecticSurfaces
import com.example.ui.theme.MonoLabelStyle
import kotlin.math.roundToInt

/**
 * Custom Vesica Piscis geometric emblem representing the intersection
 * between Subject (主我) and Object (客体) consciousness.
 */
@Composable
fun VesicaPiscisVisualizer(
    activePerspective: Perspective,
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val infiniteTransition = rememberInfiniteTransition(label = "vesica_pulse")
    val breath by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val smoothAmp by animateFloatAsState(
        targetValue = if (isActive) amplitude.coerceIn(0.05f, 1f) else 0.08f,
        animationSpec = tween(120),
        label = "amp"
    )

    Canvas(
        modifier = modifier
            .size(168.dp)
            .semantics { contentDescription = "双生意识声波几何图" }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.28f
        val separation = baseRadius * 0.62f

        val subjectScale = if (activePerspective == Perspective.SUBJECT) 1.06f + smoothAmp * 0.14f else 0.94f
        val objectScale = if (activePerspective == Perspective.OBJECT) 1.06f + smoothAmp * 0.14f else 0.94f

        // Outer ambient ripple ring
        drawCircle(
            color = primaryColor.copy(alpha = if (isActive) 0.16f + smoothAmp * 0.24f else 0.08f),
            radius = size.minDimension * 0.45f * (if (isActive) breath else 1f),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Left orbit (Subject / 主我)
        drawCircle(
            color = if (activePerspective == Perspective.SUBJECT) primaryColor.copy(alpha = 0.85f) else outlineColor.copy(alpha = 0.6f),
            radius = baseRadius * subjectScale,
            center = Offset(center.x - separation, center.y),
            style = Stroke(width = if (activePerspective == Perspective.SUBJECT) 1.8.dp.toPx() else 1.dp.toPx())
        )

        // Right orbit (Object / 客体)
        drawCircle(
            color = if (activePerspective == Perspective.OBJECT) primaryColor.copy(alpha = 0.85f) else outlineColor.copy(alpha = 0.6f),
            radius = baseRadius * objectScale,
            center = Offset(center.x + separation, center.y),
            style = Stroke(width = if (activePerspective == Perspective.OBJECT) 1.8.dp.toPx() else 1.dp.toPx())
        )

        // Central luminous lens slit (Synthesis point)
        val slitHeight = (baseRadius * 0.85f) * (0.75f + smoothAmp * 0.65f)
        val slitWidth = (4.dp + (8.dp * smoothAmp)).toPx()
        drawOval(
            color = primaryColor.copy(alpha = if (isActive) 0.9f else 0.45f),
            topLeft = Offset(center.x - slitWidth / 2f, center.y - slitHeight / 2f),
            size = androidx.compose.ui.geometry.Size(slitWidth, slitHeight)
        )
    }
}

@Composable
fun HairlineWaveformBar(
    amplitudes: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 22.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    val safeBars = if (amplitudes.isEmpty()) listOf(0.3f, 0.7f, 0.4f, 0.85f, 0.5f, 0.65f, 0.35f, 0.55f) else amplitudes
    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        safeBars.forEachIndexed { index, raw ->
            val ratio = (index + 1).toFloat() / safeBars.size.toFloat()
            val isHighlighted = if (progress > 0f) ratio <= progress else index < safeBars.size / 2
            val barHeight = (maxHeight.value * raw.coerceIn(0.15f, 1f)).dp
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (isHighlighted) activeColor else inactiveColor)
            )
        }
    }
}

@Composable
fun AudioMemoPlayerRow(
    isPlaying: Boolean,
    progress: Float,
    durationSec: Int,
    waveformCsv: String,
    badgeLabel: String? = null,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalDialecticSurfaces.current
    val amplitudes = waveformCsv.split(",")
        .mapNotNull { it.trim().toFloatOrNull() }
        .ifEmpty { listOf(0.35f, 0.65f, 0.28f, 0.88f, 0.52f, 0.68f, 0.44f, 0.55f, 0.22f, 0.62f) }

    val playedSec = (durationSec * progress).roundToInt()
    val timeText = badgeLabel ?: "${formatMinSec(playedSec)} / ${formatMinSec(durationSec)}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(surfaces.surfaceLowest.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onTogglePlay)
                .testTag("audio_play_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.cd_play_audio),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        HairlineWaveformBar(
            amplitudes = amplitudes,
            progress = progress,
            modifier = Modifier.weight(1f),
            maxHeight = 20.dp
        )

        Text(
            text = timeText,
            style = MonoLabelStyle,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

fun formatMinSec(totalSec: Int): String {
    val safe = totalSec.coerceAtLeast(0)
    val m = safe / 60
    val s = safe % 60
    return "%02d:%02d".format(m, s)
}
