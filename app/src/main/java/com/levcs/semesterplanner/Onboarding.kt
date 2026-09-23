package com.levcs.semesterplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(m: PlannerModel, bottomInset: Int) {
    val ob = m.onboard
    val jobHrs = Data.half(Data.JOB_HOURS_PER_PCT * ob.jobPercent * 2) / 2
    val total = ob.studyHours + jobHrs
    // מה שנשאר מהשבוע אחרי העבודה — התקרה הריאלית לשעות הלימוד
    val freeAfterJob = (Data.WEEK_HOURS - jobHrs).coerceAtLeast(0.0)
    val overcommitted = ob.studyHours > freeAfterJob

    Column(
        Modifier
            .fillMaxSize()
            .background(C.ground),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            if (ob.step >= 1) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    (1..5).forEach { n ->
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(
                                    if (ob.step >= n) C.accent else C.neutral400,
                                    RoundedCornerShape(999.dp),
                                ),
                        )
                    }
                }
            }

            when (ob.step) {
                0 -> Column(
                    Modifier.fillMaxWidth().padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        "מתכנן סמסטרים",
                        style = heading(27.0, lineHeight = 34.0),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "כלי שעוזר לך לבנות מסלול לימודים במדעי המחשב לפי הזמן שבאמת יש לך — עבודה, ימי מנוחה ותקציב שעות שבועי.",
                        style = body(15.0, C.neutral600, lineHeight = 24.8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 320.dp),
                    )
                    PillButton(
                        "בוא נתחיל",
                        { m.onboardStep(1) },
                        Modifier.padding(top = 10.dp).widthIn(max = 240.dp),
                        minHeight = 54,
                        fontSize = 15.0,
                        weight = 700,
                    )
                }

                // שתי הבחירות יחד, כי ביחד הן שאלה אחת: איפה אתה עכשיו. זה
                // העוגן שכל האפליקציה נשענת עליו — מה נחשב עבר ומה עתיד.
                1 -> ObStep("שלום! איפה אתה נמצא עכשיו?") {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("השנה שאתה מתחיל", style = body(12.5, C.neutral600, 700))
                            Data.YEARS.forEach { y ->
                                val on = ob.startYear == y && ob.touchedYear
                                ObChoice("שנה $y׳", on) { m.onboardPickYear(y) }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "והסמסטר שמתחיל עכשיו",
                                style = body(12.5, C.neutral600, 700),
                            )
                            listOf(
                                "אלול" to "אלול",
                                "א" to "סמסטר א׳",
                                "ב" to "סמסטר ב׳",
                            ).forEach { (t, lbl) ->
                                ObChoice(lbl, ob.startSem == t) { m.onboardPickSem(t) }
                            }
                        }
                    }
                }

                2 -> ObStep("כמה אתה עובד?") {
                    Column(
                        Modifier.fillMaxWidth().card().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            if (ob.jobPercent == 0) "לא עובד"
                            else "${ob.jobPercent}% · ${Data.n(jobHrs)} שעות עבודה",
                            style = body(15.0, weight = 600),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AccentSlider(
                            value = ob.jobPercent.toFloat(),
                            range = 0f..100f,
                            steps = 19,
                        ) { m.onboard = ob.copy(jobPercent = it.roundToInt()) }
                    }
                }

                3 -> ObStep("כמה שעות בשבוע תרצה להקדיש ללימוד עצמו?") {
                    val netFreeTime = freeAfterJob - ob.studyHours.toDouble()

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = if (jobHrs > 0) {
                                "בשבוע רגיל יש כ־${Data.n(Data.WEEK_HOURS)} שעות פנויות (א׳–ו׳, שישי קצר) לפני עבודה. " +
                                        "אחרי שקיזזנו ${Data.n(jobHrs)} שעות עבודה, נותרו לך ${Data.n(freeAfterJob)} שעות פנויות.\n" +
                                        "סמסטר מלא במסלול עולה לרוב 32–44 ש׳ בשבוע (כיתה ולמידה עצמית יחד)."
                            } else {
                                "בשבוע רגיל יש כ־${Data.n(Data.WEEK_HOURS)} שעות פנויות (א׳–ו׳, שישי קצר).\n" +
                                        "סמסטר מלא במסלול עולה לרוב 32–44 ש׳ בשבוע (כיתה ולמידה עצמית יחד)."
                            },
                            style = body(12.5, C.neutral600, lineHeight = 18.75),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Column(
                            Modifier.fillMaxWidth().card().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                "${Data.n(ob.studyHours.toDouble())} ש׳ למידה בשבוע",
                                style = body(15.0, weight = 600),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            AccentSlider(
                                value = ob.studyHours.toFloat(),
                                range = Data.STUDY_MIN.toFloat()..Data.STUDY_MAX.toFloat(),
                                steps = (Data.STUDY_MAX - Data.STUDY_MIN).toInt() - 1,
                            ) { m.onboard = ob.copy(studyHours = it.roundToInt()) }
                            Text(
                                if (ob.jobPercent == 0) "עומס שבועי מתוכנן: ${Data.n(total.toDouble())} ש׳"
                                else "עומס שבועי כולל: ${Data.n(total.toDouble())} ש׳ (מתוכן ${Data.n(jobHrs)} ש׳ עבודה)",
                                style = body(12.0, C.accent700),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                if (overcommitted)
                                    "העומס שיצרת חורג ב־${Data.n(kotlin.math.abs(netFreeTime))} ש׳ מסך הזמן הפנוי שלך בשבוע."
                                else
                                    "זמן פנוי שנותר: ${Data.n(netFreeTime)} ש׳ בשבוע (מעבר ללימודים ולעבודה).",
                                style = body(11.5, if (overcommitted) C.accent else C.neutral600, lineHeight = 17.25),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                4 -> ObStep("כמה ימים פנויים (בלי לימודים) אתה רוצה בשבוע?") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(0, 1, 2, 3).forEach { d ->
                            val on = ob.freeDays == d && ob.touchedDays
                            ObChoice(
                                if (d == 0) "ללא ימי מנוחה" else Data.daysTxt(d) + " מנוחה",
                                on,
                            ) { m.onboardPickDays(d) }
                        }
                    }
                }

                else -> Column(
                    Modifier.fillMaxWidth().padding(top = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column {
                        Text(
                            "לשבץ קורסי בחירה מומלצים אוטומטית, כל אחד לפי הסמסטר המומלץ שלו?",
                            style = heading(23.0, lineHeight = 32.2),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "אפשר לשנות את זה בכל שלב מתוך \"פעולות מהירות\".",
                            style = body(13.0, C.neutral600, lineHeight = 19.5),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PillButton(
                            "כן, שבץ לי",
                            { m.onboardFinish(true) },
                            minHeight = 58,
                            fontSize = 16.0,
                            weight = 700,
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .card()
                                .clickable { m.onboardFinish(false) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("לא, אני אעשה זאת בעצמי", style = body(16.0, C.text, 700))
                        }
                    }
                }
            }
        }

        if (ob.step >= 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(C.bg)
                    .padding(
                        start = 22.dp, end = 22.dp, top = 12.dp,
                        bottom = (16 + bottomInset).dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val backMod = if (ob.step == 5) Modifier.weight(1f) else Modifier.width(96.dp)
                OutlinePillButton(
                    "חזור",
                    { m.onboardBack() },
                    backMod,
                    minHeight = 54,
                    fontSize = 14.0,
                )
                if (ob.step in 1..4) {
                    val disabled = (ob.step == 1 && !ob.touchedYear) ||
                        (ob.step == 4 && !ob.touchedDays)
                    PillButton(
                        "הבא",
                        { if (!disabled) m.onboardNext() },
                        Modifier.weight(1f),
                        bg = if (disabled) C.neutral400 else C.accent,
                        minHeight = 54,
                        fontSize = 15.0,
                        weight = 700,
                        enabled = !disabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun ObStep(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 30.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            title,
            style = heading(23.0, lineHeight = 32.2),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        content()
    }
}

@Composable
private fun ObChoice(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .card(background = if (active) C.text else C.surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = body(16.0, if (active) C.bg else C.text, 700))
    }
}

/**
 * הסליידר מכריז על עצמו כאזור שמחזיק מחוות אופקיות.
 *
 * החלפת הטאבים ב-MainActivity מאזינה במעבר Initial — לפני הצאצאים — ותופסת
 * כל גרירה אופקית מובהקת. גרירת סליידר היא בדיוק כזו, ולכן היא הזיזה את
 * העמוד במקום את הידית. LocalHSwipeZones נועד לזה מלכתחילה (המקבילה ל-
 * `[data-hswipe]` באב-טיפוס, שם ל-input type=range יש בעלות מובנית על
 * הגרירה), אבל הוא הוחל רק על הקרוסלה ועל גרף התלויות — לא על הסליידרים.
 */
@Composable
fun AccentSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    /**
     * צבע המסילה. אדום הוא צבע האזעקה של האפליקציה, ושני סליידרים אדומים
     * זה מעל זה בהגדרות אמרו "שים לב" על שתי בחירות שגרתיות — ואז לא נשאר
     * במה לסמן חריגה אמיתית. רק מה שקובע את התקציב נשאר אדום.
     */
    track: Color = C.neutral700,
    onChange: (Float) -> Unit,
) {
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = track,
            activeTrackColor = track,
            inactiveTrackColor = C.neutral400,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().ownsHorizontalDrag(),
    )
}
