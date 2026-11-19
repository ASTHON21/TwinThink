package com.example.data.repository

import com.example.data.engine.LocalDialecticEngine
import com.example.data.local.DialecticDao
import com.example.data.local.TuningPreferencesStore
import com.example.data.model.DialecticNodeEntity
import com.example.data.model.DialecticSessionEntity
import com.example.data.model.DictionaryTermEntity
import com.example.data.model.MascotExpression
import com.example.data.model.MascotPalette
import com.example.data.model.PersonaAvatarEntity
import com.example.data.model.Perspective
import com.example.data.model.SessionWithNodes
import com.example.data.model.TuningSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

class DialecticRepository(
    private val dao: DialecticDao,
    private val tuningStore: TuningPreferencesStore
) {
    val sessionsFlow: Flow<List<SessionWithNodes>> = dao.observeAllSessionsWithNodes().map { list ->
        list.map { item ->
            item.copy(nodes = item.nodes.sortedWith(compareBy({ it.createdAt }, { it.id })))
        }
    }

    val dictionaryTermsFlow: Flow<List<DictionaryTermEntity>> = dao.observeDictionaryTerms()

    val personasFlow: Flow<List<PersonaAvatarEntity>> = dao.observePersonas()

    val activeSpeakerIdFlow: Flow<Long?> = tuningStore.activeSpeakerIdFlow

    val tuningSettingsFlow: Flow<TuningSettings> = tuningStore.settingsFlow

    private val devSeededSessions = setOf(
        "#042" to "不确定性与选择的眩晕",
        "#041" to "关于孤独的终极形态",
        "#039" to "理智与冲动的双生拉扯",
        "#036" to "深夜问答：为何要追求不可企及的完美？"
    )

    private val devSeededTerms = setOf(
        "意向性 (Intentionality)" to "意识总是关于某物的指向性结构",
        "此在 (Dasein)" to "对自身存在有所领悟与追问的存在者",
        "悬置 (Epoché)" to "暂时搁置自然态度与既定成见的判断",
        "异化 (Entfremdung)" to "主体的造物反过来成为支配自身的陌生力量"
    )

    suspend fun ensureCleanStart() {
        if (!tuningStore.isDevDemoPurged()) {
            dao.getAllSessionsOnce()
                .filter { (it.sessionCode to it.title) in devSeededSessions }
                .forEach { dao.deleteSessionById(it.id) }

            dao.getDictionaryTermsOnce()
                .filter { (it.term to it.gloss) in devSeededTerms }
                .forEach { dao.deleteTermById(it.id) }

            tuningStore.markDevDemoPurged()
        }
        ensureDefaultPersonas()
    }

    suspend fun ensureDefaultPersonas() {
        val existing = dao.getPersonasOnce()
        if (existing.isEmpty()) {
            val obsidian = MascotPalette.presets[0]
            val ivory = MascotPalette.presets[1]
            val now = System.currentTimeMillis()
            val subjectId = dao.insertPersona(
                PersonaAvatarEntity(
                    name = "主我",
                    expression = MascotExpression.CALM.name,
                    fillColorArgb = obsidian.fillArgb,
                    strokeColorArgb = obsidian.strokeArgb,
                    perspective = Perspective.SUBJECT.name,
                    isDefault = true,
                    sortOrder = 0,
                    createdAt = now
                )
            )
            dao.insertPersona(
                PersonaAvatarEntity(
                    name = "客体",
                    expression = MascotExpression.SKEPTICAL.name,
                    fillColorArgb = ivory.fillArgb,
                    strokeColorArgb = ivory.strokeArgb,
                    perspective = Perspective.OBJECT.name,
                    isDefault = true,
                    sortOrder = 1,
                    createdAt = now + 1L
                )
            )
            if (tuningStore.activeSpeakerIdFlow.first() == null) {
                tuningStore.setActiveSpeakerId(subjectId)
            }
        }
    }

    suspend fun setActiveSpeakerId(speakerId: Long) {
        tuningStore.setActiveSpeakerId(speakerId)
    }

    suspend fun upsertPersona(
        id: Long?,
        name: String,
        expression: MascotExpression,
        fillColorArgb: Long,
        strokeColorArgb: Long,
        perspective: Perspective
    ): Long {
        val trimmedName = name.trim().ifBlank { "未命名形象" }.take(12)
        val existingList = dao.getPersonasOnce()
        val existing = id?.let { targetId -> existingList.firstOrNull { it.id == targetId } }
        return if (existing != null) {
            dao.updatePersona(
                existing.copy(
                    name = trimmedName,
                    expression = expression.name,
                    fillColorArgb = fillColorArgb,
                    strokeColorArgb = strokeColorArgb,
                    perspective = perspective.name
                )
            )
            existing.id
        } else {
            val nextOrder = (existingList.maxOfOrNull { it.sortOrder } ?: 1) + 1
            dao.insertPersona(
                PersonaAvatarEntity(
                    name = trimmedName,
                    expression = expression.name,
                    fillColorArgb = fillColorArgb,
                    strokeColorArgb = strokeColorArgb,
                    perspective = perspective.name,
                    isDefault = false,
                    sortOrder = nextOrder,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deletePersona(persona: PersonaAvatarEntity) {
        if (persona.isDefault) return
        dao.deletePersonaById(persona.id)
    }

    suspend fun createNewSession(initialTitle: String): Long {
        val maxExistingNumber = dao.getAllSessionsOnce()
            .mapNotNull { it.sessionCode.removePrefix("#").toIntOrNull() }
            .maxOrNull() ?: 0
        val nextNum = maxExistingNumber + 1
        val code = "#" + nextNum.toString().padStart(3, '0')
        val now = System.currentTimeMillis()
        val title = initialTitle.trim().ifBlank { "未命名思辨 $code" }
        return dao.insertSession(
            DialecticSessionEntity(
                sessionCode = code,
                title = title,
                createdAt = now,
                updatedAt = now,
                isResolved = false,
                conclusionText = "思辨进行中",
                synthesisSummary = "尚待注入第一枚思辨火种。当主我发起自叙、客体予以镜像诘问，合题便会在张力中自然浮现。",
                resonancePercent = 0,
                tagsCsv = "#新启思辨 · #本地自省",
                durationMinutes = 1
            )
        )
    }

    suspend fun addNodeAndSynthesize(
        sessionId: Long,
        perspective: Perspective,
        rawText: String,
        audioFilePath: String? = null,
        audioDurationSec: Int = 0,
        waveformCsv: String = "",
        isFollowUp: Boolean = false,
        isBranch: Boolean = false,
        speakerAvatar: PersonaAvatarEntity? = null
    ) {
        val settings = tuningStore.settingsFlow.first()
        val dictionary = dao.getDictionaryTermsOnce()
        val formatted = LocalDialecticEngine.formatPhilosophicalText(
            raw = rawText,
            autoPunctuate = settings.philosophicalPunctuation,
            dictionary = dictionary
        )
        if (formatted.isBlank() && audioFilePath == null) return

        val resolvedAvatar = speakerAvatar ?: resolveFallbackPersona(perspective)
        val finalContent = formatted.ifBlank { "（已封存纯声波思辨片段，时长 ${audioDurationSec} 秒）" }
        val now = System.currentTimeMillis()

        dao.insertNode(
            DialecticNodeEntity(
                sessionId = sessionId,
                perspective = perspective.name,
                content = finalContent,
                createdAt = now,
                audioFilePath = audioFilePath,
                audioDurationSec = audioDurationSec,
                waveformCsv = waveformCsv,
                hasSimulatedAudioMemo = false,
                isFollowUp = isFollowUp,
                isBranch = isBranch,
                speakerAvatarId = resolvedAvatar?.id,
                speakerName = resolvedAvatar?.name ?: perspective.labelZh
            )
        )

        refreshSessionSynthesis(sessionId, dictionary)
    }

    suspend fun generateSocraticReply(
        sessionId: Long,
        latestThesis: String,
        counterpartAvatar: PersonaAvatarEntity? = null
    ): DialecticNodeEntity? {
        val settings = tuningStore.settingsFlow.first()
        val dictionary = dao.getDictionaryTermsOnce()
        val history = dao.getNodesForSession(sessionId)
        val replyText = LocalDialecticEngine.generateCounterPerspective(
            thesis = latestThesis,
            history = history,
            stance = settings.socraticStance,
            dictionary = dictionary
        )
        val resolvedCounterpart = counterpartAvatar ?: resolveFallbackPersona(Perspective.OBJECT)
        val node = DialecticNodeEntity(
            sessionId = sessionId,
            perspective = Perspective.OBJECT.name,
            content = replyText,
            createdAt = System.currentTimeMillis(),
            speakerAvatarId = resolvedCounterpart?.id,
            speakerName = resolvedCounterpart?.name ?: Perspective.OBJECT.labelZh
        )
        val id = dao.insertNode(node)
        refreshSessionSynthesis(sessionId, dictionary)
        return node.copy(id = id)
    }

    suspend fun triggerFollowUp(
        sessionId: Long,
        targetNode: DialecticNodeEntity,
        counterpartAvatar: PersonaAvatarEntity? = null
    ) {
        val settings = tuningStore.settingsFlow.first()
        val dictionary = dao.getDictionaryTermsOnce()
        val reply = LocalDialecticEngine.generateFollowUp(targetNode, settings.socraticStance)
        val resolvedCounterpart = counterpartAvatar ?: resolveFallbackPersona(Perspective.OBJECT)
        dao.insertNode(
            DialecticNodeEntity(
                sessionId = sessionId,
                perspective = Perspective.OBJECT.name,
                content = reply,
                createdAt = System.currentTimeMillis(),
                isFollowUp = true,
                speakerAvatarId = resolvedCounterpart?.id,
                speakerName = resolvedCounterpart?.name ?: Perspective.OBJECT.labelZh
            )
        )
        refreshSessionSynthesis(sessionId, dictionary)
    }

    suspend fun triggerBranchInquiry(
        sessionId: Long,
        targetNode: DialecticNodeEntity,
        counterpartAvatar: PersonaAvatarEntity? = null
    ) {
        val settings = tuningStore.settingsFlow.first()
        val dictionary = dao.getDictionaryTermsOnce()
        val reply = LocalDialecticEngine.generateBranchInquiry(targetNode, settings.socraticStance)
        val resolvedCounterpart = counterpartAvatar ?: resolveFallbackPersona(Perspective.OBJECT)
        dao.insertNode(
            DialecticNodeEntity(
                sessionId = sessionId,
                perspective = Perspective.OBJECT.name,
                content = reply,
                createdAt = System.currentTimeMillis(),
                isBranch = true,
                speakerAvatarId = resolvedCounterpart?.id,
                speakerName = resolvedCounterpart?.name ?: Perspective.OBJECT.labelZh
            )
        )
        refreshSessionSynthesis(sessionId, dictionary)
    }

    suspend fun toggleSessionResolved(session: DialecticSessionEntity) {
        val nextResolved = !session.isResolved
        dao.updateSession(
            session.copy(
                isResolved = nextResolved,
                conclusionText = if (nextResolved) "矛盾闭环 · 已归档" else "思辨演进中",
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSession(session: DialecticSessionEntity) {
        val nodes = dao.getNodesForSession(session.id)
        nodes.forEach { n ->
            n.audioFilePath?.let { path ->
                runCatching { File(path).delete() }
            }
        }
        dao.deleteSessionById(session.id)
    }

    suspend fun addDictionaryTerm(term: String, gloss: String) {
        if (term.isBlank()) return
        dao.insertTerm(
            DictionaryTermEntity(
                term = term.trim(),
                gloss = gloss.trim().ifBlank { "自定义哲学词条" }
            )
        )
    }

    suspend fun removeDictionaryTerm(termId: Long) {
        dao.deleteTermById(termId)
    }

    suspend fun saveTuningSettings(settings: TuningSettings) {
        tuningStore.updateSettings(settings)
    }

    suspend fun resetTuningToOrigin() {
        tuningStore.resetToOrigin()
        dao.clearAllTerms()
        ensureDefaultPersonas()
    }

    private suspend fun resolveFallbackPersona(perspective: Perspective): PersonaAvatarEntity? {
        ensureDefaultPersonas()
        val list = dao.getPersonasOnce()
        return list.firstOrNull { it.perspective == perspective.name && it.isDefault }
            ?: list.firstOrNull { it.perspective == perspective.name }
            ?: list.firstOrNull()
    }

    private suspend fun refreshSessionSynthesis(
        sessionId: Long,
        dictionary: List<DictionaryTermEntity>
    ) {
        val sessions = dao.observeAllSessionsWithNodes().first()
        val target = sessions.firstOrNull { it.session.id == sessionId } ?: return
        val nodes = target.nodes.sortedWith(compareBy({ it.createdAt }, { it.id }))
        val synthesis = LocalDialecticEngine.computeSynthesis(nodes, dictionary)

        // Derive a meaningful title if the session still has a generic placeholder title
        var updatedTitle = target.session.title
        if (updatedTitle.startsWith("未命名")) {
            val firstSubject = nodes.firstOrNull { it.perspective == Perspective.SUBJECT.name }?.content
                ?.removePrefix("“")?.removeSuffix("”")
                ?: nodes.firstOrNull()?.content?.removePrefix("“")?.removeSuffix("”")
            if (!firstSubject.isNullOrBlank()) {
                updatedTitle = if (firstSubject.length <= 16) firstSubject else firstSubject.take(16) + "…"
            }
        }

        val elapsedMinutes = ((nodes.lastOrNull()?.createdAt ?: target.session.createdAt) - target.session.createdAt) / 60_000L
        dao.updateSession(
            target.session.copy(
                title = updatedTitle,
                updatedAt = System.currentTimeMillis(),
                synthesisSummary = synthesis.summary,
                resonancePercent = synthesis.resonancePercent,
                tagsCsv = synthesis.tags.joinToString(" · "),
                durationMinutes = elapsedMinutes.toInt().coerceAtLeast(1)
            )
        )
    }
}
