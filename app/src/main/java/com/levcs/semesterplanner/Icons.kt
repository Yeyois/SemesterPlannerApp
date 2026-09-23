package com.levcs.semesterplanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.dp

/** שורת תגיות שנשברת לשורה הבאה. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) { content() }
}

@Composable
fun IconBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier.clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { icon() }
}

/** מציירת נתיבי SVG מ-viewBox 24×24 בקו במשקל נתון (כמו Lucide בעיצוב). */
@Composable
fun SvgIcon(
    paths: List<String>,
    color: Color,
    sizeDp: Int = 20,
    strokeWidth: Float = 1.8f,
) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val scale = size.minDimension / 24f
        paths.forEach { spec ->
            val p = PathParser().parsePathString(spec).toPath()
            val m = androidx.compose.ui.graphics.Matrix()
            m.scale(scale, scale)
            p.transform(m)
            drawPath(
                p,
                color = color,
                style = Stroke(
                    width = strokeWidth * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

@Composable
fun BellIcon(color: Color, sizeDp: Int = 19) = SvgIcon(
    listOf(
        "M10.3 21a1.94 1.94 0 0 0 3.4 0",
        "M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9",
    ),
    color, sizeDp, 1.9f,
)

@Composable
fun CalendarCheckIcon(color: Color, sizeDp: Int = 18) = SvgIcon(
    listOf(
        "M5 4h14a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        "M3 9h18M8 2v4M16 2v4",
        "m9.5 13.5 2 2 3-3.5",
    ),
    color, sizeDp, 1.9f,
)

@Composable
fun CheckIcon(color: Color, sizeDp: Int = 18) =
    SvgIcon(listOf("M20 6 9 17l-5-5"), color, sizeDp, 2.1f)

@Composable
fun HomeIcon(color: Color) = SvgIcon(
    listOf("M3 10.5 12 3l9 7.5", "M5 9.8V21h14V9.8"),
    color, 20, 1.8f,
)

@Composable
fun CalendarIcon(color: Color) = SvgIcon(
    listOf(
        "M5 4h14a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        "M3 9h18M8 2v4M16 2v4",
    ),
    color, 20, 1.8f,
)

@Composable
fun ShelfIcon(color: Color) = SvgIcon(
    listOf("M3 4h18v6H3zM3 14h18v6H3z"),
    color, 20, 1.8f,
)

@Composable
fun PersonIcon(color: Color) = SvgIcon(
    listOf(
        "M12 4a4 4 0 1 1 0 8 4 4 0 0 1 0-8z",
        "M4 21c0-4 3.6-6 8-6s8 2 8 6",
    ),
    color, 20, 1.8f,
)

@Composable
fun ArrowDownIcon(color: Color, sizeDp: Int = 16) =
    SvgIcon(listOf("M12 5v14M19 12l-7 7-7-7"), color, sizeDp, 2f)
