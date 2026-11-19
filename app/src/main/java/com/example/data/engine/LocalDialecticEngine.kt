package com.example.data.engine

import com.example.data.model.DialecticNodeEntity
import com.example.data.model.DictionaryTermEntity
import com.example.data.model.Perspective
import com.example.data.model.SocraticStance
import kotlin.math.min

data class SynthesisResult(
    val summary: String,
    val resonancePercent: Int,
    val tags: List<String>
)

/**
 * 100% offline, deterministic local philosophical dialectic analyzer.
 * Parses linguistic markers (absolutisms, conditionals, existential tensions, and custom glossary terms)
 * to produce Socratic counter-perspectives, follow-ups, branches, and synthesis summaries without network calls.
 */
object LocalDialecticEngine {

    private data class MotifRule(
        val keywords: List<String>,
        val themeTag: String,
        val deconstructPrompt: String,
        val ironyPrompt: String,
        val stoicPrompt: String
    )

    private val motifRules = listOf(
        MotifRule(
            keywords = listOf("逃避", "妥协", "选择", "决定", "决断", "平庸", "犹疑", "escape", "compromise", "choice", "decide", "decision", "hesitate"),
            themeTag = "#自由的眩晕",
            deconstructPrompt = "你把“决断”等同于“向平庸妥协”，可这是否只是为了保留“尚未落地的全能感”？如果不承担有限性，所谓的选择权究竟属于谁？",
            ironyPrompt = "妙极了——只要永远站在分岔路口赞美无限可能，你就永远不会输给任何一个具体的庸常结果，哪怕你正站在原地不动。",
            stoicPrompt = "犹豫源于把“结果必须完美”当作己有之物。你真正能主宰的，唯有此刻出手的姿态，而非未发生的结局。"
        ),
        MotifRule(
            keywords = listOf("孤独", "寂静", "虚无", "无人", "消融", "独自", "陪伴", "lonely", "loneliness", "silence", "void", "alone", "nobody"),
            themeTag = "#主体间性",
            deconstructPrompt = "当你在追问“若无人见证我的消融”时，那个提出问题的声音本身，难道不是已经在场并凝视着自己的见证者吗？",
            ironyPrompt = "你在抱怨寂静无人旁观，却又小心翼翼地把独白写成剧场台词——你渴望的究竟是绝对自由，还是一个永不退场的隐形观众？",
            stoicPrompt = "外在的见证从不属于你的本性。若你的安宁依赖他人的目光才能成立，那它从来就不是自由，而是柔和的依附。"
        ),
        MotifRule(
            keywords = listOf("完美", "瑕疵", "苛求", "恐惧", "标准", "秩序", "控制", "perfect", "perfection", "flaw", "fear", "control", "order"),
            themeTag = "#悬置完美主义",
            deconstructPrompt = "试着追问这一前提：我们对“无懈可击”的执念，究竟是出于对真理的敬畏，还是为了遮蔽“一旦暴露真实便会丧失价值”的深层恐惧？",
            ironyPrompt = "你亲手修建了一座一尘不染的无菌玻璃房，然后站在门外感叹里面没有鲜活的呼吸。究竟是世界不够完美，还是完美本身拒绝生命？",
            stoicPrompt = "秩序若成为囚笼，便背离了理性之初衷。接纳残缺作为自然流变的一部分，比在脑海中反复预演无菌的完美更有力量。"
        ),
        MotifRule(
            keywords = listOf("意义", "价值", "存在", "目的", "为何", "为什么", "虚妄", "meaning", "value", "exist", "existence", "purpose", "why"),
            themeTag = "#存在先于本质",
            deconstructPrompt = "你假设“意义”必须先于行动而存在，像矿石一样等待被挖掘；倘若意义恰恰是行动落子之后才产生的余晖呢？",
            ironyPrompt = "在深渊边缘反复核对指南针的精度，固然显得无比清醒；可若脚下始终不迈出一步，清醒与沉睡又有什么实质分别？",
            stoicPrompt = "追问宏大目的往往会掩盖当下的职责。履行眼前的具体德性，比在抽象概念里打捞永恒意义更贴近真实。"
        ),
        MotifRule(
            keywords = listOf("焦虑", "情绪", "理智", "冲动", "本能", "磨损", "冷静", "anxiety", "anxious", "emotion", "reason", "impulse", "calm"),
            themeTag = "#理性与感性二律背反",
            deconstructPrompt = "所谓“理智规避磨损”的核算，究竟是在保护生命，还是在用提前写好的防御预案替代了真实的体验？",
            ironyPrompt = "把每一步心动都换算成情绪止损比，听上去像极了高明的会计师——只是不知你最后省下来的，是一段人生，还是一份空白账本？",
            stoicPrompt = "冲动是未经理性检验的印象，而过度的预设则是对未来的怯懦。让判断力停留在当下这一刻的印证上，不必替明天提前受苦。"
        )
    )

