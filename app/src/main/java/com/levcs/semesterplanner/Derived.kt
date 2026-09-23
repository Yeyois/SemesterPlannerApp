package com.levcs.semesterplanner

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class CardVM(
    val id: String,
    val name: String,
    val crTxt: String,
    /** ש״ש בכיתה. */
    val acTxt: String,
    /** סה״כ שעות שעון בשבוע: כיתה + למידה עצמית. */
    val hoursTxt: String,
    val acEdited: Boolean,
    val color: Color,
    val diffLabel: String,
    val isElective: Boolean,
    val isDone: Boolean,
    val hasIssue: Boolean,
    val issueText: String,
    val issueSev: Int,
    val offerTxt: String,
)

/** בעיה בשיבוץ, ועד כמה היא חוסמת. 2 = הקורס לא יילמד ככה; 1 = שווה מבט. */
data class Issue(val text: String, val sev: Int)

data class Alert(
    val semKey: String,
    val semTitle: String,
    val courseId: String,
    val text: String,
    val bg: Color,
    val line: Color,
    /** מרחק בסמסטרים מ"עכשיו": 0 = הסמסטר הנוכחי, שלילי = מאחור. */
    val dist: Int,
    /** 2 = חוסם, 1 = חריגה, 0 = לידיעה. */
    val sev: Int,
) {
    /** כמה דחוף. קודם החומרה, ואז הקרבה — שנה ד׳ לא צועקת כמו השבוע הזה. */
    val rank: Int get() = sev * 100 - kotlin.math.abs(dist)
}

data class Warn(
    val text: String,
    val color: Color,
    val fixId: String,
    val fixWhy: String,
    val fixLbl: String,
)

data class SemVM(
    val key: String,
    val lbl: String,
    val title: String,
    val yearTxt: String,
    val isElul: Boolean,
    val elulTag: String,
    val isExtra: Boolean,
    /** מיקום ביחס ל"עכשיו" — מה שקובע איך הסמסטר נקרא בציר. */
    val isPast: Boolean,
    val isCurrent: Boolean,
    val dist: Int,
    val courses: List<CardVM>,
    val warns: List<Warn>,
    val empty: Boolean,
    val allDone: Boolean,
    val hoursTxt: String,
    val acTxt: String,
    val classTxt: String,
    val selfTxt: String,
    val crTxt: String,
    val pct: Int,
    val barColor: Color,
    val countTxt: String,
    val statusText: String,
    val daysText: String,
    val names: String,
)

data class ShelfGroup(val sort: Int, val title: String, val courses: List<CardVM>)

data class Derived(
    val semData: List<SemVM>,
    /** הסמסטר שהמסכים מדפדפים בו. */
    val sem: SemVM,
    /** הסמסטר שהסטודנט יושב בו עכשיו — העוגן של מסך הבית. */
    val current: SemVM,
    /** הסמסטר הבא שיש בו מה ללמוד, אם יש. */
    val next: SemVM?,
    val alerts: List<Alert>,
    /** רק מה שנוגע לעכשיו ולסמסטר הבא — מה שמסך הבית מציג. */
    val alertsNow: List<Alert>,
    val unplaced: List<Course>,
    val shelfGroups: List<ShelfGroup>,
    val shelfEmpty: Boolean,
    val shelfEmptyTxt: String,
    val shelfNote: String,
    val shelfHasElectives: Boolean,
    /** הקורסים המתוכננים, מקובצים לפי הסמסטר שהם יושבים בו. */
    val plannedGroups: List<ShelfGroup>,
    val plannedCount: Int,
    val doneCourses: List<Course>,
    /** הקורסים שהושלמו, אחרי החיפוש. */
    val doneFiltered: List<Course>,
    val doneCr: Double,
    val planCr: Double,
    val shelfCr: Double,
    val totalCr: Double,
    val degreePct: Int,
    val finishTxt: String,
    /** כמה סמסטרים נשארו מ"עכשיו" ועד הסוף. */
    val semsLeft: Int,
    val budget: Double,
    /** התקציב ביחידת התצוגה, ותווית היחידה — כדי שהמסכים לא ימירו בעצמם. */
    val budgetTxt: String,
    val unit: String,
    val screenTitle: String,
    val screenSub: String,
)

object Compute {

