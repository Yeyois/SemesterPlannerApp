package com.levcs.semesterplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** גופן הכותרות של המערכת (Archivo 800). */
fun heading(size: Double, color: Color = C.text, lineHeight: Double = size * 1.2) = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight(800),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    color = color,
)

fun body(
    size: Double,
    color: Color = C.text,
    weight: Int = 400,
    lineHeight: Double = size * 1.45,
) = TextStyle(
    fontFamily = Archivo,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    color = color,
)

/** .card — משטח בצל רך, פינות 18. */
fun Modifier.card(shape: Shape = CardShape, background: Color = C.surface): Modifier =
    this.shadow(
        if (C.isDark) 1.dp else 2.dp,
        shape,
        ambientColor = C.shadow,
        spotColor = C.shadow,
    )
        .background(background, shape)
        .then(if (C.isDark) Modifier.border(1.dp, C.hairline, shape) else Modifier)
        .clip(shape)

@Composable
fun SectLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = body(11.5, C.neutral700, 700),
        letterSpacing = 0.08.em,
        modifier = modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
    )
}

/** .tag — פינות ישרות, גובה קטן. */
@Composable
fun Tag(
    text: String,
    bg: Color = C.neutral100,
    fg: Color = C.neutral800,
    border: Color? = null,
    size: Double = 10.0,
) {
    val shape = RoundedCornerShape(0.dp)
    Box(
        Modifier
            .background(bg, shape)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = body(size, fg), maxLines = 1)
    }
}

@Composable
fun TagNeutral(text: String) = Tag(text, C.neutral100, C.neutral800)

@Composable
fun TagOutline(text: String, color: Color = C.accent) =
    Tag(text, Color.Transparent, color, color)

/** פס התקדמות אופקי עם רקע ומילוי. */
@Composable
fun ProgressBar(
    fraction: Float,
    color: Color,
    track: Color = C.neutral200,
    heightDp: Int = 8,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .background(track, shape)
            .clip(shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(heightDp.dp)
                .background(color, shape),
        )
    }
}

/** כפתור גלולה מלא (הפעולה הראשית). */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = C.accent,
    fg: Color = C.white,
    minHeight: Int = 50,
    fontSize: Double = 14.0,
    weight: Int = 600,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(minHeight.dp)
            .background(bg, shape)
            .clip(shape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = body(fontSize, fg, weight), textAlign = TextAlign.Center)
    }
}

/** כפתור גלולה עם מסגרת בלבד. */
@Composable
fun OutlinePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = C.neutral400,
    fg: Color = C.text,
    minHeight: Int = 50,
    fontSize: Double = 13.5,
    borderWidth: Int = 1,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(minHeight.dp)
            .border(borderWidth.dp, borderColor, shape)
            .clip(shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = body(fontSize, fg, 600), textAlign = TextAlign.Center)
    }
}

/** כפתור גלולה על רקע card (פעולה משנית). */
@Composable
fun CardPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fg: Color = C.text,
    minHeight: Int = 46,
    fontSize: Double = 13.5,
    weight: Int = 600,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(minHeight.dp)
            .card(shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = body(fontSize, fg, weight), textAlign = TextAlign.Center)
    }
}

/** בורר מפולח (segmented) — הפריט הפעיל בצבע הטקסט. */
@Composable
fun SegmentedRow(
    options: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    itemHeight: Int = 40,
    radius: Int = 12,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .card()
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            val shape = RoundedCornerShape(radius.dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(itemHeight.dp)
                    .background(if (active) C.text else Color.Transparent, shape)
                    .clip(shape)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = body(12.5, if (active) C.bg else C.neutral800, 600),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            if (i < options.lastIndex) Box(Modifier.padding(horizontal = 2.dp))
        }
    }
}

/** כרטיס עם פס צבע בקצה — border-inline-start/end בעיצוב המקורי. */
@Composable
fun StripeCard(
    stripe: Color,
    modifier: Modifier = Modifier,
    atEnd: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .card()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .height(IntrinsicSize.Min),
    ) {
        if (!atEnd) Box(Modifier.width(4.dp).fillMaxHeight().background(stripe))
        Column(
            Modifier.weight(1f).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            content = content,
        )
        if (atEnd) Box(Modifier.width(4.dp).fillMaxHeight().background(stripe))
    }
}
