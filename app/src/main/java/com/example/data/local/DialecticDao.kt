package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.DialecticNodeEntity
import com.example.data.model.DialecticSessionEntity
import com.example.data.model.DictionaryTermEntity
import com.example.data.model.PersonaAvatarEntity
import com.example.data.model.SessionWithNodes
import kotlinx.coroutines.flow.Flow

@Dao
interface DialecticDao {
    @Transaction
    @Query("SELECT * FROM dialectic_sessions ORDER BY updatedAt DESC")
    fun observeAllSessionsWithNodes(): Flow<List<SessionWithNodes>>

    @Transaction
    @Query("SELECT * FROM dialectic_sessions WHERE id = :sessionId")
    fun observeSessionWithNodes(sessionId: Long): Flow<SessionWithNodes?>

    @Query("SELECT * FROM dialectic_sessions ORDER BY updatedAt DESC")
    suspend fun getAllSessionsOnce(): List<DialecticSessionEntity>

    @Query("SELECT * FROM dialectic_sessions ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestSession(): DialecticSessionEntity?

    @Query("SELECT COUNT(*) FROM dialectic_sessions")
    suspend fun getSessionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DialecticSessionEntity): Long

    @Update
    suspend fun updateSession(session: DialecticSessionEntity)

    @Query("DELETE FROM dialectic_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: DialecticNodeEntity): Long

    @Query("SELECT * FROM dialectic_nodes WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
    suspend fun getNodesForSession(sessionId: Long): List<DialecticNodeEntity>

    @Query("DELETE FROM dialectic_nodes WHERE id = :nodeId")
    suspend fun deleteNodeById(nodeId: Long)

    @Query("SELECT * FROM dictionary_terms ORDER BY createdAt ASC")
    fun observeDictionaryTerms(): Flow<List<DictionaryTermEntity>>

    @Query("SELECT * FROM dictionary_terms ORDER BY createdAt ASC")
    suspend fun getDictionaryTermsOnce(): List<DictionaryTermEntity>

    @Query("SELECT COUNT(*) FROM dictionary_terms")
    suspend fun getDictionaryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerm(term: DictionaryTermEntity): Long

    @Query("DELETE FROM dictionary_terms WHERE id = :termId")
    suspend fun deleteTermById(termId: Long)

    @Query("SELECT * FROM persona_avatars ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    fun observePersonas(): Flow<List<PersonaAvatarEntity>>

    @Query("SELECT * FROM persona_avatars ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    suspend fun getPersonasOnce(): List<PersonaAvatarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: PersonaAvatarEntity): Long

    @Update
    suspend fun updatePersona(persona: PersonaAvatarEntity)

    @Query("DELETE FROM persona_avatars WHERE id = :personaId")
    suspend fun deletePersonaById(personaId: Long)

    @Query("DELETE FROM dialectic_sessions")
    suspend fun clearAllSessions()

    @Query("DELETE FROM dictionary_terms")
    suspend fun clearAllTerms()
}
