package com.levcs.semesterplanner

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

data class Setup(
    val startYear: String = "ב",
    val jobPercent: Int = 50,
    val freeDays: Int = 2,
    val capacity: Double? = null,
    val showElectives: Boolean = true,
    /** להציג שעות כשעות שעון (ברירת מחדל) או כשעות אקדמיות בנות 45 דקות. */
    val academicUnit: Boolean = false,
    /**
     * הסמסטר שהסטודנט יושב בו עכשיו — העוגן של "איפה אני".
     *
     * null = לגזור מהתאריך ומשנת ההתחלה. הסטודנט יכול לקבע אותו בהגדרות,
     * ומי שחזר על שנה או יצא להפסקה חייב: הלוח לבדו לא יודע על זה.
     */
    val currentSem: String? = null,
)

data class Onboard(
    val active: Boolean = true,
    val step: Int = 0,
    val startYear: String = "ב",
    /** הסמסטר שאני מתחיל בו. מוצע לפי החודש, ונבחר במפורש באותו מסך כמו השנה. */
    val startSem: String = Data.semTypeNow(java.time.LocalDate.now().monthValue),
    val jobPercent: Int = 50,
    val studyHours: Int = Data.STUDY_DEFAULT.toInt(),
    val freeDays: Int = 2,
    val touchedYear: Boolean = false,
    val touchedDays: Boolean = false,
)

data class ConfirmModal(val title: String, val msg: String, val onYes: () -> Unit)

class PlannerModel(private val app: Context) : ViewModel() {

    var plan by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var done by mutableStateOf<List<String>>(emptyList()); private set

    /**
     * תיקוני ש״ש של הסטודנט, לפי מזהה קורס.
     *
     * הקטלוג הוא ברירת מחדל, לא אמת: מערכת השעות בפועל משתנה בין מסלולים
     * ובין שנים, וחלק משורות הקטלוג שוחזרו ולא נקראו ממערכת אמיתית. מי שרואה
     * מספר שלא תואם למערכת שלו מתקן אותו כאן, והתיקון גובר על הקטלוג בכל
     * מקום שבו מחושב עומס.
     */
    var acOverride by mutableStateOf<Map<String, Double>>(emptyMap()); private set
    var setup by mutableStateOf(Setup())
        private set
    var onboard by mutableStateOf(Onboard())

    var tab by mutableStateOf("home")
    var semIdx by mutableStateOf(0)

    /** הסמסטר הפתוח בציר התכנון. null = הציר סגור כולו. */
    var planOpenKey by mutableStateOf<String?>(null)
    var view by mutableStateOf("sem")
    var sheetId by mutableStateOf<String?>(null)
    var moveId by mutableStateOf<String?>(null)
    var depSheetId by mutableStateOf<String?>(null)
    var pickerOpen by mutableStateOf(false)
    var alertsOpen by mutableStateOf(false)
    var query by mutableStateOf("")
    var doneOpen by mutableStateOf(false)

    /** מסנן מסך הקורסים: left (נותרו) / planned (מתוכננים) / done (הושלמו). */
    var shelfFilter by mutableStateOf("left")

    /** האם מקטע "איך הכול מחושב" בהגדרות פתוח. */
    var calcOpen by mutableStateOf(false)
    var confirmModal by mutableStateOf<ConfirmModal?>(null)
    var toast by mutableStateOf("")
        private set
    var toastUndo by mutableStateOf(false)
        private set

    // מפת תלויות
    var depsSub by mutableStateOf("list")
    var depsSemKey by mutableStateOf<String?>(null)
    var depsFilter by mutableStateOf("all")
    var graphFocus by mutableStateOf<String?>(null)

    private var undoSnapshot: Pair<Map<String, String>, List<String>>? = null
    private var toastJob: Job? = null

    private val prefs = app.getSharedPreferences(Data.PREFS, Context.MODE_PRIVATE)

    /**
     * ערכת הנושא נשמרת במפתח של גרסת הווב. בהיעדר בחירה — הולכים אחרי המכשיר;
     * "light"/"dark" הן העקיפה המפורשת, ולכן שתי הגרסאות מסכימות על המצב.
     */
    val themeMode: String
        get() = when (C.override) {
            null -> "auto"
            true -> "dark"
            else -> "light"
        }

