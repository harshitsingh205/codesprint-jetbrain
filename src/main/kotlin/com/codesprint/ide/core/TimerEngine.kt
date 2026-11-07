package com.codesprint.ide.core

object TimerLimits {
    const val MIN_DURATION_MS: Long = 1_000L
    const val MAX_DURATION_MS: Long = 24L * 60 * 60 * 1000
}

fun TimerDurations.forMode(mode: SessionMode): Long = when (mode) {
    SessionMode.FOCUS -> focusMs
    SessionMode.SHORT_BREAK -> shortBreakMs
    SessionMode.LONG_BREAK -> longBreakMs
}

fun idleState(mode: SessionMode, config: EngineConfig, focusSinceLongBreak: Int = 0): TimerState = TimerState(
    mode = mode,
    status = TimerStatus.IDLE,
    durationMs = config.durations.forMode(mode),
    elapsedMs = 0,
    segmentStartedAt = null,
    sessionStartedAt = null,
    focusSinceLongBreak = focusSinceLongBreak,
)

/**
 * Pure, timestamp-based Pomodoro state machine. It owns no threads or timers:
 * callers ask for the remaining time or poll [checkCompletion]. Because every value
 * derives from stored timestamps, the result is identical after an IDE restart, a
 * system sleep or a long gap between polls.
 *
 * Not thread-safe by design; the owning service confines it to the UI thread.
 */
class TimerEngine(
    private val config: () -> EngineConfig,
    private val clock: Clock = SystemClock,
    initial: TimerState? = null,
) {
    var state: TimerState = initial ?: idleState(SessionMode.FOCUS, config())
        private set

    val status: TimerStatus get() = state.status
    val mode: SessionMode get() = state.mode

    fun elapsedMs(now: Long = clock.now()): Long {
        val s = state
        if (s.status == TimerStatus.IDLE) return 0
        val started = s.segmentStartedAt
        if (s.status == TimerStatus.RUNNING && started != null) {
            // A clock that moved backwards must not add negative time.
            val segment = maxOf(0L, now - started)
            return minOf(s.durationMs, s.elapsedMs + segment)
        }
        return s.elapsedMs
    }

    fun remainingMs(now: Long = clock.now()): Long {
        if (state.status == TimerStatus.IDLE) return state.durationMs
        return maxOf(0L, state.durationMs - elapsedMs(now))
    }

    fun snapshot(now: Long = clock.now()): TimerSnapshot {
        val s = state
        val cfg = config()
        return TimerSnapshot(
            mode = s.mode,
            status = s.status,
            durationMs = s.durationMs,
            remainingMs = remainingMs(now),
            cycleDone = minOf(s.focusSinceLongBreak, cfg.sessionsBeforeLongBreak),
            cycleLength = cfg.sessionsBeforeLongBreak,
        )
    }

    /** Starts a fresh session from idle, or resumes when paused. */
    fun start(): Boolean {
        val s = state
        if (s.status == TimerStatus.RUNNING) return false
        if (s.status == TimerStatus.PAUSED) return resume()
        val now = clock.now()
        state = s.copy(
            status = TimerStatus.RUNNING,
            durationMs = config().durations.forMode(s.mode),
            elapsedMs = 0,
            segmentStartedAt = now,
            sessionStartedAt = now,
        )
        return true
    }

    fun pause(): Boolean {
        val s = state
        if (s.status != TimerStatus.RUNNING) return false
        val now = clock.now()
        if (remainingMs(now) <= 0) return false // already finished; checkCompletion() records it
        state = s.copy(status = TimerStatus.PAUSED, elapsedMs = elapsedMs(now), segmentStartedAt = null)
        return true
    }

    fun resume(): Boolean {
        val s = state
        if (s.status != TimerStatus.PAUSED) return false
        state = s.copy(status = TimerStatus.RUNNING, segmentStartedAt = clock.now())
        return true
    }

    /** Discards the current session (if any) and returns to idle in the same mode. */
    fun reset(): Boolean {
        val s = state
        if (s.status == TimerStatus.IDLE) return false
        state = idleState(s.mode, config(), s.focusSinceLongBreak)
        return true
    }

    /** Switches session type; any session in progress is discarded. */
    fun setMode(mode: SessionMode) {
        state = idleState(mode, config(), state.focusSinceLongBreak)
    }

    /** Returns to a pristine focus state (used when stored data is cleared). */
    fun restart() {
        state = idleState(SessionMode.FOCUS, config(), 0)
    }

    /** Applies changed duration settings to an idle timer. In-progress sessions keep their length. */
    fun syncDurations(): Boolean {
        val s = state
        if (s.status != TimerStatus.IDLE) return false
        val durationMs = config().durations.forMode(s.mode)
        if (durationMs == s.durationMs) return false
        state = s.copy(durationMs = durationMs)
        return true
    }

    /**
     * If a running session has reached its end, finalises it and moves the timer to the
     * next (idle) mode. The end time is derived from the stored timestamps, so a session
     * that finished while the IDE was closed is credited correctly.
     */
    fun checkCompletion(): Completion? {
        val s = state
        val segmentStart = s.segmentStartedAt
        if (s.status != TimerStatus.RUNNING || segmentStart == null) return null
        if (remainingMs() > 0) return null

        val endedAt = segmentStart + (s.durationMs - s.elapsedMs)
        val startedAt = s.sessionStartedAt ?: segmentStart
        val session = CompletedSession(
            id = "$startedAt-${s.mode.name.lowercase()}",
            mode = s.mode,
            plannedMs = s.durationMs,
            startedAt = startedAt,
            endedAt = endedAt,
        )

        val cfg = config()
        var nextMode = SessionMode.FOCUS
        var focusSinceLongBreak = s.focusSinceLongBreak
        when (s.mode) {
            SessionMode.FOCUS -> {
                focusSinceLongBreak += 1
                if (cfg.autoBreakSuggestions) {
                    nextMode = if (focusSinceLongBreak >= cfg.sessionsBeforeLongBreak) SessionMode.LONG_BREAK
                    else SessionMode.SHORT_BREAK
                }
            }
            SessionMode.LONG_BREAK -> focusSinceLongBreak = 0
            SessionMode.SHORT_BREAK -> Unit
        }
        state = idleState(nextMode, cfg, focusSinceLongBreak)
        return Completion(session, nextMode)
    }
}
