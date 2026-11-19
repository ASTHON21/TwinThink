package com.example

import com.example.data.engine.LocalDialecticEngine
import com.example.data.model.DialecticNodeEntity
import com.example.data.model.DictionaryTermEntity
import com.example.data.model.Perspective
import com.example.data.model.SocraticStance
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDialecticEngineTest {

    private val defaultTerms = listOf(
        DictionaryTermEntity(id = 1, term = "此在 (Dasein)", gloss = "对自身存在有所领悟的存在者"),
        DictionaryTermEntity(id = 2, term = "悬置 (Epoché)", gloss = "暂时搁置自然态度的判断")
    )

    @Test
    fun formatPhilosophicalText_appliesDashAndHomophoneCorrection() {
        val raw = "此刻存在常常感到孤独 但是不愿妥协"
        val formatted = LocalDialecticEngine.formatPhilosophicalText(
            raw = raw,
            autoPunctuate = true,
            dictionary = defaultTerms
        )
        assertTrue(formatted.contains("此在"))
        assertTrue(formatted.contains("——但是"))
        assertTrue(formatted.endsWith("。"))
    }

    @Test
    fun generateCounterPerspective_variesBySocraticStance() {
        val thesis = "如果所有的选择本质上都是对不确定性的妥协，我究竟在逃避什么？"
        val deconstruct = LocalDialecticEngine.generateCounterPerspective(
            thesis = thesis,
            history = emptyList(),
            stance = SocraticStance.DECONSTRUCT,
            dictionary = defaultTerms
        )
        val stoic = LocalDialecticEngine.generateCounterPerspective(
            thesis = thesis,
            history = emptyList(),
            stance = SocraticStance.STOIC_DETACHMENT,
            dictionary = defaultTerms
        )
        assertTrue(deconstruct.isNotBlank())
        assertTrue(stoic.isNotBlank())
        assertTrue(deconstruct != stoic)
    }

    @Test
    fun computeSynthesis_calculatesResonanceAndTags() {
        val nodes = listOf(
            DialecticNodeEntity(
                sessionId = 1,
                perspective = Perspective.SUBJECT.name,
                content = "我对完美感到恐惧与焦虑"
            ),
            DialecticNodeEntity(
                sessionId = 1,
                perspective = Perspective.OBJECT.name,
                content = "完美只是逃避真实暴露的借口"
            )
        )
        val result = LocalDialecticEngine.computeSynthesis(nodes, defaultTerms)
        assertTrue(result.resonancePercent in 58..98)
        assertTrue(result.tags.isNotEmpty())
        assertTrue(result.summary.isNotBlank())
    }

    @Test
    fun formatAndRespond_handlesBilingualEnglishInput() {
        val raw = "WHY DO I FEAR MAKING A CHOICE"
        val formatted = LocalDialecticEngine.formatPhilosophicalText(
            raw = raw,
            autoPunctuate = true,
            dictionary = defaultTerms
        )
        assertTrue(formatted == "Why do I fear making a choice?")

        val reply = LocalDialecticEngine.generateCounterPerspective(
            thesis = formatted,
            history = emptyList(),
            stance = SocraticStance.DECONSTRUCT,
            dictionary = defaultTerms
        )
        assertTrue(reply.isNotBlank())
    }
}
