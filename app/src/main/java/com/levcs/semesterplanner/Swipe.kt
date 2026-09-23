package com.levcs.semesterplanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * אזורים שמחזיקים מחוות אופקיות משלהם (הדפדפן, פס הגלילה של הגרף).
 * החלפת הטאבים מוותרת עליהם — המקבילה ל-`.mscroll, [data-hswipe]` במקור.
 */
val LocalHSwipeZones = compositionLocalOf<SnapshotStateMap<Any, Rect>> {
    error("LocalHSwipeZones was not provided")
}

@Composable
fun Modifier.ownsHorizontalDrag(): Modifier {
    val zones = LocalHSwipeZones.current
    val key = remember { Any() }
    DisposableEffect(key) { onDispose { zones.remove(key) } }
    return this.onGloballyPositioned { zones[key] = it.boundsInWindow() }
}

fun SnapshotStateMap<Any, Rect>.containsPoint(p: Offset): Boolean =
    values.any { it.contains(p) }
