package com.levcs.semesterplanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * מודל השעות מול המציאות.
 *
 * הקבועים ב-Hours והעמודה ac ב-Data לא נבחרו בהערכה — הם נגזרו משלוש מערכות
 * שעות אמיתיות של מכון לב. הקובץ הזה מחזיק את אותן מערכות כנתוני בדיקה, כך
 * שכל שינוי עתידי בקטלוג או בקבועים שיוציא את המודל מכיול ייפול כאן ולא
 * יתגלה רק כשמישהו יסתכל על מספר שגוי במסך.
 */
class HoursTest {

    /** מפגש בלוח: שעת התחלה, שעת סיום, ומספר ש״ש שהוא אמור לייצג. */
    private data class Slot(val from: String, val to: String, val ac: Int)

    private fun minutes(s: Slot): Int {
        fun p(t: String) = t.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
        return p(s.to) - p(s.from)
    }

    private fun clockHours(slots: List<Slot>) = slots.sumOf { minutes(it) } / 60.0

    // ── מערכת שעות: שנה א׳ סמסטר א׳ ─────────────────────────────────────────
    private val semA = mapOf(
        "150005" to listOf(Slot("14:30", "17:05", 3), Slot("18:10", "19:50", 2)),
        "120131" to listOf(
            Slot("14:30", "16:10", 2), Slot("16:20", "18:00", 2), Slot("17:15", "18:55", 2),
        ),
        "120302" to listOf(Slot("16:20", "18:00", 2), Slot("19:05", "19:50", 1)),
        "150301" to listOf(Slot("14:30", "17:05", 3), Slot("18:10", "18:55", 1)),
    )

    // ── מערכת שעות: שנה א׳ סמסטר ב׳ ─────────────────────────────────────────
    private val semB = mapOf(
        "120201" to listOf(Slot("14:30", "16:10", 2), Slot("18:10", "19:50", 2)),
        "151101" to listOf(Slot("16:20", "18:55", 3)),
        "151103" to listOf(Slot("16:20", "18:00", 2)),
        "150015" to listOf(Slot("14:30", "17:05", 3), Slot("17:15", "18:00", 1)),
        "150024" to listOf(Slot("14:30", "16:10", 2), Slot("18:10", "19:50", 2)),
    )

    /**
     * אלול תשפ״ו. באלול הלוח צפוף אחרת — הרצפים ארוכים ובלי אותן הפסקות —
     * ולכן נבדק כאן רק סך שעות השעון בשבוע, לא פירוק לרצפים.
     */
    private val elul = mapOf(
        "120701" to listOf(
            Slot("14:30", "16:00", 2), Slot("14:30", "16:00", 2), Slot("14:30", "17:00", 3),
            Slot("14:30", "16:00", 2), Slot("14:30", "16:00", 2),
        ),
        "120304" to listOf(
            Slot("16:15", "18:45", 3), Slot("16:15", "17:45", 2), Slot("17:00", "18:45", 2),
            Slot("16:15", "17:45", 2), Slot("16:15", "17:45", 2),
        ),
    )

    // ── לוח הזמנים ──────────────────────────────────────────────────────────

    /** רצף של n ש״ש תופס 55n − 10 דקות: 45 דקות לימוד, ובין שעה לשעה הפסקה. */
    @Test
    fun `block length follows the 55n minus 10 rule`() {
        listOf(1 to 45, 2 to 100, 3 to 155, 4 to 210).forEach { (n, mins) ->
            assertEquals("רצף של $n ש״ש", mins, Hours.blockMinutes(n))
        }
    }

    /** כל מפגש בשתי המערכות ההנדסיות נופל על הכלל בדיוק, בלי סטייה. */
    @Test
    fun `every meeting in the real timetables decodes to whole academic hours`() {
        (semA + semB).forEach { (id, slots) ->
            slots.forEach { s ->
                assertEquals(
                    "${Data.byId(id)?.name}: ${s.from}-${s.to}",
                    Hours.blockMinutes(s.ac),
                    minutes(s),
                )
            }
        }
    }

    @Test
    fun `an academic hour is not simply three quarters of an hour`() {
        // ×0.75 היה נותן 1.5 שעות למפגש כפול; בלוח הוא 1:40.
        assertEquals(100.0 / 60.0, Hours.toReal(2.0), 1e-9)
        assertTrue(Hours.toReal(2.0) > 2.0 * 0.75)
        // שעה בודדת היא בדיוק 45 דקות — אין הפסקה אחריה בתוך הרצף.
        assertEquals(0.75, Hours.toReal(1.0), 1e-9)
        assertEquals(0.0, Hours.toReal(0.0), 1e-9)
    }

    // ── הקטלוג מול המערכות ──────────────────────────────────────────────────

    /** ש״ש בקטלוג = סך ש״ש שהמערכת האמיתית מקצה לקורס. */
    @Test
    fun `catalog contact hours match the observed timetables`() {
        (semA + semB).forEach { (id, slots) ->
            val c = Data.byId(id) ?: error("קורס $id נעלם מהקטלוג")
            assertEquals(c.name, slots.sumOf { it.ac }.toDouble(), c.ac, 1e-9)
        }
    }

