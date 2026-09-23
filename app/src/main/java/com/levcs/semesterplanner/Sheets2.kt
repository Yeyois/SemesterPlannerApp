package com.levcs.semesterplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

// ── גיליון התראות ───────────────────────────────────────────────────────────

@Composable
fun AlertsSheet(m: PlannerModel, d: Derived) {
    BottomSheet({ m.alertsOpen = false }, 0.80f) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("התראות", style = heading(16.0, lineHeight = 20.8), modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(32.dp).clickable { m.alertsOpen = false },
                    contentAlignment = Alignment.Center,
                ) { Text("×", style = body(18.0, C.neutral700)) }
            }
            if (d.alerts.isEmpty()) {
                Text(
                    "אין התראות כרגע.",
                    style = body(13.0, C.neutral600),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            // מחולק לפי מרחק מ"עכשיו": רשימה שטוחה של שש התראות מארבע שנים
            // הציגה חריגה בשנה ג׳ באותו משקל כמו קדם חסר בסמסטר שאתה יושב בו.
            val near = d.alerts.filter { it.dist <= 1 }
            val far = d.alerts.filter { it.dist > 1 }
            listOf("עכשיו ובסמסטר הבא" to near, "בהמשך התואר" to far).forEach { (lbl, list) ->
                if (list.isEmpty()) return@forEach
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectLabel(lbl)
                    list.forEach { a ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .card(background = a.bg)
                                .border(if (a.sev >= 2) 2.dp else 1.dp, a.line, CardShape)
                                .clickable {
                                    val i = Data.SEMS.indexOfFirst { it.key == a.semKey }
                                    m.tab = "plan"; m.view = "sem"
                                    m.semIdx = if (i < 0) 0 else i
                                    m.planOpenKey = a.semKey
                                    m.sheetId = a.courseId.ifEmpty { null }
                                    m.alertsOpen = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                a.semTitle + if (a.dist < 0) " · מאחוריך" else "",
                                style = body(10.5, C.neutral600, 700),
                            )
                            Text(a.text, style = body(13.0, C.text, 600, 17.55))
                        }
                    }
                }
            }
        }
    }
}

// ── גיליון הוספת קורס לסמסטר ────────────────────────────────────────────────

@Composable
fun PickerSheet(m: PlannerModel, d: Derived) {
    val sem = d.sem
    val showEl = m.setup.showElectives

    val items = Data.courses.filter { c ->
        if (c.id in m.done) return@filter false
        if (m.plan[c.id] == sem.key) return@filter false
        if (!showEl && !c.mand) return@filter false
        if (sem.isElul) true else Data.fits(c, sem.key.replace(Regex("^y\\d"), ""))
    }.map { c ->
        // עומס הקורס בסמסטר היעד — באלול הוא גבוה יותר, וזו ההחלטה שנעשית כאן
        val dd = Data.diff(m.loadOf(c, sem.key).total)
        val iss = Compute.issuesFor(m, c, sem.key)
        val from = m.plan[c.id]?.let { Data.semByKey(it)?.title }
        Triple(c, dd, iss to (if (from != null) "כרגע ב$from" else "מהמדף"))
    }.sortedBy { it.third.first.isEmpty() }

    BottomSheet({ m.pickerOpen = false }, 0.86f) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(C.ground)
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("הוסף קורס ל", style = body(10.5, C.neutral600, 600))
                    Text(sem.title, style = heading(17.0))
                }
                Box(
                    Modifier
                        .size(38.dp)
                        .card(RoundedCornerShape(12.dp))
                        .clickable { m.pickerOpen = false },
                    contentAlignment = Alignment.Center,
                ) { Text("×", style = body(18.0, C.neutral700)) }
            }

            Text(
                if (sem.isElul)
                    "באלול מוצגים גם קורסים שלא מסומנים כמתקיימים — ודא מול מערכת השעות."
                else "מוצגים רק קורסים שנפתחים בסמסטר הזה. אזהרת קדם־דרישה מופיעה מתחת לשם.",
                style = body(12.0, C.neutral600, lineHeight = 18.6),
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
            )

            Column(
                Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEach { (c, dd, meta) ->
                    val (iss, fromTxt) = meta
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .card()
                            .clickable {
                                m.place(c.id, sem.key)
                                m.pickerOpen = false
                                m.say("${c.name} נוסף ל${sem.title}", true)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier
                                .padding(top = 6.dp)
                                .size(8.dp)
                                .background(dd.third, RoundedCornerShape(999.dp)),
                        )
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(c.name, style = body(14.0, C.text, 600, 18.2))
                            TagFlow {
                                TagNeutral("${Data.n(c.cr)} נ״ז")
                                TagNeutral("${m.hTxt(m.loadOf(c, sem.key).total)} ${m.hUnit}")
                                TagOutline(fromTxt)
                            }
                            if (iss.isNotEmpty()) {
                                Box(
                                    Modifier
                                        .background(C.accent100, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        iss.joinToString(" · ") { it.text },
                                        style = body(11.5, C.accent700, lineHeight = 16.1),
                                    )
                                }
                            }
                        }
                        Text(
                            "+",
                            style = body(16.0, C.neutral500),
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
                if (items.isEmpty()) {
                    Text(
                        "אין קורס פנוי שנפתח בסמסטר הזה. נסה סמסטר אחר, או סמן קורס שהושלם כדי לפנות קדם־דרישה.",
                        style = body(13.0, C.neutral600, lineHeight = 20.8),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

// ── דיאלוג אישור ────────────────────────────────────────────────────────────

@Composable
fun ConfirmDialog(cm: ConfirmModal, onNo: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(C.scrim)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onNo() },
        )
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .background(C.ground, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {}
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(cm.title, style = heading(16.0, lineHeight = 20.8))
            Text(cm.msg, style = body(13.5, C.neutral700, lineHeight = 20.9))
            Row(
                Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinePillButton("לא", onNo, Modifier.weight(1f), minHeight = 46, fontSize = 14.0)
                PillButton(
                    "כן",
                    cm.onYes,
                    Modifier.weight(1f),
                    minHeight = 46,
                    fontSize = 14.0,
                    weight = 700,
                )
            }
        }
    }
}
