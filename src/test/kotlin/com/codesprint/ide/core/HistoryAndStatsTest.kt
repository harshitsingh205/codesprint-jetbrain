package com.codesprint.ide.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryAndStatsTest {
    private val day = TimeUtil.dayKey(T0, UTC)

    @Test
    fun `recording is idempotent by session id`() {
        val h = History()
        val s = session(T0)
        assertTrue(h.addSession(s, UTC))
        assertFalse(h.addSession(s, UTC))
        assertEquals(1, h.totalSessions)
        assertEquals(25 * MIN, h.days[day]!!.focusMs)
    }

    @Test
    fun `breaks are kept as recent sessions but never counted as focus`() {
        val h = History()
        h.addSession(session(T0, SessionMode.SHORT_BREAK, 5 * MIN), UTC)
        assertEquals(0, h.totalSessions)
        assertTrue(h.days.isEmpty())
        assertEquals(1, h.recent.size)
    }

    @Test
    fun `recent sessions and days are capped`() {
        val h = History()
        repeat(History.MAX_RECENT_SESSIONS + 20) { i -> h.addSession(session(T0 + i * MIN), UTC) }
        assertEquals(History.MAX_RECENT_SESSIONS, h.recent.size)
        val big = History()
        repeat(History.MAX_DAYS + 5) { i -> big.addSession(session(T0 + i * DAY_MS), UTC) }
        assertEquals(History.MAX_DAYS, big.days.size)
    }

    @Test
    fun `streak counts consecutive days and survives an empty today`() {
        val h = History()
        for (i in 1..3) h.addSession(session(T0 - i * DAY_MS), UTC) // three days ending yesterday
        val stats = StatsCalculator.compute(h, T0, 120 * MIN, UTC)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
        h.addSession(session(T0), UTC)
        assertEquals(4, StatsCalculator.compute(h, T0, 120 * MIN, UTC).currentStreak)
    }

    @Test
    fun `a missed day resets the current streak but keeps the longest`() {
        val h = History()
        for (i in 4..6) h.addSession(session(T0 - i * DAY_MS), UTC)
        val stats = StatsCalculator.compute(h, T0, 120 * MIN, UTC)
        assertEquals(0, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
        assertEquals(3, stats.activeDays)
    }

    @Test
    fun `week has seven entries ending today and goal percent is capped`() {
        val h = History()
        repeat(6) { i -> h.addSession(session(T0 - i * 25 * MIN, id = "s$i"), UTC) } // 150 minutes, same day
        val stats = StatsCalculator.compute(h, T0, 120 * MIN, UTC)
        assertEquals(7, stats.week.size)
        assertTrue(stats.week.last().isToday)
        assertEquals(100, stats.goalPercent)
        assertTrue(stats.goalMet)
        assertEquals(150 * MIN, stats.todayFocusMs)
    }
}
