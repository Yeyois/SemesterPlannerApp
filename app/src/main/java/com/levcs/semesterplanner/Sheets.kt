package com.levcs.semesterplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** מעטפת גיליון תחתון: רקע כהה, משטח מעוגל למעלה, גובה מרבי כאחוז מהמסך. */
@Composable
fun BottomSheet(
    onDismiss: () -> Unit,
    maxHeightFraction: Float = 0.88f,
    content: @Composable () -> Unit,
) {
    val screenH = LocalConfiguration.current.screenHeightDp
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(C.scrim)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onDismiss() },
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = (screenH * maxHeightFraction).dp)
                .background(C.ground, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {},
        ) { content() }
    }
}

@Composable
private fun CloseButton(glyph: String = "×", onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .card(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(glyph, style = body(18.0, C.neutral700)) }
}

// ── גיליון פרטי קורס ────────────────────────────────────────────────────────

@Composable
fun CourseSheet(m: PlannerModel) {
    val id = m.sheetId ?: return
    val c = Data.byId(id) ?: return
    val semKey = m.plan[id]
    val l = m.loadOf(c, semKey)
    val d = Data.diff(l.total)
    val semObj = Data.semByKey(semKey)
    val desc = m.descendants(id)
    val isDone = id in m.done
    val idx = Data.semIndex

    BottomSheet({ m.sheetId = null }) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(bottom = 26.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(C.ground)
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .background(d.third, RoundedCornerShape(999.dp)),
                )
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "${c.id} · ${if (c.mand) "חובה" else "בחירה"} · ${Data.catLabel(c.cat)}",
                        style = body(10.5, C.neutral600, 600),
                    )
                    Text(c.name, style = heading(18.0, lineHeight = 20.7))
                    Text(
                        if (isDone) "סומן כהושלם"
                        else semObj?.let { "מתוכנן ל${it.title}" } ?: "על המדף — לא מתוכנן",
                        style = body(12.5, C.accent700),
                    )
                }
                CloseButton { m.sheetId = null }
            }

            // ── השעות, משתי היחידות ──
            Column(
                Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("נ״ז", Data.n(c.cr), Modifier.weight(1f))
                    StatBox("עומס", d.second, Modifier.weight(1f), d.third)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("בכיתה · ${m.hUnit}", m.hTxt(l.classH), Modifier.weight(1f))
                    StatBox("למידה עצמית · ${m.hUnit}", m.hTxt(l.selfH), Modifier.weight(1f))
                }
                StatBox(
                    if (Data.isElul(semKey)) "סה״כ ${m.hUnit}/שבוע · אלול ×${Data.n(Hours.elulMul)}"
                    else "סה״כ ${m.hUnit}/שבוע",
                    m.hTxt(l.total),
                    Modifier.fillMaxWidth(),
                    d.third,
                )
                HoursEditor(m, c, semKey)
            }

            Column(
                Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // לקורס על המדף אין סמסטר להזיז ממנו, ו"דחה"/"הקדם" היו נוחתים
                // בקצוות הלוח עם תווית הפוכה. שם מוצגת פעולת השיבוץ במקומן.
                if (semKey == null) {
                    PillButton(
                        "שבץ ל" + Compute.recShortTxt(m, id),
                        { m.autoOne(id) },
                        minHeight = 46,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton(
                            "דחה סמסטר",
                            { m.shift(id, 1) },
                            Modifier.weight(1f),
                            minHeight = 46,
                        )
                        CardPillButton("הקדם", { m.shift(id, -1) }, Modifier.weight(1f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardPillButton(
                        if (isDone) "בטל \"הושלם\"" else "סמן כהושלם",
                        { m.toggleDone(id) },
                        Modifier.weight(1f),
                        weight = 400,
                    )
                    if (semKey != null) {
                        CardPillButton(
                            "החזר למדף",
                            { m.place(id, null); m.say("הוחזר למדף.", true) },
                            Modifier.weight(1f),
                            weight = 400,
                        )
                    }
                }
                CardPillButton("בחר סמסטר יעד…", { m.moveId = id; m.sheetId = null })
            }

            // קדם־דרישות
            Column(
                Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("קדם־דרישות", style = body(10.5, C.neutral600, 700))
                val pre = Data.PRE[id] ?: emptyList()
                if (pre.isEmpty()) {
                    Text("אין קדם־דרישות לקורס הזה.", style = body(13.0, C.neutral600))
                }
                pre.forEach { p ->
                    val ok = p in m.done
                    val pk = m.plan[p]
                    val before = pk != null && semKey != null &&
                        (idx[pk] ?: 0) < (idx[semKey] ?: 0)
                    val good = ok || before
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .background(
                                if (good) C.surface else C.accent100,
                                RoundedCornerShape(12.dp),
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { m.sheetId = p }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            Data.byId(p)?.name ?: p,
                            style = body(13.0, C.text),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (ok) "הושלם ✓" else if (before) "מתוכנן קודם ✓" else "לא מסודר ✕",
                            style = body(12.0, if (good) C.neutral700 else C.accent),
                        )
                    }
                }
            }

            // מה נחסם אם דוחים
            Column(
                Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "מה נחסם אם דוחים · ${desc.size}",
                    style = body(10.5, C.neutral600, 700),
                )
                Text(
                    when {
                        desc.isEmpty() ->
                            "קורס עלה — דחייה שלו לא חוסמת שום קורס אחר. זה המועמד הראשון לדחייה כשהסמסטר עמוס."
                        desc.size <= 2 ->
                            "דחייה דוחה איתו ${desc.size} קורסים בהמשך. אפשרי, אבל בדוק שאין פקק בסמסטר היעד."
                        else ->
                            "צוואר בקבוק: ${desc.size} קורסים תלויים בו. עדיף לדחות משהו אחר ולא אותו."
                    },
                    style = body(13.0, C.text, lineHeight = 20.15),
                )
                TagFlow {
                    desc.forEach { x ->
                        Box(
                            Modifier
                                .heightIn(min = 38.dp)
                                .card(RoundedCornerShape(999.dp))
                                .clickable { m.sheetId = x }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(Data.byId(x)?.name ?: x, style = body(12.0, C.text))
                        }
                    }
                }
            }

            Text(
                "נפתח ב: ${c.offer.joinToString(" / ")}" +
                    (if ("שנתי" in c.offer) " (קורס שנתי)" else "") + "\n" +
                    if (c.id == "120132")
                        "ההשלמה שלך: הקורס נפתח גם בסמסטר א׳ (מועד השלמה) וגם בסמסטר ב׳."
                    else "הערכת שעות הלמידה כוללת שיעור, תרגול והכנה מוערכת.",
                style = body(12.0, C.neutral600, lineHeight = 19.8),
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = C.text,
) {
    Column(
        modifier.card().padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, style = body(10.0, C.neutral600, 600))
        Text(value, style = heading(18.0, valueColor))
    }
}

