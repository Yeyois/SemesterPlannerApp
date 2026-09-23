package com.levcs.semesterplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.abs
/**
 * מסלול התואר — ציר אחד עם כל שנים־עשר הסמסטרים.
 *
 * קודם עמדו כאן שלוש שכבות ניווט זו על זו: בורר תצוגה (סמסטר/ציר/תלויות),
 * שורת שנים, ודפדפן סמסטרים עם חצים ונקודות — כשליש מהמסך לפני התוכן, וכולן
 * כדי לענות על שאלה אחת: "איפה הסמסטר שאני רוצה". הציר עונה עליה בעצמו:
 * הכול גלוי, "עכשיו" מסומן, ולחיצה פותחת סמסטר במקום.
 */
@Composable
fun TabPlan(m: PlannerModel, d: Derived) {
    if (m.view == "deps") {
        DepsScreen(m, d)
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Data.YEARS.forEach { y ->
            val yearTxt = "שנה $y׳"
            val sems = d.semData.filter { it.yearTxt == yearTxt }
            if (sems.isEmpty()) return@forEach
            Column {
                YearHeader(yearTxt, sems)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sems.forEach { s -> SemBlock(m, d, s) }
                }
            }
        }

        // מפת התלויות ירדה מבורר התצוגה לשורה אחת בתחתית: היא כלי לרגע שבו
        // משהו לא מסתדר, לא אחת משלוש הדרכים הרגילות להסתכל על התכנון.
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .card()
                .clickable { m.view = "deps" }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("מפת קדם־דרישות", style = body(13.5, C.text, 600))
                Text("מה חוסם מה לאורך התואר", style = body(11.0, C.neutral600))
            }
            Text("›", style = body(16.0, C.neutral500))
        }
    }
}

/** כותרת שנה עם סיכום קצר — כמה נ״ז ומה הסטטוס שלה ביחס להיום. */
@Composable
private fun YearHeader(yearTxt: String, sems: List<SemVM>) {
    val allPast = sems.all { it.isPast }
    val hasNow = sems.any { it.isCurrent }
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            yearTxt,
            style = body(11.5, if (hasNow) C.accent700 else C.neutral700, 700),
        )
        Box(Modifier.weight(1f))
        Text(
            when {
                hasNow -> "השנה"
                allPast -> "מאחוריך"
                else -> "לפניך"
            },
            style = body(10.5, C.neutral500),
        )
    }
}

/** סמסטר בציר: שורה מקופלת, ובלחיצה נפתח מתחתיה כל מה שאפשר לערוך בו. */
@Composable
private fun SemBlock(m: PlannerModel, d: Derived, s: SemVM) {
    val open = m.planOpenKey == s.key
    Column(
        Modifier.fillMaxWidth().card(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    m.planOpenKey = if (open) null else s.key
                    m.semIdx = Data.semIndex[s.key] ?: m.semIdx
                }
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // הנקודה היא הציר עצמו: מלאה בהווה, חלולה בעתיד, מסומנת ✓ בעבר.
            Box(Modifier.padding(top = 3.dp).size(12.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(if (s.isCurrent) 12.dp else 9.dp)
                        .background(
                            when {
                                s.isCurrent -> C.accent
                                s.isPast -> C.neutral400
                                else -> C.neutral300
                            },
                            RoundedCornerShape(999.dp),
                        ),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        s.lbl,
                        style = heading(15.0, if (s.isPast) C.neutral600 else C.text),
                    )
                    if (s.isCurrent) Tag("עכשיו", C.accent, C.white)
                    Text(s.countTxt, style = body(11.0, C.neutral600))
                    Box(Modifier.weight(1f))
                    // סמסטר שנגמר לא מתואר בשעות: "0 ש׳" ופס ריק תיארו סמסטר
                    // מלא שכולו מאחוריך כאילו לא קרה בו כלום.
                    if (s.allDone) TagNeutral("✓ הושלם")
                    else if (!s.empty) {
                        Text(
                            "${s.hoursTxt} ${d.unit}",
                            style = body(12.5, if (s.isPast) C.neutral500 else s.barColor, 700),
                            maxLines = 1,
                        )
                    }
                }
                if (!s.empty && !s.allDone) {
                    ProgressBar(
                        s.pct / 100f,
                        if (s.isPast) C.neutral400 else s.barColor,
                        C.neutral200,
                        6,
                    )
                }
                Text(
                    s.names,
                    style = body(11.0, C.neutral600, lineHeight = 16.5),
                )
                if (s.warns.isNotEmpty() && !open) {
                    Text(
                        "${s.warns.size} " + if (s.warns.size == 1) "הערה" else "הערות",
                        style = body(10.5, C.accent700, 700),
                    )
                }
            }
            Text(
                if (open) "−" else "+",
                style = heading(16.0, C.neutral500),
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (open) SemDetail(m, d, s)
    }
}

