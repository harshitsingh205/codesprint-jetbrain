package com.codesprint.ide.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageAndModelTest {
    private val config = testConfig()

    private fun runningBean() = TimerBean().apply {
        mode = "FOCUS"; status = "RUNNING"; durationMs = 25 * MIN; elapsedMs = 0
        segmentStartedAt = T0 - 5 * MIN; sessionStartedAt = T0 - 5 * MIN; focusSinceLongBreak = 1
    }

    @Test
    fun `valid running timer is restored`() {
        val state = StorageCodec.restoreTimer(runningBean(), T0, config)
        assertNotNull(state)
        assertEquals(TimerStatus.RUNNING, state!!.status)
        assertEquals(T0 - 5 * MIN, state.segmentStartedAt)
    }

    @Test
    fun `corrupted timer data falls back to null`() {
        assertNull(StorageCodec.restoreTimer(null, T0, config))
        assertNull(StorageCodec.restoreTimer(runningBean().apply { mode = "NOPE" }, T0, config))
        assertNull(StorageCodec.restoreTimer(runningBean().apply { status = "???" }, T0, config))
        assertNull(StorageCodec.restoreTimer(runningBean().apply { durationMs = -1 }, T0, config))
        assertNull(StorageCodec.restoreTimer(runningBean().apply { elapsedMs = 26 * MIN }, T0, config))
        assertNull(StorageCodec.restoreTimer(runningBean().apply { segmentStartedAt = 0 }, T0, config))
    }

    @Test
    fun `a start time in the future is never trusted`() {
        val state = StorageCodec.restoreTimer(runningBean().apply { segmentStartedAt = T0 + 999 * MIN }, T0, config)
        assertEquals(T0, state!!.segmentStartedAt)
    }

    @Test
    fun `idle timer restores with the configured duration`() {
        val state = StorageCodec.restoreTimer(TimerBean().apply { status = "IDLE"; mode = "SHORT_BREAK" }, T0, config)
        assertEquals(5 * MIN, state!!.durationMs)
    }

    @Test
    fun `history round trips and rejects garbage`() {
        val h = History()
        h.addSession(session(T0), UTC)
        val bean = StorageCodec.historyToBean(h)
        bean.days.add(DayBean().apply { key = "not-a-day"; focusMs = 5; sessions = 1 })
        bean.days.add(DayBean().apply { key = "2023-02-30"; focusMs = 5; sessions = 1 })
        bean.recent.add(SessionBean().apply { id = ""; plannedMs = 5 })
        bean.recent.add(SessionBean().apply { id = "x"; mode = "BAD"; plannedMs = 5 })
        val restored = StorageCodec.restoreHistory(bean)
        assertEquals(1, restored.days.size)
        assertEquals(1, restored.recent.size)
        assertEquals(1, restored.totalSessions)
        assertTrue(StorageCodec.restoreHistory(null).days.isEmpty())
    }

    @Test
    fun `settings are clamped and invalid enums default`() {
        val s = Settings.normalized(0, 999, -5, 1, 100000, true, false, 500, "garbage")
        assertEquals(1, s.focusMinutes)
        assertEquals(60, s.shortBreakMinutes)
        assertEquals(1, s.longBreakMinutes)
        assertEquals(2, s.sessionsBeforeLongBreak)
        assertEquals(1440, s.dailyGoalMinutes)
        assertEquals(180, s.motivationalIntervalMinutes)
        assertEquals(MotivationalDisplay.STATUS_BAR, s.motivationalDisplay)
        assertEquals(1 * MIN, s.toEngineConfig().durations.focusMs)
    }

    @Test
    fun `time helpers format as expected`() {
        assertEquals("25:00", TimeUtil.formatClock(25 * MIN))
        assertEquals("00:01", TimeUtil.formatClock(1))
        assertEquals("1h 15m", TimeUtil.formatDuration(75 * MIN))
        assertEquals("2h", TimeUtil.formatDuration(120 * MIN))
        assertEquals("45m", TimeUtil.formatDuration(45 * MIN))
        assertTrue(TimeUtil.isDayKey("2024-02-29"))
        assertTrue(!TimeUtil.isDayKey("2023-02-29"))
        assertEquals("2024-03-01", TimeUtil.shiftDayKey("2024-02-29", 1))
        assertEquals("Good morning", TimeUtil.greetingForHour(8))
        assertEquals("Good evening", TimeUtil.greetingForHour(20))
    }

    @Test
    fun `messages are deterministic per day and never repeat back to back`() {
        assertEquals(Messages.daily("2026-11-07"), Messages.daily("2026-11-07"))
        var last = -1
        repeat(200) {
            val (_, index) = Messages.randomQuote(last)
            assertTrue(index != last)
            last = index
        }
        assertTrue((Messages.QUOTES + Messages.DAILY + Messages.COMPLETION + Messages.BREAK).all { m -> m.all { it.code < 128 } })
    }

    @Test
    fun `view model picks the right message and welcome text`() {
        val timer = TimerEngine({ config }, FakeClock()).snapshot()
        val empty = ViewModelBuilder.build(T0, timer, Settings(), History(), zone = UTC)
        assertEquals(MessageKind.DAILY, empty.messageKind)
        assertTrue(empty.welcome.startsWith("Start a focus session"))

        val done = ViewModelBuilder.build(T0, timer, Settings(), History(), completionMessage = "Done", zone = UTC)
        assertEquals(MessageKind.COMPLETION, done.messageKind)

        val h = History()
        h.addSession(session(T0), UTC)
        val partial = ViewModelBuilder.build(T0, timer, Settings(), h, zone = UTC)
        assertEquals("1h 35m left to reach today\u2019s goal.", partial.welcome)
    }
}
