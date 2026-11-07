package com.codesprint.ide.core

import java.time.ZoneId

data class WeekDay(
    val key: String,
    val label: String,
    val fullLabel: String,
    val focusMs: Long,
    val sessions: Int,
    val active: Boolean,
    val isToday: Boolean,
)

data class Streaks(val current: Int, val longest: Int, val activeDays: Int)

data class Stats(
    val todayFocusMs: Long,
    val totalSessions: Int,
    val weekFocusMs: Long,
    val goalPercent: Int,
    val goalMet: Boolean,
    val currentStreak: Int,
    val longestStreak: Int,
    val activeDays: Int,
    val week: List<WeekDay>,
    val weekActiveDays: Int,
    val weekMaxMs: Long,
)

object StatsCalculator {
    private fun isActive(d: DayRecord?): Boolean = d != null && d.sessions > 0

    /** Length of the run of consecutive active days ending at [endKey]. */
    private fun runEndingAt(days: Map<String, DayRecord>, endKey: String): Int {
        var count = 0
        var key = endKey
        while (isActive(days[key])) {
            count += 1
            key = TimeUtil.shiftDayKey(key, -1)
        }
        return count
    }

    fun computeStreaks(days: Map<String, DayRecord>, todayKey: String): Streaks {
        val activeKeys = days.filter { isActive(it.value) }.keys.sorted()

        var longest = 0
        var run = 0
        var previous: String? = null
        for (key in activeKeys) {
            run = if (previous != null && TimeUtil.shiftDayKey(previous, 1) == key) run + 1 else 1
            longest = maxOf(longest, run)
            previous = key
        }

        // A streak is still alive if today has no session yet but yesterday did.
        val current = if (isActive(days[todayKey])) runEndingAt(days, todayKey)
        else runEndingAt(days, TimeUtil.shiftDayKey(todayKey, -1))

        return Streaks(current, longest, activeKeys.size)
    }

    fun compute(history: History, now: Long, goalMs: Long, zone: ZoneId = ZoneId.systemDefault()): Stats {
        val todayKey = TimeUtil.dayKey(now, zone)
        val days = history.days
        val today = days[todayKey]?.focusMs ?: 0L

        val week = (6 downTo 0).map { i ->
            val key = TimeUtil.shiftDayKey(todayKey, -i.toLong())
            val record = days[key]
            WeekDay(
                key = key,
                label = TimeUtil.shortWeekday(key),
                fullLabel = TimeUtil.fullWeekday(key),
                focusMs = record?.focusMs ?: 0L,
                sessions = record?.sessions ?: 0,
                active = isActive(record),
                isToday = i == 0,
            )
        }

        val streaks = computeStreaks(days, todayKey)
        val goalPercent = if (goalMs > 0) minOf(100L, Math.round(today.toDouble() / goalMs * 100)).toInt() else 0

        return Stats(
            todayFocusMs = today,
            totalSessions = history.totalSessions,
            weekFocusMs = week.sumOf { it.focusMs },
            goalPercent = goalPercent,
            goalMet = goalMs > 0 && today >= goalMs,
            currentStreak = streaks.current,
            longestStreak = streaks.longest,
            activeDays = streaks.activeDays,
            week = week,
            weekActiveDays = week.count { it.active },
            weekMaxMs = maxOf(goalMs, week.maxOf { it.focusMs }, 1L),
        )
    }
}
