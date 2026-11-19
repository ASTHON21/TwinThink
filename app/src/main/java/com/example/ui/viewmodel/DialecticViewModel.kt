package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.LocalAudioPlayer
import com.example.data.export.LocalExportManager
import com.example.data.local.DialecticDatabase
import com.example.data.local.TuningPreferencesStore
import com.example.data.model.ArchiveFilter
import com.example.data.model.DialecticNodeEntity
import com.example.data.model.DialecticSessionEntity
import com.example.data.model.DictionaryTermEntity
import com.example.data.model.MascotExpression
import com.example.data.model.PersonaAvatarEntity
import com.example.data.model.Perspective
import com.example.data.model.SessionWithNodes
import com.example.data.model.TuningSettings
import com.example.data.repository.DialecticRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArchiveMetrics(
    val totalCount: Int = 0,
    val resolvedPercent: Int = 0,
    val audioCount: Int = 0
)

class DialecticViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DialecticDatabase.getInstance(application)
    private val tuningStore = TuningPreferencesStore(application)
    private val repository = DialecticRepository(database.dialecticDao(), tuningStore)
    private val audioPlayer = LocalAudioPlayer()

    val sessions: StateFlow<List<SessionWithNodes>> = repository.sessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tuningSettings: StateFlow<TuningSettings> = repository.tuningSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TuningSettings())

    val dictionaryTerms: StateFlow<List<DictionaryTermEntity>> = repository.dictionaryTermsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personas: StateFlow<List<PersonaAvatarEntity>> = repository.personasFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeSpeakerId = MutableStateFlow<Long?>(null)

    val activeSpeaker: StateFlow<PersonaAvatarEntity?> = combine(
        personas,
        repository.activeSpeakerIdFlow,
        _activeSpeakerId
    ) { list, persistedId, localId ->
        val targetId = localId ?: persistedId
        list.firstOrNull { it.id == targetId } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    val activeSession: StateFlow<SessionWithNodes?> = combine(sessions, _activeSessionId) { list, id ->
        if (list.isEmpty()) null
        else list.firstOrNull { it.session.id == id } ?: list.first()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activePerspective = MutableStateFlow(Perspective.SUBJECT)
    val activePerspective: StateFlow<Perspective> = _activePerspective.asStateFlow()

    private val _autoSocraticReply = MutableStateFlow(true)
    val autoSocraticReply: StateFlow<Boolean> = _autoSocraticReply.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _archiveFilter = MutableStateFlow(ArchiveFilter.ALL)
    val archiveFilter: StateFlow<ArchiveFilter> = _archiveFilter.asStateFlow()

    val filteredArchiveSessions: StateFlow<List<SessionWithNodes>> = combine(
        sessions,
        _searchQuery,
        _archiveFilter
    ) { list, query, filter ->
        val trimmed = query.trim().lowercase()
        list.filter { item ->
            val matchesFilter = when (filter) {
                ArchiveFilter.ALL -> true
                ArchiveFilter.SUBJECT -> item.nodes.any { it.perspective == Perspective.SUBJECT.name }
                ArchiveFilter.OBJECT -> item.nodes.any { it.perspective == Perspective.OBJECT.name }
                ArchiveFilter.AUDIO -> item.nodes.any { !it.audioFilePath.isNullOrBlank() }
                ArchiveFilter.RESOLVED -> item.session.isResolved
            }
            val matchesSearch = if (trimmed.isEmpty()) true else {
                item.session.title.lowercase().contains(trimmed) ||
                    item.session.tagsCsv.lowercase().contains(trimmed) ||
                    item.session.synthesisSummary.lowercase().contains(trimmed) ||
                    item.nodes.any {
                        it.content.lowercase().contains(trimmed) ||
                            it.speakerName.lowercase().contains(trimmed)
                    }
            }
            matchesFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedArchiveSessionId = MutableStateFlow<Long?>(null)

    val selectedArchiveSession: StateFlow<SessionWithNodes?> = combine(
        filteredArchiveSessions,
        _selectedArchiveSessionId
    ) { list, selectedId ->
        if (list.isEmpty()) null
        else list.firstOrNull { it.session.id == selectedId } ?: list.first()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val archiveMetrics: StateFlow<ArchiveMetrics> = sessions.combine(sessions) { list, _ ->
        val total = list.size
        val resolved = list.count { it.session.isResolved }
        val resolvedPct = if (total == 0) 0 else (resolved * 100) / total
        val audioNodes = list.sumOf { s ->
            s.nodes.count { !it.audioFilePath.isNullOrBlank() }
        }
        ArchiveMetrics(totalCount = total, resolvedPercent = resolvedPct, audioCount = audioNodes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchiveMetrics())

    private val _playingNodeId = MutableStateFlow<Long?>(null)
    val playingNodeId: StateFlow<Long?> = _playingNodeId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureCleanStart()
        }
    }

    fun togglePerspective() {
        val next = if (_activePerspective.value == Perspective.SUBJECT) {
            Perspective.OBJECT
        } else {
            Perspective.SUBJECT
        }
        _activePerspective.value = next
        val matching = personas.value.firstOrNull { it.perspective == next.name }
        if (matching != null) {
            _activeSpeakerId.value = matching.id
            viewModelScope.launch {
                repository.setActiveSpeakerId(matching.id)
            }
        }
    }

    fun setPerspective(perspective: Perspective) {
        _activePerspective.value = perspective
        val currentSpeaker = activeSpeaker.value
        if (currentSpeaker == null || currentSpeaker.perspective != perspective.name) {
            val matching = personas.value.firstOrNull { it.perspective == perspective.name }
            if (matching != null) {
                _activeSpeakerId.value = matching.id
                viewModelScope.launch {
                    repository.setActiveSpeakerId(matching.id)
                }
            }
        }
    }

    fun selectSpeaker(persona: PersonaAvatarEntity) {
        _activeSpeakerId.value = persona.id
        _activePerspective.value = Perspective.entries.firstOrNull { it.name == persona.perspective }
            ?: Perspective.SUBJECT
        viewModelScope.launch {
            repository.setActiveSpeakerId(persona.id)
        }
    }

    fun cycleSpeaker() {
        val list = personas.value
        if (list.isEmpty()) {
            togglePerspective()
            return
        }
        val currentId = activeSpeaker.value?.id
        val idx = list.indexOfFirst { it.id == currentId }
        val nextPersona = list[(idx + 1) % list.size]
        selectSpeaker(nextPersona)
    }

    fun savePersona(
        id: Long?,
        name: String,
        expression: MascotExpression,
        fillColorArgb: Long,
        strokeColorArgb: Long,
        perspective: Perspective
    ) {
        viewModelScope.launch {
            val savedId = repository.upsertPersona(
                id = id,
                name = name,
                expression = expression,
                fillColorArgb = fillColorArgb,
                strokeColorArgb = strokeColorArgb,
                perspective = perspective
            )
            _activeSpeakerId.value = savedId
            _activePerspective.value = perspective
            repository.setActiveSpeakerId(savedId)
        }
    }

    fun deletePersona(persona: PersonaAvatarEntity) {
        if (persona.isDefault) return
        viewModelScope.launch {
            val remaining = personas.value.filter { it.id != persona.id }
            repository.deletePersona(persona)
            if (_activeSpeakerId.value == persona.id || activeSpeaker.value?.id == persona.id) {
                remaining.firstOrNull()?.let { fallback ->
                    _activeSpeakerId.value = fallback.id
                    _activePerspective.value = Perspective.entries.firstOrNull { it.name == fallback.perspective }
                        ?: Perspective.SUBJECT
                    repository.setActiveSpeakerId(fallback.id)
                }
            }
        }
    }

    fun toggleAutoSocraticReply() {
        _autoSocraticReply.value = !_autoSocraticReply.value
    }

    fun selectSession(sessionId: Long) {
        _activeSessionId.value = sessionId
    }

    fun selectArchiveSession(sessionId: Long) {
        _selectedArchiveSessionId.value = sessionId
    }

    fun createNewSession(title: String, onCreated: (() -> Unit)? = null) {
        viewModelScope.launch {
            val newId = repository.createNewSession(title)
            _activeSessionId.value = newId
            _selectedArchiveSessionId.value = newId
            val subjectAvatar = personas.value.firstOrNull { it.perspective == Perspective.SUBJECT.name }
            if (subjectAvatar != null) {
                selectSpeaker(subjectAvatar)
            } else {
                _activePerspective.value = Perspective.SUBJECT
            }
            onCreated?.invoke()
        }
    }

    fun submitDialogueTurn(text: String, targetSessionId: Long? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val speaker = activeSpeaker.value
        val perspective = speaker?.perspective?.let { raw ->
            Perspective.entries.firstOrNull { it.name == raw }
        } ?: _activePerspective.value

        viewModelScope.launch {
            val resolvedSessionId = targetSessionId
                ?: activeSession.value?.session?.id
                ?: repository.createNewSession("").also { createdId ->
                    _activeSessionId.value = createdId
                    _selectedArchiveSessionId.value = createdId
                }

            repository.addNodeAndSynthesize(
                sessionId = resolvedSessionId,
                perspective = perspective,
                rawText = trimmed,
                speakerAvatar = speaker
            )
            if (perspective == Perspective.SUBJECT && _autoSocraticReply.value) {
                _isAnalyzing.value = true
                delay(420)
                val counterpart = personas.value.firstOrNull { it.perspective == Perspective.OBJECT.name }
                repository.generateSocraticReply(
                    sessionId = resolvedSessionId,
                    latestThesis = trimmed,
                    counterpartAvatar = counterpart
                )
                _isAnalyzing.value = false
            }
        }
    }

    fun commitVoiceCapture(
        perspective: Perspective,
        transcript: String,
        audioFilePath: String?,
        durationSec: Int,
        waveformCsv: String,
        targetSessionId: Long? = null
    ) {
        val speaker = activeSpeaker.value?.takeIf { it.perspective == perspective.name }
            ?: personas.value.firstOrNull { it.perspective == perspective.name }

        viewModelScope.launch {
            val resolvedSessionId = targetSessionId
                ?: activeSession.value?.session?.id
                ?: repository.createNewSession("").also { createdId ->
                    _activeSessionId.value = createdId
                    _selectedArchiveSessionId.value = createdId
                }

            repository.addNodeAndSynthesize(
                sessionId = resolvedSessionId,
                perspective = perspective,
                rawText = transcript,
                audioFilePath = audioFilePath,
                audioDurationSec = durationSec,
                waveformCsv = waveformCsv,
                speakerAvatar = speaker
            )
            if (perspective == Perspective.SUBJECT && _autoSocraticReply.value && transcript.isNotBlank()) {
                _isAnalyzing.value = true
                delay(420)
                val counterpart = personas.value.firstOrNull { it.perspective == Perspective.OBJECT.name }
                repository.generateSocraticReply(
                    sessionId = resolvedSessionId,
                    latestThesis = transcript,
                    counterpartAvatar = counterpart
                )
                _isAnalyzing.value = false
            }
        }
    }

    fun triggerFollowUp(node: DialecticNodeEntity) {
        val sessionId = node.sessionId
        viewModelScope.launch {
            _isAnalyzing.value = true
            delay(320)
            val counterpart = personas.value.firstOrNull { it.perspective == Perspective.OBJECT.name }
            repository.triggerFollowUp(sessionId, node, counterpart)
            _isAnalyzing.value = false
        }
    }

    fun triggerBranchInquiry(node: DialecticNodeEntity) {
        val sessionId = node.sessionId
        viewModelScope.launch {
            _isAnalyzing.value = true
            delay(320)
            val counterpart = personas.value.firstOrNull { it.perspective == Perspective.OBJECT.name }
            repository.triggerBranchInquiry(sessionId, node, counterpart)
            _isAnalyzing.value = false
        }
    }

    fun toggleSessionResolved(session: DialecticSessionEntity) {
        viewModelScope.launch {
            repository.toggleSessionResolved(session)
        }
    }

    fun deleteSession(session: DialecticSessionEntity) {
        viewModelScope.launch {
            if (_activeSessionId.value == session.id) {
                _activeSessionId.value = null
            }
            if (_selectedArchiveSessionId.value == session.id) {
                _selectedArchiveSessionId.value = null
            }
            repository.deleteSession(session)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setArchiveFilter(filter: ArchiveFilter) {
        _archiveFilter.value = filter
    }

    fun resetArchiveFilters() {
        _searchQuery.value = ""
        _archiveFilter.value = ArchiveFilter.ALL
    }

    fun saveTuningSettings(settings: TuningSettings) {
        viewModelScope.launch {
            repository.saveTuningSettings(settings)
        }
    }

    fun addDictionaryTerm(term: String, gloss: String) {
        viewModelScope.launch {
            repository.addDictionaryTerm(term, gloss)
        }
    }

    fun removeDictionaryTerm(termId: Long) {
        viewModelScope.launch {
            repository.removeDictionaryTerm(termId)
        }
    }

    fun resetTuningToOrigin() {
        viewModelScope.launch {
            repository.resetTuningToOrigin()
        }
    }

    fun toggleAudioPlayback(node: DialecticNodeEntity) {
        if (_playingNodeId.value == node.id) {
            stopPlayback()
            return
        }
        val path = node.audioFilePath
        if (path.isNullOrBlank()) {
            stopPlayback()
            return
        }
        stopPlayback()
        _playingNodeId.value = node.id
        _playbackProgress.value = 0f

        val started = audioPlayer.playOrToggle(
            filePath = path,
            onCompletion = {
                _playingNodeId.value = null
                _playbackProgress.value = 0f
                playbackJob?.cancel()
            },
            onError = {
                stopPlayback()
            }
        )
        if (started) {
            playbackJob = viewModelScope.launch {
                while (_playingNodeId.value == node.id && audioPlayer.isPlaying()) {
                    _playbackProgress.value = audioPlayer.currentProgress()
                    delay(80)
                }
            }
        } else {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        audioPlayer.stop()
        _playingNodeId.value = null
        _playbackProgress.value = 0f
    }

    fun buildMarkdownExport(): String = LocalExportManager.buildMarkdownManuscript(sessions.value)

    fun buildJsonExport(): String = LocalExportManager.buildJsonGraph(
        sessions = sessions.value,
        settings = tuningSettings.value,
        dictionary = dictionaryTerms.value
    )

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
