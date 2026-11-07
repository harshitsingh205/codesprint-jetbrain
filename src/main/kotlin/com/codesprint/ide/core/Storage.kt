package com.codesprint.ide.core

/*
 * Plain, mutable beans that the IDE's XML serializer can read and write, plus the
 * validation that turns untrusted stored data back into domain objects. Nothing here
 * depends on the IntelliJ Platform, so it is covered by ordinary unit tests.
 */

class TimerBean {
    var mode: String = SessionMode.FOCUS.name
    var status: String = TimerStatus.IDLE.name
    var durationMs: Long = 0
    var elapsedMs: Long = 0
    var segmentStartedAt: Long = 0
    var sessionStartedAt: Long = 0
    var focusSinceLongBreak: Int = 0

    companion object {
        fun from(state: TimerState) = TimerBean().also {
            it.mode = state.mode.name
            it.status = state.status.name
            it.durationMs = state.durationMs
            it.elapsedMs = state.elapsedMs
            it.segmentStartedAt = state.segmentStartedAt ?: 0
            it.sessionStartedAt = state.sessionStartedAt ?: 0
            it.focusSinceLongBreak = state.focusSinceLongBreak
        }
    }
}

class DayBean {
    var key: String = ""
    var focusMs: Long = 0
    var sessions: Int = 0
}

class SessionBean {
    var id: String = ""
    var mode: String = SessionMode.FOCUS.name
    var plannedMs: Long = 0
    var startedAt: Long = 0
    var endedAt: Long = 0
}

class HistoryBean {
    var totalSessions: Int = 0
    var days: MutableList<DayBean> = mutableListOf()
    var recent: MutableList<SessionBean> = mutableListOf()
}

object StorageCodec {
    private const val MAX_REASONABLE_MS = 24L * 60 * 60 * 1000

    private fun modeOrNull(name: String): SessionMode? = SessionMode.values().firstOrNull { it.name == name }
    private fun statusOrNull(name: String): TimerStatus? = TimerStatus.values().firstOrNull { it.name == name }

    /**
     * Validates stored timer data. Anything malformed or out of range yields null so the
     * caller falls back to a clean idle timer instead of failing on corrupted storage.
     */
    fun restoreTimer(bean: TimerBean?, now: Long, config: EngineConfig): TimerState? {
        if (bean == null) return null
        val mode = modeOrNull(bean.mode) ?: return null
        val status = statusOrNull(bean.status) ?: return null
        val cycle = bean.focusSinceLongBreak.coerceIn(0, 1000)

        if (status == TimerStatus.IDLE) return idleState(mode, config, cycle)

        val duration = bean.durationMs
        if (duration < TimerLimits.MIN_DURATION_MS || duration > TimerLimits.MAX_DURATION_MS) return null
        if (bean.elapsedMs < 0 || bean.elapsedMs > duration) return null

        var segment: Long? = null
        if (status == TimerStatus.RUNNING) {
            if (bean.segmentStartedAt <= 0) return null
            segment = minOf(bean.segmentStartedAt, now) // never trust a start time in the future
        }
        val started = if (bean.sessionStartedAt > 0) minOf(bean.sessionStartedAt, now) else segment ?: now

        return TimerState(
            mode = mode,
            status = status,
            durationMs = duration,
            elapsedMs = bean.elapsedMs,
            segmentStartedAt = segment,
            sessionStartedAt = started,
            focusSinceLongBreak = cycle,
        )
    }

    fun historyToBean(history: History): HistoryBean = HistoryBean().also { bean ->
        bean.totalSessions = history.totalSessions
        bean.days = history.days.entries.sortedBy { it.key }.map { (key, record) ->
            DayBean().also { it.key = key; it.focusMs = record.focusMs; it.sessions = record.sessions }
        }.toMutableList()
        bean.recent = history.recent.map { s ->
            SessionBean().also {
                it.id = s.id; it.mode = s.mode.name; it.plannedMs = s.plannedMs
                it.startedAt = s.startedAt; it.endedAt = s.endedAt
            }
        }.toMutableList()
    }

    /** Rebuilds a valid [History] from untrusted stored data; never throws. */
    fun restoreHistory(bean: HistoryBean?): History {
        val history = History()
        if (bean == null) return history

        for (day in bean.days) {
            if (TimeUtil.isDayKey(day.key) && day.focusMs >= 0 && day.sessions >= 0) {
                history.days[day.key] = DayRecord(day.focusMs, day.sessions)
            }
        }
        for (s in bean.recent) {
            val mode = modeOrNull(s.mode) ?: continue
            if (s.id.isEmpty() || s.id.length > 64 || s.plannedMs <= 0 || s.plannedMs > MAX_REASONABLE_MS) continue
            history.recent.add(CompletedSession(s.id, mode, s.plannedMs, s.startedAt, s.endedAt))
        }
        if (history.recent.size > History.MAX_RECENT_SESSIONS) {
            history.recent.subList(0, history.recent.size - History.MAX_RECENT_SESSIONS).clear()
        }
        val sumDays = history.days.values.sumOf { it.sessions }
        history.totalSessions = maxOf(bean.totalSessions.coerceAtLeast(0), sumDays)
        return history
    }
}
