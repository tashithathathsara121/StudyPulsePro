package com.studypulse.pro.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.studypulse.pro.MainActivity
import com.studypulse.pro.R

object Notif {
    const val LIVE = "live"
    const val ALERT = "alert"

    fun channels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(LIVE, "Session status", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(ALERT, "Session alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null); enableVibration(false)
            }
        )
    }

    private fun open(c: Context, alarm: Boolean): PendingIntent {
        val i = Intent(c, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("alarm", alarm)
        return PendingIntent.getActivity(c, if (alarm) 3 else 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun live(c: Context, s: Snap?): Notification {
        val b = NotificationCompat.Builder(c, LIVE)
            .setSmallIcon(R.drawable.ic_notif)
            .setOngoing(true).setOnlyAlertOnce(true)
            .setContentIntent(open(c, false))
        val cur = s?.cur
        when {
            cur != null && !cur.isBreak -> b.setContentTitle("🔒 ${cur.title}").setContentText("Focus Mode · calls allowed")
            cur != null -> b.setContentTitle("☕ Break").setContentText("Next: ${s?.next?.title ?: "—"}")
            s != null -> b.setContentTitle("Free time").setContentText("Next: ${s?.next?.title ?: "—"}")
            else -> b.setContentTitle("StudyPulse Pro").setContentText("Syncing timetable…")
        }
        if (s != null && s.boundary != Long.MAX_VALUE) {
            b.setUsesChronometer(true).setChronometerCountDown(true).setWhen(s.boundary).setShowWhen(true)
        }
        return b.build()
    }

    fun alert(c: Context, title: String, text: String) {
        val pi = open(c, true)
        val n = NotificationCompat.Builder(c, ALERT)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title).setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi).setAutoCancel(true).build()
        c.getSystemService(NotificationManager::class.java).notify(2, n)

        runCatching {
            c.startActivity(
                Intent(c, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("alarm", true)
            )
        }
    }
}
