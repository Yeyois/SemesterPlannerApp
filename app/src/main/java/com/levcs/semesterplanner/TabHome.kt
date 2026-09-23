package com.levcs.semesterplanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * מסך "עכשיו".
 *
 * הסדר כאן הוא סדר השאלות שסטודנט שואל, מהשכיחה לנדירה: מה אני לומד השבוע
 * וכמה זה עולה לי, מה שבור ודורש טיפול, כמה נשאר לתואר, ומה בא אחרי. קודם
 * המסך נפתח בטבעת אחוזים ובתקציר ההגדרות, ואחריהם קיר של שש התראות מכל
 * ארבע השנים — כולל שנה ג׳ — כך שמה שקורה השבוע היה הדבר הרביעי בעמוד.
 */
@Composable
fun TabHome(m: PlannerModel, d: Derived) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        NowCard(m, d)
        AttentionSection(m, d)
        DegreeCard(m, d)
        NextCard(m, d)
    }
}

// ── הסמסטר הנוכחי ───────────────────────────────────────────────────────────

@Composable
private fun NowCard(m: PlannerModel, d: Derived) {
    val sem = d.current
    Column {
        SectLabel("מה אני לומד עכשיו")
        Column(
            Modifier.fillMaxWidth().card().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(9.dp).background(C.accent, RoundedCornerShape(999.dp)))
                Text(sem.title, style = heading(19.0, lineHeight = 22.0))
                Box(Modifier.weight(1f))
                if (sem.isElul) TagOutline(sem.elulTag)
            }

            if (sem.empty) {
                Text(
                    "אין קורסים משובצים לסמסטר הזה. אם אתה כן לומד עכשיו — " +
                        "הוסף אותם בתכנון, ואם לא — עדכן בהגדרות באיזה סמסטר אתה.",
                    style = body(13.0, C.neutral700, lineHeight = 20.8),
                )
            } else {
                // המספר הגדול הוא השעות בשבוע, כי זו היחידה שהסטודנט מרגיש.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(sem.hoursTxt, style = heading(34.0, sem.barColor, 34.0))
                        Text(
                            "${d.unit} בשבוע\nמתוך ${d.budgetTxt} שהקצבת",
                            style = body(11.5, C.neutral600, lineHeight = 16.1),
                        )
                    }
                    ProgressBar(sem.pct / 100f, sem.barColor, C.neutral200, 10)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "בכיתה ${sem.classTxt} · למידה עצמית ${sem.selfTxt}",
                            style = body(11.0, C.neutral600),
                        )
                        Text(sem.daysText, style = body(11.0, C.neutral600))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sem.courses.forEach { c -> NowCourseRow(m, c) }
                }
            }

            Text(
                if (sem.empty) "פתח את התכנון ›" else "ערוך את הסמסטר בתכנון ›",
                style = body(12.5, C.accent700, 700),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        m.tab = "plan"; m.view = "sem"
                        m.semIdx = Data.semIndex[sem.key] ?: m.semIdx
                        m.planOpenKey = sem.key
                    }
                    .padding(vertical = 4.dp),
            )
        }
    }
}

/**
 * שורת קורס בסמסטר הנוכחי — שם, כמה הוא עולה בשבוע, וסימון "סיימתי".
 *
 * זו הפעולה היחידה שנעשית מכאן: להעביר סמסטר או לפתוח גיליון עושים בתכנון.
 */
