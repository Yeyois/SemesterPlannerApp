package com.levcs.semesterplanner

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** שלושת מסלולי הייצוא של האב-טיפוס: שיתוף טקסט, CSV והדפסה ל-PDF. */
object Export {

    fun run(ctx: Context, m: PlannerModel, kind: String) {
        when (kind) {
            "share" -> share(ctx, m)
            "csv" -> csv(ctx, m)
            "table" -> printTable(ctx, m)
        }
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun share(ctx: Context, m: PlannerModel) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "תכנון סמסטרים")
            putExtra(Intent.EXTRA_TEXT, m.shareText())
        }
        ctx.startActivity(
            Intent.createChooser(intent, "שתף את התכנון")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun csv(ctx: Context, m: PlannerModel) {
        val (head, rows) = m.exportRows()
        val csv = (listOf(head) + rows).joinToString("\r\n") { r ->
            r.joinToString(",") { "\"" + it.replace("\"", "\"\"") + "\"" }
        }
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val f = File(dir, "תכנון-סמסטרים-${stamp()}.csv")
        // BOM כדי שאקסל יזהה UTF-8 ויציג עברית נכון
        f.writeBytes("﻿$csv".toByteArray(Charsets.UTF_8))

        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(
            Intent.createChooser(intent, "קובץ אקסל (CSV)")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        m.say("קובץ CSV נוצר — נפתח ישירות באקסל.")
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;")

    private fun printTable(ctx: Context, m: PlannerModel) {
        val (head, rows) = m.exportRows()
        val st = m.setup
        val today = SimpleDateFormat("dd/MM/yyyy", Locale("he")).format(Date())
        val table = buildString {
            append("<table><thead><tr>")
            head.forEach { append("<th>").append(esc(it)).append("</th>") }
            append("</tr></thead><tbody>")
            rows.forEach { r ->
                append("<tr>")
                r.forEach { append("<td>").append(esc(it)).append("</td>") }
                append("</tr>")
            }
            append("</tbody></table>")
        }
        val html = """
            <!DOCTYPE html><html dir="rtl" lang="he"><head><meta charset="utf-8">
            <title>תכנון סמסטרים</title><style>
            @page{size:A4 portrait;margin:12mm}
            body{background:#fff;padding:0;font-family:sans-serif}
            h1{font-weight:800;font-size:20px;margin:0 0 4px}
            p{font-size:11px;color:#605d5d;margin:0 0 12px}
            table{width:100%;border-collapse:collapse;font-size:10px}
            th{text-align:start;font-weight:700;border-bottom:2px solid #201e1d;padding:5px}
            td{border-bottom:1px solid #cfcccb;padding:4px 5px}
            </style></head><body>
            <h1>מתכנן סמסטרים</h1>
            <p>מתחיל שנה ${st.startYear} · עבודה ${st.jobPercent}% ·
               תקציב ${Data.n(m.budgetHours())} ש׳/שבוע · $today<br>
               ש״ש = שעה אקדמית (${Data.n(Hours.AC_MIN)} דק׳) ·
               ש׳ = שעת שעון · אלול נספר ×${Data.n(Hours.elulMul)}</p>
            $table</body></html>
        """.trimIndent()

        val web = WebView(ctx)
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
                pm.print(
                    "תכנון סמסטרים",
                    view.createPrintDocumentAdapter("תכנון סמסטרים"),
                    PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .build(),
                )
            }
        }
        web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        m.say("נפתח חלון הדפסה — בחר \"שמור כ־PDF\".")
    }
}