    fun setThemeMode(mode: String) {
        C.override = when (mode) {
            "dark" -> true
            "light" -> false
            else -> null
        }
        val e = prefs.edit()
        if (mode == "auto") e.remove(Data.THEME_KEY) else e.putString(Data.THEME_KEY, mode)
        e.apply()
    }

    private fun deviceIsDark(): Boolean =
        (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    init {
        C.systemDark = deviceIsDark()
        C.override = when (prefs.getString(Data.THEME_KEY, null)) {
            "dark" -> true
            "light" -> false
            else -> null
        }
        val d = buildDefaults("ב", Data.OWED0)
        plan = d.first
        done = d.second
        semIdx = Data.semIndex[currentSemKey] ?: startIdx()
        planOpenKey = currentSemKey
        onboard = Onboard(active = prefs.getString(Data.ONBOARD_KEY, null) != "1")
        load()
    }

    // ── חישובי בסיס ─────────────────────────────────────────────────────────

    fun elulMul() = Hours.elulMul

    // ── יחידת התצוגה ────────────────────────────────────────────────────────
    // כל החישובים רצים בשעות שעון; רק המספרים שמוצגים עוברים דרך כאן.

    val academicUnit: Boolean get() = setup.academicUnit

    /** "ש׳" או "ש״ש", לפי ההעדפה. */
    val hUnit: String get() = Hours.unitLabel(academicUnit)

    /** מספר שעות מוכן לתצוגה, ביחידה שנבחרה. */
    fun hTxt(realHours: Double): String = Data.n(Hours.toUnit(realHours, academicUnit))

    /** ש״ש של הקורס אצל הסטודנט הזה — תיקון אישי אם יש, אחרת הקטלוג. */
    fun acOf(c: Course): Double = acOverride[c.id] ?: c.ac

    fun acOf(id: String): Double = Data.byId(id)?.let { acOf(it) } ?: 0.0

    fun isAcEdited(id: String) = id in acOverride

    /** העומס של קורס בסמסטר שהוא משובץ בו (או בסמסטר שנשאל עליו). */
    fun loadOf(c: Course, semKey: String? = plan[c.id]): Load =
        Data.load(c, acOf(c), semKey)

    /** תיקון ש״ש. ערך זהה לקטלוג מוחק את התיקון במקום להנציח אותו. */
    fun setAc(id: String, v: Double) {
        val c = Data.byId(id) ?: return
        val clean = max(0.0, Data.half(v * 2) / 2)
        val m = LinkedHashMap(acOverride)
        if (clean == c.ac) m.remove(id) else m[id] = clean
        acOverride = m
        save()
    }

    fun resetAc(id: String) {
        if (id !in acOverride) return
        acOverride = acOverride - id
        save()
        say("שעות המפגש הוחזרו לערך שבקטלוג.")
    }

    /**
     * שעות הלימוד השבועיות שהמשתמש הקצה — וגם התקציב עצמו.
     *
     * capacity מחזיק שעות לימוד בלבד. שעות העבודה כבר נוכו מהשבוע לפני
     * שהמשתמש בחר את המספר הזה, ולכן הן לא מתווספות לו ולא מופחתות ממנו;
     * הן משמשות רק להצגת הזמן שנשאר בשבוע (freeWeekHours).
     */
    fun cap() = max(Data.STUDY_MIN, setup.capacity ?: Data.STUDY_DEFAULT)

    fun jobHours() = Data.half(Data.JOB_HOURS_PER_PCT * setup.jobPercent * 2) / 2

    fun budgetHours() = Data.half(cap() * 2) / 2

    /** מה שנשאר מהשבוע אחרי העבודה — התקרה הריאלית לסליידר הלימוד. */
    fun freeWeekHours() = max(0.0, Data.WEEK_HOURS - jobHours())

    /** האם הוקצו יותר שעות לימוד ממה שנשאר בשבוע אחרי העבודה. */
    fun overcommitted() = cap() > freeWeekHours()

    /** אינדקס הסמסטר הראשון של שנת ההתחלה — שורש בניית המסלול התקני. */
    fun startIdx() = (Data.YEARORD[setup.startYear] ?: 1) * 3

    /** ברירת המחדל ל"איפה אני": שנת ההתחלה, והסמסטר שרץ בחודש הנוכחי. */
    fun defaultCurrentSem(startYear: String = setup.startYear): String {
        val y = Data.YEARORD[startYear] ?: 1
        return "y$y" + Data.semTypeNow(java.time.LocalDate.now().monthValue)
    }

    /** מפתח הסמסטר הנוכחי — בחירת הסטודנט, ואם אין, הגזירה מהתאריך. */
    val currentSemKey: String
        get() = setup.currentSem?.takeIf { it in Data.semIndex } ?: defaultCurrentSem()

    /**
     * אינדקס הסמסטר הנוכחי. זה הקו שמפריד בין מה שמאחור למה שלפנים, ולכן
     * גם הרצפה לשיבוץ: אין לדחוף קורס לסמסטר שכבר נגמר.
     */
    fun curIdx() = Data.semIndex[currentSemKey] ?: startIdx()

    fun setCurrentSem(key: String) {
        if (key !in Data.semIndex) return
        applySetup(setup.copy(currentSem = key), false)
        semIdx = Data.semIndex[key] ?: semIdx
        planOpenKey = key
    }

    /** סכום העומס של הקורסים הפעילים בסמסטר — ש״ש, שעות כיתה ושעות למידה. */
    fun semLoad(key: String, p: Map<String, String> = plan, d: List<String> = done): Load =
        Data.courses.filter { p[it.id] == key && it.id !in d }
            .fold(Load.ZERO) { acc, c -> acc + Data.load(c, acOf(c), key) }

    /** כל הקורסים שתלויים בקורס הזה, במעבר לרוחב שרשרת הקדם. */
    fun descendants(id: String): List<String> {
        val seen = HashSet<String>()
        val out = ArrayList<String>()
        val stack = ArrayDeque<String>()
        stack.addLast(id)
        while (stack.isNotEmpty()) {
            val cur = stack.removeLast()
            for ((k, pres) in Data.PRE) {
                if (cur !in pres || k in seen || k == id) continue
                seen.add(k)
                if (k !in done) out.add(k)
                stack.addLast(k)
            }
        }
        return out
    }

    // ── ברירות מחדל ושמירה ──────────────────────────────────────────────────

    /**
     * דוחף קדימה קורסים שנחתו לפני הסמסטר הנוכחי.
     *
     * המסלול התקני נבנה לפי שנה שלמה, ולכן סטודנט שמצטרף באמצע שנה קיבל את
     * קורסי אלול וסמסטר א׳ שלו משובצים מאחורי "עכשיו" — ופתח את האפליקציה
     * מול קיר התראות "משובץ בסמסטר שכבר עבר" על תכנון שהיא בנתה בעצמה.
     */
    private fun pushPastForward(p: LinkedHashMap<String, String>, floor: Int) {
        if (floor <= 0) return
        val idx = Data.semIndex
        p.keys.toList().forEach { id ->
            val k = p[id] ?: return@forEach
            if ((idx[k] ?: 0) >= floor) return@forEach
            val c = Data.byId(id) ?: return@forEach
            val target = Data.SEMS.drop(floor).firstOrNull { Data.fits(c, it.type) }
            if (target != null) p[id] = target.key else p.remove(id)
        }
    }

    fun buildDefaults(
        startYear: String,
        keepOwed: List<String>?,
        /** אינדקס הסמסטר הנוכחי; קורסים שנופלים לפניו נדחפים קדימה. */
        floorIdx: Int = -1,
    ): Pair<Map<String, String>, List<String>> {
        val start = Data.YEARORD[startYear] ?: 1
        val p = LinkedHashMap<String, String>()
        val d = ArrayList<String>()
        Data.courses.forEach { c ->
            val r = Data.REC[c.id] ?: return@forEach
            val y = Data.YEARORD[r.first]!!
            val sm = if (r.second == "שנתי") "א" else r.second
            if (y < start) {
                if (keepOwed != null && c.id in keepOwed) p[c.id] = "y${min(start, 3)}א"
                else if (c.mand) { p[c.id] = "y$y$sm"; d.add(c.id) }
            } else if (c.mand) {
                p[c.id] = "y${min(y, 3)}$sm"
            }
        }
        pushPastForward(p, floorIdx)
        return p to d
    }

    private fun save(p: Map<String, String> = plan, d: List<String> = done, s: Setup = setup) {
        val o = JSONObject()
        o.put("v", STATE_VERSION)
        o.put("plan", JSONObject(p as Map<*, *>))
        o.put("done", JSONArray(d))
        o.put("ac", JSONObject(acOverride as Map<*, *>))
        o.put(
            "setup",
            JSONObject().apply {
                put("startYear", s.startYear)
                put("jobPercent", s.jobPercent)
                put("freeDays", s.freeDays)
                if (s.capacity != null) put("capacity", s.capacity) else put("capacity", JSONObject.NULL)
                put("showElectives", s.showElectives)
                put("academicUnit", s.academicUnit)
                if (s.currentSem != null) put("currentSem", s.currentSem)
                else put("currentSem", JSONObject.NULL)
            },
        )
        prefs.edit().putString(Data.KEY, o.toString()).apply()
    }

    private fun load() {
        val raw = prefs.getString(Data.KEY, null) ?: return
        try {
            val v = JSONObject(raw)
            val ver = v.optInt("v", 1)
            val planJson = v.optJSONObject("plan") ?: return
            var s = setup
            v.optJSONObject("setup")?.let { js ->
                s = Setup(
                    startYear = js.optString("startYear", s.startYear),
                    jobPercent = js.optInt("jobPercent", s.jobPercent),
                    freeDays = js.optInt("freeDays", s.freeDays),
                    // optDouble מחזיר NaN על ערך לא-מספרי, ו-NaN מחלחל עד "NaN ש׳" במסך
                    capacity = if (js.isNull("capacity")) null
                    else js.optDouble("capacity").takeIf { it.isFinite() },
                    showElectives = js.optBoolean("showElectives", true),
                    academicUnit = js.optBoolean("academicUnit", false),
                    currentSem = if (js.isNull("currentSem")) null
                    else js.optString("currentSem").takeIf { it in Data.semIndex },
                )
            }
            val valid = Data.SEMS.map { it.key }.toSet()
            val p = LinkedHashMap<String, String>()
            planJson.keys().forEach { id ->
                val v0 = planJson.optString(id)
                val k = Data.LEGACY[v0] ?: v0
                if (k in valid) p[id] = k
            }
            val d = ArrayList<String>()
            v.optJSONArray("done")?.let { arr ->
                for (i in 0 until arr.length()) d.add(arr.optString(i))
            }
            val ac = LinkedHashMap<String, Double>()
            v.optJSONObject("ac")?.let { js ->
                js.keys().forEach { id ->
                    val x = js.optDouble(id)
                    // ערך לא-מספרי היה מחלחל עד "NaN ש׳" בכל מסך שמסכם עומס
                    if (x.isFinite() && x >= 0 && Data.byId(id) != null) ac[id] = x
                }
            }
            // קורסי חובה משנים שכבר עברו נחשבים כהושלמו — הגירה חד-פעמית בלבד.
            // בעבר זה רץ בכל טעינה ודרס את המשתמש: ביטול "הושלם" לקורס שנכשל בו,
            // או העברתו לסמסטר עתידי, שרדו עד ההפעלה הבאה ואז חזרו בשקט —
            // והשעות שלו נעלמו מהסמסטר בלי סיבה נראית. עכשיו done נשמר כמו שהוא.
            if (ver < STATE_VERSION) {
                val start = Data.YEARORD[s.startYear] ?: 1
                Data.courses.forEach { c ->
                    val r = Data.REC[c.id] ?: return@forEach
                    if (!c.mand) return@forEach
                    val y = Data.YEARORD[r.first]!!
                    if (y >= start) return@forEach
                    val slot = p[c.id]
                    // קורס שמשובץ לסמסטר שעוד לפניך אינו "הושלם" — זה בדיוק המקרה
                    // של אינפי 2 החייב (OWED0), שההגירה הקודמת סימנה כהושלם בכל
                    // הפעלה ובכך מחקה את הקורס שכל מנגנון החוב קיים בשבילו.
                    if (slot != null && (Data.semIndex[slot] ?: 0) >= start * 3) return@forEach
                    if (slot == null) p[c.id] = "y$y" + if (r.second == "שנתי") "א" else r.second
                    if (c.id !in d) d.add(c.id)
                }
            }
            plan = p
            done = d
            setup = s
            acOverride = ac
            semIdx = Data.semIndex[currentSemKey] ?: startIdx()
        planOpenKey = currentSemKey
            if (ver < STATE_VERSION) save(p, d, s)
        } catch (_: Exception) {
        }
    }

    // ── הודעות וביטול ───────────────────────────────────────────────────────

    fun say(msg: String, canUndo: Boolean = false) {
        toast = msg
        toastUndo = canUndo
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(if (canUndo) 5000 else 3200)
            toast = ""
            toastUndo = false
        }
    }

