package com.studypulse.pro

import android.app.Application
import com.studypulse.pro.core.Notif

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notif.channels(this)
    }
}