@Composable
private fun NowCourseRow(m: PlannerModel, c: CardVM) {
    Row(
        Modifier
            .fillMaxWidth()
            .card(RoundedCornerShape(12.dp), C.bg)
            .clickable { m.sheetId = c.id }
            .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(8.dp).background(c.color, RoundedCornerShape(999.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                c.name,
                style = body(13.5, C.text, 600, 17.6),
                textDecoration = if (c.isDone) TextDecoration.LineThrough else null,
            )
            Text(
                "${c.hoursTxt} ${m.hUnit}/שבוע · ${c.crTxt} נ״ז" +
                    if (c.isDone) " · הושלם" else "",
                style = body(10.5, C.neutral600),
            )
        }
        if (c.hasIssue) {
            Box(
                Modifier
                    .size(20.dp)
                    .background(C.accent100, RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("!", style = body(11.0, C.accent700, 800)) }
        }
        Box(
            Modifier
                .size(38.dp)
                .background(
                    if (c.isDone) C.accent else Color.Transparent,
                    RoundedCornerShape(10.dp),
                )
                .clip(RoundedCornerShape(10.dp))
                .clickable { m.toggleDone(c.id) },
            contentAlignment = Alignment.Center,
        ) { CheckIcon(if (c.isDone) C.white else C.neutral500) }
    }
}

// ── מה דורש טיפול ───────────────────────────────────────────────────────────

@Composable
private fun AttentionSection(m: PlannerModel, d: Derived) {
    val now = d.alertsNow
    val later = d.alerts.size - now.size
    Column {
        SectLabel("מה דורש טיפול")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (now.isEmpty()) {
                Box(Modifier.fillMaxWidth().card().padding(16.dp)) {
                    Text(
                        if (d.alerts.isEmpty())
                            "אין חריגות פתוחות — התכנון עומד בתקציב ובקדם־הדרישות."
                        else "שום דבר לא דורש טיפול בסמסטר הזה ובבא אחריו.",
                        style = body(13.0, C.neutral700, lineHeight = 20.8),
                    )
                }
            }
            now.take(3).forEach { a -> AlertRow(m, a) }
            // ההתראות הרחוקות לא נעלמות, הן פשוט מפסיקות לצעוק: שנה ג׳ היא
            // משהו לתקן בעונת הרישום, לא בבוקר של יום שלישי.
            if (later > 0 || now.size > 3) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .card()
                        .clickable { m.alertsOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "כל ההתראות (${d.alerts.size}) — כולל סמסטרים רחוקים",
                        style = body(12.5, C.accent700, 600),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertRow(m: PlannerModel, a: Alert) {
    StripeCard(if (a.sev >= 2) C.accent else C.neutral500, onClick = {
        val i = Data.SEMS.indexOfFirst { it.key == a.semKey }
        m.tab = "plan"; m.view = "sem"
        m.semIdx = if (i < 0) 0 else i
        m.planOpenKey = a.semKey
        m.sheetId = a.courseId.ifEmpty { null }
    }) {
        Text(
            a.semTitle + " · " + when {
                a.dist < 0 -> "מאחוריך"
                a.dist == 0 -> "עכשיו"
                else -> "הסמסטר הבא"
            },
            style = body(10.5, if (a.sev >= 2) C.accent700 else C.neutral600, 700),
        )
        Text(a.text, style = body(12.5, C.text, lineHeight = 18.75))
    }
}

// ── ההתקדמות בתואר ──────────────────────────────────────────────────────────

@Composable
private fun DegreeCard(m: PlannerModel, d: Derived) {
    Column {
        SectLabel("איפה אני בתואר")
        Row(
            Modifier
                .fillMaxWidth()
                .card()
                .clickable { m.tab = "shelf" }
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Donut(d.degreePct)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${Data.n(d.doneCr)} מתוך ${Data.n(d.totalCr)} נ״ז",
                        style = body(13.5, weight = 700),
                    )
                    Text(
                        "נשארו ${Data.n(d.totalCr - d.doneCr)} נ״ז",
                        style = body(11.5, C.neutral600),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("סיום צפוי", style = body(11.0, C.neutral600))
                    Text(d.finishTxt, style = heading(15.0, lineHeight = 19.5))
                    if (d.semsLeft > 0) {
                        Text(
                            "עוד ${d.semsLeft} סמסטרים מהיום",
                            style = body(11.0, C.neutral600),
                        )
                    }
                }
            }
        }
    }
}

// ── מה בא אחרי ──────────────────────────────────────────────────────────────

@Composable
private fun NextCard(m: PlannerModel, d: Derived) {
    val next = d.next ?: return
    Column {
        SectLabel("הבא בתור")
        Column(
            Modifier
                .fillMaxWidth()
                .card()
                .clickable {
                    m.tab = "plan"; m.view = "sem"
                    m.semIdx = Data.semIndex[next.key] ?: m.semIdx
                    m.planOpenKey = next.key
                }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(next.title, style = heading(16.0))
                Box(Modifier.weight(1f))
                Text(
                    "${next.hoursTxt} ${d.unit}",
                    style = body(12.5, next.barColor, 700),
                    maxLines = 1,
                )
            }
            ProgressBar(next.pct / 100f, next.barColor, C.neutral200, 6)
            Text(next.names, style = body(12.0, C.neutral700, lineHeight = 18.0))
        }
    }
}

/** טבעת ההתקדמות של התואר — conic-gradient בעיצוב המקורי. */
@Composable
fun Donut(pct: Int) {
    Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                color = C.neutral300,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = true,
            )
            drawArc(
                color = C.text,
                startAngle = -90f,
                sweepAngle = 360f * (pct / 100f),
                useCenter = true,
            )
            val inset = 15.dp.toPx()
            drawCircle(
                color = C.bg,
                radius = size.minDimension / 2 - inset,
                center = center,
            )
        }
        Text("$pct%", style = heading(22.0))
    }
}
