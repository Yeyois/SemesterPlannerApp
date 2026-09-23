package com.levcs.semesterplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun TabSettings(m: PlannerModel, d: Derived, onExport: (String) -> Unit) {
    val su = m.setup
    // עוגן התקציב: הסמסטר הכבד ביותר במסלול התקני. נגזר מ-REC ולא מרשימה
    // ידנית, אחרת אינפי 2 נושר ממנו והעוגן יוצא נמוך מהעומס האמיתי.
    // מכבד תיקוני ש״ש של הסטודנט, כדי שהעוגן יתאר את המסלול שלו בפועל.
    val refLoad = Data.courses
        .filter { it.mand && Data.REC[it.id]?.let { r -> r.first == "א" && r.second == "ב" } == true }
        .fold(Load.ZERO) { acc, c -> acc + Data.load(c, m.acOf(c)) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectLabel("איפה אני")

        CurrentSemCard(m)

        // שנת התחלה
        Column(
            Modifier.fillMaxWidth().card().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("שנה שאתה מתחיל בה", style = body(10.5, C.neutral600, 700))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Data.YEARS.forEach { y ->
                    val on = su.startYear == y
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                if (on) C.text else C.neutral200,
                                RoundedCornerShape(12.dp),
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (y == su.startYear) return@clickable
                                m.confirmModal = ConfirmModal(
                                    "לבנות מחדש את התכנון?",
                                    "מעבר לשנה $y׳ בונה את המסלול התקני מחדש ומוחק כל שינוי " +
                                        "שעשית בתכנון. אי אפשר לבטל את זה.",
                                ) {
                                    m.applySetup(su.copy(startYear = y), true)
                                    m.confirmModal = null
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "שנה $y׳",
                            style = body(13.0, if (on) C.bg else C.neutral800, 600),
                        )
                    }
                }
            }
            Text(
                "שינוי השנה בונה מחדש את המסלול התקני ומאפס שינויים שעשית.",
                style = body(11.5, C.accent700, lineHeight = 17.25),
            )
        }

        SectLabel("השבוע שלי")

        // אחוז משרה
        Column(
            Modifier.fillMaxWidth().card().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("אחוז משרה", style = body(10.5, C.neutral600, 700))
                Box(Modifier.weight(1f))
                Text(
                    if (su.jobPercent == 0) "לא עובד"
                    else "${su.jobPercent}% · ${Data.n(m.jobHours())} שעות עבודה",
                    style = body(13.0, C.neutral800),
                )
            }
            AccentSlider(su.jobPercent.toFloat(), 0f..100f, 19) {
                m.applySetup(su.copy(jobPercent = it.roundToInt()), false)
            }
            Text(
                "← ${Data.n(m.cap() + m.jobHours())} ש׳ בשבוע בסך הכול (לימוד + עבודה) · " +
                    "נשארו ${Data.n(m.freeWeekHours() - m.cap())} מתוך ${Data.n(Data.WEEK_HOURS)}",
                style = body(12.0, if (m.overcommitted()) C.accent else C.accent700, lineHeight = 18.0),
            )
        }

        // שעות לימוד
        Column(
            Modifier.fillMaxWidth().card().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("שעות פנויות ללימוד בשבוע", style = body(10.5, C.neutral600, 700))
                Box(Modifier.weight(1f))
                Text("${Data.n(m.cap())} ש׳", style = body(13.0, C.neutral800))
            }
            // עד 70: הסמסטר הכבד ביותר במסלול (שנה א׳ ב׳) עולה 44 ש׳, ואלול
            // של שנה ב׳ 42 — התקרה צריכה להשאיר מרווח מעליהם ולא הרבה יותר.
            AccentSlider(
                m.cap().toFloat().coerceIn(Data.STUDY_MIN.toFloat(), Data.STUDY_MAX.toFloat()),
                Data.STUDY_MIN.toFloat()..Data.STUDY_MAX.toFloat(),
                (Data.STUDY_MAX - Data.STUDY_MIN).toInt() - 1,
                track = if (m.overcommitted()) C.accent else C.text,
            ) {
                m.applySetup(su.copy(capacity = it.roundToInt().toDouble()), false)
            }
            if (m.overcommitted()) {
                Text(
                    "יותר ממה שנשאר בשבוע אחרי ${Data.n(m.jobHours())} שעות עבודה " +
                        "(${Data.n(m.freeWeekHours())} ש׳).",
                    style = body(11.5, C.accent, lineHeight = 17.25),
                )
            }
        }

        // ימים פנויים
        Column(
            Modifier.fillMaxWidth().card().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("ימים פנויים שאתה רוצה בשבוע", style = body(10.5, C.neutral600, 700))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 1, 2, 3).forEach { dd ->
                    val on = su.freeDays == dd
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                if (on) C.text else C.neutral200,
                                RoundedCornerShape(12.dp),
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { m.applySetup(su.copy(freeDays = dd), false) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            Data.daysTxt(dd),
                            style = body(13.0, if (on) C.bg else C.neutral800, 600),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        SectLabel("תצוגה")

        // ── יחידת התצוגה ──
        Column(
            Modifier.fillMaxWidth().card().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("איך להציג שעות", style = body(10.5, C.neutral600, 700))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(false to "שעות שעון", true to "שעות אקדמיות").forEach { (academic, label) ->
                    val on = su.academicUnit == academic
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(if (on) C.text else C.neutral200, RoundedCornerShape(12.dp))
                            .clickable { m.applySetup(su.copy(academicUnit = academic), false) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = body(13.0, if (on) C.white else C.neutral800, 600),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Text(
                if (su.academicUnit)
                    "כל שעה מוצגת כיחידות של ${Data.n(Hours.AC_MIN)} דקות — היחידה של " +
                        "השנתון. התקציב שלך, ${Data.n(m.cap())} שעות שעון, הוא " +
                        "${Data.n(Hours.toUnit(m.cap(), true))} ש״ש."
                else "כל שעה מוצגת כשעת שעון של 60 דקות. ש״ש בכיתה מוצגות תמיד " +
                    "כפי שהן בשנתון, לצד הזמן שהן תופסות בפועל.",
                style = body(11.5, C.neutral600, lineHeight = 17.25),
            )
        }

        SwitchRow("הצג קורסי בחירה", su.showElectives) {
            m.applySetup(su.copy(showElectives = !su.showElectives), false)
        }

        // ערכת נושא
        Column(
            Modifier.fillMaxWidth().card().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("ערכת נושא", style = body(10.5, C.neutral600, 700))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("auto" to "לפי המכשיר", "light" to "בהיר", "dark" to "כהה")
                    .forEach { (mode, lbl) ->
                        val on = m.themeMode == mode
                        Box(
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(
                                    if (on) C.text else C.neutral200,
                                    RoundedCornerShape(12.dp),
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { m.setThemeMode(mode) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                lbl,
                                style = body(13.0, if (on) C.bg else C.neutral800, 600),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
            }
        }

        SectLabel("איך הכול מחושב")

        // ההסבר והמחשבון ירדו לקיפול: הם עונים על שאלה שנשאלת פעם אחת,
        // ותפסו קודם שליש ממסך שנכנסים אליו כדי לגרור סליידר.
        Collapsible("שעות, ש״ש, ואיך נספר אלול", m.calcOpen) { m.calcOpen = !m.calcOpen }
        if (m.calcOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                HoursCalculator()

                // הסבר החישוב
                Column(
                    Modifier.fillMaxWidth().card().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("איך מחושב העומס", style = body(10.5, C.neutral600, 700))
                    Text(
                        "שעה אקדמית (ש״ש) היא ${Data.n(Hours.AC_MIN)} דקות, ואחריה הפסקה של " +
                            "${Data.n(Hours.BREAK_MIN)} — לכן רצף של n ש״ש תופס בלוח " +
                            "55n−10 דקות: מפגש של 2 ש״ש הוא 14:30–16:10, ושל 3 הוא 14:30–17:05. " +
                            "בשבוע שמחולק לרצפים כפולים כל ש״ש עולה בפועל 50 דקות ולא 45.",
                        style = body(12.0, C.neutral600, lineHeight = 19.8),
                    )
                    Text(
                        "סה״כ ש׳/שבוע = שעות השעון בכיתה + למידה עצמית " +
                            "(נ״ז × מקדם תחום: מתמטי ${Data.n(Hours.FACTOR["math"]!!)} · " +
                            "תכנותי ${Data.n(Hours.FACTOR["code"]!!)} · " +
                            "כללי ${Data.n(Hours.FACTOR["other"]!!)}). שני האגפים בשעות של 60 דקות, " +
                            "ולכן מותר לחבר אותם.",
                        style = body(12.0, C.neutral600, lineHeight = 19.8),
                    )
                    Text(
                        "אלול נספר ×${Data.n(Hours.elulMul)} — ${Data.n(Hours.REG_WEEKS)} שבועות " +
                            "הוראה נדחסים ל-${Data.n(Hours.ELUL_WEEKS)}, ולכן אותו קורס עולה " +
                            "פי כך בשבוע.",
                        style = body(12.0, C.neutral600, lineHeight = 19.8),
                    )
                    Text(
                        "התקציב = שעות הלימוד שקבעת בסליידר (${Data.n(m.cap())} ש׳/שבוע), " +
                            "בלי שעות העבודה (${Data.n(m.jobHours())} ש׳, ${su.jobPercent}% משרה) — " +
                            "אלו כבר נוכו מהשבוע שלך. לעיגון: סמסטר ב׳ של שנה א׳ במסלול התקני " +
                            "הוא ${Data.n(refLoad.ac)} ש״ש בכיתה = ${Data.n(refLoad.classH)} ש׳, " +
                            "ועוד ${Data.n(refLoad.selfH)} ש׳ למידה — סה״כ ${Data.n(refLoad.total)} ש׳ " +
                            "בשבוע.",
                        style = body(12.0, C.neutral600, lineHeight = 19.8),
                    )
                                Text(
                        "קורסים חוזרים לנכשלים סוננו; מועדי ההשלמה של אינפי 2 נשמרו כי הוא עוד חסר לך.",
                        style = body(12.0, C.neutral600, lineHeight = 19.8),
                    )
                }

            }
        }

        // ייצוא
        Column {
            SectLabel("ייצוא התכנון")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("share", "שתף את התכנון", "טקסט לוואטסאפ או להעתקה"),
                    Triple("csv", "קובץ אקסל (CSV)", "נפתח ישירות ב־Excel"),
                    Triple("table", "PDF להדפסה", "טבלת כל הקורסים"),
                ).forEach { (kind, lbl, sub) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .card()
                            .clickable { onExport(kind) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(lbl, style = body(14.0, C.text, 600))
                            Text(sub, style = body(11.5, C.neutral600))
                        }
                        Text("›", style = body(16.0, C.neutral500))
                    }
                }
            }
        }

        // איפוס
        Column(
            Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinePillButton(
                "אפס למסלול התקני",
                {
                    m.confirmModal = ConfirmModal(
                        "איפוס קורסים",
                        "כל הקורסים ישובצו מחדש בסמסטר ברירת המחדל שלהם לפי המסלול התקני. ההגדרות שלך יישמרו.",
                    ) { m.resetCoursesOnly() }
                },
                borderColor = C.accent,
                fg = C.accent700,
                borderWidth = 2,
            )
            PillButton("אפס את כל הנתונים", {
                m.confirmModal = ConfirmModal(
                    "איפוס כל הנתונים",
                    "פעולה זו תמחק את כל התכנון וההגדרות ותחזיר אותך למסך ברוך הבא. האם אתה בטוח?",
                ) { m.resetAllData() }
            }, fontSize = 13.5)
            Text(
                "התכנון נשמר במכשיר הזה אוטומטית, ומשותף עם גרסת המסך הרחב.",
                style = body(11.5, C.neutral600, lineHeight = 17.25),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** מתג הפעלה/כיבוי בסגנון המערכת — הידית נעה לקצה ה-inline-start. */
/**
 * מחשבון היחידות — ש״ש מול שעות שעון, חי.
 *
 * זו השאלה שכל סטודנט שואל כשהוא מסתכל בשנתון ואז בלוח: "4 ש״ש, כמה זמן זה
 * באמת?" התשובה אינה ×0.75, כי בין שעה לשעה יש הפסקה, והיא נשארת בלוח.
 */
@Composable
private fun HoursCalculator() {
    var ac by remember { mutableStateOf(4.0) }
    val real = Hours.toReal(ac)

    Column(
        Modifier.fillMaxWidth().card().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("מחשבון שעות", style = body(10.5, C.neutral600, 700))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CalcButton("−") { ac = max(0.5, ac - 0.5) }
            Column(Modifier.width(72.dp)) {
                Text(Data.n(ac), style = heading(24.0))
                Text("ש״ש", style = body(10.5, C.neutral600))
            }
            CalcButton("+") { ac = min(40.0, ac + 0.5) }
            Box(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(Data.n(real), style = heading(24.0, C.accent))
                Text("שעות שעון בשבוע", style = body(10.5, C.neutral600))
            }
        }
        Text(
            "${Data.n(ac)} × ${Data.n(Hours.AC_MIN)} דק׳ לימוד " +
                "+ ההפסקות שביניהן = ${(real * 60).roundToInt()} דקות.",
            style = body(11.5, C.neutral600, lineHeight = 17.25),
        )
        // איך זה נראה בלוח עצמו — השורה שמאפשרת לאמת את המספר מול המערכת.
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(1, 2, 3, 4).forEach { n ->
                Text(
                    "מפגש של $n ש״ש = ${Hours.blockMinutes(n)} דקות " +
                        "(${Hours.blockLabel(n)} שעה)",
                    style = body(11.0, C.neutral700),
                )
            }
        }
    }
}

@Composable
private fun CalcButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .card(background = C.neutral100)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(glyph, style = heading(18.0, C.neutral800)) }
}

@Composable
private fun SwitchRow(label: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .card()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, style = body(14.0, C.text, 600), modifier = Modifier.weight(1f))
        Box(
            Modifier
                .width(46.dp)
                .height(26.dp)
                .background(if (on) C.accent else C.neutral400, RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .offset(x = if (on) 23.dp else 3.dp)
                    .size(20.dp)
                    .background(C.white, RoundedCornerShape(999.dp)),
            )
        }
    }
}