    private val absolutismWords = listOf("所有", "全部", "永远", "总是", "必然", "一定", "绝对", "唯一", "从不", "毫无", "always", "never", "everything", "nothing", "absolute")
    private val conditionalWords = listOf("如果", "倘若", "假如", "只要", "一旦", "抑或", "但是", "然而", "if ", "however", "although", "unless")

    fun formatPhilosophicalText(
        raw: String,
        autoPunctuate: Boolean,
        dictionary: List<DictionaryTermEntity>
    ): String {
        var result = raw.trim()
        if (result.isEmpty()) return result

        // Normalize all-uppercase English tokens emitted by bilingual ASR models into readable sentence case
        val asciiLetters = result.filter { it in 'A'..'Z' || it in 'a'..'z' }
        if (asciiLetters.length >= 2 && asciiLetters.all { it in 'A'..'Z' }) {
            result = normalizeUppercaseEnglishSegments(result)
        }

        if (autoPunctuate) {
            val pivotWords = listOf("但是", "然而", "倘若", "抑或", "可是", "不过")
            for (word in pivotWords) {
                result = result.replace(" $word", "——$word")
                if (!result.startsWith(word) && result.contains(word) && !result.contains("——$word") && !result.contains("，$word")) {
                    result = result.replaceFirst(word, "——$word")
                }
            }
            val endsWithPunct = result.endsWith("。") || result.endsWith("？") || result.endsWith("！") ||
                result.endsWith("……") || result.endsWith(".") || result.endsWith("?") || result.endsWith("!")
            if (!endsWithPunct) {
                val hasCjk = result.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }
                val lower = result.lowercase()
                val isQuestion = if (hasCjk) {
                    result.contains("吗") || result.contains("么") || result.contains("何") ||
                        result.contains("谁") || result.contains("哪") || result.contains("怎么")
                } else {
                    lower.startsWith("why ") || lower.startsWith("what ") || lower.startsWith("how ") ||
                        lower.startsWith("who ") || lower.startsWith("where ") || lower.startsWith("when ") ||
                        lower.startsWith("is ") || lower.startsWith("are ") || lower.startsWith("do ") ||
                        lower.startsWith("does ") || lower.startsWith("can ") || lower.startsWith("could ") ||
                        lower.startsWith("should ") || lower.startsWith("would ")
                }
                result += if (hasCjk) {
                    if (isQuestion) "？" else "。"
                } else {
                    if (isQuestion) "?" else "."
                }
            }
        }

        // Normalize common homophone variants toward custom philosophical glossary terms
        dictionary.forEach { entry ->
            val pureTerm = entry.term.substringBefore(" (").trim()
            if (pureTerm == "此在" && result.contains("此刻存在")) {
                result = result.replace("此刻存在", "此在")
            } else if (pureTerm == "悬置" && result.contains("先置")) {
                result = result.replace("先置", "悬置")
            } else if (pureTerm == "意向性" && result.contains("异向性")) {
                result = result.replace("异向性", "意向性")
            }
        }