/**
 * עורך ש״ש — הנקודה שבה הקטלוג מפסיק להיות אמת מוחלטת.
 *
 * מערכת השעות בפועל שונה בין מסלולים ובין שנים, וחלק משורות הקטלוג שוחזרו
 * ולא נקראו ממערכת אמיתית. במקום להציג מספר שגוי בביטחון, אפשר לתקן אותו
 * כאן מול המערכת — והתיקון נשמר ומחלחל לכל חישובי העומס.
 */
@Composable
private fun HoursEditor(m: PlannerModel, c: Course, semKey: String?) {
    val ac = m.acOf(c)
    val edited = m.isAcEdited(c.id)
    Column(
        Modifier.fillMaxWidth().card().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ש״ש בכיתה · לפי השנתון",
                style = body(10.0, C.neutral600, 600),
                modifier = Modifier.weight(1f),
            )
            if (edited) Text("תוקן על ידך", style = body(10.0, C.accent700, 600))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StepButton("−") { m.setAc(c.id, ac - 0.5) }
            Text(Data.n(ac), style = heading(18.0), modifier = Modifier.width(38.dp))
            StepButton("+") { m.setAc(c.id, ac + 0.5) }
            Column(Modifier.weight(1f)) {
                // אותה שורה שהסטודנט רואה בלוח: 2 ש״ש הן מפגש של 1:40, לא 1:30.
                Text(
                    "= ${Data.n(Hours.toReal(ac))} ש׳ שעון בשבוע",
                    style = body(12.0, C.neutral800, 600),
                )
                Text(
                    if (ac <= 0) "אין מפגשים"
                    else "מפגש של ${Data.n(Hours.BLOCK_AC)} ש״ש נמשך " +
                        "${Hours.blockLabel(Hours.BLOCK_AC.toInt())} שעה",
                    style = body(10.5, C.neutral600),
                )
            }
        }
        if (edited) {
            Text(
                "בקטלוג: ${Data.n(c.ac)} ש״ש · לחץ כדי להחזיר",
                style = body(11.0, C.accent700),
                modifier = Modifier.clickable { m.resetAc(c.id) },
            )
        } else {
            Text(
                "לא תואם למערכת השעות שלך? תקן כאן, וכל החישובים יתעדכנו.",
                style = body(11.0, C.neutral600, lineHeight = 16.5),
            )
        }
    }
}

