package com.studypulse.pro.core

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import com.studypulse.pro.MainActivity
import com.studypulse.pro.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object Controller {
    private val lock = Mutex()
    const val ACTION_TICK = "com.studypulse.pro.TICK"

    suspend fun sync(c: Context): Snap? = lock.withLock {
        val dao = Db.get(c).dao()
        Seed.ifEmpty(dao)
        val slots = dao.slotsNow().map { Slot(it.id, it.title, it.startMin, it.endMin, it.isBreak) }
        if (slots.isEmpty()) return@withLock null
        val now = System.currentTimeMillis()
        val s = Engine.snap(slots, now)

        slots.filter { !it.isBreak && Engine.end(it, s.dayStart) <= now }
            .forEach { dao.complete(Completion(s.dayStart, it.id)) }

        val cur = s.cur
        val study = cur != null && !cur.isBreak
        if (study) Dnd.enter(c) else Dnd.exit(c)

        val key = "${s.dayStart}:${cur?.id}"
        val last = c.ds.data.first()[K.lastKey]
        if (key != last) {
            c.ds.edit { it[K.lastKey] = key }
            if (cur != null && study) {
                Alarm.start(c)
                Notif.alert(c, "📚 ${cur.title}", "Focus Mode started")
            } else if (cur != null) {
                Alarm.buzz(c)
            }
        }
        schedule(c, s)
        s
    }

    private fun schedule(c: Context, s: Snap) {
        if (s.boundary == Long.MAX_VALUE) return
        val am = c.getSystemService(AlarmManager::class.java)
        val fire = PendingIntent.getBroadcast(
            c, 1, Intent(c, AlarmReceiver::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val show = PendingIntent.getActivity(c, 2, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        am.setAlarmClock(AlarmManager.AlarmClockInfo(s.boundary, show), fire)
    }
}
