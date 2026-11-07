package com.codesprint.ide.core

import java.time.ZoneId

data class DayRecord(
    /** Total completed focus time on the day. */
    var focusMs: Long,
    /** Number of completed focus sessions on the day. */
    var sessions: Int,
)

/** Aggregated productivity history. Only completed sessions are ever added. */
class History(
    var totalSessions: Int = 0,
    /** Per-day aggregates (focus sessions only), keyed by local day. */
    val days: MutableMap<String, DayRecord> = linkedMapOf(),
    /** Most recent completed sessions (focus and breaks). */
    val recent: MutableList<CompletedSession> = mutableListOf(),
) {
    /**
     * Records a completed session. Returns false (and changes nothing) when the session
     * id was already recorded, which makes recording idempotent.
     */
    fun addSession(session: CompletedSession, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (recent.any { it.id == session.id }) return false
        recent.add(session)
        if (recent.size > MAX_RECENT_SESSIONS) {
            recent.subList(0, recent.size - MAX_RECENT_SESSIONS).clear()
        }
        if (session.mode == SessionMode.FOCUS) {
            val key = TimeUtil.dayKey(session.endedAt, zone)
            val day = days.getOrPut(key) { DayRecord(0, 0) }
            day.focusMs += session.plannedMs
            day.sessions += 1
            totalSessions += 1
            if (days.size > MAX_DAYS) {
                days.keys.sorted().take(days.size - MAX_DAYS).forEach { days.remove(it) }
            }
        }
        return true
    }

    companion object {
        const val MAX_RECENT_SESSIONS = 200
        const val MAX_DAYS = 1100
    }
}