@Composable
private fun StepButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .card(background = C.neutral100)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(glyph, style = heading(16.0, C.neutral800)) }
}

// ── גיליון העברה לסמסטר ─────────────────────────────────────────────────────

@Composable
fun MoveSheet(m: PlannerModel) {
    val id = m.moveId ?: return
    val c = Data.byId(id) ?: return
    val semKey = m.plan[id]
    val budget = m.budgetHours()

    BottomSheet({ m.moveId = null }, 0.80f) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    c.name,
                    style = heading(16.0, lineHeight = 20.8),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(32.dp).clickable { m.moveId = null },
                    contentAlignment = Alignment.Center,
                ) { Text("×", style = body(18.0, C.neutral700)) }
            }
            Text("העבר לסמסטר", style = body(10.5, C.neutral600, 700))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Data.SEMS.filter {
                    (it.type == "אלול" || Data.fits(c, it.type)) && it.key != semKey
                }.forEach { s ->
                    val after = m.semLoad(s.key).total + m.loadOf(c, s.key).total
                    val room = budget - after
                    // אותו סף כמו בכרטיס הסמסטר: חריגה זעירה לא נצבעת באדום
                    // בגיליון אחד ומתעלמים ממנה בשני.
                    val tight = after > budget * Data.OVER_BUDGET
                    val warnElul = s.type == "אלול" && "אלול" !in c.offer
                    val flag = tight || warnElul
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .card(background = if (flag) C.accent100 else C.surface)
                            .border(
                                1.dp,
                                if (flag) C.accent300 else C.neutral500,
                                CardShape,
                            )
                            .clickable {
                                m.place(id, s.key)
                                m.moveId = null
                                m.say("הועבר ל${s.title}", true)
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(s.title, style = body(13.5, C.text, 600))
                        Text(
                            when {
                                warnElul -> "⚠ ודא שנפתח באלול"
                                room < 0 -> "חריגה של ${m.hTxt(-room)} ${m.hUnit}"
                                else -> "נשאר ${m.hTxt(room)} ${m.hUnit}"
                            },
                            style = body(11.0, if (flag) C.accent700 else C.neutral700),
                        )
                    }
                }
            }

            OutlinePillButton(
                "החזר למדף",
                {
                    m.place(id, null)
                    m.moveId = null
                    m.say("הוחזר למדף.", true)
                },
                minHeight = 46,
                fontSize = 13.0,
            )
        }
    }
}

// ── גיליון שרשרת התלויות ────────────────────────────────────────────────────

