package com.levcs.semesterplanner

import androidx.compose.ui.graphics.Color
import kotlin.math.floor

/**
 * מקור הנתונים: קטלוג הקורסים (RAW), שרשרת הקדם (PRE) והמסלול המומלץ (REC).
 * יחידות הזמן וחישוב העומס יושבים ב-Hours.
 */
data class Course(
    val id: String,
    val name: String,
    /** נ״ז — מה שהתואר סופר. */
    val cr: Double,
    /** ש״ש: שעות אקדמיות בכיתה בשבוע (הרצאה + תרגול + מעבדה). */
    val ac: Double,
    val mand: Boolean,
    val cat: String,
    val offer: List<String>,
)

data class Sem(
    val key: String,
    val title: String,
    val type: String,
    val extra: Boolean,
    val yearTxt: String,
)

object Data {

    const val PREFS = "lev-cs-planner"
    const val KEY = "lev-cs-planner-v2"
    const val ONBOARD_KEY = "lev-cs-planner-onboarded"
    const val THEME_KEY = "lev-cs-planner-theme"

    val LEGACY = mapOf(
        "e7" to "y1אלול", "a7" to "y1א", "b7" to "y1ב",
        "e8" to "y2אלול", "a8" to "y2א", "b8" to "y2ב",
        "e9" to "y3אלול", "a9" to "y3א", "b9" to "y3ב",
    )

    val SEMORD = mapOf("אלול" to 0, "א" to 1, "ב" to 2, "שנתי" to 3)
    val YEARORD = mapOf("א" to 0, "ב" to 1, "ג" to 2, "ד" to 3)
    val YEARS = listOf("א", "ב", "ג", "ד")
    val OWED0 = listOf("120132")

    // ── קבועי התקציב השבועי ─────────────────────────────────────────────────
    // כל מה שנוגע ליחידות הזמן ולעומס עצמו יושב ב-Hours; כאן נשאר רק מה
    // שמתאר את השבוע של הסטודנט.

    /** מעל כמה מהתקציב נחשב "חריגה". סף אחד לכל האפליקציה. */
    const val OVER_BUDGET = 1.05

    /** שעות פנויות בשבוע רגיל (א׳–ו׳, שישי קצר) — לחישוב הזמן שנשאר. */
    const val WEEK_HOURS = 84.0

    /** טווח סליידר שעות הלימוד. סמסטר מלא בשנה א׳ עולה 32–44 ש׳ בשבוע. */
    const val STUDY_MIN = 10.0
    const val STUDY_MAX = 70.0
    const val STUDY_DEFAULT = 40.0

    /** שעות עבודה בשבוע לכל אחוז משרה (משרה מלאה = 42 ש׳). */
    const val JOB_HOURS_PER_PCT = 0.42

    private data class Row(
        val id: String, val name: String, val cr: Double, val ac: Double,
        val mand: Int, val cat: String, val offer: List<String>,
    )

