package com.example.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.DictionaryTermEntity
import com.example.data.model.Perspective
import com.example.data.model.SessionWithNodes
import com.example.data.model.TuningSettings
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalExportManager {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    fun buildMarkdownManuscript(sessions: List<SessionWithNodes>): String {
        val sb = StringBuilder()
        sb.appendLine("# 双生思辨 · 本地哲学档案手稿")
        sb.appendLine("> 导出时间：${timeFormat.format(Date())} · 存储模式：100% 本地离线")
        sb.appendLine()

        sessions.forEach { entry ->
            val s = entry.session
            sb.appendLine("---")
            sb.appendLine("## ${s.sessionCode} ${s.title}")
            sb.appendLine("- 状态：${if (s.isResolved) "矛盾闭环" else "思辨进行中"} · 耗时 ${s.durationMinutes} 分钟")
            if (s.tagsCsv.isNotBlank()) {
                sb.appendLine("- 标签：${s.tagsCsv}")
            }
            sb.appendLine()
            entry.nodes.sortedBy { it.createdAt }.forEach { node ->
                val defaultRole = if (node.perspective == Perspective.SUBJECT.name) "主体 / THESIS" else "客体 / ANTITHESIS"
                val role = if (node.speakerName.isNotBlank()) "${node.speakerName} ($defaultRole)" else defaultRole
                val time = timeFormat.format(Date(node.createdAt))
                sb.appendLine("### [$role] · $time")
                sb.appendLine("> ${node.content}")
                if (node.audioDurationSec > 0) {
                    sb.appendLine("*(声波记录时长：${node.audioDurationSec} 秒)*")
                }
                sb.appendLine()
            }
            if (s.synthesisSummary.isNotBlank()) {
                sb.appendLine("**SYNTHESIS · 合题演化（共振 ${s.resonancePercent}%）**：${s.synthesisSummary}")
                sb.appendLine()
            }
        }
        return sb.toString()
    }

    fun buildJsonGraph(
        sessions: List<SessionWithNodes>,
        settings: TuningSettings,
        dictionary: List<DictionaryTermEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "双生思辨")
        root.put("mode", "local_offline")
        root.put("exportedAt", timeFormat.format(Date()))

        val tuningJson = JSONObject().apply {
            put("visualTheme", settings.visualThemeMode.name)
            put("highContrastInversion", settings.highContrastInversion)
            put("focusDimming", settings.focusDimming)
            put("acousticMode", settings.acousticMode.name)
            put("silenceThresholdSec", settings.silenceThresholdSec.toDouble())
            put("philosophicalPunctuation", settings.philosophicalPunctuation)
            put("socraticStance", settings.socraticStance.name)
        }
        root.put("tuning", tuningJson)

        val dictArray = JSONArray()
        dictionary.forEach { term ->
            dictArray.put(JSONObject().apply {
                put("term", term.term)
                put("gloss", term.gloss)
            })
        }
        root.put("dictionary", dictArray)

        val sessionArray = JSONArray()
        sessions.forEach { item ->
            val s = item.session
            val nodesArr = JSONArray()
            item.nodes.sortedBy { it.createdAt }.forEach { node ->
                nodesArr.put(JSONObject().apply {
                    put("id", node.id)
                    put("perspective", node.perspective)
                    put("speakerName", node.speakerName)
                    put("content", node.content)
                    put("createdAt", node.createdAt)
                    put("audioDurationSec", node.audioDurationSec)
                })
            }
            sessionArray.put(JSONObject().apply {
                put("sessionCode", s.sessionCode)
                put("title", s.title)
                put("isResolved", s.isResolved)
                put("resonancePercent", s.resonancePercent)
                put("synthesisSummary", s.synthesisSummary)
                put("tags", s.tagsCsv)
                put("nodes", nodesArr)
            })
        }
        root.put("sessions", sessionArray)
        return root.toString(2)
    }

    fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: return Result.failure(IllegalStateException("无法打开目标文件输出流"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareText(context: Context, title: String, content: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun printDialecticCards(context: Context, sessions: List<SessionWithNodes>) {
        val html = buildString {
            append("<html><head><meta charset='utf-8'><style>")
            append("body{font-family:serif;color:#111;padding:24px;line-height:1.7;}")
            append(".card{border:1px solid #222;padding:16px;margin-bottom:20px;page-break-inside:avoid;}")
            append(".meta{font-family:monospace;font-size:11px;color:#555;margin-bottom:6px;}")
            append("h2{margin:4px 0 12px 0;font-size:18px;}")
            append(".node{margin:8px 0;padding-left:10px;border-left:2px solid #222;}")
            append(".role{font-family:monospace;font-size:10px;font-weight:bold;}")
            append("</style></head><body>")
            append("<h1>双生思辨 · 极简实体归档卡片</h1>")
            sessions.forEach { entry ->
                val s = entry.session
                append("<div class='card'>")
                append("<div class='meta'>${s.sessionCode} · ${timeFormat.format(Date(s.createdAt))} · ${if (s.isResolved) "闭环" else "演进中"}</div>")
                append("<h2>${escapeHtml(s.title)}</h2>")
                entry.nodes.sortedBy { it.createdAt }.forEach { n ->
                    val defaultRole = if (n.perspective == Perspective.SUBJECT.name) "主体 / THESIS" else "客体 / ANTITHESIS"
                    val role = if (n.speakerName.isNotBlank()) "${escapeHtml(n.speakerName)} ($defaultRole)" else defaultRole
                    append("<div class='node'><div class='role'>$role</div><div>${escapeHtml(n.content)}</div></div>")
                }
                if (s.synthesisSummary.isNotBlank()) {
                    append("<p><strong>合题演化 (${s.resonancePercent}%)：</strong>${escapeHtml(s.synthesisSummary)}</p>")
                }
                append("</div>")
            }
            append("</body></html>")
        }

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val jobName = "双生思辨_归档卡片"
                val adapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun escapeHtml(raw: String): String {
        return raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
