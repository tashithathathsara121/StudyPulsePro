package com.studypulse.pro.core

import android.app.NotificationManager
import android.app.NotificationManager.Policy
import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import com.studypulse.pro.data.K
import com.studypulse.pro.data.ds
import kotlinx.coroutines.flow.first

object Dnd {
    private fun nm(c: Context) = c.getSystemService(NotificationManager::class.java)

    fun hasAccess(c: Context) = nm(c).isNotificationPolicyAccessGranted

    suspend fun enter(c: Context) {
        val n = nm(c)
        if (!n.isNotificationPolicyAccessGranted) return
        val p = c.ds.data.first()
        if (p[K.savedValid] != true) {
            val pol = n.notificationPolicy
            c.ds.edit {
                it[K.savedValid] = true
                it[K.savedFilter] = n.currentInterruptionFilter
                it[K.savedCats] = pol.priorityCategories
                it[K.savedCall] = pol.priorityCallSenders
                it[K.savedMsg] = pol.priorityMessageSenders
            }
        }
        var cats = Policy.PRIORITY_CATEGORY_CALLS or Policy.PRIORITY_CATEGORY_REPEAT_CALLERS
        if (Build.VERSION.SDK_INT >= 28) {

            cats = cats or Policy.PRIORITY_CATEGORY_ALARMS or Policy.PRIORITY_CATEGORY_MEDIA
        }
        n.notificationPolicy = Policy(cats, Policy.PRIORITY_SENDERS_ANY, Policy.PRIORITY_SENDERS_ANY)
        n.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    suspend fun exit(c: Context) {
        val n = nm(c)
        val p = c.ds.data.first()
        if (p[K.savedValid] != true) return
        if (n.isNotificationPolicyAccessGranted) {
            n.notificationPolicy = Policy(
                p[K.savedCats] ?: 0,
                p[K.savedCall] ?: Policy.PRIORITY_SENDERS_ANY,
                p[K.savedMsg] ?: Policy.PRIORITY_SENDERS_ANY
            )
            val f = p[K.savedFilter] ?: NotificationManager.INTERRUPTION_FILTER_ALL
            n.setInterruptionFilter(
                if (f == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) NotificationManager.INTERRUPTION_FILTER_ALL else f
            )
        }
        c.ds.edit { it[K.savedValid] = false }
    }
}