@Composable
fun DepSheet(m: PlannerModel) {
    val id = m.depSheetId ?: return
    val c = Data.byId(id) ?: return
    val idx = Data.semIndex
    val info = m.depInfo(c)
    val semKey = m.plan[id]
    val semObj = Data.semByKey(semKey)
    val isDone = id in m.done

    BottomSheet({ m.depSheetId = null }) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 22.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "שרשרת תלויות",
                    style = body(10.5, C.neutral600, 600),
                    modifier = Modifier.weight(1f),
                )
                CloseButton("✕") { m.depSheetId = null }
            }

            // דרישות קדם
            Column(
                Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectLabel("דרישות קדם (מהעבר)", Modifier.padding(bottom = 4.dp))
                val pre = Data.PRE[id] ?: emptyList()
                if (pre.isEmpty()) {
                    Box(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text("אין דרישות קדם לקורס הזה.", style = body(12.5, C.neutral600))
                    }
                }
                pre.forEach { p ->
                    val pDone = p in m.done
                    val pk = m.plan[p]
                    val status = when {
                        pDone -> "done"
                        pk == null -> "blocked"
                        semKey != null && (idx[pk] ?: 0) > (idx[semKey] ?: 0) -> "blocked"
                        semKey != null && idx[pk] == idx[semKey] -> "warn"
                        else -> "ok"
                    }
                    DepRow(
                        name = Data.byId(p)?.name ?: p,
                        semLbl = if (pDone) "הושלם"
                        else Data.semByKey(pk)?.title ?: "לא משובץ",
                        statusLabel = m.depStatusLabel(status),
                        statusColor = m.depStatusColor(status),
                    ) { m.depSheetId = p }
                }
            }

            Box(Modifier.fillMaxWidth().padding(bottom = 6.dp), Alignment.Center) {
                ArrowDownIcon(C.neutral500)
            }

            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .card()
                    .border(2.dp, C.text, CardShape)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(10.dp).background(
                        if (isDone) C.depDone else m.depStatusColor(info.status),
                        RoundedCornerShape(999.dp),
                    ),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(c.id, style = body(10.0, C.neutral600))
                    Text(c.name, style = heading(16.0, lineHeight = 20.0))
                    Text(
                        "${Data.n(c.cr)} נ״ז · " +
                            (if (isDone) "הושלם" else semObj?.title ?: "לא משובץ עדיין"),
                        style = body(12.0, C.neutral600),
                    )
                }
                Text(
                    if (isDone) "הושלם" else m.depStatusLabel(info.status),
                    style = body(
                        12.0,
                        if (isDone) C.depDone else m.depStatusColor(info.status),
                        700,
                    ),
                )
            }

            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), Alignment.Center) {
                ArrowDownIcon(C.neutral500)
            }

            // חוסם את
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectLabel("חוסם את (לעתיד)", Modifier.padding(bottom = 4.dp))
                val future = Data.courses.filter { id in (Data.PRE[it.id] ?: emptyList()) }
                if (future.isEmpty()) {
                    Box(Modifier.fillMaxWidth().card().padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text("שום קורס לא תלוי בקורס הזה.", style = body(12.5, C.neutral600))
                    }
                }
                future.forEach { fc ->
                    val dDone = fc.id in m.done
                    val dInfo = m.depInfo(fc)
                    val st = if (dDone) "done" else dInfo.status
                    DepRow(
                        name = fc.name,
                        semLbl = if (dDone) "הושלם"
                        else Data.semByKey(m.plan[fc.id])?.title ?: "לא משובץ עדיין",
                        statusLabel = m.depStatusLabel(st),
                        statusColor = m.depStatusColor(st),
                    ) { m.depSheetId = fc.id }
                }
            }
        }
    }
}

@Composable
private fun DepRow(
    name: String,
    semLbl: String,
    statusLabel: String,
    statusColor: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .card()
            .clickable { onClick() }
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = body(13.0, C.text, 600))
                Text(semLbl, style = body(11.0, C.neutral600))
            }
            Text(statusLabel, style = body(11.5, statusColor, 700))
        }
        // הפס בקצה הסיום (border-inline-end)
        Box(Modifier.width(4.dp).fillMaxHeight().background(statusColor))
    }
}