    private fun snapshot() { undoSnapshot = plan to done }

    fun undo() {
        val u = undoSnapshot ?: return
        undoSnapshot = null
        plan = u.first
        done = u.second
        save(u.first, u.second)
        say("הפעולה בוטלה.")
    }

    // ── פעולות על התכנון ────────────────────────────────────────────────────

    fun place(id: String, key: String?) {
        val c = Data.byId(id) ?: return
        val sem = Data.semByKey(key)
        if (sem != null && sem.type != "אלול" && !Data.fits(c, sem.type)) return
        snapshot()
        val p = LinkedHashMap(plan)
        if (key != null) p[id] = key else p.remove(id)
        val d = done.filter { it != id }
        plan = p
        done = d
        save(p, d, setup)
    }

    fun shift(id: String, dir: Int) {
        val c = Data.byId(id) ?: return
        val cur = plan[id]
        val start = curIdx()
        var i = Data.SEMS.indexOfFirst { it.key == cur }
        // קורס על המדף אין לו מיקום: "דחה" סורק קדימה מהסמסטר הנוכחי ולא
        // מאינדקס 0 — אחרת הוא נוחת בשנה א׳ עם הודעה "נדחה ל…".
        if (i < 0) i = if (dir > 0) start - 1 else Data.SEMS.size
        // אין לזוז אחורה אל סמסטר שכבר עבר, אלא אם הקורס ממילא כבר שם.
        val floor = if (i >= start) start else 0
        var j = i + dir
        while (j >= floor && j < Data.SEMS.size) {
            if (Data.fits(c, Data.SEMS[j].type)) {
                place(id, Data.SEMS[j].key)
                say((if (dir > 0) "נדחה ל" else "הוקדם ל") + Data.SEMS[j].title, true)
                return
            }
            j += dir
        }
        if (dir > 0) {
            place(id, null)
            say("הועבר למדף — אין סמסטר מתאים אחרי זה.", true)
        } else {
            say("אין סמסטר מוקדם יותר שבו הקורס נפתח.")
        }
    }

