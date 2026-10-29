package com.studypulse.pro

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studypulse.pro.core.*
import com.studypulse.pro.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainVM(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val dao = Db.get(ctx).dao()

    init { viewModelScope.launch { Seed.ifEmpty(dao) } }

    val slots: StateFlow<List<Slot>> = dao.slots()
        .map { l -> l.map { Slot(it.id, it.title, it.startMin, it.endMin, it.isBreak) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val ticker = flow {
        while (true) {
            val n = System.currentTimeMillis()
            emit(n)
            delay(1005 - n % 1000)
        }
    }

    val snap: StateFlow<Snap?> = combine(slots, ticker) { s, t -> if (s.isEmpty()) null else Engine.snap(s, t) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cfg: StateFlow<Cfg?> = ctx.ds.data.map { p ->
        Cfg(p[K.kiosk] == true, p[K.pin] == true, p[K.onboarded] == true, p[K.emergency] ?: 0L, p[K.sites] ?: DEFAULT_SITES)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val totalStudy: StateFlow<Int> = slots.map { l -> l.count { !it.isBreak } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val done: StateFlow<Int> = combine(
        slots,
        snap.map { it?.dayStart }.filterNotNull().distinctUntilChanged().flatMapLatest { dao.done(it) }
    ) { sl, ids -> sl.count { !it.isBreak && it.id in ids } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val online: StateFlow<Boolean> = callbackFlow {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }
        trySend(cm.activeNetwork != null)
        cm.registerDefaultNetworkCallback(cb)
        awaitClose { cm.unregisterNetworkCallback(cb) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private fun edit(f: (MutablePreferences) -> Unit) = viewModelScope.launch { ctx.ds.edit { f(it) } }

    fun resync() {
        runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, FocusService::class.java)) }
    }

    fun setKiosk(b: Boolean) = edit { it[K.kiosk] = b }
    fun setPin(b: Boolean) = edit { it[K.pin] = b }
    fun finishOnboarding() = edit { it[K.onboarded] = true; resync() }
    fun emergencyExit(until: Long) = edit { it[K.emergency] = until }

    fun addSite(raw: String) {
        val h = raw.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore('/')
        if (h.contains('.')) edit { it[K.sites] = (it[K.sites] ?: DEFAULT_SITES) + h }
    }

    fun removeSite(h: String) = edit { it[K.sites] = (it[K.sites] ?: DEFAULT_SITES) - h }

    fun addSlot(title: String, a: Int, b: Int, isBreak: Boolean): String? {
        if (title.isBlank()) return "Enter a title"
        if (b <= a) return "End must be after start"
        if (slots.value.any { a < it.endMin && b > it.startMin }) return "Overlaps another slot"
        viewModelScope.launch {
            dao.insert(SlotEntity(title = title.trim(), startMin = a, endMin = b, isBreak = isBreak))
            resync()
        }
        return null
    }

    fun deleteSlot(id: Int) = viewModelScope.launch {
        dao.delete(SlotEntity(id = id, title = "", startMin = 0, endMin = 0, isBreak = false))
        resync()
    }

    fun resetSample() = viewModelScope.launch {
        dao.clear(); dao.insertAll(Seed.sample); resync()
    }
}