    /*
     * ש״ש — שעות המפגש השבועיות, עמודה אחת במקום שלוש.
     *
     * הטבלה הקודמת החזיקה שלוש עמודות מפגש (הרצאה/תרגול/מעבדה) והן לא סכמו
     * לשום דבר נכון. מערכות השעות האמיתיות של שנה א׳ מראות למה: ב-4 מ-9
     * הקורסים שאפשר היה לבדוק עמודת ה"הרצאה" כבר הכילה את התרגול והעמודה
     * השנייה שכפלה אותה (אינפי 1 נספר 12 ש״ש במקום 6), ובשלושה אחרים דווקא
     * חסר תרגול קיים (תכנות מתקדם נספר 2 במקום 4, המעבדה 0 במקום 2). סכימת
     * שלוש העמודות נתנה 48 ש״ש לסמסטר שבפועל יש בו 32.
     *
     * מה שכן מסתדר מושלם — 9 מתוך 9 — הוא זהות הנ״ז: נ״ז = הרצאה + תרגול/2.
     * לכן נ״ז אמין, וממנו נגזר הרצפה ש-ש״ש ≥ נ״ז (קורס לא יכול לתת יותר נ״ז
     * משעות המפגש שלו, למעט פרויקטים שהנ״ז בהם על עבודה עצמאית).
     *
     * הערכים המסומנים ✓ נקראו ישירות ממערכת שעות; השאר שוחזרו מהקטלוג לפי
     * הרצפה הזאת. סטודנט שרואה מספר שלא תואם למערכת שלו יכול לתקן אותו
     * בגיליון הקורס, והתיקון נשמר.
     */
    private val RAW = listOf(
        Row("150000", "מבוא למחשבים", 0.0, 2.0, 1, "other", listOf("אלול", "א")),
        Row("120000", "קורס קדם במתמטיקה", 0.0, 5.0, 1, "math", listOf("אלול", "א")),
        Row("900011", "הכרת משאבי הספרייה", 0.0, 2.0, 1, "other", listOf("א")),
        Row("120131", "חשבון אינפי' להנדסה 1", 5.0, 6.0, 1, "math", listOf("א", "ב")),  // ✓ מאומת מול מערכת שעות
        Row("150005", "מבוא למדעי המחשב", 4.0, 5.0, 1, "code", listOf("א")),  // ✓ מאומת מול מערכת שעות
        Row("150301", "מערכות ספרתיות", 3.5, 4.0, 1, "other", listOf("א")),  // ✓ מאומת מול מערכת שעות
        Row("120302", "מתמטיקה בדידה", 2.5, 3.0, 1, "math", listOf("א")),  // ✓ מאומת מול מערכת שעות
        Row("141001", "פיזיקה להנדסת מחשבים", 3.5, 4.0, 0, "other", listOf("א")),
        Row("120201", "אלגברה לינארית א", 3.0, 4.0, 1, "math", listOf("ב")),  // ✓ מאומת מול מערכת שעות
        Row("120132", "חשבון אינפי' להנדסה 2", 5.0, 6.0, 1, "math", listOf("ב", "א")),
        Row("151101", "מבנה המחשב", 3.0, 3.0, 1, "other", listOf("ב")),  // ✓ מאומת מול מערכת שעות
        Row("150015", "מבני נתונים א", 3.5, 4.0, 1, "code", listOf("ב", "א")),  // ✓ מאומת מול מערכת שעות
        Row("151103", "מעבדה במבנה המחשב", 1.0, 2.0, 1, "other", listOf("ב")),  // ✓ מאומת מול מערכת שעות
        Row("150024", "תכנות מתקדם C++", 3.0, 4.0, 1, "code", listOf("ב")),  // ✓ מאומת מול מערכת שעות
        Row("120701", "הסתברות", 2.5, 3.0, 1, "math", listOf("אלול", "א")),
        Row("120304", "לוגיקה מתמטית", 2.5, 3.0, 1, "math", listOf("אלול", "א")),
        Row("150101", "אוטומטים ושפות פורמליות", 4.0, 5.0, 1, "math", listOf("א", "ב")),
        Row("120221", "אלגברה לינארית ב", 2.5, 3.0, 1, "math", listOf("א")),
        Row("150090", "מבני נתונים ב", 4.0, 5.0, 1, "code", listOf("א")),
        Row("151111", "מערכות הפעלה", 4.0, 5.0, 1, "code", listOf("א")),
        Row("141037", "פיזיקה ב' להנדסת מחשבים", 3.5, 4.0, 0, "other", listOf("א")),
        Row("156360", "מבוא לאבטחת מידע", 3.0, 4.0, 0, "code", listOf("ב")),
        Row("157100", "מבוא לבינה מלאכותית ומדע הנתונים", 3.0, 3.0, 0, "code", listOf("ב")),
        Row("156200", "מבוא לתקשורת מחשבים", 4.0, 5.0, 1, "other", listOf("ב")),
        Row("153007", "מיני פרויקט במערכות חלונות", 3.0, 2.0, 1, "code", listOf("ב")),
        Row("150134", "ניתוח אלגוריתמים וסיבוכיות", 5.0, 6.0, 1, "math", listOf("ב")),
        Row("120711", "סטטיסטיקה למהנדסים", 2.5, 3.0, 1, "math", listOf("ב")),
        Row("151123", "מערכות UNIX", 2.5, 2.5, 0, "other", listOf("אלול")),
        Row("150790", "אנגלית למדעי המחשב", 2.0, 3.0, 1, "other", listOf("אלול", "ב")),
        Row("151080", "הנדסת תוכנה בעידן AI", 2.5, 2.5, 0, "code", listOf("א")),
        Row("150221", "בסיסי נתונים", 3.0, 3.0, 1, "code", listOf("א")),
        Row("151075", "הנדסת תכנה וגרפיקה ממוחשבת", 3.0, 3.0, 0, "code", listOf("א")),
        Row("151285", "חישוביות ומורכבות החישובים", 4.0, 4.0, 1, "math", listOf("א", "ב")),
        Row("156363", "יסודות באבטחת תוכנה", 3.5, 3.5, 0, "code", listOf("א")),
        Row("151131", "קומפיילרים ומתרגמים", 3.5, 3.5, 1, "code", listOf("א")),
        Row("156336", "רשתות מחשבים מתקדמות", 3.0, 3.0, 0, "other", listOf("א")),
        Row("157200", "אקסלנטים", 3.0, 2.0, 0, "other", listOf("ב")),
        Row("156364", "יסודות לאבטחת רשתות", 3.0, 3.0, 0, "other", listOf("ב")),
        Row("157130", "מבוא לרברסינג", 3.5, 3.5, 0, "code", listOf("ב")),
        Row("150225", "מיני פרויקט בבסיסי נתונים", 1.5, 2.0, 1, "code", listOf("ב")),
        Row("120501", "משוואות דיפרנציאליות", 3.5, 3.5, 0, "math", listOf("ב")),
        Row("157109", "עיצוב ותכנות מונחי עצמים", 5.0, 5.0, 1, "code", listOf("ב")),
        Row("150060", "עקרונות שפות תכנה", 3.5, 3.5, 1, "other", listOf("ב")),
        Row("151030", "Full-Stack Web Development", 4.0, 2.0, 0, "code", listOf("שנתי")),
        Row("153300", "מהשכלה לתעסוקה", 2.0, 2.0, 0, "other", listOf("שנתי")),
        Row("150858", "סמינריון למדעי המחשב", 1.0, 2.0, 0, "other", listOf("שנתי")),
    )