    /** בעיות שיבוץ של קורס בסמסטר נתון — קדם חסר או פתיחה באלול. */
    fun issuesFor(m: PlannerModel, c: Course, semKey: String?): List<Issue> {
        val out = ArrayList<Issue>()
        if (semKey == null || c.id in m.done) return out
        val idx = Data.semIndex
        (Data.PRE[c.id] ?: emptyList()).forEach { p ->
            val pk = m.plan[p]
            val ok = p in m.done || (pk != null && (idx[pk] ?: 0) < (idx[semKey] ?: 0))
            if (!ok) out.add(Issue("חסר קדם: " + (Data.byId(p)?.name ?: p), 2))
        }
        if (semKey.endsWith("אלול") && "אלול" !in c.offer) {
            out.add(Issue("לא מופיע כמתקיים באלול — ודא מול המערכת", 1))
        }
        // קורס שנשאר מאחורי "עכשיו" לא יילמד לעולם, ושעותיו נספרות לסמסטר
        // שכבר נגמר — לכן זו חסימה ולא הערה.
        if ((idx[semKey] ?: 0) < m.curIdx()) {
            out.add(Issue("משובץ בסמסטר שכבר עבר — העבר קדימה או סמן כהושלם", 2))
        }
        return out
    }

    fun card(m: PlannerModel, c: Course, semKey: String?): CardVM {
        // העומס בפועל בסמסטר הזה — באלול הוא נדחס לפחות שבועות ולכן גדול יותר.
        // בלי זה שני כרטיסים של 6 ש׳ יושבים תחת כותרת של 42 ש׳.
        val l = m.loadOf(c, semKey)
        val d = Data.diff(l.total)
        val iss = issuesFor(m, c, semKey)
        val isDone = c.id in m.done
        return CardVM(
            id = c.id,
            name = c.name,
            crTxt = Data.n(c.cr),
            acTxt = Data.n(l.ac),
            hoursTxt = m.hTxt(l.total),
            acEdited = m.isAcEdited(c.id),
            color = d.third,
            diffLabel = d.second,
            isElective = !c.mand,
            isDone = isDone,
            hasIssue = iss.isNotEmpty(),
            issueText = iss.joinToString(" · ") { it.text },
            issueSev = iss.maxOfOrNull { it.sev } ?: 0,
            offerTxt = c.offer.joinToString(" / "),
        )
    }

    /**
     * למה הקורס לא ניתן לשיבוץ אוטומטי — או null אם הוא כן.
     *
     * הכפתור הציג "שבץ ל / אין מקום" ולא עשה כלום בלחיצה: הסטודנט ראה מבוי
     * סתום בלי לדעת שהחסם הוא קדם־דרישה שהוא עצמו לא שיבץ.
     */
    fun placeBlockTxt(m: PlannerModel, id: String): String? {
        if (m.recommend(id) != null) return null
        val c = Data.byId(id) ?: return null
        val missing = (Data.PRE[c.id] ?: emptyList())
            .filter { it !in m.done && m.plan[it] == null }
        return if (missing.isNotEmpty())
            "כדי לשבץ אותו צריך קודם לשבץ: " +
                missing.joinToString(", ") { Data.byId(it)?.name ?: it }
        else "אין סמסטר פנוי שבו הקורס נפתח אחרי קדם־הדרישות שלו."
    }

    /** תווית "שבץ ל…" במדף — הסמסטר שהאלגוריתם ימליץ עליו. */
    fun recShortTxt(m: PlannerModel, id: String): String {
        val k = m.recommend(id) ?: return "אין מקום"
        val s = Data.semByKey(k) ?: return "אין מקום"
        val yr = s.yearTxt.replace("שנה ", "")
        return if (s.type == "אלול") "אלול $yr" else "סמ׳ ${s.type}׳ $yr"
    }