    fun toggleDone(id: String) {
        val was = id in done
        snapshot()
        val d = if (was) done.filter { it != id } else done + id
        done = d
        save(plan, d, setup)
        say(if (was) "הסימון \"הושלם\" בוטל." else "סומן כהושלם.", true)
    }

    /** בוחר את הסמסטר הטוב ביותר לקורס: מומלץ קודם, ואז מי שנשאר בתקציב. */
    fun recommend(
        id: String,
        p: Map<String, String> = plan,
        d: List<String> = done,
    ): String? {
        val c = Data.byId(id) ?: return null
        val idx = Data.semIndex
        val budget = budgetHours()
        val rec = Data.REC[id]
        val recYearIdx = rec?.let { Data.YEARORD[it.first] }
        val recSem = rec?.let { if (it.second == "שנתי") "א" else it.second }
        val start = curIdx()
        var best: String? = null
        var bestScore = Double.NEGATIVE_INFINITY
        Data.SEMS.forEach { s ->
            // סמסטר שכבר עבר אינו יעד. הוא ריק מקורסים פעילים (הכול בו "הושלם"),
            // ולכן בלי החסם הזה הוא נראה לניקוד כמשבצת הפנויה ביותר בלוח.
            if ((idx[s.key] ?: 0) < start) return@forEach
            if (!Data.fits(c, s.type)) return@forEach
            val preOk = (Data.PRE[c.id] ?: emptyList()).all { pre ->
                val pk = p[pre]
                pre in d || (pk != null && (idx[pk] ?: 0) < (idx[s.key] ?: 0))
            }
            if (!preOk) return@forEach
            val after = semLoad(s.key, p, d).total + Data.load(c, acOf(c), s.key).total
            val over = max(0.0, after - budget)
            val m = Regex("^y(\\d)(.+)$").find(s.key)
            val sYear = m?.groupValues?.get(1)?.toInt()
            val sSem = m?.groupValues?.get(2)
            val recBonus = if (rec != null) {
                when {
                    sYear == recYearIdx && sSem == recSem -> 1000.0
                    sYear == recYearIdx -> 400.0
                    else -> 0.0
                }
            } else 0.0
            // קנס החריגה יחסי לתקציב ולא בשעות גולמיות: בשעות הוא היה מגיע
            // ל-2500− כשכל סמסטר אמיתי חורג, ומוחק את בונוס המסלול (1000).
            val score = recBonus - (over / budget) * 500 + (budget - after) -
                (idx[s.key] ?: 0) * 1.2 -
                (if (s.type == "אלול") 6.0 else 0.0) -
                (if (s.extra) 9.0 else 0.0)
            if (score > bestScore) { bestScore = score; best = s.key }
        }
        return best
    }