        return result
    }

    private fun normalizeUppercaseEnglishSegments(text: String): String {
        val sb = StringBuilder(text.length)
        var capitalizeNextAscii = true
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c in 'A'..'Z') {
                // Keep standalone pronoun "I" uppercase
                val prevIsSpaceOrStart = i == 0 || !text[i - 1].isLetterOrDigit()
                val nextIsSpaceOrPunct = i + 1 >= text.length || !text[i + 1].isLetterOrDigit()
                if (c == 'I' && prevIsSpaceOrStart && nextIsSpaceOrPunct) {
                    sb.append('I')
                    capitalizeNextAscii = false
                } else if (capitalizeNextAscii) {
                    sb.append(c)
                    capitalizeNextAscii = false
                } else {
                    sb.append(c.lowercaseChar())
                }
            } else {
                sb.append(c)
                if (c == '.' || c == '?' || c == '!' || c == '。' || c == '？' || c == '！') {
                    capitalizeNextAscii = true
                }
            }
            i++
        }
        return sb.toString()
    }

    fun generateCounterPerspective(
        thesis: String,
        history: List<DialecticNodeEntity>,
        stance: SocraticStance,
        dictionary: List<DictionaryTermEntity>
    ): String {
        val normalized = thesis.trim()
        val normalizedLower = normalized.lowercase()
        val matchedRule = motifRules.firstOrNull { rule ->
            rule.keywords.any { normalizedLower.contains(it) }
        }
        val matchedTerm = dictionary.firstOrNull { entry ->
            val zhTerm = entry.term.substringBefore(" (").trim().lowercase()
            val enTerm = entry.term.substringAfter("(", "").substringBefore(")").trim().lowercase()
            (zhTerm.isNotEmpty() && normalizedLower.contains(zhTerm)) ||
                (enTerm.isNotEmpty() && normalizedLower.contains(enTerm))
        }
        val foundAbsolutism = absolutismWords.firstOrNull { normalizedLower.contains(it) }
        val foundConditional = conditionalWords.firstOrNull { normalizedLower.contains(it) }

        val coreResponse = when {
            matchedTerm != null -> {
                val zhTerm = matchedTerm.term.substringBefore(" (").trim()
                when (stance) {
                    SocraticStance.DECONSTRUCT ->
                        "你刚刚援引了「$zhTerm」这一概念，但请先悬置术语带来的安全感：如果剥去「$zhTerm」的学术外壳，你此刻具体抗拒或渴望的究竟是什么？"
                    SocraticStance.ABSURD_IRONY ->
                        "用「$zhTerm」为心绪命名确实体面，可焦虑并不会因为被贴上现象学标签就自动退场。那个正在使用概念的人，是否也在用概念躲避直视？"
                    SocraticStance.STOIC_DETACHMENT ->
                        "「$zhTerm」只是观察的透镜，而非借口。把注意力从名词定义收回到你的意志边界：此刻你能修正的判断是哪一个？"
                }
            }
            matchedRule != null -> when (stance) {
                SocraticStance.DECONSTRUCT -> matchedRule.deconstructPrompt
                SocraticStance.ABSURD_IRONY -> matchedRule.ironyPrompt
                SocraticStance.STOIC_DETACHMENT -> matchedRule.stoicPrompt
            }
            foundAbsolutism != null -> when (stance) {
                SocraticStance.DECONSTRUCT ->
                    "你的陈述里出现了「$foundAbsolutism」这个全称判断。试着举出一个哪怕微小的反例——当「$foundAbsolutism」出现裂痕时，你真正担心的后果是什么？"
                SocraticStance.ABSURD_IRONY ->
                    "每次人类动用「$foundAbsolutism」来概括自己的处境，通常不是因为看到了终极规律，而是想一口气把模糊地带全数打包封存。"
                SocraticStance.STOIC_DETACHMENT ->
                    "「$foundAbsolutism」往往是情绪把局部放大成了永恒。退后半步看，这真的是一成不变的法则，还是此刻的单一印象？"
            }
            foundConditional != null -> when (stance) {
                SocraticStance.DECONSTRUCT ->
                    "你以「$foundConditional」构筑了一道假设的前提。可如果这个前提本身就是你用来推迟行动的保护壳，去掉「$foundConditional」之后还剩下什么？"
                SocraticStance.ABSURD_IRONY ->
                    "「$foundConditional……」是我们最擅长的思想避风港。只要条件尚未集齐，你就永远拥有旁观的豁免权。"
                SocraticStance.STOIC_DETACHMENT ->
                    "不要把安宁押注在「$foundConditional」这种未定之数上。无论那个假设成立与否，你此刻该如何保持清明？"
            }
            else -> {
                val turnIndex = history.size
                when (stance) {
                    SocraticStance.DECONSTRUCT -> when (turnIndex % 3) {
                        0 -> "在“${extractFragment(normalized)}”背后，似乎隐含着一个未经检验的预设：你认为这一切必须先向你证明其确定性，才值得投入吗？"
                        1 -> "且慢。你描述的是客观事实，还是经过自我防御修饰后的叙事版本？如果换一个毫不偏袒你的旁观者来复述这句话，他会删掉哪几个词？"
                        else -> "试着把刚才的判断倒转过来审视：倘若你所回避的困境，恰好是你此刻唯一能触碰真实自我的入口呢？"
                    }
                    SocraticStance.ABSURD_IRONY -> when (turnIndex % 3) {
                        0 -> "“${extractFragment(normalized)}”——多么精致的自我说服。有时候我们反复剖析痛苦，只是为了把无力感装裱成一种深刻。"
                        1 -> "世界沉默不语，而你却在脑内为它拟定了三份起诉书和一份辩护词。你确定这是在追求真相，而不是一场盛大的独角戏？"
                        else -> "如果把你刚才的严肃思考拉远到十年后的某个午后，它会像宏大悲剧，还是像一次过度认真的误会？"
                    }
                    SocraticStance.STOIC_DETACHMENT -> when (turnIndex % 3) {
                        0 -> "把评价从事件上剥离：除去“${extractFragment(normalized)}”带给你的主观波澜，这件事本身还剩下什么不可承受之处？"
                        1 -> "你正在为尚未落下的评判消耗当下的心智。问自己一句：此事在我的意志控制之内吗？若不在，为何仍交出宁静？"
                        else -> "真正的清醒不在于反复解释困境，而在于辨认出哪一部分只是你的观念投射，然后平静地放下它。"
                    }
                }
            }
        }

        return coreResponse
    }

    fun generateFollowUp(targetNode: DialecticNodeEntity, stance: SocraticStance): String {
        val frag = extractFragment(targetNode.content)
        return when (stance) {
            SocraticStance.DECONSTRUCT ->
                "沿着“$frag”继续下沉一层：如果将这个结论推演到极致，它会瓦解你现在所维护的哪一种自我认同？"
            SocraticStance.ABSURD_IRONY ->
                "再追问一步：假设“$frag”完全成立，那明天清晨醒来时，你打算如何与这个略显滑稽却真实无比的结论共处？"
            SocraticStance.STOIC_DETACHMENT ->
                "就“$frag”而言，剥去所有情绪形容词之后，此刻唯一需要你做出回应的具体行动是什么？"
        }
    }

    fun generateBranchInquiry(targetNode: DialecticNodeEntity, stance: SocraticStance): String {
        val frag = extractFragment(targetNode.content)
        return when (stance) {
            SocraticStance.DECONSTRUCT ->
                "【衍生支点】倘若把“$frag”的主语从“我”替换成“一个完全陌生的人”，你对他的苛责或宽容会发生变化吗？"
            SocraticStance.ABSURD_IRONY ->
                "【衍生支点】反过来想：如果这正是你潜意识故意选择的困境（只因它熟悉且安全），你愿意为此鼓掌还是喊停？"
            SocraticStance.STOIC_DETACHMENT ->
                "【衍生支点】从时间轴的另一端回望“$frag”：五年后的你，会感谢现在的审慎，还是遗憾此刻的迟疑？"
        }
    }

    fun computeSynthesis(
        nodes: List<DialecticNodeEntity>,
        dictionary: List<DictionaryTermEntity>
    ): SynthesisResult {
        if (nodes.isEmpty()) {
            return SynthesisResult(
                summary = "尚待注入第一枚思辨火种。当主我发起自叙、客体予以镜像诘问，合题便会在张力中自然浮现。",
                resonancePercent = 0,
                tags = listOf("#静默前夜", "#待启之思")
            )
        }

        val allText = nodes.joinToString(" ") { it.content }
        val subjectCount = nodes.count { it.perspective == Perspective.SUBJECT.name }
        val objectCount = nodes.count { it.perspective == Perspective.OBJECT.name }

        val tags = linkedSetOf<String>()
        motifRules.forEach { rule ->
            if (rule.keywords.any { allText.contains(it) }) {
                tags.add(rule.themeTag)
            }
        }
        dictionary.forEach { entry ->
            val zhTerm = entry.term.substringBefore(" (").trim()
            if (zhTerm.isNotEmpty() && allText.contains(zhTerm)) {
                tags.add("#$zhTerm")
            }
        }
        if (tags.isEmpty()) {
            tags.add("#自我辩证")
            tags.add("#主体与他者")
        }
        if (subjectCount > 0 && objectCount > 0) {
            tags.add("#对偶校准")
        }

        val balanceScore = if (subjectCount + objectCount == 0) 0 else {
            val ratio = min(subjectCount, objectCount).toFloat() / maxOf(subjectCount, objectCount).toFloat()
            (62 + (ratio * 28) + min(nodes.size, 6) * 1.5f).toInt().coerceIn(58, 98)
        }

        val summary = when {
            objectCount == 0 ->
                "当前流仅由「主我」单向倾诉（共 $subjectCount 段）。建议切换「客体」或触发审视诘问，使情绪经验凝结为可检验的命题。"
            subjectCount == 0 ->
                "「客体」完成了 $objectCount 次冷静解剖，尚缺「主我」真实感受的肉身锚点。理性需要回到具身体验中完成闭环。"
            else -> {
                val primaryTheme = tags.first().removePrefix("#")
                "两极意识历经 $subjectCount 次主我自叙与 $objectCount 次客体折射，张力已聚拢至「$primaryTheme」核心处：真正的止痛并非取消矛盾，而是在看清前提漏洞之后，依然承担起具体选择的重量。"
            }
        }

        return SynthesisResult(
            summary = summary,
            resonancePercent = balanceScore,
            tags = tags.take(4)
        )
    }

    private fun extractFragment(text: String): String {
        val cleaned = text.trim()
            .removePrefix("“").removeSuffix("”")
            .removePrefix("「").removeSuffix("」")
        return if (cleaned.length <= 16) cleaned else cleaned.take(16) + "…"
    }
}
