package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class Perspective(val labelZh: String, val codeEn: String) {
    SUBJECT("主我", "THESIS"),
    OBJECT("客体", "ANTITHESIS")
}

enum class MascotExpression(val titleZh: String, val subtitleZh: String) {
    CALM("静观", "平和短弧线"),
    CONTEMPLATIVE("凝思", "微挑单眉与偏置圆点"),
    SKEPTICAL("质疑", "错落斜眉与折线"),
    GENTLE("欣然", "舒展弯弧"),
    STOIC("默然", "极简水平细线"),
    CURIOUS("追问", "同心双瞳与波纹")
}

data class MascotColorPreset(
    val id: String,
    val labelZh: String,
    val fillArgb: Long,
    val strokeArgb: Long
)

object MascotPalette {
    val presets = listOf(
        MascotColorPreset("obsidian", "黑底白线", 0xFF131313, 0xFFFFFFFF),
        MascotColorPreset("ivory", "白底黑线", 0xFFFFFFFF, 0xFF131313),
        MascotColorPreset("graphite", "玄灰银线", 0xFF2B2D31, 0xFFF3F3F1),
        MascotColorPreset("slate", "石青霜线", 0xFF253543, 0xFFECF3F8),
        MascotColorPreset("moss", "苔墨浅米", 0xFF2B3A32, 0xFFF0F4EE),
        MascotColorPreset("terracotta", "暖陶象牙", 0xFF7B4338, 0xFFFFF5EE),
        MascotColorPreset("ochre", "砂金墨线", 0xFFDCC69B, 0xFF171717),
        MascotColorPreset("mist", "浅岚炭线", 0xFFD4DEE4, 0xFF161C22)
    )
}

@Entity(tableName = "persona_avatars")
data class PersonaAvatarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val expression: String = MascotExpression.CALM.name,
    val fillColorArgb: Long = 0xFF131313,
    val strokeColorArgb: Long = 0xFFFFFFFF,
    val perspective: String = Perspective.SUBJECT.name,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class VisualThemeMode(val titleZh: String, val subtitleZh: String) {
    DARK("黑曜深邃", "主体意识为主"),
    LIGHT("纯白皓月", "客体审视视角"),
    DUAL("双生回旋", "发言时底色互换")
}

enum class AcousticMode(val titleZh: String, val descZh: String) {
    OFFLINE_RECOGNIZER("端侧中英双语识别", "Sherpa-ONNX 中英双语流式识别 · VAD 自动断句"),
    PURE_AUDIO_ARCHIVE("纯声波本地归档", "静默录制 WAV 音频 · 保留原始语调波形")
}

enum class SocraticStance(val titleZh: String, val promptGuideZh: String) {
    DECONSTRUCT("解构本质", "剥离情绪修辞，直指前提假设中的逻辑缝隙。"),
    ABSURD_IRONY("荒谬反讽", "以加缪式的清醒反讽，照见日常执念的荒诞镜像。"),
    STOIC_DETACHMENT("斯多葛超然", "区分可控与不可控之界，剥除冗余焦虑与占有欲。")
}

enum class ArchiveFilter(val labelZh: String) {
    ALL("全部"),
    SUBJECT("主体视角"),
    OBJECT("客体视角"),
    AUDIO("语音记录"),
    RESOLVED("矛盾闭环")
}

@Entity(tableName = "dialectic_sessions")
data class DialecticSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionCode: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val conclusionText: String = "",
    val synthesisSummary: String = "",
    val resonancePercent: Int = 0,
    val tagsCsv: String = "",
    val durationMinutes: Int = 1
)

@Entity(
    tableName = "dialectic_nodes",
    foreignKeys = [
        ForeignKey(
            entity = DialecticSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class DialecticNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val perspective: String, // Perspective.name
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val audioFilePath: String? = null,
    val audioDurationSec: Int = 0,
    val waveformCsv: String = "", // Comma-separated normalized amplitudes 0.1..1.0
    val hasSimulatedAudioMemo: Boolean = false,
    val isFollowUp: Boolean = false,
    val isBranch: Boolean = false,
    val speakerAvatarId: Long? = null,
    val speakerName: String = ""
)

@Entity(tableName = "dictionary_terms")
data class DictionaryTermEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val term: String,
    val gloss: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class SessionWithNodes(
    @Embedded val session: DialecticSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val nodes: List<DialecticNodeEntity>
)

data class TuningSettings(
    val visualThemeMode: VisualThemeMode = VisualThemeMode.DARK,
    val highContrastInversion: Boolean = true,
    val focusDimming: Boolean = true,
    val acousticMode: AcousticMode = AcousticMode.OFFLINE_RECOGNIZER,
    val silenceThresholdSec: Float = 3.0f,
    val philosophicalPunctuation: Boolean = true,
    val subjectArchetypeTitle: String = "内在体验者 / 困惑者",
    val subjectArchetypeTrait: String = "感性流淌 · 即刻自叙",
    val subjectArchetypeVoice: String = "第一人称私语 (Mono)",
    val objectArchetypeTitle: String = "冷彻观察者 / 诘问者",
    val socraticStance: SocraticStance = SocraticStance.DECONSTRUCT
)