    fun autoElectives() {
        val p = LinkedHashMap(plan)
        val d = done
        val list = Data.courses
            .filter { !it.mand && p[it.id] == null && it.id !in d }
            .sortedByDescending { loadOf(it, null).total }
        var n = 0
        list.forEach { c ->
            val k = recommend(c.id, p, d)
            if (k != null) { p[c.id] = k; n++ }
        }
        if (n == 0) { say("לא נמצא סמסטר פנוי בתקציב לקורסי הבחירה."); return }
        snapshot()
        plan = p
        save(p, d)
        say("$n קורסי בחירה שובצו לסמסטר המומלץ.", true)
    }

    fun autoOne(id: String) {
        val k = recommend(id)
        if (k == null) { say("אין סמסטר שמתאים לקורס הזה (בעיית קדם-דרישות או סוג הצעה)."); return }
        place(id, k)
        val s = Data.semByKey(k)
        // אותו סף כמו בכרטיס הסמסטר ובגיליון ההעברה — אחרת הטוסט מזהיר
        // על חריגה והכרטיס שאליו הוא מוביל לא מראה שום אזהרה.
        val over = semLoad(k).total > budgetHours() * Data.OVER_BUDGET
        say(
            "שובץ ל" + (s?.title ?: "") +
                if (over) " — לב לב, זה חורג מהתקציב השבועי." else "",
            true,
        )
    }

