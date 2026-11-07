package com.codesprint.ide.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {
    private val clock = FakeClock()
    private var config = testConfig()
    private fun engine(initial: TimerState? = null) = TimerEngine({ config }, clock, initial)

    @Test
    fun `starts idle with the full focus duration`() {
        val e = engine()
        assertEquals(TimerStatus.IDLE, e.status)
        assertEquals(25 * MIN, e.remainingMs())
    }

    @Test
    fun `counts down from timestamps not ticks`() {
        val e = engine()
        e.start()
        clock.advance(10 * MIN + 30_000)
        assertEquals(14 * MIN + 30_000, e.remainingMs())
        assertNull(e.checkCompletion())
    }

    @Test
    fun `pause freezes remaining time and resume continues from it`() {
        val e = engine()
        e.start()
        clock.advance(5 * MIN)
        assertTrue(e.pause())
        clock.advance(60 * MIN)
        assertEquals(20 * MIN, e.remainingMs())
        assertTrue(e.resume())
        clock.advance(2 * MIN)
        assertEquals(18 * MIN, e.remainingMs())
    }

    @Test
    fun `invalid transitions are ignored`() {
        val e = engine()
        assertFalse(e.pause())
        assertFalse(e.resume())
        assertFalse(e.reset())
        e.start()
        assertFalse(e.start())
    }

    @Test
    fun `reset discards progress without recording a completion`() {
        val e = engine()
        e.start()
        clock.advance(24 * MIN)
        assertTrue(e.reset())
        assertEquals(TimerStatus.IDLE, e.status)
        clock.advance(10 * MIN)
        assertNull(e.checkCompletion())
        assertEquals(25 * MIN, e.remainingMs())
    }

    @Test
    fun `completion is credited with the true end time after a long gap`() {
        val e = engine()
        e.start()
        val startedAt = clock.now()
        clock.advance(8 * 60 * MIN) // IDE was closed for hours
        val completion = e.checkCompletion()
        assertNotNull(completion)
        assertEquals(startedAt + 25 * MIN, completion!!.session.endedAt)
        assertEquals(startedAt, completion.session.startedAt)
        assertEquals(25 * MIN, completion.session.plannedMs)
        assertEquals(SessionMode.SHORT_BREAK, completion.nextMode)
        assertEquals(TimerStatus.IDLE, e.status)
        assertNull(e.checkCompletion()) // never counted twice
    }

    @Test
    fun `pause then resume still completes at the right moment`() {
        val e = engine()
        e.start()
        val startedAt = clock.now()
        clock.advance(10 * MIN)
        e.pause()
        clock.advance(30 * MIN)
        e.resume()
        clock.advance(15 * MIN)
        val completion = e.checkCompletion()
        assertNotNull(completion)
        assertEquals(startedAt, completion!!.session.startedAt)
    }

    @Test
    fun `long break is suggested after the configured number of focus sessions`() {
        config = testConfig(cycle = 3)
        val e = engine()
        repeat(3) { i ->
            e.start()
            clock.advance(25 * MIN)
            val c = e.checkCompletion()!!
            if (i < 2) {
                assertEquals(SessionMode.SHORT_BREAK, c.nextMode)
                e.start()
                clock.advance(5 * MIN)
                assertEquals(SessionMode.FOCUS, e.checkCompletion()!!.nextMode)
            } else {
                assertEquals(SessionMode.LONG_BREAK, c.nextMode)
            }
        }
        e.start()
        clock.advance(15 * MIN)
        e.checkCompletion()
        assertEquals(0, e.state.focusSinceLongBreak)
    }

    @Test
    fun `without break suggestions the next mode stays focus`() {
        config = testConfig(auto = false)
        val e = engine()
        e.start()
        clock.advance(25 * MIN)
        assertEquals(SessionMode.FOCUS, e.checkCompletion()!!.nextMode)
    }

    @Test
    fun `switching mode discards a running session`() {
        val e = engine()
        e.start()
        clock.advance(3 * MIN)
        e.setMode(SessionMode.LONG_BREAK)
        assertEquals(TimerStatus.IDLE, e.status)
        assertEquals(15 * MIN, e.remainingMs())
    }

    @Test
    fun `changed durations apply to an idle timer only`() {
        val e = engine()
        config = testConfig(focus = 50)
        assertTrue(e.syncDurations())
        assertEquals(50 * MIN, e.remainingMs())
        e.start()
        config = testConfig(focus = 10)
        assertFalse(e.syncDurations())
        assertEquals(50 * MIN, e.state.durationMs)
    }

    @Test
    fun `a clock that moves backwards never adds negative time`() {
        val e = engine()
        e.start()
        clock.advance(-5 * MIN)
        assertEquals(25 * MIN, e.remainingMs())
    }

    @Test
    fun `restored running state completes exactly like an uninterrupted one`() {
        val first = engine()
        first.start()
        clock.advance(10 * MIN)
        val saved = first.state
        clock.advance(20 * MIN) // "IDE closed"
        val restored = engine(saved)
        assertNotNull(restored.checkCompletion())
    }

    @Test
    fun `snapshot reports progress`() {
        val e = engine()
        assertEquals(0.0, e.snapshot().progress, 1e-9)
        e.start()
        clock.advance(5 * MIN)
        assertEquals(0.2, e.snapshot().progress, 1e-9)
    }
}
