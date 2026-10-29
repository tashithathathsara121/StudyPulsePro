package com.studypulse.pro.core

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.telecom.TelecomManager
import com.studypulse.pro.data.Cfg

object Lock {
    private fun dpm(c: Context) = c.getSystemService(DevicePolicyManager::class.java)

    fun isOwner(c: Context) = dpm(c).isDeviceOwnerApp(c.packageName)

    private fun prepareKiosk(c: Context) {
        val admin = ComponentName(c, AdminReceiver::class.java)
        val pkgs = mutableListOf(c.packageName)

        runCatching { c.getSystemService(TelecomManager::class.java).defaultDialerPackage }
            .getOrNull()?.let { pkgs += it }
        dpm(c).setLockTaskPackages(admin, pkgs.toTypedArray())
        if (Build.VERSION.SDK_INT >= 28) {
            dpm(c).setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS)
        }
    }

    fun start(a: Activity, cfg: Cfg) {
        runCatching {
            if (cfg.kiosk && isOwner(a)) {
                prepareKiosk(a)
                a.startLockTask()
            } else if (cfg.pin) {
                a.startLockTask()
            }
        }
    }

    fun stop(a: Activity) {
        runCatching { a.stopLockTask() }
    }
}