    /**
     * זהות הנ״ז — נ״ז = הרצאה + תרגול/2 — מתקיימת בכל תשעת הקורסים שנבדקו,
     * ומכאן ש-ש״ש ≥ נ״ז. זו הרצפה ששוחזרו לפיה שאר שורות הקטלוג, ולכן היא
     * נבדקת על הקטלוג כולו ולא רק על התשע.
     */
    @Test
    fun `contact hours are never below credits except for project courses`() {
        val projects = setOf("153007", "150225", "151030", "150858", "157200", "153300")
        Data.courses.filter { it.id !in projects }.forEach { c ->
            assertTrue(
                "${c.name}: ${c.ac} ש״ש מול ${c.cr} נ״ז — קורס לא יכול לתת יותר נ״ז ממפגשים",
                c.ac >= c.cr,
            )
        }
    }

    // ── הכיול: שעות הכיתה שהמודל מנבא מול השעון ─────────────────────────────

    private fun classHours(ids: Collection<String>, semKey: String? = null) =
        ids.fold(Load.ZERO) { acc, id -> acc + Data.load(Data.byId(id)!!, semKey = semKey) }

    @Test
    fun `predicted class hours match the first-year timetables within one percent`() {
        listOf("שנה א׳ סמסטר א׳" to semA, "שנה א׳ סמסטר ב׳" to semB).forEach { (name, sched) ->
            val actual = clockHours(sched.values.flatten())
            val model = classHours(sched.keys).classH
            assertEquals(name, actual, model, actual * 0.01)
        }
    }

    /**
     * אלול הוא המקדם היחיד במודל שאי אפשר לקרוא ישירות מהקטלוג. הוא נגזר
     * מיחס שבועות ההוראה, והבדיקה היא שהיחס הזה מייצר את מה שיושב בלוח.
     */
    @Test
    fun `the elul multiplier reproduces the observed elul week`() {
        val actual = clockHours(elul.values.flatten())
        val model = classHours(elul.keys, "y1אלול").classH
        assertEquals("אלול", actual, model, actual * 0.02)
        assertTrue("אלול חייב להיות יקר מסמסטר רגיל", Hours.elulMul > 3.0)
        assertEquals(Hours.REG_WEEKS / Hours.ELUL_WEEKS, Hours.elulMul, 1e-9)
    }

    // ── היחידות לא מתערבבות ─────────────────────────────────────────────────

    /**
     * הבאג שהמודל הזה בא לתקן: הגרסה הקודמת חיברה ש״ש (45 דק׳) לשעות למידה
     * (60 דק׳). classH ו-selfH הן שתיהן שעות שעון, ורק הן נסכמות.
     */
    @Test
    fun `total sums only clock hours`() {
        Data.courses.forEach { c ->
            val l = Data.load(c)
            assertEquals(c.name, l.classH + l.selfH, l.total, 1e-9)
            if (c.ac == 0.0) assertEquals(c.name, 0.0, l.classH, 1e-9)
            if (c.cr == 0.0) assertEquals(c.name, 0.0, l.selfH, 1e-9)
            assertTrue("${c.name}: ש״ש נשמרות כפי שהן", abs(l.ac - c.ac) < 1e-9)
        }
    }

    @Test
    fun `load arithmetic is linear`() {
        val a = Data.load(Data.byId("120131")!!)
        val b = Data.load(Data.byId("150005")!!)
        val sum = a + b
        assertEquals(a.ac + b.ac, sum.ac, 1e-9)
        assertEquals(a.total + b.total, sum.total, 1e-9)
        assertEquals(a.total * 2, (a * 2.0).total, 1e-9)
        assertEquals(Load.ZERO.total, 0.0, 1e-9)
    }

    /** אותו קורס באלול עולה בדיוק פי המקדם — בשלושת המספרים. */
    @Test
    fun `elul scales every component of the load`() {
        val c = Data.byId("120701")!!
        val reg = Data.load(c)
        val el = Data.load(c, semKey = "y1אלול")
        assertEquals(reg.ac * Hours.elulMul, el.ac, 1e-9)
        assertEquals(reg.classH * Hours.elulMul, el.classH, 1e-9)
        assertEquals(reg.selfH * Hours.elulMul, el.selfH, 1e-9)
    }

    // ── הצגה ────────────────────────────────────────────────────────────────

    /** מעבר יחידות תצוגה הוא המרה אחידה, ולכן יחסים נשמרים. */
    @Test
    fun `display unit conversion preserves ratios`() {
        val load = 30.0
        val budget = 40.0
        assertEquals(load / budget, Hours.toUnit(load, true) / Hours.toUnit(budget, true), 1e-9)
        assertEquals(load, Hours.toUnit(load, false), 1e-9)
        // שעה אחת בשעון היא 60/45 שעות אקדמיות.
        assertEquals(4.0 / 3.0, Hours.toUnit(1.0, true), 1e-9)
    }

    /** אומדן ימי הלימוד נגזר מהמפגשים ולא חורג משבוע לימודים. */
    @Test
    fun `study days stay within the week`() {
        assertEquals(0, Hours.days(0.0))
        assertEquals(1, Hours.days(6.0))
        assertEquals(2, Hours.days(6.5))
        assertTrue(Hours.days(100.0) <= Hours.STUDY_DAYS)
    }
}
