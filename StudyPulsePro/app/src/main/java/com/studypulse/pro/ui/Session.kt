package com.studypulse.pro.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studypulse.pro.core.Snap

fun t12(min: Int): String {
    val h = (min / 60) % 24
    val m = min % 60
    val ap = if (h < 12) "AM" else "PM"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, ap)
}

fun dur(ms: Long): String {
    val m = (ms + 59_999) / 60_000
    return if (m >= 60) "${m / 60}h ${"%02d".format(m % 60)}m" else "$m min"
}

fun clock(ms: Long): String {
    val s = (ms + 999) / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val x = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, x) else "%02d:%02d".format(m, x)
}

@Composable
fun SessionView(
    s: Snap,
    focus: Boolean,
    progressInfo: Pair<Int, Int>?,
    onEmergency: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val land = maxWidth > maxHeight
        if (land) {
            Row(
                Modifier.fillMaxSize().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RingBlock(s, focus, onEmergency, Modifier.fillMaxHeight().aspectRatio(1f))
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                ) {
                    Header(s, focus)
                    LectureCard(s)
                    NextBreakCard(s)
                    progressInfo?.let { DayProgress(it) }
                    extra()
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Header(s, focus)
                RingBlock(s, focus, onEmergency, Modifier.fillMaxWidth(.84f).widthIn(max = 420.dp).aspectRatio(1f))
                LectureCard(s)
                if (!focus) NextBreakCard(s)
                progressInfo?.let { DayProgress(it) }
                extra()
            }
        }
    }
}

@Composable
private fun Header(s: Snap, focus: Boolean) {
    val c = s.cur
    val label = when {
        c == null -> "FREE TIME"
        c.isBreak -> "BREAK"
        else -> "STUDY"
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("StudyPulse Pro", color = TextP, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Chip(label, focus)
    }
}

@Composable
private fun RingBlock(s: Snap, focus: Boolean, onEmergency: () -> Unit, m: Modifier) {
    val cur = s.cur
    val prog = if (cur != null) ((s.now - s.curStart).toFloat() / (s.curEnd - s.curStart)).coerceIn(0f, 1f) else 0f
    val remain = (s.boundary - s.now).coerceAtLeast(0)
    val text = clock(remain)
    Ring(prog, m) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(26.dp), contentAlignment = Alignment.Center) {
            val base = maxWidth.value
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text, color = TextP, maxLines = 1, softWrap = false,
                    fontSize = (base * (if (text.length > 5) .15f else .21f)).sp,
                    fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        focus -> "🔒 FOCUS LOCK"
                        cur != null -> "🔓 BREAK"
                        else -> "NEXT SESSION IN"
                    },
                    color = if (focus) Yellow else TextS, fontSize = (base * .06f).sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.pointerInput(focus) {
                        if (focus) detectTapGestures(onLongPress = { onEmergency() })
                    }
                )
            }
        }
    }
}

@Composable
private fun LectureCard(s: Snap) {
    val c = s.cur
    Glass(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (c != null) {
                Text(if (c.isBreak) "BREAK" else "CURRENT LECTURE", color = TextS, fontSize = 12.sp, letterSpacing = 1.5.sp)
                Text(c.title, color = TextP, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text("${t12(c.startMin)} – ${t12(c.endMin)}", color = TextS)
                Text("${dur(s.curEnd - s.now)} remaining", color = Yellow, fontWeight = FontWeight.Medium)
                val n = s.next
                if (c.isBreak && n != null) Text("Up next: ${n.title} · ${t12(n.startMin)}", color = TextS)
            } else {
                val n = s.next
                Text("NEXT SESSION", color = TextS, fontSize = 12.sp, letterSpacing = 1.5.sp)
                Text(n?.title ?: "No timetable", color = TextP, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                if (n != null) Text("Starts ${t12(n.startMin)}", color = Yellow, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun NextBreakCard(s: Snap) {
    val nb = s.nextBreak ?: return
    val c = s.cur
    if (c != null && c.isBreak) return
    Glass(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("NEXT BREAK", color = TextS, fontSize = 12.sp, letterSpacing = 1.5.sp)
            Text("${nb.title} · ${t12(nb.startMin)}", color = TextP, fontWeight = FontWeight.Medium)
            Text("in ${dur(s.nextBreakStart - s.now)}", color = Yellow, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DayProgress(p: Pair<Int, Int>) {
    Glass(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Today · ${p.first} of ${p.second} sessions done", color = TextP, fontWeight = FontWeight.Medium)
            LinearProgressIndicator(
                progress = { if (p.second == 0) 0f else p.first.toFloat() / p.second },
                modifier = Modifier.fillMaxWidth(), color = Yellow, trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = .1f)
            )
        }
    }
}