    val PRE: Map<String, List<String>> = mapOf(
        "120132" to listOf("120131"), "120221" to listOf("120201"),
        "120701" to listOf("120131"), "120711" to listOf("120701"),
        "120304" to listOf("120302"), "120501" to listOf("120132"),
        "150015" to listOf("150005"), "150024" to listOf("150005"),
        "150090" to listOf("150015"),
        "151101" to listOf("150301"), "151103" to listOf("151101"),
        "151111" to listOf("151101", "150024"),
        "150101" to listOf("120302"), "150134" to listOf("150090", "120302"),
        "151285" to listOf("150101"),
        "151131" to listOf("150101", "150024"), "151070" to listOf("150024"),
        "153007" to listOf("150024"),
        "150221" to listOf("150015"), "150225" to listOf("150221"),
        "156200" to listOf("151111"), "156336" to listOf("156200"),
        "156360" to listOf("151111"), "156363" to listOf("150024"),
        "156364" to listOf("156200"), "157130" to listOf("151101"),
        "157100" to listOf("150015", "120701"), "150060" to listOf("150024"),
        "157109" to listOf("150024"),
        "151075" to listOf("151070"), "151080" to listOf("151070"),
        "141037" to listOf("141001"), "151030" to listOf("150024"),
        "150858" to listOf("150090"),
    )

    val REC: Map<String, Pair<String, String>> = mapOf(
        "150000" to ("א" to "אלול"), "120000" to ("א" to "אלול"), "900011" to ("א" to "א"),
        "120131" to ("א" to "א"), "150005" to ("א" to "א"),
        "150301" to ("א" to "א"), "120302" to ("א" to "א"), "141001" to ("א" to "א"),
        "120201" to ("א" to "ב"), "120132" to ("א" to "ב"),
        "151101" to ("א" to "ב"), "150015" to ("א" to "ב"), "151103" to ("א" to "ב"),
        "150024" to ("א" to "ב"),
        "120701" to ("ב" to "אלול"), "120304" to ("ב" to "אלול"), "150101" to ("ב" to "א"),
        "120221" to ("ב" to "א"), "151070" to ("ב" to "א"),
        "150090" to ("ב" to "א"), "151111" to ("ב" to "א"), "141037" to ("ב" to "א"),
        "156360" to ("ב" to "ב"), "157100" to ("ב" to "ב"),
        "156200" to ("ב" to "ב"), "153007" to ("ב" to "ב"), "150134" to ("ב" to "ב"),
        "120711" to ("ב" to "ב"),
        "151123" to ("ג" to "אלול"), "150790" to ("ג" to "אלול"), "151080" to ("ג" to "א"),
        "150221" to ("ג" to "א"), "151075" to ("ג" to "א"),
        "151285" to ("ג" to "א"), "156363" to ("ג" to "א"), "151131" to ("ג" to "א"),
        "156336" to ("ג" to "א"),
        "157200" to ("ג" to "ב"), "156364" to ("ג" to "ב"), "157130" to ("ג" to "ב"),
        "150225" to ("ג" to "ב"), "120501" to ("ג" to "ב"), "157109" to ("ג" to "ב"),
        "150060" to ("ג" to "ב"), "151030" to ("ג" to "שנתי"), "153300" to ("ג" to "שנתי"),
        "150858" to ("ג" to "שנתי"),
    )

