package com.levcs.semesterplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * אסימוני הצבע של Modernist. הערכים הבהירים הועתקו מ-_ds/…/styles.css;
 * הכהים נגזרו לפי הכללים שהמערכת עצמה מתעדת ב-readme.md — סולם הנייטרל
 * מתהפך (מילויים בצעדים הנמוכים, טקסט בגבוהים) והאקסנט עולה צעד על רקע כהה.
 *
 * ערכת הנושא נגזרת ממצב Compose, ולכן כל קריאה לאסימון בתוך composable
 * נרשמת ומחילה הרכבה מחדש בהחלפתה — כולל בקוד הנגזר (Compute/Data).
 */
object C {

    /** מצב המכשיר, ובחירה מפורשת של המשתמש שגוברת עליו (null = לפי המכשיר). */
    var systemDark by mutableStateOf(false)
    var override by mutableStateOf<Boolean?>(null)

    val isDark: Boolean get() = override ?: systemDark

    private fun t(light: Long, dark: Long) = Color(if (isDark) dark else light)

    /** הרקע שמאחורי הכול. בהיר: neutral-300 כבמקור; כהה: דיו כמעט שחור. */
    val ground: Color get() = t(0xFFD7D3D3, 0xFF121110)

    val bg: Color get() = t(0xFFF3F2F2, 0xFF2A2827)
    val surface: Color get() = t(0xFFEAE9E9, 0xFF201E1D)
    val text: Color get() = t(0xFF201E1D, 0xFFF3F2F2)

    /** בסיס האקסנט זהה בשתי הערכות — יחס הניגודיות שלו נשמר משני הצדדים. */
    val accent: Color get() = Color(0xFFEC3013)

    // סולם הנייטרל — בכהה הוא מתהפך: 100 הכי כהה (מילוי), 900 הכי בהיר (טקסט)
    val neutral100: Color get() = t(0xFFF8F4F4, 0xFF2D2B2B)
    val neutral200: Color get() = t(0xFFEAE7E7, 0xFF444141)
    val neutral300: Color get() = t(0xFFD7D3D3, 0xFF605D5D)
    val neutral400: Color get() = t(0xFFBAB6B6, 0xFF7D7979)
    val neutral500: Color get() = t(0xFF9B9797, 0xFF9B9797)
    val neutral600: Color get() = t(0xFF7D7979, 0xFFBAB6B6)
    val neutral700: Color get() = t(0xFF605D5D, 0xFFD7D3D3)
    val neutral800: Color get() = t(0xFF444141, 0xFFEAE7E7)
    val neutral900: Color get() = t(0xFF2D2B2B, 0xFFF8F4F4)

    // סולם האקסנט — מילוי מתכהה, וטקסט-על-מילוי מתבהר
    val accent100: Color get() = t(0xFFFFF2EF, 0xFF3A1A13)
    val accent300: Color get() = t(0xFFFFC4B8, 0xFF8C2A18)
    val accent600: Color get() = t(0xFFDD2B0F, 0xFFFF7A63)
    val accent700: Color get() = t(0xFFAE1800, 0xFFFF9783)
    val accent800: Color get() = t(0xFF7C1405, 0xFFFFC4B8)

    val white = Color(0xFFFFFFFF)
    val scrim: Color get() = t(0x75201E1D, 0x9E000000)

    /** צל תמיד דיו — לא מתהפך עם הסולם. */
    val shadow: Color get() = t(0xFF2D2B2B, 0xFF000000)

    /** קו שיער לקצה הכרטיס; המערכת מבקשת אותו במקום צל רך על רקע כהה. */
    val hairline: Color get() = Color(0xFF35312F)

    // צבעי מפת התלויות (DEPCOL) — מתבהרים כדי להישאר קריאים על כהה
    val depOk: Color get() = t(0xFF1F8A4C, 0xFF41BE77)
    val depWarn: Color get() = t(0xFFC8890C, 0xFFE0A82E)
    val depBlocked: Color get() = accent
    val depDone: Color get() = neutral500
    val depUnplaced: Color get() = neutral400
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun archivoWeight(w: Int) =
    Font(
        R.font.archivo_variable,
        FontWeight(w),
        variationSettings = FontVariation.Settings(FontVariation.weight(w)),
    )

/** Archivo כמו בעיצוב; לעברית המערכת נופלת חזרה לגופן ברירת המחדל. */
val Archivo = FontFamily(
    archivoWeight(400),
    archivoWeight(600),
    archivoWeight(800),
)

/** .card — משטח עם פינות 18 וצל רך. */
val CardShape = RoundedCornerShape(18.dp)

@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = CardShape,
    background: Color = C.surface,
    content: @Composable () -> Unit,
) {
    Box(modifier.background(background, shape)) { content() }
}
