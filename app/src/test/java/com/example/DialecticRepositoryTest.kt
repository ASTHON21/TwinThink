package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DialecticDatabase
import com.example.data.local.TuningPreferencesStore
import com.example.data.model.DialecticSessionEntity
import com.example.data.model.DictionaryTermEntity
import com.example.data.model.MascotExpression
import com.example.data.model.MascotPalette
import com.example.data.model.Perspective
import com.example.data.repository.DialecticRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DialecticRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: DialecticDatabase
    private lateinit var tuningStore: TuningPreferencesStore
    private lateinit var repository: DialecticRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, DialecticDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tuningStore = TuningPreferencesStore(context)
        repository = DialecticRepository(db.dialecticDao(), tuningStore)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun freshStart_hasZeroSessionsAndZeroDictionaryTerms_andStartsAt001() = runTest {
        tuningStore.resetToOrigin()
        repository.ensureCleanStart()

        val initialSessions = repository.sessionsFlow.first()
        val initialTerms = repository.dictionaryTermsFlow.first()
        val initialPersonas = repository.personasFlow.first()
        assertTrue(initialSessions.isEmpty())
        assertTrue(initialTerms.isEmpty())
        assertEquals(2, initialPersonas.size)
        assertEquals("主我", initialPersonas[0].name)
        assertEquals(MascotPalette.presets[0].fillArgb, initialPersonas[0].fillColorArgb)
        assertEquals("客体", initialPersonas[1].name)
        assertEquals(MascotPalette.presets[1].fillArgb, initialPersonas[1].fillColorArgb)

        val id1 = repository.createNewSession("我的首个真实思辨")
        val id2 = repository.createNewSession("第二个真实命题")

        val sessions = repository.sessionsFlow.first()
        assertEquals(2, sessions.size)
        val byId = sessions.associateBy { it.session.id }
        assertEquals("#001", byId[id1]?.session?.sessionCode)
        assertEquals("#002", byId[id2]?.session?.sessionCode)
    }

    @Test
    fun customPersonas_canBeAddedEditedDeleted_andBoundToNodes() = runTest {
        repository.ensureCleanStart()

        val customId = repository.upsertPersona(
            id = null,
            name = "深夜旁白",
            expression = MascotExpression.CURIOUS,
            fillColorArgb = MascotPalette.presets[3].fillArgb,
            strokeColorArgb = MascotPalette.presets[3].strokeArgb,
            perspective = Perspective.SUBJECT
        )

        var personas = repository.personasFlow.first()
        assertEquals(3, personas.size)
        val created = personas.first { it.id == customId }
        assertEquals("深夜旁白", created.name)
        assertEquals(MascotExpression.CURIOUS.name, created.expression)
        assertFalse(created.isDefault)

        val sessionId = repository.createNewSession("形象绑定测试")
        repository.addNodeAndSynthesize(
            sessionId = sessionId,
            perspective = Perspective.SUBJECT,
            rawText = "这是自定义形象的第一句发言",
            speakerAvatar = created
        )

        val nodes = repository.sessionsFlow.first().first { it.session.id == sessionId }.nodes
        assertEquals(1, nodes.size)
        assertEquals(customId, nodes.first().speakerAvatarId)
        assertEquals("深夜旁白", nodes.first().speakerName)

        // Default personas cannot be deleted, custom ones can
        val defaultSubject = personas.first { it.isDefault }
        repository.deletePersona(defaultSubject)
        assertEquals(3, repository.personasFlow.first().size)

        repository.deletePersona(created)
        personas = repository.personasFlow.first()
        assertEquals(2, personas.size)
    }

    @Test
    fun ensureCleanStart_purgesDevDemoRecords_whilePreservingUserSessionsAndOrdering() = runTest {
        val dao = db.dialecticDao()
        val now = System.currentTimeMillis()

        // Insert legacy dev-seeded demo records + terms
        dao.insertSession(
            DialecticSessionEntity(
                sessionCode = "#042",
                title = "不确定性与选择的眩晕",
                createdAt = now - 10_000L,
                updatedAt = now - 10_000L
            )
        )
        dao.insertTerm(DictionaryTermEntity(term = "此在 (Dasein)", gloss = "对自身存在有所领悟与追问的存在者"))

        // Insert real user records (should NOT be deleted)
        val userOldId = dao.insertSession(
            DialecticSessionEntity(
                sessionCode = "#001",
                title = "用户真实对话一",
                createdAt = now - 5_000L,
                updatedAt = now - 5_000L
            )
        )
        val userNewId = dao.insertSession(
            DialecticSessionEntity(
                sessionCode = "#002",
                title = "用户真实对话二",
                createdAt = now - 1_000L,
                updatedAt = now - 1_000L
            )
        )
        dao.insertTerm(DictionaryTermEntity(term = "Logos", gloss = "自定义理性词条"))

        repository.addNodeAndSynthesize(userNewId, Perspective.SUBJECT, "First thought")
        repository.addNodeAndSynthesize(userNewId, Perspective.OBJECT, "Counter thought")

        repository.ensureCleanStart()

        val remainingSessions = repository.sessionsFlow.first()
        assertEquals(2, remainingSessions.size)
        assertEquals(userNewId, remainingSessions[0].session.id)
        assertEquals(userOldId, remainingSessions[1].session.id)
        val nodes = remainingSessions[0].nodes
        assertEquals(2, nodes.size)
        assertEquals(Perspective.SUBJECT.name, nodes[0].perspective)
        assertEquals(Perspective.OBJECT.name, nodes[1].perspective)

        val remainingTerms = repository.dictionaryTermsFlow.first()
        assertEquals(1, remainingTerms.size)
        assertEquals("Logos", remainingTerms.first().term)

        // Resetting to origin clears dictionary terms to zero without re-seeding demo terms
        repository.resetTuningToOrigin()
        assertTrue(repository.dictionaryTermsFlow.first().isEmpty())
    }
}