    fun step(dir: Int) {
        semIdx = max(0, min(Data.SEMS.size - 1, semIdx + dir))
    }

    fun depsStep(dir: Int) {
        val cur = depsSemKey ?: Data.SEMS[0].key
        val i = Data.SEMS.indexOfFirst { it.key == cur }
        val ni = max(0, min(Data.SEMS.size - 1, (if (i < 0) 0 else i) + dir))
        depsSemKey = Data.SEMS[ni].key
    }

    // ── הגדרות ואיפוס ───────────────────────────────────────────────────────

    fun applySetup(patch: Setup, rebuild: Boolean) {
        if (rebuild) {
            // שנה חדשה מבטלת עוגן שנקבע לשנה הקודמת — אחרת "אני כאן" נשאר מאחור.
            val fresh = patch.copy(currentSem = null)
            val d = buildDefaults(
                patch.startYear,
                if (patch.startYear == "ב") Data.OWED0 else null,
                Data.semIndex[defaultCurrentSem(patch.startYear)] ?: -1,
            )
            setup = fresh
            plan = d.first
            done = d.second
            sheetId = null
            semIdx = Data.semIndex[currentSemKey] ?: startIdx()
            planOpenKey = currentSemKey
            save(d.first, d.second, fresh)
            say("התכנון נבנה מחדש לפי המסלול התקני.")
        } else {
            setup = patch
            save(plan, done, patch)
        }
    }

