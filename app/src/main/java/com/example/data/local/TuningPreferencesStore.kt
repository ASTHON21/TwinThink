package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AcousticMode
import com.example.data.model.SocraticStance
import com.example.data.model.TuningSettings
import com.example.data.model.VisualThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tuningDataStore: DataStore<Preferences> by preferencesDataStore(name = "dialectic_tuning")

class TuningPreferencesStore(private val context: Context) {
    private object Keys {
        val VISUAL_THEME = stringPreferencesKey("visual_theme")
        val HIGH_CONTRAST_INVERSION = booleanPreferencesKey("high_contrast_inversion")
        val FOCUS_DIMMING = booleanPreferencesKey("focus_dimming")
        val ACOUSTIC_MODE = stringPreferencesKey("acoustic_mode")
        val SILENCE_THRESHOLD = floatPreferencesKey("silence_threshold")
        val PHILOSOPHICAL_PUNCTUATION = booleanPreferencesKey("philosophical_punctuation")
        val SUBJECT_TITLE = stringPreferencesKey("subject_title")
        val SUBJECT_TRAIT = stringPreferencesKey("subject_trait")
        val SUBJECT_VOICE = stringPreferencesKey("subject_voice")
        val OBJECT_TITLE = stringPreferencesKey("object_title")
        val SOCRATIC_STANCE = stringPreferencesKey("socratic_stance")
        val DEV_DEMO_PURGED_V1 = booleanPreferencesKey("dev_demo_purged_v1")
        val ACTIVE_SPEAKER_ID = longPreferencesKey("active_speaker_id")
    }

    val activeSpeakerIdFlow: Flow<Long?> = context.tuningDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_SPEAKER_ID]
    }

    suspend fun setActiveSpeakerId(speakerId: Long) {
        context.tuningDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_SPEAKER_ID] = speakerId
        }
    }

    val settingsFlow: Flow<TuningSettings> = context.tuningDataStore.data.map { prefs ->
        TuningSettings(
            visualThemeMode = prefs[Keys.VISUAL_THEME]
                ?.let { raw -> VisualThemeMode.entries.firstOrNull { it.name == raw } }
                ?: VisualThemeMode.DARK,
            highContrastInversion = prefs[Keys.HIGH_CONTRAST_INVERSION] ?: true,
            focusDimming = prefs[Keys.FOCUS_DIMMING] ?: true,
            acousticMode = prefs[Keys.ACOUSTIC_MODE]
                ?.let { raw -> AcousticMode.entries.firstOrNull { it.name == raw } }
                ?: AcousticMode.OFFLINE_RECOGNIZER,
            silenceThresholdSec = prefs[Keys.SILENCE_THRESHOLD] ?: 3.0f,
            philosophicalPunctuation = prefs[Keys.PHILOSOPHICAL_PUNCTUATION] ?: true,
            subjectArchetypeTitle = prefs[Keys.SUBJECT_TITLE] ?: "内在体验者 / 困惑者",
            subjectArchetypeTrait = prefs[Keys.SUBJECT_TRAIT] ?: "感性流淌 · 即刻自叙",
            subjectArchetypeVoice = prefs[Keys.SUBJECT_VOICE] ?: "第一人称私语 (Mono)",
            objectArchetypeTitle = prefs[Keys.OBJECT_TITLE] ?: "冷彻观察者 / 诘问者",
            socraticStance = prefs[Keys.SOCRATIC_STANCE]
                ?.let { raw -> SocraticStance.entries.firstOrNull { it.name == raw } }
                ?: SocraticStance.DECONSTRUCT
        )
    }

    suspend fun isDevDemoPurged(): Boolean {
        return context.tuningDataStore.data.map { it[Keys.DEV_DEMO_PURGED_V1] ?: false }.first()
    }

    suspend fun markDevDemoPurged() {
        context.tuningDataStore.edit { prefs ->
            prefs[Keys.DEV_DEMO_PURGED_V1] = true
        }
    }

    suspend fun updateSettings(settings: TuningSettings) {
        context.tuningDataStore.edit { prefs ->
            prefs[Keys.VISUAL_THEME] = settings.visualThemeMode.name
            prefs[Keys.HIGH_CONTRAST_INVERSION] = settings.highContrastInversion
            prefs[Keys.FOCUS_DIMMING] = settings.focusDimming
            prefs[Keys.ACOUSTIC_MODE] = settings.acousticMode.name
            prefs[Keys.SILENCE_THRESHOLD] = settings.silenceThresholdSec
            prefs[Keys.PHILOSOPHICAL_PUNCTUATION] = settings.philosophicalPunctuation
            prefs[Keys.SUBJECT_TITLE] = settings.subjectArchetypeTitle
            prefs[Keys.SUBJECT_TRAIT] = settings.subjectArchetypeTrait
            prefs[Keys.SUBJECT_VOICE] = settings.subjectArchetypeVoice
            prefs[Keys.OBJECT_TITLE] = settings.objectArchetypeTitle
            prefs[Keys.SOCRATIC_STANCE] = settings.socraticStance.name
        }
    }

    suspend fun resetToOrigin() {
        context.tuningDataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.DEV_DEMO_PURGED_V1] = true
        }
    }
}
