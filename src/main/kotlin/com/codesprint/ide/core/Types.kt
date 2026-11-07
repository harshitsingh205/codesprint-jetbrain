package com.codesprint.ide.core

enum class SessionMode(val label: String) {
    FOCUS("Focus"),
    SHORT_BREAK("Short break"),
    LONG_BREAK("Long break"),
}

enum class TimerStatus { IDLE, RUNNING, PAUSED }

/** Injectable clock so timer maths can be tested deterministically. */
fun interface Clock {
    fun now(): Long
}

object SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
}

data class TimerDurations(val focusMs: Long, val shortBreakMs: Long, val longBreakMs: Long)

data class EngineConfig(
    val durations: TimerDurations,
    val autoBreakSuggestions: Boolean,
    val sessionsBeforeLongBreak: Int,
)

/**
 * Everything needed to rebuild the timer after an IDE restart. Time is stored as
 * timestamps, never as a decrementing counter, so restoration is exact.
 */
data class TimerState(
    val mode: SessionMode,
    val status: TimerStatus,
    /** Planned length of the current session. */
    val durationMs: Long,
    /** Time already consumed before the current running segment began. */
    val elapsedMs: Long,
    /** Epoch ms at which the current running segment started (running only). */
    val segmentStartedAt: Long?,
    /** Epoch ms at which the session was first started. */
    val sessionStartedAt: Long?,
    /** Completed focus sessions since the last long break. */
    val focusSinceLongBreak: Int,
)

/** A session that ran to completion. Incomplete sessions are never recorded. */
data class CompletedSession(
    val id: String,
    val mode: SessionMode,
    val plannedMs: Long,
    val startedAt: Long,
    val endedAt: Long,
)

data class Completion(
    val session: CompletedSession,
    /** Mode the timer has been moved to (idle) after completion. */
    val nextMode: SessionMode,
)

data class TimerSnapshot(
    val mode: SessionMode,
    val status: TimerStatus,
    val durationMs: Long,
    val remainingMs: Long,
    val cycleDone: Int,
    val cycleLength: Int,
) {
    /** Fraction of the session already consumed, 0.0 to 1.0. */
    val progress: Double
        get() = if (status == TimerStatus.IDLE || durationMs <= 0) 0.0
        else (1.0 - remainingMs.toDouble() / durationMs).coerceIn(0.0, 1.0)
}