    fun run(m: PlannerModel): Derived {
        val su = m.setup
        val cs = Data.courses
        val plan = m.plan
        val done = m.done
        val showEl = su.showElectives
        val sems = Data.SEMS
        val idx = Data.semIndex
        val budget = m.budgetHours()
        val semIdx = max(0, min(sems.size - 1, m.semIdx))
        val nowIdx = m.curIdx()

        val alertList = ArrayList<Alert>()

        val semData = sems.mapIndexed { sIdx, s ->
            val dist = sIdx - nowIdx
            val list = cs.filter { plan[it.id] == s.key && (showEl || it.mand) }
                .sortedWith(
                    compareBy<Course> { if (it.id in done) 1 else 0 }
                        .thenByDescending { m.loadOf(it, s.key).total },
                )
            val active = list.filter { it.id !in done }
            val isElul = s.type == "אלול"
            val mul = if (isElul) m.elulMul() else 1.0
            // מסוכם מ-active ולא מ-semLoad כדי שהכותרת תסכם בדיוק את הכרטיסים
            // שמתחתיה: כשמסתירים קורסי בחירה, גם המספר הגדול מפסיק לספור אותם.
            val load = active.fold(Load.ZERO) { acc, c -> acc + m.loadOf(c, s.key) }
            val hours = load.total
            val cr = active.sumOf { it.cr }
            val over = if (budget != 0.0) hours / budget else 0.0
            val barColor = when {
                over > Data.OVER_BUDGET -> C.accent
                over > 0.9 -> C.accent600
                else -> C.text
            }
            // ימי הלימוד נגזרים מ-ש״ש, לא משעות הלמידה: מה שקובע כמה ימים אתה
            // בקמפוס הוא המפגשים, לא שיעורי הבית.
            val days = Hours.days(load.ac)
            val cards = list.map { card(m, it, s.key) }
            val warns = ArrayList<Warn>()

            cards.forEach { c ->
                if (!c.hasIssue) return@forEach
                alertList.add(
                    Alert(
                        s.key, s.title, c.id, c.name + " — " + c.issueText,
                        C.accent100, C.accent, dist, c.issueSev,
                    ),
                )
            }

            if (over > Data.OVER_BUDGET) {
                var fix: Course? = null
                var fixScore = Double.NEGATIVE_INFINITY
                active.forEach { c ->
                    val sc = m.loadOf(c, s.key).total - m.descendants(c.id).size * 4
                    if (sc > fixScore) { fixScore = sc; fix = c }
                }
                val f = fix
                val fixDesc = if (f != null) m.descendants(f.id).size else 0
                warns.add(
                    Warn(
                        text = "חריגה של " + m.hTxt(hours - budget) + " ${m.hUnit} מהתקציב השבועי.",
                        color = C.accent,
                        fixId = f?.id ?: "",
                        fixWhy = if (f == null) "" else if (fixDesc > 0)
                            "הקורס הכבד ביותר שחוסם הכי מעט: $fixDesc קורסים תלויים בו."
                        else "הקורס הכבד ביותר שאינו חוסם אף קורס בהמשך.",
                        fixLbl = if (f != null) "דחה את " + f.name else "",
                    ),
                )
                alertList.add(
                    Alert(
                        s.key, s.title, "",
                        "חריגה של " + m.hTxt(hours - budget) + " ${m.hUnit} מהתקציב — כדאי לדחות קורס אחד.",
                        C.accent100, C.accent, dist, 1,
                    ),
                )
            }

            if (list.isNotEmpty() && Hours.STUDY_DAYS - days < su.freeDays) {
                warns.add(
                    Warn(
                        "$days ימי לימוד — פחות מ" + Data.daysTxt(su.freeDays) + " פנוי כפי שהגדרת.",
                        C.neutral500, "", "", "",
                    ),
                )
                alertList.add(
                    Alert(
                        s.key, s.title, "",
                        "$days ימי לימוד בשבוע — פחות ממה שהגדרת.",
                        C.surface, C.neutral600, dist, 0,
                    ),
                )
            }

            // רק קורסים פעילים: קודם נספרו גם קורסים שהושלמו, כך שסמסטר שכולו
            // מאחורי המשתמש הציג "2 קורסים כבדים" לצד תגית ✓ הושלם.
            val heavy = cards.count {
                !it.isDone && (it.diffLabel == "כבד" || it.diffLabel == "קשה מאוד")
            }
            if (heavy >= 2) {
                warns.add(Warn("$heavy קורסים כבדים באותה תקופת מבחנים.", C.neutral500, "", "", ""))
            }

            SemVM(
                key = s.key,
                lbl = if (isElul) "אלול" else "סמסטר ${s.type}׳",
                title = s.title,
                yearTxt = s.yearTxt,
                isElul = isElul,
                elulTag = "מרוכז ×" + Data.n(mul),
                isExtra = s.extra,
                isPast = dist < 0,
                isCurrent = dist == 0,
                dist = dist,
                courses = cards,
                warns = warns,
                empty = list.isEmpty(),
                allDone = list.isNotEmpty() && active.isEmpty(),
                hoursTxt = m.hTxt(hours),
                acTxt = Data.n(load.ac),
                classTxt = m.hTxt(load.classH),
                selfTxt = m.hTxt(load.selfH),
                crTxt = Data.n(cr),
                pct = min(100, (over * 100).roundToInt()),
                barColor = barColor,
                countTxt = if (list.isNotEmpty()) "${list.size} קורסים" else "ריק",
                statusText = if (list.isNotEmpty()) "${(over * 100).roundToInt()}% מהתקציב" else "סמסטר פנוי",
                daysText = if (list.isNotEmpty())
                    "$days ימי לימוד · " + Data.daysTxt(max(0, Hours.STUDY_DAYS - days)) +
                    " פנוי" else "—",
                names = when {
                    active.isNotEmpty() -> active.take(3).joinToString(" · ") { it.name } +
                        (if (active.size > 3) " +${active.size - 3}" else "")
                    // "2 קורסים" לצד "אין קורסים" סתר את עצמו בסמסטר שהושלם
                    list.isNotEmpty() -> "כל הקורסים הושלמו"
                    else -> "אין קורסים"
                },
            )
        }

        val sem = semData.getOrElse(semIdx) { semData[0] }
        val current = semData.getOrElse(nowIdx) { semData[0] }
        // הבא בתור הוא הסמסטר הקרוב שיש בו מה ללמוד, לא בהכרח הסמוך: מי שלא
        // לומד באלול צריך לראות את סמסטר א׳, לא כרטיס ריק.
        val next = semData.drop(nowIdx + 1).firstOrNull { !it.empty && !it.allDone }

        val unplaced = cs.filter { plan[it.id] == null && it.id !in done && (showEl || it.mand) }
            .sortedWith(
                compareByDescending<Course> { it.mand }.thenByDescending { it.cr },
            )
        val q = m.query.trim()
        val filtered = if (q.isEmpty()) unplaced
        else unplaced.filter { it.name.contains(q) || it.id.contains(q) }

        val missMand = unplaced.count { it.mand }
        val doneCourses = cs.filter { it.id in done }
        val planned = cs.filter { plan[it.id] != null && it.id !in done }
        // הסמסטר האחרון שיש בו קורס שעוד צריך ללמוד. קודם נספרו גם קורסים
        // שהושלמו, ולכן "סיום צפוי" יכול היה להיגזר מקורס שכבר מאחור.
        val lastKey = cs.fold(-1) { mx, c ->
            if (c.id in done) mx else {
                val k = plan[c.id]
                if (k != null) max(mx, idx[k] ?: -1) else mx
            }
        }
        val doneCr = doneCourses.sumOf { it.cr }
        val planCr = planned.sumOf { it.cr }
        val shelfCr = unplaced.sumOf { it.cr }
        // מכנה אחוז התואר = מה שהתואר באמת דורש ממך: כל מה שהושלם, כל מה
        // שמתוכנן, ועוד קורסי החובה שעדיין על המדף. קודם נספר כל הקטלוג
        // (142.5 נ״ז), ולכן סטודנט שסיים את כל קורסי החובה ראה 67% בלבד.
        // חושב מ-cs ולא מ-unplaced כדי שסינון "הצג קורסי בחירה" לא יזיז אותו.
        val mandShelfCr = cs.filter { it.mand && plan[it.id] == null && it.id !in done }
            .sumOf { it.cr }
        val totalCr = doneCr + planCr + mandShelfCr

        // קיבוץ המדף לפי הסמסטר המומלץ
        val groupMap = LinkedHashMap<Int, ShelfGroup>()
        filtered.forEach { c ->
            val (sort, title) = Data.recKey(c.id)
            val g = groupMap.getOrPut(sort) { ShelfGroup(sort, title, emptyList()) }
            groupMap[sort] = g.copy(courses = g.courses + card(m, c, null))
        }
        val groups = groupMap.values.sortedBy { it.sort }

        // ── שתי הרשימות האחרות של אותו מסך: מה שמתוכנן, ומה שכבר נלמד ──
        // קודם היו הקורסים שהושלמו מקופלים בתחתית "המדף", מתחת לרשימת מה
        // שלא שובץ. זה גיליון הציונים של הסטודנט — הוא לא נספח.
        fun match(c: Course) = q.isEmpty() || c.name.contains(q) || c.id.contains(q)
        val plannedShown = planned.filter { showEl || it.mand }
        val plannedGroupMap = LinkedHashMap<Int, ShelfGroup>()
        plannedShown.filter(::match).forEach { c ->
            val k = plan[c.id] ?: return@forEach
            val sort = idx[k] ?: 99
            val g = plannedGroupMap.getOrPut(sort) {
                ShelfGroup(sort, Data.semByKey(k)?.title ?: "", emptyList())
            }
            plannedGroupMap[sort] = g.copy(courses = g.courses + card(m, c, k))
        }
        val plannedGroups = plannedGroupMap.values.sortedBy { it.sort }
        val doneFiltered = doneCourses.filter(::match)

        // הכותרת עונה "איפה אני באפליקציה", והשורה מתחתיה "מה המצב כאן" —
        // ולא תקציר של ההגדרות, שהיה קודם השורה הראשונה שהסטודנט קרא בבוקר.
        val titles = mapOf(
            "home" to current.title, "plan" to "מסלול התואר",
            "shelf" to "הקורסים שלי", "me" to "הגדרות",
        )
        val remainCr = totalCr - doneCr
        val subs = mapOf(
            "home" to "הסמסטר הנוכחי · ${current.countTxt} · " +
                "${current.hoursTxt} ${m.hUnit} בשבוע",
            "plan" to "${Data.n(doneCr)} מתוך ${Data.n(totalCr)} נ״ז · " +
                "נשארו ${Data.n(remainCr)}",
            "shelf" to "${doneCourses.size} הושלמו · ${planned.size} מתוכננים · " +
                "${unplaced.size} על המדף",
            "me" to "שנה ${su.startYear}׳ · ${su.jobPercent}% משרה · " +
                "${m.hTxt(budget)} ${m.hUnit} בשבוע",
        )

        val alerts = alertList.sortedByDescending { it.rank }

        return Derived(
            semData = semData,
            sem = sem,
            current = current,
            next = next,
            alerts = alerts,
            // מה שקורה עכשיו, מה שכבר פספסת, ומה שנוגע לסמסטר הבא. שנה ג׳
            // עדיין ברשימה המלאה — היא פשוט לא הדבר שפותחים איתו את הבוקר.
            alertsNow = alerts.filter { it.dist <= 1 },
            unplaced = unplaced,
            shelfGroups = groups,
            shelfEmpty = filtered.isEmpty(),
            shelfEmptyTxt = if (q.isNotEmpty()) "לא נמצא קורס בשם או במספר הזה."
            else "המדף ריק — כל הקורסים משובצים או סומנו כהושלמו.",
            shelfNote = if (missMand > 0)
                "$missMand קורסי חובה עוד לא מתוכננים. לחץ \"שבץ ל\" כדי לשלוח קורס לסמסטר המומלץ, או פתח אותו לבחירת סמסטר."
            else "כל קורסי החובה מתוכננים. כאן נשארו קורסי הבחירה.",
            shelfHasElectives = unplaced.any { !it.mand },
            plannedGroups = plannedGroups,
            plannedCount = plannedShown.size,
            doneCourses = doneCourses,
            doneFiltered = doneFiltered,
            doneCr = doneCr,
            planCr = planCr,
            shelfCr = shelfCr,
            totalCr = totalCr,
            degreePct = if (totalCr > 0) (doneCr / totalCr * 100).roundToInt() else 0,
            // קורסי חובה שעוד על המדף ידחו את הסיום מעבר לסמסטר האחרון המתוכנן.
            finishTxt = if (lastKey < 0) "—"
            else sems[lastKey].title.replace("סמסטר ", "סמ׳ ") +
                if (missMand > 0) " ואילך" else "",
            // כמה סמסטרים נשארו מ"עכשיו" ועד הסמסטר האחרון שיש בו מה ללמוד.
            semsLeft = if (lastKey < 0) 0 else max(0, lastKey - nowIdx + 1),
            budget = budget,
            budgetTxt = m.hTxt(budget),
            unit = m.hUnit,
            screenTitle = titles[m.tab] ?: "",
            screenSub = subs[m.tab] ?: "",
        )
    }
}