/**
 * "איפה אני" — הסמסטר שהסטודנט יושב בו עכשיו.
 *
 * זה העוגן שכל שאר המסכים נשענים עליו: מה מוצג במסך "עכשיו", אילו סמסטרים
 * נקראים כעבר, ואילו התראות מגיעות לראש הרשימה. ברירת המחדל נגזרת מהחודש,
 * אבל היא ניחוש: מי שחזר על שנה, יצא להפסקה או מתחיל באמצע — מתקן כאן.
 */
@Composable
private fun CurrentSemCard(m: PlannerModel) {
    val cur = Data.semByKey(m.currentSemKey) ?: Data.SEMS[0]
    val curYear = cur.yearTxt
    Column(
        Modifier.fillMaxWidth().card().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("הסמסטר שאני בו עכשיו", style = body(10.5, C.neutral600, 700))
            Box(Modifier.weight(1f))
            Text(cur.title, style = body(13.0, C.neutral800, 600))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Data.YEARS.forEach { y ->
                val yearTxt = "שנה $y׳"
                val on = curYear == yearTxt
                ChipBox(Modifier.weight(1f), on, "שנה $y׳") {
                    val k = Data.SEMS.firstOrNull {
                        it.yearTxt == yearTxt && it.type == cur.type
                    } ?: Data.SEMS.first { it.yearTxt == yearTxt }
                    m.setCurrentSem(k.key)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("אלול" to "אלול", "א" to "סמסטר א׳", "ב" to "סמסטר ב׳").forEach { (t, lbl) ->
                val on = cur.type == t
                ChipBox(Modifier.weight(1f), on, lbl) {
                    Data.SEMS.firstOrNull { it.yearTxt == curYear && it.type == t }
                        ?.let { m.setCurrentSem(it.key) }
                }
            }
        }

        Text(
            "קורסים בסמסטרים שלפני זה נחשבים מאחוריך: הם לא נספרים בעומס, " +
                "אי אפשר לשבץ אליהם, וההתראות שלהם יורדות בעדיפות.",
            style = body(11.5, C.neutral600, lineHeight = 17.25),
        )
    }
}

@Composable
private fun ChipBox(modifier: Modifier, on: Boolean, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(44.dp)
            .background(if (on) C.text else C.neutral200, shape)
            .clip(shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = body(12.5, if (on) C.bg else C.neutral800, 600),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** שורת כותרת שפותחת וסוגרת מקטע. */
@Composable
private fun Collapsible(label: String, open: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .card()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = body(13.5, C.text, 600), modifier = Modifier.weight(1f))
        Text(if (open) "−" else "+", style = heading(16.0, C.neutral600))
    }
}