/** הפנים הפתוחות של סמסטר: הפירוק, ההערות, הקורסים, והוספה. */
@Composable
private fun SemDetail(m: PlannerModel, d: Derived, s: SemVM) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(C.bg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!s.empty) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "בכיתה ${s.classTxt} ${d.unit} · למידה עצמית ${s.selfTxt} ${d.unit} · " +
                        "${s.acTxt} ש״ש בשנתון",
                    style = body(11.0, C.neutral600, lineHeight = 16.5),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${s.crTxt} נ״ז · ${s.statusText}", style = body(11.0, C.neutral600))
                    Text(s.daysText, style = body(11.0, C.neutral600))
                }
            }
        }

        if (s.isElul || s.isExtra || s.allDone) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (s.isElul) TagOutline(s.elulTag)
                if (s.isExtra) Tag("שנה ד׳ · תוספת", C.accent100, C.accent800)
                if (s.allDone) TagOutline("✓ הושלם")
            }
        }

        s.warns.forEach { w ->
            StripeCard(w.color) {
                Text(w.text, style = body(12.5, C.text, lineHeight = 18.75))
                if (w.fixId.isNotEmpty()) {
                    Text(w.fixWhy, style = body(11.5, C.neutral700, lineHeight = 16.7))
                    Box(
                        Modifier
                            .height(38.dp)
                            .background(C.accent, RoundedCornerShape(999.dp))
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { m.moveId = w.fixId }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(w.fixLbl, style = body(12.5, C.white, 600))
                    }
                }
            }
        }

        if (s.empty) {
            Text(
                "סמסטר פנוי. הוסף קורס — רק קורסים שנפתחים בסמסטר הזה יוצעו.",
                style = body(12.5, C.neutral600, lineHeight = 20.0),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.courses.forEach { c -> PlanCourseRow(m, c) }
            }
        }

        PillButton(
            "+ הוסף קורס ל${s.lbl}",
            {
                m.semIdx = Data.semIndex[s.key] ?: m.semIdx
                m.pickerOpen = true
            },
            minHeight = 46,
            fontSize = 13.0,
        )
    }
}

// ── מפת התלויות כמסך משלה ───────────────────────────────────────────────────

@Composable
private fun DepsScreen(m: PlannerModel, d: Derived) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .card()
                .clickable { m.view = "sem" }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("‹", style = body(16.0, C.text))
            Text("חזרה למסלול התואר", style = body(13.0, C.text, 600))
        }
        DepsView(m, d)
    }
}
/** שורת קורס: פתיחה, העברה לסמסטר, וסימון הושלם. */
@Composable
private fun PlanCourseRow(m: PlannerModel, c: CardVM) {
    // בלי IntrinsicSize.Min: FlowRow לא תומך במדידה אינטרינזית, ותחתיה השורה
    // השנייה של התגיות נחתכה בשקט — תגית העומס ("כבד"/"קשה מאוד") פשוט לא
    // הופיעה. הכפתורים מקבלים גובה קבוע במקום להימתח לגובה השורה.
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
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    c.name,
                    style = body(14.5, C.text, 600, 18.85),
                    textDecoration = if (c.isDone)
                        androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                )
                // ש״ש ירדה מהשורה: היא כבר בסיכום הסמסטר שמעל ובגיליון הקורס,
                // וששה תגים על שורה אחת הפכו כל קורס לגוש טקסט אחד.
                TagFlow {
                    TagNeutral("${c.crTxt} נ״ז")
                    TagNeutral("${c.hoursTxt} ${m.hUnit}/שב׳")
                    TagOutline(c.diffLabel, c.color)
                    if (c.isElective) TagOutline("בחירה", C.neutral500)
                    if (c.isDone) TagOutline("✓ הושלם", C.neutral500)
                }
                if (c.hasIssue) {
                    Box(
                        Modifier
                            .background(C.accent100, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    ) {
                        Text(c.issueText, style = body(11.5, C.accent700, lineHeight = 16.1))
                    }
                }
            }
        }

        IconBox(
            onClick = { m.moveId = c.id },
            modifier = Modifier.size(44.dp),
        ) { CalendarCheckIcon(C.neutral700) }

        Box(
            Modifier
                .padding(6.dp)
                .size(44.dp, 44.dp)
                .background(
                    if (c.isDone) C.accent else Color.Transparent,
                    RoundedCornerShape(12.dp),
                )
                .clip(RoundedCornerShape(12.dp))
                .clickable { m.toggleDone(c.id) },
            contentAlignment = Alignment.Center,
        ) { CheckIcon(if (c.isDone) C.white else C.neutral600) }
    }
}