    fun resetCoursesOnly() {
        val s = setup
        val d = buildDefaults(
            s.startYear,
            if (s.startYear == "ב") Data.OWED0 else null,
            curIdx(),
        )
        snapshot()
        plan = d.first
        done = d.second
        sheetId = null
        moveId = null
        doneOpen = false
        semIdx = Data.semIndex[currentSemKey] ?: startIdx()
        confirmModal = null
        save(d.first, d.second, s)
        say("כל הקורסים אופסו למסלול התקני.", true)
    }

    fun resetAllData() {
        prefs.edit().remove(Data.ONBOARD_KEY).remove(Data.KEY).apply()
        confirmModal = null
        tab = "home"
        sheetId = null
        moveId = null
        toast = ""
        toastUndo = false
        onboard = Onboard(active = true)
    }

    // ── אשף הפתיחה ──────────────────────────────────────────────────────────

    fun onboardStep(n: Int) { onboard = onboard.copy(step = n) }
    fun onboardNext() { onboardStep(min(5, onboard.step + 1)) }
    fun onboardBack() { onboardStep(max(0, onboard.step - 1)) }

    fun onboardPickYear(y: String) {
        onboard = onboard.copy(startYear = y, touchedYear = true)
    }

    fun onboardPickSem(t: String) {
        onboard = onboard.copy(startSem = t, touchedYear = true)
    }

    fun onboardPickDays(d: Int) {
        onboard = onboard.copy(freeDays = d, touchedDays = true)
        onboardNext()
    }

    fun onboardFinish(auto: Boolean) {
        val ob = onboard
        val s = Setup(
            startYear = ob.startYear,
            jobPercent = ob.jobPercent,
            // שעות לימוד בלבד. בעבר נוספו כאן גם שעות העבודה, כך שהתקציב
            // נופח ב-60% ואחוז משרה גבוה יותר דווקא הגדיל אותו — בניגוד
            // למה שהאשף ומסך ההגדרות מבטיחים שניהם ("בלי העבודה").
            capacity = ob.studyHours.toDouble(),
            freeDays = ob.freeDays,
            showElectives = true,
            currentSem = "y${Data.YEARORD[ob.startYear] ?: 1}${ob.startSem}",
        )
        val d = buildDefaults(
            s.startYear,
            if (s.startYear == "ב") Data.OWED0 else null,
            Data.semIndex[s.currentSem] ?: -1,
        )
        setup = s
        plan = d.first
        done = d.second
        tab = "home"
        semIdx = Data.semIndex[currentSemKey] ?: startIdx()
        onboard = ob.copy(active = false)
        save(d.first, d.second, s)
        prefs.edit().putString(Data.ONBOARD_KEY, "1").apply()
        if (auto) viewModelScope.launch { delay(50); autoElectives() }
    }

    // ── מפת תלויות ──────────────────────────────────────────────────────────

    data class DepInfo(val status: String, val text: String)

