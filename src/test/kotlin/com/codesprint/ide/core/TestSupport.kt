package com.codesprint.ide.core

import java.time.ZoneId

const val MIN: Long = 60_000L
const val T0: Long = 1_700_000_000_000L // 2023-11-14T22:13:20Z
val UTC: ZoneId = ZoneId.of("UTC")

class FakeClock(var current: Long = T0) : Clock {
    override fun now(): Long = current
    fun advance(ms: Long) {
        current += ms
    }
}

fun testConfig(
    focus: Long = 25,
    short: Long = 5,
    long: Long = 15,
    cycle: Int = 4,
    auto: Boolean = true,
) = EngineConfig(TimerDurations(focus * MIN, short * MIN, long * MIN), auto, cycle)

fun session(
    endedAt: Long,
    mode: SessionMode = SessionMode.FOCUS,
    planned: Long = 25 * MIN,
    id: String = "$endedAt-${mode.name.lowercase()}",
) = CompletedSession(id, mode, planned, endedAt - planned, endedAt)

const val DAY_MS: Long = 24L * 60 * 60 * 1000
