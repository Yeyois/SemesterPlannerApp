package com.levcs.semesterplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val m: PlannerModel = viewModel(
                factory = object : Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        PlannerModel(applicationContext) as T
                },
            )
            // האפליקציה כולה בעברית — כיוון מימין לשמאל.
            val zones = remember { mutableStateMapOf<Any, Rect>() }
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalHSwipeZones provides zones,
            ) {
                App(m)
            }
        }
    }
}

private val TABORDER = listOf("home", "plan", "shelf", "me")

@Composable
fun App(m: PlannerModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val bottomInset = WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding().value.toInt()
    val topInset = WindowInsets.statusBars.asPaddingValues()
        .calculateTopPadding().value.toInt()

    // המכשיר עשוי להחליף ערכה תוך כדי ריצה; uiMode ב-configChanges מונע יצירה מחדש
    val systemDark = isSystemInDarkTheme()
    SideEffect { C.systemDark = systemDark }

    // אייקוני שורת הסטטוס וסרגל הניווט מתהפכים יחד עם הערכה
    val view = LocalView.current
    SideEffect {
        val window = (view.context as android.app.Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !C.isDark
            isAppearanceLightNavigationBars = !C.isDark
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(C.ground),
    ) {
        if (m.onboard.active) {
            Box(Modifier.fillMaxSize().padding(top = topInset.dp)) {
                OnboardingScreen(m, bottomInset)
            }
            return@Box
        }

        // Escape בגרסת הווב — כאן כפתור החזרה סוגר את השכבה העליונה בלבד.
        val overlay = m.confirmModal != null || m.pickerOpen || m.alertsOpen ||
            m.depSheetId != null || m.moveId != null || m.sheetId != null
        BackHandler(enabled = overlay) {
            when {
                m.confirmModal != null -> m.confirmModal = null
                m.pickerOpen -> m.pickerOpen = false
                m.alertsOpen -> m.alertsOpen = false
                m.depSheetId != null -> m.depSheetId = null
                m.moveId != null -> m.moveId = null
                m.sheetId != null -> m.sheetId = null
            }
        }

        val d = Compute.run(m)

        Column(Modifier.fillMaxSize()) {
            Header(m, d, topInset)

            // אזור התוכן — הפאנל נגרר עם האצבע ומתחלף בשחרור, כמו במקור.
            val density = LocalDensity.current
            val commitPx = with(density) { 60.dp.toPx() }
            // הגרירה מתעדכנת סינכרונית; האנימציה מתחילה רק בשחרור
            var dragX by remember { mutableFloatStateOf(0f) }
            var animX by remember { mutableStateOf<Float?>(null) }
            var dragDir by remember { mutableIntStateOf(0) }
            var panelW by remember { mutableFloatStateOf(1f) }
            var swipeCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
            val zones = LocalHSwipeZones.current
            val scope = rememberCoroutineScope()
            val shownX = animX ?: dragX

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { panelW = it.size.width.toFloat() }
                    .onGloballyPositioned { swipeCoords = it }
                    .pointerInput(m.tab) {
                        // זיהוי במעבר Initial כדי לקדום לגלילה האנכית של התוכן,
                        // אבל תופסים רק כשהתנועה אופקית מובהקת — בדיוק כמו במקור.
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                            val startsInZone = swipeCoords?.let { c ->
                                zones.containsPoint(c.localToWindow(down.position))
                            } ?: false
                            if (startsInZone) return@awaitEachGesture

                            val slop = viewConfiguration.touchSlop
                            var claimed = false
                            var abandoned = false
                            dragX = 0f
                            dragDir = 0
                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Initial)
                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                val totalX = ch.position.x - down.position.x
                                val totalY = ch.position.y - down.position.y
                                if (!claimed && !abandoned) {
                                    if (abs(totalX) > slop && abs(totalX) > abs(totalY) * 1.3f) {
                                        claimed = true
                                    } else if (abs(totalY) > slop) {
                                        abandoned = true
                                    }
                                }
                                if (claimed) {
                                    val amount = ch.positionChange().x
                                    val dir = if (dragX + amount > 0) 1 else -1
                                    val hasTarget = TABORDER
                                        .getOrNull(TABORDER.indexOf(m.tab) + dir) != null
                                    dragX += if (hasTarget) amount else amount * 0.3f
                                    dragDir = dir
                                    ch.consume()
                                }
                                if (abandoned) break
                            }

                            if (!claimed) {
                                dragX = 0f
                                dragDir = 0
                                return@awaitEachGesture
                            }
                            val d = dragX
                            val dir = if (d > 0) 1 else -1
                            val tgt = TABORDER.getOrNull(TABORDER.indexOf(m.tab) + dir)
                            val commit = tgt != null && abs(d) > commitPx
                            val end = if (commit) dir * panelW else 0f
                            scope.launch {
                                animate(d, end, animationSpec = tween(300)) { v, _ -> animX = v }
                                if (commit && tgt != null) {
                                    m.tab = tgt
                                    m.sheetId = null
                                }
                                dragX = 0f
                                animX = null
                                dragDir = 0
                            }
                        }
                    },
            ) {
                val cur = TABORDER.indexOf(m.tab)
                val target = if (shownX != 0f) TABORDER.getOrNull(cur + dragDir) else null

                if (target != null) {
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            translationX = (if (dragDir > 0) -panelW else panelW) + shownX
                        },
                    ) { TabContent(m, d, target, ctx) }
                }
                Box(
                    Modifier.fillMaxSize().graphicsLayer { translationX = shownX },
                ) { TabContent(m, d, m.tab, ctx) }
            }
        }

        BottomNav(m, bottomInset, Modifier.align(Alignment.BottomCenter))

        if (m.toast.isNotEmpty()) {
            Toast(m, bottomInset, Modifier.align(Alignment.BottomCenter))
        }

        if (m.sheetId != null) CourseSheet(m)
        if (m.depSheetId != null) DepSheet(m)
        if (m.moveId != null) MoveSheet(m)
        if (m.alertsOpen) AlertsSheet(m, d)
        if (m.pickerOpen) PickerSheet(m, d)
        m.confirmModal?.let { ConfirmDialog(it) { m.confirmModal = null } }
    }
}