    fun depInfo(c: Course): DepInfo {
        if (c.id in done) return DepInfo("done", "קורס הושלם")
        val sk = plan[c.id] ?: return DepInfo("unplaced", "לא משובץ עדיין")
        val pre = Data.PRE[c.id] ?: emptyList()
        if (pre.isEmpty()) return DepInfo("ok", "אין דרישות קדם")
        val idx = Data.semIndex
        var worst = "ok"
        val problems = ArrayList<String>()
        pre.forEach { p ->
            if (p in done) return@forEach
            val pk = plan[p]
            val pname = Data.byId(p)?.name ?: p
            when {
                pk == null -> { worst = "blocked"; problems.add("$pname · לא משובץ") }
                (idx[pk] ?: 0) > (idx[sk] ?: 0) -> { worst = "blocked"; problems.add("$pname · עתידי") }
                idx[pk] == idx[sk] -> {
                    if (worst != "blocked") worst = "warn"
                    problems.add("$pname · אותו סמסטר")
                }
            }
        }
        return DepInfo(
            worst,
            if (worst == "ok") "קדם: " + pre.joinToString(", ") { Data.byId(it)?.name ?: it }
            else problems.joinToString(" · "),
        )
    }

    private val levelMemo = HashMap<String, Int>()

    /** עומק שרשרת הקדם — עמודה בגרף. מעגלים נעצרים ברמה 0. */
    fun courseLevel(id: String, stack: MutableSet<String> = HashSet()): Int {
        levelMemo[id]?.let { return it }
        if (id in stack) return 0
        stack.add(id)
        val pre = Data.PRE[id] ?: emptyList()
        val lv = if (pre.isEmpty()) 0 else 1 + pre.maxOf { courseLevel(it, stack) }
        stack.remove(id)
        levelMemo[id] = lv
        return lv
    }

    fun depStatusColor(s: String) = when (s) {
        "ok" -> C.depOk
        "warn" -> C.depWarn
        "blocked" -> C.depBlocked
        "done" -> C.depDone
        else -> C.depUnplaced
    }

    fun depStatusLabel(s: String) = when (s) {
        "ok" -> "תקין"
        "warn" -> "אזהרה"
        "blocked" -> "חסום"
        "done" -> "הושלם"
        else -> "לא משובץ"
    }

    // ── ייצוא ───────────────────────────────────────────────────────────────

    fun exportRows(): Pair<List<String>, List<List<String>>> {
        val head = listOf(
            "מס׳ קורס", "שם הקורס", "סוג", "תחום", "נ״ז",
            "ש״ש בכיתה", "ש׳ בכיתה", "ש׳ למידה עצמית", "סה״כ ש׳/שבוע",
            "שנה", "סמסטר", "סטטוס",
        )
        val rows = Data.courses.map { c ->
            val isDone = c.id in done
            val key = plan[c.id]
            val s = key?.let { Data.semByKey(it) }
            val l = loadOf(c, key)
            listOf(
                c.id, c.name, if (c.mand) "חובה" else "בחירה", Data.catLabel(c.cat),
                Data.n(c.cr), Data.n(l.ac), Data.n(l.classH), Data.n(l.selfH), Data.n(l.total),
                if (isDone) "—" else s?.yearTxt ?: "",
                if (isDone) "—" else s?.title ?: "",
                if (isDone) "הושלם" else if (s != null) "מתוכנן" else "על המדף",
            )
        }.sortedWith(compareBy { it[9] + it[10] })
        return head to rows
    }

    companion object {
        /** גרסת מבנה השמירה. 2 = done נשמר כמו שהוא ולא נגזר מחדש בכל טעינה. */
        const val STATE_VERSION = 2
    }

    fun shareText(): String {
        val st = setup
        val lines = Data.SEMS.mapNotNull { s ->
            val list = Data.courses.filter { plan[it.id] == s.key && it.id !in done }
            if (list.isEmpty()) return@mapNotNull null
            val l = semLoad(s.key)
            s.title + " (" + Data.n(l.ac) + " ש״ש · " + Data.n(l.total) + " ש׳/שבוע):\n" +
                list.joinToString("\n") { "· " + it.name }
        }.joinToString("\n\n")
        val cur = Data.semByKey(currentSemKey)?.title ?: ""
        return "תכנון סמסטרים — מדעי המחשב, מכון לב\nאני עכשיו ב$cur · " +
            "${st.jobPercent}% משרה · תקציב ${Data.n(budgetHours())} ש׳/שבוע\n\n$lines"
    }
}
