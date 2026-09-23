package com.levcs.semesterplanner

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * העומס השבועי של קורס, בשלושה מספרים שכל אחד מהם ניתן להראות לסטודנט.
 *
 * ac      — ש״ש: שעות אקדמיות (45 דק׳) בכיתה בשבוע. זו היחידה של השנתון.
 * classH  — אותן שעות בשעון: כמה זמן אתה בפועל בבניין, כולל ההפסקות.
 * selfH   — שעות למידה עצמית בשבוע, בשעון.
 *
 * classH ו-selfH הן שתיהן שעות של 60 דקות, ולכן מותר לחבר אותן. זה בדיוק
 * מה שלא היה נכון קודם: המודל חיבר ש״ש (45 דק׳) לשעות למידה (60 דק׳)
 * וקרא לסכום "שעות".
 */
data class Load(val ac: Double, val classH: Double, val selfH: Double) {

    val total: Double get() = classH + selfH

    operator fun plus(o: Load) = Load(ac + o.ac, classH + o.classH, selfH + o.selfH)

    operator fun times(k: Double) = Load(ac * k, classH * k, selfH * k)

    companion object {
        val ZERO = Load(0.0, 0.0, 0.0)
    }
}

/**
 * ── יחידות הזמן של מכון לב ─────────────────────────────────────────────────
 *
 * המרה אחת, במקום אחד, ובה כל הקבועים שקובעים כמה "עולה" קורס.
 *
 * הכלל אומת מול שלוש מערכות שעות אמיתיות (שנה א׳ סמסטרים א׳+ב׳, ואלול שנה
 * ב׳): רצף של n שעות אקדמיות תופס בלוח 55n − 10 דקות — 45 דקות לימוד לכל
 * שעה, ועוד הפסקה של 10 דקות בין שעה לשעה. 14:30–16:10 הוא 2 ש״ש,
 * 14:30–17:05 הוא 3, ו-17:15–18:00 הוא אחת. תשעת מפגשי הקורסים ההנדסיים
 * בלוח נופלים על הכלל בדיוק; שני מפגשי בית המדרש (09:30–12:45, 13:00–14:30)
 * רצים על קצב אחר ולא נגזרים ממנו.
 */
object Hours {

    /** אורך שעה אקדמית בדקות. */
    const val AC_MIN = 45.0

    /** הפסקה בין שתי שעות אקדמיות רצופות. */
    const val BREAK_MIN = 10.0

    /** אורך רצף טיפוסי. רוב המפגשים בלוח הם "כפולים" — שתי ש״ש ברצף. */
    const val BLOCK_AC = 2.0

    /** שבועות הוראה בסמסטר רגיל. */
    const val REG_WEEKS = 13.0

    /**
     * שבועות הוראה באלול. אותו קורס נלמד בפחות שבועות ולכן עולה יותר בשבוע.
     *
     * 3.75 ולא הערכה: באלול תשפ״ו הסתברות (3 ש״ש בסמסטר רגיל) יושבת 8.5 שעות
     * שעון בשבוע ולוגיקה מתמטית 8.75 — פי 3.4–3.5 מהקצב הרגיל. המקדם שנגזר
     * כאן, 13/3.75, מנבא 17.3 שעות שבועיות לשתיהן מול 17.25 בפועל.
     */
    const val ELUL_WEEKS = 3.75

    /** ש״ש שנכנסות ליום לימודים אחד (14:30–19:50 ≈ 6). לאומדן ימי השבוע. */
    const val AC_PER_DAY = 6.0

    /** ימי לימוד בשבוע (א׳–ה׳). */
    const val STUDY_DAYS = 5

    /** שעות למידה עצמית לכל נ״ז, לפי אופי הקורס. */
    val FACTOR = mapOf("math" to 1.4, "code" to 1.5, "other" to 0.9)

    /** כמה יקר שבוע באלול ביחס לשבוע רגיל. */
    val elulMul: Double get() = REG_WEEKS / ELUL_WEEKS

    /** דקות שתופס בלוח רצף של n שעות אקדמיות: 45n + 10 הפסקות שביניהן. */
    fun blockMinutes(n: Int): Int =
        if (n <= 0) 0 else (AC_MIN * n + BREAK_MIN * (n - 1)).roundToInt()

    /**
     * ש״ש → שעות שעון. לא ×0.75: השעה האקדמית גוררת אחריה הפסקה, ובשבוע
     * שמחולק לרצפים של שתיים כל ש״ש תופסת בפועל 50 דקות ולא 45.
     */
    fun toReal(ac: Double): Double {
        if (ac <= 0) return 0.0
        val blocks = max(1.0, ac / BLOCK_AC)
        return (ac * AC_MIN + (ac - blocks) * BREAK_MIN) / 60.0
    }

    /**
     * יחידת התצוגה. החישוב כולו רץ בשעות שעון; זו עדשה בלבד, ולכן ההמרה
     * אחידה — גם העומס וגם התקציב עוברים בה, והיחס ביניהם נשמר.
     *
     * ההמרה כאן היא ×60/45 נקייה, בלי ההפסקות: היא עונה "כמה יחידות של 45
     * דקות יש בזמן הזה", ולא "איך זה יושב בלוח" — לזה יש את toReal.
     */
    fun toUnit(realHours: Double, academic: Boolean): Double =
        if (academic) realHours * 60.0 / AC_MIN else realHours

    fun unitLabel(academic: Boolean): String = if (academic) "ש״ש" else "ש׳"

    /** אומדן ימי הלימוד בשבוע שנדרשים כדי להכיל את המפגשים. */
    fun days(ac: Double): Int =
        if (ac <= 0) 0 else min(STUDY_DAYS.toDouble(), ceil(ac / AC_PER_DAY)).toInt()

    /** העומס של קורס בודד: ש״ש מהשנתון, זמן בכיתה, ולמידה עצמית לפי תחום. */
    fun load(cr: Double, cat: String, ac: Double, elul: Boolean = false): Load {
        val base = Load(ac, toReal(ac), cr * (FACTOR[cat] ?: 1.0))
        return if (elul) base * elulMul else base
    }

    /** אורך רצף כשעון: 2 ש״ש → "1:40". */
    fun blockLabel(n: Int): String {
        val mins = blockMinutes(n)
        return "${mins / 60}:${(mins % 60).toString().padStart(2, '0')}"
    }
}