// ── מפת תלויות ──────────────────────────────────────────────────────────────

@Composable
private fun DepsView(m: PlannerModel, d: Derived) {
    val depsSemKey = m.depsSemKey ?: d.sem.key
    val filterIssues = m.depsFilter == "issues"

    var warnCount = 0
    var blockedCount = 0
    Data.courses.forEach { c ->
        if (c.id in m.done || m.plan[c.id] == null) return@forEach
        when (m.depInfo(c).status) {
            "warn" -> warnCount++
            "blocked" -> blockedCount++
        }
    }
    val parts = buildList {
        if (blockedCount > 0) add("$blockedCount " + if (blockedCount == 1) "חסימה" else "חסימות")
        if (warnCount > 0) add("$warnCount " + if (warnCount == 1) "אזהרה" else "אזהרות")
    }
    val summaryText = if (parts.isNotEmpty()) parts.joinToString(" · ") + " בדרישות קדם"
    else "כל דרישות הקדם תקינות"
    val summaryColor = when {
        blockedCount > 0 -> C.depBlocked
        warnCount > 0 -> C.depWarn
        else -> C.depOk
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth().card().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(10.dp).background(summaryColor, RoundedCornerShape(999.dp)))
            Text(summaryText, style = body(13.0, C.text, 600, 18.2))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f)) {
                SegmentedRow(
                    listOf("רשימה", "גרף"),
                    if (m.depsSub == "graph") 1 else 0,
                    itemHeight = 38,
                    radius = 10,
                ) { m.depsSub = if (it == 0) "list" else "graph" }
            }
            Box(
                Modifier
                    .height(46.dp)
                    .card()
                    .clickable { m.depsFilter = if (filterIssues) "all" else "issues" }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (filterIssues) "חריגים בלבד" else "כל הקורסים",
                    style = body(12.0, if (filterIssues) C.accent else C.neutral700, 600),
                    maxLines = 1,
                )
            }
        }

        if (m.depsSub == "graph") {
            DepsGraph(m)
        } else {
            val cur = Data.semByKey(depsSemKey) ?: Data.SEMS[0]
            val dsIdx = Data.SEMS.indexOfFirst { it.key == depsSemKey }.coerceAtLeast(0)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                YearChips(cur.yearTxt) { m.depsSemKey = it }
                Pager(
                    yearTxt = cur.yearTxt,
                    lbl = cur.yearTxt.replace("שנה ", "") + " " +
                        if (cur.type == "אלול") "אלול" else "${cur.type}׳",
                    index = dsIdx,
                    arrowSize = 22.0,
                    onPrev = { m.depsStep(-1) },
                    onNext = { m.depsStep(1) },
                    onSwipe = { dir -> m.depsStep(dir) },
                )

                var list = Data.courses.filter { m.plan[it.id] == depsSemKey }.map { c ->
                    val r = m.depInfo(c)
                    Triple(c, r, r.status == "warn" || r.status == "blocked")
                }
                if (filterIssues) list = list.filter { it.third }
                list = list.sortedByDescending { it.second.status == "blocked" }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.forEach { (c, info, _) ->
                        StripeCard(
                            m.depStatusColor(info.status),
                            onClick = { m.depSheetId = c.id },
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    c.name,
                                    style = body(14.0, C.text, 600, 18.2),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    m.depStatusLabel(info.status),
                                    style = body(11.5, m.depStatusColor(info.status), 700),
                                    maxLines = 1,
                                )
                            }
                            TagFlow { TagNeutral("${Data.n(c.cr)} נ״ז") }
                            Text(info.text, style = body(12.0, C.neutral600, lineHeight = 16.8))
                        }
                    }
                    if (list.isEmpty()) {
                        Box(Modifier.fillMaxWidth().card().padding(18.dp)) {
                            Text(
                                if (filterIssues)
                                    "אין חריגים בסמסטר זה — כל דרישות הקדם תקינות."
                                else "אין קורסים משובצים לסמסטר זה.",
                                style = body(13.0, C.neutral600, lineHeight = 20.8),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** גרף התלויות — עמודות לפי עומק שרשרת הקדם, מיקוד בלחיצה מצייר את הקשתות. */
@Composable
private fun DepsGraph(m: PlannerModel) {
    val filterIssues = m.depsFilter == "issues"
    val focusId = m.graphFocus
    val related: Set<String>? = if (focusId != null) {
        buildSet {
            add(focusId)
            addAll(Data.PRE[focusId] ?: emptyList())
            Data.courses.forEach { c ->
                if (focusId in (Data.PRE[c.id] ?: emptyList())) add(c.id)
            }
        }
    } else null

    val levels = Data.courses.associate { it.id to m.courseLevel(it.id) }
    val maxLvl = levels.values.maxOrNull() ?: 0

    // מלבני הצמתים במרחב הגרף — נאספים בזמן הפריסה כדי לצייר את הקשתות.
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    var host by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Row(
        Modifier
            .fillMaxWidth()
            .ownsHorizontalDrag()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
    ) {
        Box {
            Row(
                Modifier.onGloballyPositioned { host = it },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (lv in 0..maxLvl) {
                    Column(
                        Modifier.width(148.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectLabel(
                            if (lv == 0) "רמה 1 (קורסי יסוד)" else "רמה ${lv + 1}",
                            Modifier.padding(horizontal = 2.dp),
                        )
                        Data.courses.filter { levels[it.id] == lv }.forEach { c ->
                            val info = m.depInfo(c)
                            val isFocus = focusId == c.id
                            val isRelated = related?.contains(c.id) ?: true
                            val dim = if (related != null) !isRelated
                            else filterIssues && info.status != "warn" && info.status != "blocked"
                            val emph = related != null && isRelated && !isFocus
                            val bg = when {
                                isFocus -> C.accent100
                                emph -> C.neutral200
                                else -> C.surface
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { co ->
                                        host?.let { h -> bounds[c.id] = h.localBoundingBoxOf(co) }
                                    }
                                    .card(background = bg)
                                    .then(
                                        if (isFocus) Modifier.border(2.dp, C.text, CardShape)
                                        else Modifier,
                                    )
                                    .clickable {
                                        m.graphFocus = if (m.graphFocus == c.id) null else c.id
                                    }
                                    .height(IntrinsicSize.Min)
                                    .alpha(if (dim) 0.28f else 1f),
                            ) {
                                Box(
                                    Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(m.depStatusColor(info.status)),
                                )
                                Column(
                                    Modifier.weight(1f).padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(c.name, style = body(12.0, C.text, 600, 15.6))
                                    Text("${Data.n(c.cr)} נ״ז", style = body(10.0, C.neutral600))
                                }
                            }
                        }
                    }
                }
            }

            // הקשתות מצוירות רק סביב הצומת שבמוקד, כמו ב-SVG של המקור.
            if (focusId != null) {
                Canvas(Modifier.matchParentSize()) {
                    Data.courses.forEach { c ->
                        (Data.PRE[c.id] ?: emptyList()).forEach { p ->
                            if (p != focusId && c.id != focusId) return@forEach
                            val ra = bounds[p] ?: return@forEach
                            val rb = bounds[c.id] ?: return@forEach
                            val x1 = ra.left
                            val y1 = ra.top + ra.height / 2
                            val x2 = rb.right
                            val y2 = rb.top + rb.height / 2
                            val pDone = p in m.done
                            val pk = m.plan[p]
                            val sk = m.plan[c.id]
                            val idx = Data.semIndex
                            val broken = !pDone && (
                                pk == null ||
                                    (sk != null && (idx[pk] ?: 0) >= (idx[sk] ?: 0))
                                )
                            val stroke = if (broken) C.accent else C.neutral500
                            drawPath(
                                orthoPath(x1, y1, x2, y2, 10f),
                                color = stroke,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                            )
                            listOf(Offset(x1, y1), Offset(x2, y2)).forEach { o ->
                                drawCircle(C.surface, 3.5.dp.toPx(), o)
                                drawCircle(
                                    stroke, 3.5.dp.toPx(), o,
                                    style = Stroke(width = 1.5.dp.toPx()),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** נתיב אורתוגונלי עם פינות מעוגלות — תרגום של orthoPath מהמקור. */
private fun orthoPath(x1: Float, y1: Float, x2: Float, y2: Float, r: Float): Path {
    val path = Path()
    path.moveTo(x1, y1)
    if (y1 == y2) {
        path.lineTo(x2, y2)
        return path
    }
    val midX = (x1 + x2) / 2
    val dx = if (x2 > x1) 1 else -1
    val dy = if (y2 > y1) 1 else -1
    val rr = minOf(r, abs(midX - x1), abs(midX - x2), abs(y2 - y1) / 2)
    path.lineTo(midX - dx * rr, y1)
    path.quadraticBezierTo(midX, y1, midX, y1 + dy * rr)
    path.lineTo(midX, y2 - dy * rr)
    path.quadraticBezierTo(midX, y2, midX + dx * rr, y2)
    path.lineTo(x2, y2)
    return path
}

// ── רכיבי ניווט משותפים ─────────────────────────────────────────────────────

@Composable
fun YearChips(activeYearTxt: String, onJump: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Data.YEARS.forEach { y ->
            val yearTxt = "שנה $y׳"
            val first = Data.SEMS.firstOrNull { it.yearTxt == yearTxt } ?: return@forEach
            val active = activeYearTxt == yearTxt
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .card(RoundedCornerShape(999.dp), if (active) C.text else C.surface)
                    .clickable { onJump(first.key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    yearTxt,
                    style = body(12.5, if (active) C.bg else C.neutral800, 600),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** דפדוף סמסטרים: חצים, כותרת ונקודות. הכרטיס נגרר, עף החוצה ונכנס מהצד השני. */
@Composable
fun Pager(
    yearTxt: String,
    lbl: String,
    index: Int,
    arrowSize: Double,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSwipe: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val flyPx = with(density) { 480.dp.toPx() }
    val enterPx = with(density) { 70.dp.toPx() }
    val thresholdPx = with(density) { 70.dp.toPx() }
    // גרירה סינכרונית; האנימציה (עף החוצה ונכנס) רצה רק בשחרור
    var dragX by remember { mutableFloatStateOf(0f) }
    var animX by remember { mutableStateOf<Float?>(null) }
    var animAlpha by remember { mutableStateOf<Float?>(null) }
    var flying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val shownX = animX ?: dragX
    val shownAlpha = animAlpha ?: max(0.5f, 1f - abs(shownX) / flyPx)

    Row(
        Modifier
            .fillMaxWidth()
            .ownsHorizontalDrag()
            .graphicsLayer {
                translationX = shownX
                rotationZ = shownX / 18.dp.toPx()
                alpha = shownAlpha
            }
            .card()
            .padding(4.dp)
            .pointerInput(index) {
                detectHorizontalDragGestures(
                    onDragStart = { if (!flying) dragX = 0f },
                    onDragEnd = {
                        if (flying) return@detectHorizontalDragGestures
                        val d = dragX
                        if (abs(d) > thresholdPx) {
                            val dir = if (d > 0) 1 else -1
                            flying = true
                            scope.launch {
                                launch {
                                    animate(1f, 0f, animationSpec = tween(260)) { v, _ ->
                                        animAlpha = v
                                    }
                                }
                                animate(d, dir * flyPx, animationSpec = tween(260)) { v, _ ->
                                    animX = v
                                }
                                onSwipe(dir)
                                animX = -dir * enterPx
                                dragX = 0f
                                launch {
                                    animate(0f, 1f, animationSpec = tween(280)) { v, _ ->
                                        animAlpha = v
                                    }
                                }
                                animate(-dir * enterPx, 0f, animationSpec = tween(320)) { v, _ ->
                                    animX = v
                                }
                                animX = null
                                animAlpha = null
                                flying = false
                            }
                        } else {
                            scope.launch {
                                animate(d, 0f, animationSpec = tween(280)) { v, _ -> animX = v }
                                dragX = 0f
                                animX = null
                            }
                        }
                    },
                    onDragCancel = { dragX = 0f },
                ) { _, amount ->
                    if (!flying) dragX += amount
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ב-RTL הכפתור הראשון יושב בימין; התו ‹ נהפך אוטומטית בכיוון הזה
        ArrowButton("‹", arrowSize, if (index == 0) C.neutral400 else C.text, onPrev)
        Column(
            Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(yearTxt, style = body(10.5, C.neutral600, 600))
            Text(lbl, style = heading(20.0, lineHeight = 22.0), textAlign = TextAlign.Center)
            Row(
                Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Data.SEMS.indices.forEach { i ->
                    Box(
                        Modifier
                            .width(if (i == index) 18.dp else 6.dp)
                            .height(6.dp)
                            .background(
                                if (i == index) C.accent else C.neutral400,
                                RoundedCornerShape(999.dp),
                            ),
                    )
                }
            }
        }
        ArrowButton(
            "›",
            arrowSize,
            if (index == Data.SEMS.size - 1) C.neutral400 else C.text,
            onNext,
        )
    }
}

@Composable
private fun ArrowButton(glyph: String, size: Double, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .width(46.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = body(size, color), textAlign = TextAlign.Center)
    }
}
