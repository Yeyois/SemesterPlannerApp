package com.levcs.semesterplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * "הקורסים שלי" — כל הקטלוג של הסטודנט במקום אחד, לפי הסטטוס.
 *
 * קודם היה זה "מדף": רשימת הקורסים שלא שובצו, ומתחתיה כפתור אדום גדול
 * שמשבץ את כל קורסי הבחירה בבת אחת. הקורסים שכבר נלמדו — גיליון הציונים —
 * היו שורה מקופלת בתחתית, מתחת לכפתור. המסך עונה עכשיו על שלוש השאלות
 * באותו משקל: מה נשאר לי, מה כבר מתוכנן, ומה כבר עשיתי.
 */
@Composable
fun TabShelf(m: PlannerModel, d: Derived) {
    val f = m.shelfFilter
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SearchBox(m)

        FilterChips(
            listOf(
                "left" to "נותרו (${d.unplaced.size})",
                "planned" to "מתוכננים (${d.plannedCount})",
                "done" to "הושלמו (${d.doneCourses.size})",
            ),
            f,
        ) { m.shelfFilter = it }

        when (f) {
            "planned" -> PlannedList(m, d)
            "done" -> DoneList(m, d)
            else -> LeftList(m, d)
        }
    }
}

// ── מה נשאר לשבץ ────────────────────────────────────────────────────────────

@Composable
private fun LeftList(m: PlannerModel, d: Derived) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().card().padding(14.dp)) {
            Text(d.shelfNote, style = body(12.5, C.neutral600, lineHeight = 19.4))
        }

        d.shelfGroups.forEach { g ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectLabel(g.title)
                g.courses.forEach { c -> CourseRow(m, c, action = Action.PLACE) }
            }
        }

        if (d.shelfEmpty) {
            Box(Modifier.fillMaxWidth().card().padding(18.dp)) {
                Text(d.shelfEmptyTxt, style = body(13.0, C.neutral600, lineHeight = 20.8))
            }
        }

        // השיבוץ ההמוני ירד מראש המסך לתחתיתו, ומאדום מלא לכפתור משני: הוא
        // מזיז עשרות קורסים בלחיצה אחת, ולא זה מה שנכנסים למסך הזה בשבילו.
        if (d.shelfHasElectives) {
            CardPillButton("שבץ אוטומטית את כל קורסי הבחירה", { m.autoElectives() })
        }
    }
}

// ── מה כבר מתוכנן ───────────────────────────────────────────────────────────

@Composable
private fun PlannedList(m: PlannerModel, d: Derived) {
    if (d.plannedGroups.isEmpty()) {
        Box(Modifier.fillMaxWidth().card().padding(18.dp)) {
            Text(
                if (m.query.isNotEmpty()) "לא נמצא קורס מתוכנן בשם או במספר הזה."
                else "אין קורסים מתוכננים עדיין.",
                style = body(13.0, C.neutral600, lineHeight = 20.8),
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        d.plannedGroups.forEach { g ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectLabel(g.title)
                g.courses.forEach { c -> CourseRow(m, c, action = Action.MOVE) }
            }
        }
    }
}

// ── מה כבר נלמד ─────────────────────────────────────────────────────────────

@Composable
private fun DoneList(m: PlannerModel, d: Derived) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().card().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("סך הכול", style = body(12.5, C.neutral600))
            Box(Modifier.weight(1f))
            Text(
                "${d.doneCourses.size} קורסים · ${Data.n(d.doneCr)} נ״ז",
                style = body(13.0, C.text, 700),
            )
        }

        if (d.doneFiltered.isEmpty()) {
            Box(Modifier.fillMaxWidth().card().padding(18.dp)) {
                Text(
                    if (m.query.isNotEmpty()) "לא נמצא קורס שהושלם בשם או במספר הזה."
                    else "עוד לא סימנת קורסים כהושלמו.",
                    style = body(13.0, C.neutral600, lineHeight = 20.8),
                )
            }
        }

        d.doneFiltered.forEach { c ->
            Row(
                Modifier.fillMaxWidth().card().height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clickable { m.sheetId = c.id }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        c.name,
                        style = body(13.5, C.text, lineHeight = 18.2),
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.weight(1f),
                    )
                    TagNeutral("${Data.n(c.cr)} נ״ז")
                }
                Box(
                    Modifier
                        .width(78.dp)
                        .height(48.dp)
                        .clickable { m.toggleDone(c.id) },
                    contentAlignment = Alignment.Center,
                ) { Text("בטל", style = body(11.5, C.neutral800)) }
            }
        }
    }
}

