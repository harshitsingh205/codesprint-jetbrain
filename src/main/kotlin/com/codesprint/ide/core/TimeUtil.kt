package com.codesprint.ide.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** Small, dependency-free date and duration helpers (local time). */
object TimeUtil {
    const val MINUTE_MS: Long = 60_000L

    private val DAY_KEY = Regex("""\d{4}-\d{2}-\d{2}""")

    /** Local calendar day key, e.g. "2026-11-04". */
    fun dayKey(ts: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(ts).atZone(zone).toLocalDate().toString()

    fun isDayKey(value: String): Boolean {
        if (!DAY_KEY.matches(value)) return false
        return try {
            LocalDate.parse(value).toString() == value
        } catch (_: DateTimeParseException) {
            false
        }
    }

    fun shiftDayKey(key: String, deltaDays: Long): String = LocalDate.parse(key).plusDays(deltaDays).toString()

    /** Short weekday label, index 0 = Sunday. */
    private val SHORT = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    private val FULL = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    fun shortWeekday(key: String): String = SHORT[LocalDate.parse(key).dayOfWeek.value % 7]
    fun fullWeekday(key: String): String = FULL[LocalDate.parse(key).dayOfWeek.value % 7]

    /** mm:ss countdown text. Rounds up so a fresh 25 minute session reads 25:00. */
    fun formatClock(ms: Long): String {
        val total = maxOf(0L, (ms + 999) / 1000)
        return "%02d:%02d".format(total / 60, total % 60)
    }

    /** Compact duration such as "1h 15m", "45m" or "2h". */
    fun formatDuration(ms: Long): String {
        val minutes = maxOf(0L, Math.round(ms.toDouble() / MINUTE_MS))
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun greetingForHour(hour: Int): String = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}
