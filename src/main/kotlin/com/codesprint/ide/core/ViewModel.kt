package com.codesprint.ide.core

import java.time.Instant
import java.time.ZoneId

enum class MessageKind(val label: String) {
    DAILY("Daily reminder"),
    COMPLETION("Session complete"),
    BREAK("Break time"),
    MOTIVATION("Motivation"),
}

data class GoalView(val goalMs: Long, val todayMs: Long, val percent: Int, val met: Boolean, val remainingMs: Long)

/** Everything the dashboard renders, derived from plain data so it can be unit tested. */
data class ViewModel(
    val timer: TimerSnapshot,
    val settings: Settings,
    val goal: GoalView,
    val stats: Stats,
    val greeting: String,
    val welcome: String,
    val messageKind: MessageKind,
    val message: String,
)

object ViewModelBuilder {
    fun build(
        now: Long,
        timer: TimerSnapshot,
        settings: Settings,
        history: History,
        completionMessage: String? = null,
        motivationalQuote: String? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ViewModel {
        val goalMs = settings.dailyGoalMinutes * TimeUtil.MINUTE_MS
        val stats = StatsCalculator.compute(history, now, goalMs, zone)
        val goal = GoalView(
            goalMs = goalMs,
            todayMs = stats.todayFocusMs,
            percent = stats.goalPercent,
            met = stats.goalMet,
            remainingMs = maxOf(0L, goalMs - stats.todayFocusMs),
        )

        val welcome = when {
            goal.met -> "Daily goal complete. Keep the momentum going."
            stats.todayFocusMs == 0L -> "Start a focus session to begin today\u2019s progress."
            else -> "${TimeUtil.formatDuration(goal.remainingMs)} left to reach today\u2019s goal."
        }

        val key = TimeUtil.dayKey(now, zone)
        val (kind, text) = when {
            completionMessage != null -> MessageKind.COMPLETION to completionMessage
            timer.mode != SessionMode.FOCUS && timer.status == TimerStatus.RUNNING ->
                MessageKind.BREAK to Messages.breakMessage(key)
            motivationalQuote != null -> MessageKind.MOTIVATION to motivationalQuote
            else -> MessageKind.DAILY to Messages.daily(key)
        }

        return ViewModel(
            timer = timer,
            settings = settings,
            goal = goal,
            stats = stats,
            greeting = TimeUtil.greetingForHour(Instant.ofEpochMilli(now).atZone(zone).hour),
            welcome = welcome,
            messageKind = kind,
            message = text,
        )
    }
}
