package com.codesprint.ide.core

enum class MotivationalDisplay(val label: String) {
    STATUS_BAR("Status bar"),
    NOTIFICATION("Notification"),
    BOTH("Status bar and notification"),
}

/** Validated user settings. Every numeric value is guaranteed to be inside its range. */
data class Settings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val dailyGoalMinutes: Int = 120,
    val notifications: Boolean = true,
    val autoBreakSuggestions: Boolean = true,
    val motivationalIntervalMinutes: Int = 30,
    val motivationalDisplay: MotivationalDisplay = MotivationalDisplay.STATUS_BAR,
) {
    fun toEngineConfig(): EngineConfig = EngineConfig(
        durations = TimerDurations(
            focusMs = focusMinutes * TimeUtil.MINUTE_MS,
            shortBreakMs = shortBreakMinutes * TimeUtil.MINUTE_MS,
            longBreakMs = longBreakMinutes * TimeUtil.MINUTE_MS,
        ),
        autoBreakSuggestions = autoBreakSuggestions,
        sessionsBeforeLongBreak = sessionsBeforeLongBreak,
    )

    companion object {
        val FOCUS_RANGE = 1..180
        val SHORT_BREAK_RANGE = 1..60
        val LONG_BREAK_RANGE = 1..120
        val CYCLE_RANGE = 2..10
        val GOAL_RANGE = 5..1440
        val MOTIVATION_RANGE = 0..180

        /** Builds settings from untrusted stored values, clamping numbers into range. */
        fun normalized(
            focusMinutes: Int,
            shortBreakMinutes: Int,
            longBreakMinutes: Int,
            sessionsBeforeLongBreak: Int,
            dailyGoalMinutes: Int,
            notifications: Boolean,
            autoBreakSuggestions: Boolean,
            motivationalIntervalMinutes: Int,
            motivationalDisplay: String,
        ): Settings = Settings(
            focusMinutes = focusMinutes.coerceIn(FOCUS_RANGE),
            shortBreakMinutes = shortBreakMinutes.coerceIn(SHORT_BREAK_RANGE),
            longBreakMinutes = longBreakMinutes.coerceIn(LONG_BREAK_RANGE),
            sessionsBeforeLongBreak = sessionsBeforeLongBreak.coerceIn(CYCLE_RANGE),
            dailyGoalMinutes = dailyGoalMinutes.coerceIn(GOAL_RANGE),
            notifications = notifications,
            autoBreakSuggestions = autoBreakSuggestions,
            motivationalIntervalMinutes = motivationalIntervalMinutes.coerceIn(MOTIVATION_RANGE),
            motivationalDisplay = MotivationalDisplay.values().firstOrNull { it.name == motivationalDisplay }
                ?: MotivationalDisplay.STATUS_BAR,
        )
    }
}
