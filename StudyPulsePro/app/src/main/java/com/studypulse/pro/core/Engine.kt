package com.studypulse.pro.core

import java.time.Instant
import java.time.ZoneId

data class Slot(val id: Int, val title: String, val startMin: Int, val endMin: Int, val isBreak: Boolean)

data class Snap(
    val now: Long,
    val dayStart: Long,
    val cur: Slot?,
    val curStart: Long,
    val curEnd: Long,
    val next: Slot?,
    val nextStart: Long,
    val nextBreak: Slot?,
    val nextBreakStart: Long,
    val boundary: Long
)

object Engine {
    private const val MIN = 60_000L
    private const val DAY = 86_400_000L

    fun dayStart(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun start(s: Slot, day0: Long) = day0 + s.startMin * MIN
    fun end(s: Slot, day0: Long) = day0 + s.endMin * MIN

    fun snap(slots: List<Slot>, now: Long): Snap {
        val d0 = dayStart(now)
        val sorted = slots.sortedBy { it.startMin }
        val cur = sorted.firstOrNull { now >= start(it, d0) && now < end(it, d0) }
        val upcoming = (0..1).flatMap { d -> sorted.map { it to start(it, d0 + d * DAY) } }
            .filter { it.second > now }
        val next = upcoming.firstOrNull()
        val nb = upcoming.firstOrNull { it.first.isBreak }
        val boundary = if (cur != null) end(cur, d0) else next?.second ?: Long.MAX_VALUE
        return Snap(
            now, d0, cur,
            cur?.let { start(it, d0) } ?: 0L, cur?.let { end(it, d0) } ?: 0L,
            next?.first, next?.second ?: 0L,
            nb?.first, nb?.second ?: 0L,
            boundary
        )
    }
}
