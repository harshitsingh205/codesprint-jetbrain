package com.codesprint.ide.settings

import com.codesprint.ide.core.MotivationalDisplay
import com.codesprint.ide.core.Settings
import com.codesprint.ide.state.CodeSprintSettings
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

/** Settings | Tools | CodeSprint. Values are bound straight to the persistent bean. */
class CodeSprintConfigurable : BoundSearchableConfigurable("CodeSprint", "com.codesprint.ide.settings") {

    override fun createPanel(): DialogPanel {
        val bean = CodeSprintSettings.getInstance().editable
        return panel {
            group("Session Lengths") {
                row("Focus (minutes):") {
                    spinner(Settings.FOCUS_RANGE).bindIntValue(bean::focusMinutes)
                }
                row("Short break (minutes):") {
                    spinner(Settings.SHORT_BREAK_RANGE).bindIntValue(bean::shortBreakMinutes)
                }
                row("Long break (minutes):") {
                    spinner(Settings.LONG_BREAK_RANGE).bindIntValue(bean::longBreakMinutes)
                }
                row("Focus sessions before a long break:") {
                    spinner(Settings.CYCLE_RANGE).bindIntValue(bean::sessionsBeforeLongBreak)
                }
            }
            group("Goals and Notifications") {
                row("Daily focus goal (minutes):") {
                    spinner(Settings.GOAL_RANGE, 5).bindIntValue(bean::dailyGoalMinutes)
                }
                row {
                    checkBox("Show a notification when a session finishes").bindSelected(bean::notifications)
                }
                row {
                    checkBox("Switch to a break after a focus session and offer to start it")
                        .bindSelected(bean::autoBreakSuggestions)
                }
            }
            group("Motivational Reminders") {
                row("Show a reminder every (minutes):") {
                    spinner(Settings.MOTIVATION_RANGE).bindIntValue(bean::motivationalIntervalMinutes)
                        .comment("Set to 0 to turn automatic reminders off.")
                }
                row("Display reminders in:") {
                    comboBox(
                        MotivationalDisplay.values().toList(),
                        SimpleListCellRenderer.create("") { it?.label ?: "" },
                    ).bindItem(
                        { MotivationalDisplay.values().firstOrNull { it.name == bean.motivationalDisplay } },
                        { bean.motivationalDisplay = (it ?: MotivationalDisplay.STATUS_BAR).name },
                    )
                }
            }
        }
    }

    override fun apply() {
        super.apply()
        CodeSprintSettings.getInstance().notifyChanged()
    }
}