    /** עיגול חצי כלפי מעלה (kotlin.math.round הוא עיגול בנקאי). */
    fun half(x: Double) = floor(x + 0.5)

    val courses: List<Course> =
        RAW.map { Course(it.id, it.name, it.cr, it.ac, it.mand == 1, it.cat, it.offer) }

    private val byIdMap: Map<String, Course> = courses.associateBy { it.id }

    fun byId(id: String): Course? = byIdMap[id]

    /** ארבע שנים × (אלול, א׳, ב׳). שנה ד׳ מסומנת כתוספת. */
    val SEMS: List<Sem> = buildList {
        for (y in 0..3) {
            for (sm in listOf("אלול", "א", "ב")) {
                add(
                    Sem(
                        key = "y$y$sm",
                        title = "שנה ${YEARS[y]}׳ · " + if (sm == "אלול") "אלול" else "סמסטר $sm׳",
                        type = sm,
                        extra = y == 3,
                        yearTxt = "שנה ${YEARS[y]}׳",
                    )
                )
            }
        }
    }

    val semIndex: Map<String, Int> = SEMS.withIndex().associate { (i, s) -> s.key to i }

    fun semByKey(key: String?): Sem? = SEMS.firstOrNull { it.key == key }

    /**
     * הסמסטר שרץ בחודש נתון, לפי לוח השנה של מכון לב.
     *
     * אלול יושב על אוגוסט–ספטמבר, סמסטר א׳ נפתח אחרי החגים ורץ עד פברואר,
     * וסמסטר ב׳ תופס את מרץ–יולי כולל תקופת המבחנים. זו הצעה בלבד: הסטודנט
     * יכול לתקן את הסמסטר הנוכחי בהגדרות, ומי שחזר על שנה או יצא להפסקה
     * חייב לתקן.
     */
    fun semTypeNow(month: Int): String = when (month) {
        8, 9 -> "אלול"
        10, 11, 12, 1, 2 -> "א"
        else -> "ב"
    }

    /** "יום אחד" ולא "1 ימים" — הרבים בעברית לא נגזר מהמספר בלי טיפול. */
    fun daysTxt(n: Int): String = when (n) {
        0 -> "ללא"
        1 -> "יום אחד"
        2 -> "יומיים"
        else -> "$n ימים"
    }

    /** מספר יפה: עד ספרה עשרונית אחת, בלי ".0" מיותר. */
    fun n(x: Double): String {
        val r = half(x * 10) / 10
        return if (r == floor(r)) r.toLong().toString() else r.toString()
    }

    fun isElul(semKey: String?) = semKey != null && semKey.endsWith("אלול")

    /**
     * העומס של קורס בסמסטר נתון. ac מגיע מבחוץ כי לסטודנט מותר לתקן אותו
     * מול מערכת השעות שלו; באלול אותו קורס נדחס לפחות שבועות ולכן עולה יותר.
     */
    fun load(c: Course, ac: Double = c.ac, semKey: String? = null): Load =
        Hours.load(c.cr, c.cat, ac, isElul(semKey))

    /** דירוג עומס לפי שעות אמת בשבוע (כיתה + למידה עצמית). */
    fun diff(h: Double): Triple<Int, String, Color> = when {
        h <= 5 -> Triple(1, "קל", C.neutral500)
        h <= 8 -> Triple(2, "בינוני", C.neutral700)
        h <= 11 -> Triple(3, "מאתגר", C.accent600)
        h <= 14 -> Triple(4, "כבד", C.accent)
        else -> Triple(5, "קשה מאוד", C.accent700)
    }

    fun fits(c: Course, type: String): Boolean =
        c.offer.contains(type) || (c.offer.contains("שנתי") && type == "א")

    fun catLabel(cat: String) = when (cat) {
        "math" -> "מתמטי/תיאורטי"
        "code" -> "תכנותי"
        else -> "כללי"
    }

    /** כותרת הקיבוץ במדף — לפי הסמסטר המומלץ של הקורס. */
    fun recKey(id: String): Pair<Int, String> {
        val r = REC[id] ?: return 99 to "ללא שיוך"
        val semTxt = when (r.second) {
            "שנתי" -> "שנתי"
            "אלול" -> "אלול"
            else -> "סמסטר ${r.second}׳"
        }
        return (YEARORD[r.first]!! * 10 + SEMORD[r.second]!!) to "מומלץ: שנה ${r.first}׳ · $semTxt"
    }
}
