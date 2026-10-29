package com.studypulse.pro.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studypulse.pro.MainVM
import com.studypulse.pro.core.Lock
import com.studypulse.pro.data.Cfg

@Composable
fun Root(vm: MainVM, act: Activity) {
    val snap by vm.snap.collectAsStateWithLifecycle()
    val cfgN by vm.cfg.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    val total by vm.totalStudy.collectAsStateWithLifecycle()
    val online by vm.online.collectAsStateWithLifecycle()

    val cfg = cfgN ?: run { Liquid {}; return }
    val s = snap
    val emergency = s != null && cfg.emergencyUntil > s.now
    val focus = s?.cur?.isBreak == false && !emergency

    FocusEffects(act, focus, cfg)
    BackHandler(enabled = focus) {  }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var setup by rememberSaveable { mutableStateOf(false) }
    var browser by rememberSaveable { mutableStateOf(false) }
    var askExit by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(focus) { browser = false }

    Liquid {
        when {
            !cfg.onboarded || setup -> SetupScreen(onDone = { vm.finishOnboarding(); setup = false })

            focus && s != null -> Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                SessionView(s, true, null, { askExit = true }) {
                    Glass(Modifier.fillMaxWidth().clickable { browser = true }) {
                        Text("🌐  Study Browser", Modifier.padding(18.dp), color = TextP, fontWeight = FontWeight.Medium)
                    }
                    Text("Emergency exit: press and hold the 🔒 label", color = TextS, fontSize = 12.sp)
                }
                if (browser) {
                    Column(Modifier.fillMaxSize().background(Black)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                            IconButton({ browser = false }) { Icon(Icons.Default.Close, "Close browser", tint = Yellow) }
                        }
                        StudyBrowser(true, cfg.sites, online, Modifier.weight(1f))
                    }
                }
            }

            else -> Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = GlassC.copy(alpha = .85f)) {
                        val items = listOf("Home" to Icons.Default.Home, "Schedule" to Icons.Default.DateRange,
                            "Browser" to Icons.Default.Search, "Settings" to Icons.Default.Settings)
                        items.forEachIndexed { i, (name, icon) ->
                            NavigationBarItem(
                                selected = tab == i, onClick = { tab = i },
                                icon = { Icon(icon, name) }, label = { Text(name) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Black, selectedTextColor = Yellow,
                                    indicatorColor = Yellow, unselectedIconColor = TextS, unselectedTextColor = TextS
                                )
                            )
                        }
                    }
                }
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    when (tab) {
                        0 -> if (s != null) SessionView(s, false, done to total, {}) else
                            Text("Loading timetable…", Modifier.padding(24.dp), color = TextS)
                        1 -> ScheduleScreen(vm)
                        2 -> StudyBrowser(false, cfg.sites, online, Modifier.fillMaxSize())
                        else -> SettingsScreen(vm, cfg) { setup = true }
                    }
                }
            }
        }
    }

    if (askExit && s != null) {
        var txt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { askExit = false },
            containerColor = GlassC,
            title = { Text("Emergency exit", color = TextP) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This releases Focus Lock until the current session ends. Type EXIT to confirm.", color = TextS)
                    OutlinedTextField(txt, { txt = it }, singleLine = true, colors = fieldColors())
                }
            },
            confirmButton = {
                TextButton(enabled = txt.trim().equals("EXIT", true), onClick = { vm.emergencyExit(s.curEnd); askExit = false }) {
                    Text("Release", color = Yellow)
                }
            },
            dismissButton = { TextButton({ askExit = false }) { Text("Cancel", color = TextS) } }
        )
    }
}

@Composable
private fun FocusEffects(act: Activity, focus: Boolean, cfg: Cfg) {
    LaunchedEffect(focus, cfg.kiosk, cfg.pin) {
        val ctrl = WindowCompat.getInsetsController(act.window, act.window.decorView)
        if (focus) {
            ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctrl.hide(WindowInsetsCompat.Type.systemBars())
            Lock.start(act, cfg)
        } else {
            ctrl.show(WindowInsetsCompat.Type.systemBars())
            Lock.stop(act)
            if (Build.VERSION.SDK_INT >= 27) act.setShowWhenLocked(false)
        }
    }
}
