package com.studypulse.pro

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.studypulse.pro.core.Alarm
import com.studypulse.pro.core.FocusService
import com.studypulse.pro.ui.AppTheme
import com.studypulse.pro.ui.Root

class MainActivity : ComponentActivity() {
    private val vm: MainVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        alarmFlags(intent)
        runCatching { ContextCompat.startForegroundService(this, Intent(this, FocusService::class.java)) }
        setContent { AppTheme { Root(vm, this) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alarmFlags(intent)
    }

    private fun alarmFlags(i: Intent?) {
        if (i?.getBooleanExtra("alarm", false) == true && Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        Alarm.stop()
        return super.dispatchTouchEvent(ev)
    }
}