@Composable
private fun TabContent(m: PlannerModel, d: Derived, key: String, ctx: android.content.Context) {
    when (key) {
        "home" -> TabHome(m, d)
        "plan" -> TabPlan(m, d)
        "shelf" -> TabShelf(m, d)
        else -> TabSettings(m, d) { kind -> Export.run(ctx, m, kind) }
    }
}

@Composable
private fun Header(m: PlannerModel, d: Derived, topInset: Int) {
    val shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(C.surface, shape)
            .clip(shape)
            .padding(top = topInset.dp)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("מדעי המחשב · מכון לב", style = body(10.0, C.accent700, 700))
            Text(d.screenTitle, style = heading(24.0, lineHeight = 26.4))
            Text(d.screenSub, style = body(12.0, C.neutral600), maxLines = 1)
        }
        // תג ההתראות יושב בפינת ה-inline-start של הפעמון (ימין ב-RTL).
        // כופים LTR מקומי כדי שהמיקום יהיה מוחלט ולא יתהפך.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    Modifier
                        .size(44.dp)
                        .card(
                            RoundedCornerShape(14.dp),
                            if (d.alerts.isNotEmpty()) C.accent100 else C.surface,
                        )
                        .clickable {
                            m.alertsOpen = true
                            m.sheetId = null; m.moveId = null
                            m.depSheetId = null; m.pickerOpen = false
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BellIcon(if (d.alerts.isNotEmpty()) C.accent700 else C.text)
                }
                if (d.alerts.isNotEmpty()) {
                    Box(
                        Modifier
                            .absoluteOffset(x = 4.dp, y = (-4).dp)
                            .heightIn(min = 18.dp)
                            .widthIn(min = 18.dp)
                            .background(C.accent, RoundedCornerShape(999.dp))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${d.alerts.size}", style = body(10.0, C.white, 800))
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNav(m: PlannerModel, bottomInset: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            .widthIn(max = 436.dp)
            .card()
            .padding(top = 6.dp, start = 6.dp, end = 6.dp, bottom = (6 + bottomInset).dp),
    ) {
        NavItem(m, "home", "עכשיו", Modifier.weight(1f)) { HomeIcon(it) }
        NavItem(m, "plan", "מסלול", Modifier.weight(1f)) { CalendarIcon(it) }
        NavItem(m, "shelf", "קורסים", Modifier.weight(1f)) { ShelfIcon(it) }
        NavItem(m, "me", "הגדרות", Modifier.weight(1f)) { PersonIcon(it) }
    }
}

@Composable
private fun NavItem(
    m: PlannerModel,
    key: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit,
) {
    val active = m.tab == key
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .height(52.dp)
            .background(if (active) C.accent100 else Color.Transparent, shape)
            .clip(shape)
            .clickable { m.tab = key; m.sheetId = null },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon(if (active) C.accent700 else C.neutral600)
        Text(
            label,
            style = body(10.5, if (active) C.accent700 else C.neutral600, if (active) 700 else 500),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun Toast(m: PlannerModel, bottomInset: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = (88 + bottomInset).dp)
            .widthIn(max = 420.dp)
            .background(C.neutral900, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            m.toast,
            style = body(12.5, C.neutral100, lineHeight = 18.1),
            modifier = Modifier.weight(1f, fill = false),
        )
        if (m.toastUndo) {
            Box(
                Modifier
                    .height(34.dp)
                    .background(C.neutral100, RoundedCornerShape(999.dp))
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { m.undo() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("בטל", style = body(12.5, C.neutral900, 700))
            }
        }
    }
}
