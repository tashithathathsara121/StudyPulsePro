package com.studypulse.pro.ui

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studypulse.pro.MainVM
import com.studypulse.pro.core.Dnd
import com.studypulse.pro.core.Lock
import com.studypulse.pro.data.Cfg

private fun parseTime(t: String): Int? {
    val m = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(t.trim()) ?: return null
    val v = m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
    return if (m.groupValues[2].toInt() < 60 && v <= 1440) v else null
}

@Composable
fun ScheduleScreen(vm: MainVM) {
    val slots by vm.slots.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Schedule", color = TextP, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                IconButton({ adding = true }) { Icon(Icons.Default.Add, "Add slot", tint = Yellow) }
            }
        }
        items(slots, key = { it.id }) { s ->
            Glass(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.title, color = TextP, fontWeight = FontWeight.Medium)
                        Text("${t12(s.startMin)} – ${t12(s.endMin)}", color = TextS, fontSize = 13.sp)
                    }
                    Text(if (s.isBreak) "BREAK" else "STUDY", color = if (s.isBreak) TextS else Yellow, fontSize = 11.sp, letterSpacing = 1.sp)
                    IconButton({ vm.deleteSlot(s.id) }) { Icon(Icons.Default.Delete, "Delete", tint = TextS) }
                }
            }
        }
        item {
            TextButton({ vm.resetSample() }) { Text("Reset to sample timetable", color = TextS) }
        }
    }
    if (adding) AddSlotDialog(vm) { adding = false }
}

@Composable
private fun AddSlotDialog(vm: MainVM, onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    var isBreak by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onClose, containerColor = GlassC,
        title = { Text("Add slot", color = TextP) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, colors = fieldColors())
                OutlinedTextField(a, { a = it }, label = { Text("Start (24h, e.g. 08:30)") }, singleLine = true, colors = fieldColors())
                OutlinedTextField(b, { b = it }, label = { Text("End (24h, 24:00 = midnight)") }, singleLine = true, colors = fieldColors())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(isBreak, { isBreak = it }, colors = SwitchDefaults.colors(checkedTrackColor = Yellow, checkedThumbColor = Black))
                    Spacer(Modifier.width(10.dp)); Text("This is a break", color = TextP)
                }
                err?.let { Text(it, color = Yellow) }
            }
        },
        confirmButton = {
            TextButton({
                val x = parseTime(a); val y = parseTime(b)
                err = if (x == null || y == null) "Use HH:mm times" else vm.addSlot(title, x, y, isBreak)
                if (err == null) onClose()
            }) { Text("Add", color = Yellow) }
        },
        dismissButton = { TextButton(onClose) { Text("Cancel", color = TextS) } }
    )
}

@Composable
fun SettingsScreen(vm: MainVM, cfg: Cfg, onSetup: () -> Unit) {
    val ctx = LocalContext.current
    var owner by remember { mutableStateOf(Lock.isOwner(ctx)) }
    var newSite by remember { mutableStateOf("") }
    LazyColumn(
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("Settings", color = TextP, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        item {
            Glass(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SwitchRow(
                        "Managed Kiosk Mode",
                        if (owner) "Device Owner active: Home and Recents locked during study, released on breaks."
                        else "Needs Device Owner provisioning over adb (see README).",
                        cfg.kiosk && owner, owner
                    ) { vm.setKiosk(it) }
                    SwitchRow(
                        "Screen pinning (Standard Focus)",
                        "Asks Android to pin the app during study. Android shows its own confirmation.",
                        cfg.pin, true
                    ) { vm.setPin(it) }
                }
            }
        }
        item { PrimaryButton("Open permission setup", Modifier.fillMaxWidth(), onSetup) }
        item { Text("Approved study sites", color = TextP, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        items(cfg.sites.sorted(), key = { it }) { site ->
            Glass(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(site, Modifier.weight(1f), color = TextP)
                    IconButton({ vm.removeSite(site) }) { Icon(Icons.Default.Delete, "Remove", tint = TextS) }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    newSite, { newSite = it }, Modifier.weight(1f), singleLine = true,
                    label = { Text("Add site, e.g. lms.myuni.edu") }, colors = fieldColors(), shape = RoundedCornerShape(50)
                )
                PrimaryButton("Add") { vm.addSite(newSite); newSite = "" }
            }
        }
        item {
            Text(
                "Approved sites apply during Focus Mode. On breaks the browser is unrestricted.",
                color = TextS, fontSize = 13.sp
            )
        }
        if (owner) item {
            OutlinedButton(
                onClick = {
                    runCatching { ctx.getSystemService(android.app.admin.DevicePolicyManager::class.java).clearDeviceOwnerApp(ctx.packageName) }
                    owner = Lock.isOwner(ctx); vm.setKiosk(false)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Release Device Owner", color = Yellow) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextP, fontWeight = FontWeight.Medium)
            Text(desc, color = TextS, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked, onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Yellow, checkedThumbColor = Black)
        )
    }
}

@Composable
fun SetupScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val pkg = ctx.packageName
    val uri = Uri.parse("package:$pkg")

    fun go(i: Intent) {
        runCatching { ctx.startActivity(i) }.onFailure {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)) }
        }
    }

    val dnd = remember(tick) { Dnd.hasAccess(ctx) }
    val notif = remember(tick) {
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val overlay = remember(tick) { Settings.canDrawOverlays(ctx) }
    val fsi = remember(tick) {
        Build.VERSION.SDK_INT < 34 || ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
    val battery = remember(tick) { ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(pkg) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).safeDrawingPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Setup", color = TextP, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            "Grant these once so sessions start on time and Focus Mode works reliably. Calls are always allowed.",
            color = TextS
        )
        SetupRow("Do Not Disturb access", "Silences distracting notifications (calls and alarms stay on) and restores your old settings afterwards.", dnd) {
            go(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        SetupRow("Notifications", "Shows the live session countdown.", notif) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        SetupRow("Display over other apps", "Lets the next session open itself when the alarm fires.", overlay) {
            go(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))
        }
        SetupRow("Full-screen alerts", "Wakes the screen when a session starts.", fsi) {
            if (Build.VERSION.SDK_INT >= 34) go(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, uri))
        }
        SetupRow("Unrestricted battery", "Keeps alarms accurate with the screen off.", battery) {
            go(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, uri))
        }
        SetupRow("Xiaomi Autostart (manual)", "Redmi/HyperOS: enable Autostart and set Battery saver to No restrictions.", null) {
            go(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
        }
        PrimaryButton("Continue", Modifier.fillMaxWidth(), onDone)
    }
}

@Composable
private fun SetupRow(title: String, desc: String, ok: Boolean?, onClick: () -> Unit) {
    Glass(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextP, fontWeight = FontWeight.Medium)
                Text(desc, color = TextS, fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            if (ok == true) Text("✓ On", color = Yellow, fontWeight = FontWeight.SemiBold)
            else OutlinedButton(onClick, shape = RoundedCornerShape(50)) { Text(if (ok == null) "Open" else "Grant", color = Yellow) }
        }
    }
}