// ── חלקים משותפים ───────────────────────────────────────────────────────────

@Composable
private fun SearchBox(m: PlannerModel) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .card(RoundedCornerShape(14.dp), C.bg)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = m.query,
            onValueChange = { m.query = it },
            textStyle = body(14.0, C.text),
            singleLine = true,
            cursorBrush = SolidColor(C.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (m.query.isEmpty()) {
                    Text("חפש קורס לפי שם או מספר", style = body(14.0, C.neutral600))
                }
                inner()
            },
        )
    }
}

@Composable
private fun FilterChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().card().padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (key, label) ->
            val on = key == selected
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(if (on) C.text else androidx.compose.ui.graphics.Color.Transparent, shape)
                    .clip(shape)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = body(11.5, if (on) C.bg else C.neutral800, 600),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

private enum class Action { PLACE, MOVE }

/**
 * שורת קורס במסך הקורסים.
 *
 * הפעולה בקצה משתנה לפי הרשימה: קורס שעל המדף מקבל "שבץ ל…" עם הסמסטר
 * שהאלגוריתם ממליץ עליו, וקורס מתוכנן מקבל "העבר" — כי הוא כבר במקום.
 */
@Composable
private fun CourseRow(m: PlannerModel, c: CardVM, action: Action) {
    // קורס שאין לו סמסטר אוטומטי מקבל כפתור שפותח בחירה ידנית, והסיבה
    // נכתבת מתחתיו — במקום כפתור שכתוב עליו "אין מקום" ולא קורה בו כלום.
    val blocked = if (action == Action.PLACE) Compute.placeBlockTxt(m, c.id) else null
    // בלי IntrinsicSize.Min, מאותה סיבה כמו בשורת הקורס בציר: FlowRow נחתכת
    // תחת מדידה אינטרינזית. הכפתור בקצה ממילא בגובה קבוע.
    Row(
        Modifier.fillMaxWidth().card(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clickable { m.sheetId = c.id }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .background(c.color, RoundedCornerShape(999.dp)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(c.name, style = body(14.5, C.text, 600, 18.85))
                TagFlow {
                    TagNeutral("${c.crTxt} נ״ז")
                    TagNeutral("${c.hoursTxt} ${m.hUnit}/שב׳")
                    if (action == Action.PLACE) TagOutline("נפתח: ${c.offerTxt}", C.neutral500)
                    if (c.isElective) TagOutline("בחירה", C.neutral500)
                }
                val note = blocked ?: c.issueText.takeIf { c.hasIssue }
                if (note != null) {
                    Box(
                        Modifier
                            .background(C.accent100, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    ) {
                        Text(note, style = body(11.0, C.accent700, lineHeight = 15.4))
                    }
                }
            }
        }

        Column(
            Modifier
                .padding(6.dp)
                .width(88.dp)
                .height(58.dp)
                .background(C.accent100, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    if (action == Action.PLACE && blocked == null) m.autoOne(c.id)
                    else m.moveId = c.id
                }
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (action == Action.PLACE && blocked == null) {
                Text("שבץ ל", style = body(10.5, C.accent700, 600), maxLines = 1)
                Text(
                    Compute.recShortTxt(m, c.id),
                    style = body(10.5, C.accent700, 800),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            } else if (action == Action.PLACE) {
                Text(
                    "בחר\nסמסטר",
                    style = body(10.5, C.accent700, 700, 13.7),
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text("העבר", style = body(11.5, C.accent700, 700), maxLines = 1)
                Text(
                    "לסמסטר אחר",
                    style = body(9.5, C.accent700, 600),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
