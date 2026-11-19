package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.export.LocalExportManager
import com.example.data.model.AcousticMode
import com.example.data.model.DictionaryTermEntity
import com.example.data.model.SocraticStance
import com.example.data.model.TuningSettings
import com.example.data.model.VisualThemeMode
import com.example.ui.theme.LocalDialecticSurfaces
import com.example.ui.theme.MonoLabelStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TuningScreen(
    settings: TuningSettings,
    dictionaryTerms: List<DictionaryTermEntity>,
    onTogglePreviewPerspective: () -> Unit,
    onSaveSettings: (TuningSettings) -> Unit,
    onAddTerm: (String, String) -> Unit,
    onRemoveTerm: (Long) -> Unit,
    onResetToOrigin: () -> Unit,
    onBuildJsonExport: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val surfaces = LocalDialecticSurfaces.current

    var draft by remember(settings) { mutableStateOf(settings) }
    LaunchedEffect(settings) {
        draft = settings
    }

    var showAddTermDialog by remember { mutableStateOf(false) }
    var newTermText by remember { mutableStateOf("") }
    var newTermGloss by remember { mutableStateOf("") }

    var showEditSubjectDialog by remember { mutableStateOf(false) }
    var editSubjectTitle by remember { mutableStateOf(draft.subjectArchetypeTitle) }
    var editSubjectTrait by remember { mutableStateOf(draft.subjectArchetypeTrait) }

    var showResetConfirm by remember { mutableStateOf(false) }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val json = onBuildJsonExport()
            val result = LocalExportManager.writeTextToUri(context, uri, json)
            Toast.makeText(
                context,
                if (result.isSuccess) "已导出思辨图谱 JSON" else "导出失败",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.header_tuning_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    onClick = onTogglePreviewPerspective,
                    shape = RoundedCornerShape(99.dp),
                    color = surfaces.surfaceHigh,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("tuning_preview_swap_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = stringResource(R.string.cd_toggle_perspective),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "主体 / 客体",
                            style = MonoLabelStyle.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Intro Title
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "调谐与自省架构",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.tuning_subtitle),
                    style = MonoLabelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 6.dp),
                    color = surfaces.surfaceHigh
                )
            }

            // SECTION 1: VISUAL DUALISM
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "视觉对偶呈现",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "α / β 视角",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisualThemeMode.entries.forEach { mode ->
                        val selected = draft.visualThemeMode == mode
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(98.dp)
                                .alpha(if (selected) 1f else 0.72f)
                                .background(if (selected) surfaces.surfaceHigh else surfaces.surfaceLow)
                                .border(
                                    width = if (selected) 1.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surface,
                                )
                                .clickable {
                                    draft = draft.copy(visualThemeMode = mode)
                                    onSaveSettings(draft)
                                }
                                .padding(10.dp)
                                .testTag("theme_card_${mode.name.lowercase()}"),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.titleZh,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已选中",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            if (mode == VisualThemeMode.DUAL) {
                                Row(
                                    modifier = Modifier
                                        .width(46.dp)
                                        .height(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .background(surfaces.surfaceLowest)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (mode == VisualThemeMode.DARK) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(4.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                }
                            }

                            Text(
                                text = mode.subtitleZh,
                                style = MonoLabelStyle.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                TuningSwitchRow(
                    title = "对立观点自动高反差反转",
                    subtitle = "第二视角回应时采用高反差块面标记",
                    checked = draft.highContrastInversion,
                    onCheckedChange = {
                        draft = draft.copy(highContrastInversion = it)
                        onSaveSettings(draft)
                    },
                    testTag = "switch_high_contrast"
                )

                TuningSwitchRow(
                    title = "非活跃气泡深度降暗 (Focus Dimming)",
                    subtitle = "书写或回溯时上文透明度压低至 42%",
                    checked = draft.focusDimming,
                    onCheckedChange = {
                        draft = draft.copy(focusDimming = it)
                        onSaveSettings(draft)
                    },
                    testTag = "switch_focus_dimming"
                )
            }

            // SECTION 2: LOCAL ACOUSTIC ENGINE
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "本地声学与词法引擎",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "SHERPA-ONNX 中英双语 INT8",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaces.surfaceLowest)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AcousticMode.entries.forEach { mode ->
                        val selected = draft.acousticMode == mode
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .alpha(if (selected) 1f else 0.65f)
                                .background(if (selected) surfaces.surfaceHigh else surfaces.surfaceLowest)
                                .clickable {
                                    draft = draft.copy(acousticMode = mode)
                                    onSaveSettings(draft)
                                }
                                .padding(10.dp)
                                .testTag("acoustic_mode_${mode.name.lowercase()}"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.titleZh,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = if (mode == AcousticMode.OFFLINE_RECOGNIZER) Icons.Default.Lock else Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = mode.descZh,
                                style = MonoLabelStyle.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Silence Threshold Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaces.surfaceLow)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "深思沉寂自动封存阈值",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", draft.silenceThresholdSec)} 秒",
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(surfaces.surfaceContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("1.5s", style = MonoLabelStyle, color = MaterialTheme.colorScheme.outline)
                        Slider(
                            value = draft.silenceThresholdSec,
                            onValueChange = { raw ->
                                val stepped = ((raw * 2f).roundToInt() / 2f).coerceIn(1.5f, 8.0f)
                                draft = draft.copy(silenceThresholdSec = stepped)
                            },
                            onValueChangeFinished = { onSaveSettings(draft) },
                            valueRange = 1.5f..8.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = surfaces.surfaceHigh
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("silence_threshold_slider")
                        )
                        Text("8.0s", style = MonoLabelStyle, color = MaterialTheme.colorScheme.outline)
                    }

                    Text(
                        text = "当语声停止超过阈值，系统自动固化为单段思辨命题并移交发言棒",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                TuningSwitchRow(
                    title = "哲学语境断句与迟疑停顿保留",
                    subtitle = "识别「但是 / 倘若 / 抑或」并转化为破折号与意群留白",
                    checked = draft.philosophicalPunctuation,
                    onCheckedChange = {
                        draft = draft.copy(philosophicalPunctuation = it)
                        onSaveSettings(draft)
                    },
                    testTag = "switch_philosophical_punctuation"
                )

                // Dictionary Terms
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaces.surfaceLow)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "同音哲学专有词即时订正",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "优先映射唯识、现象学与存在主义词根",
                                style = MonoLabelStyle,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Surface(
                            onClick = { showAddTermDialog = true },
                            color = surfaces.surfaceHigh,
                            modifier = Modifier.testTag("add_dictionary_term_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "增补词条",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "增补词条",
                                    style = MonoLabelStyle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (dictionaryTerms.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfaces.surfaceContainer)
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                                .testTag("dictionary_empty_state")
                        ) {
                            Text(
                                text = "暂无自定义专有词条 — 点击右上角「增补词条」录入你常用的哲学或专业词汇，语音识别与思辨分析时将自动纠偏匹配。",
                                style = MonoLabelStyle.copy(lineHeight = 16.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dictionaryTerms.forEach { term ->
                                Row(
                                    modifier = Modifier
                                        .background(surfaces.surfaceContainer)
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = term.term,
                                        style = MonoLabelStyle,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "移除词条 ${term.term}",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { onRemoveTerm(term.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: DUAL ARCHETYPES
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "分裂人格锚定",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dual Archetypes",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Role A Card: Subject
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaces.surfaceLow)
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "α",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Column {
                                Text(
                                    text = "主体角色 (SUBJECT)",
                                    style = MonoLabelStyle,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = draft.subjectArchetypeTitle,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(surfaces.surfaceContainer)
                                .clickable {
                                    editSubjectTitle = draft.subjectArchetypeTitle
                                    editSubjectTrait = draft.subjectArchetypeTrait
                                    showEditSubjectDialog = true
                                }
                                .testTag("edit_subject_archetype_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑主体人格",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(surfaces.surfaceContainer)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "心智特质",
                                style = MonoLabelStyle.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = draft.subjectArchetypeTrait,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(surfaces.surfaceContainer)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "发声方式",
                                style = MonoLabelStyle.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = draft.subjectArchetypeVoice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Role B Card: Object / Socratic Stance
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaces.surfaceLow)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(surfaces.surfaceHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "β",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = "对立客体 (THE OTHER)",
                                style = MonoLabelStyle,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = draft.objectArchetypeTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "苏格拉底式提问立场预设：",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SocraticStance.entries.forEach { stance ->
                            val selected = draft.socraticStance == stance
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else surfaces.surfaceContainer
                                    )
                                    .clickable {
                                        draft = draft.copy(socraticStance = stance)
                                        onSaveSettings(draft)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                                    .testTag("socratic_stance_${stance.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stance.titleZh,
                                    style = MonoLabelStyle.copy(fontSize = 11.sp),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaces.surfaceLowest)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "“${draft.socraticStance.promptGuideZh} 客体视角将不予同情性附和，而是在你陈述后捕捉前提漏洞进行本地镜像折射。”",
                            style = MonoLabelStyle.copy(lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Action Deck
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onSaveSettings(draft)
                        Toast.makeText(context, "调谐设定已保存至本地并实时生效", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_tuning_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.tuning_save_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(surfaces.surfaceLow)
                            .clickable {
                                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                jsonExportLauncher.launch("dialectic_graph_$stamp.json")
                            }
                            .testTag("export_json_graph_button"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.tuning_export_json),
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(surfaces.surfaceLow)
                            .clickable { showResetConfirm = true }
                            .testTag("reset_origin_button"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.tuning_reset_origin),
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddTermDialog) {
        AlertDialog(
            onDismissRequest = { showAddTermDialog = false },
            containerColor = surfaces.surfaceHigh,
            title = {
                Text(
                    text = "增补哲学专有词条",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTermText,
                        onValueChange = { newTermText = it },
                        label = { Text("术语（例如：本体论 / Logos）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTermGloss,
                        onValueChange = { newTermGloss = it },
                        label = { Text("简短释义（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTermText.isNotBlank()) {
                            onAddTerm(newTermText, newTermGloss)
                            newTermText = ""
                            newTermGloss = ""
                        }
                        showAddTermDialog = false
                    }
                ) {
                    Text("纳入词库")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTermDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showEditSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showEditSubjectDialog = false },
            containerColor = surfaces.surfaceHigh,
            title = {
                Text(
                    text = "锚定主体角色 (α)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editSubjectTitle,
                        onValueChange = { editSubjectTitle = it },
                        label = { Text("角色称谓") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSubjectTrait,
                        onValueChange = { editSubjectTrait = it },
                        label = { Text("心智特质") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        draft = draft.copy(
                            subjectArchetypeTitle = editSubjectTitle.ifBlank { draft.subjectArchetypeTitle },
                            subjectArchetypeTrait = editSubjectTrait.ifBlank { draft.subjectArchetypeTrait }
                        )
                        onSaveSettings(draft)
                        showEditSubjectDialog = false
                    }
                ) {
                    Text("锚定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSubjectDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = surfaces.surfaceHigh,
            title = {
                Text(
                    text = "重置人格原点？",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "将把视觉主题、声学阈值与人格立场恢复至默认设定，并清空自定义专有词条（已保存的思辨档案不会被删除）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetToOrigin()
                        showResetConfirm = false
                        Toast.makeText(context, "已恢复初始人格原点", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("确认重置", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TuningSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val surfaces = LocalDialecticSurfaces.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaces.surfaceLow)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MonoLabelStyle.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = surfaces.surfaceHigh
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
