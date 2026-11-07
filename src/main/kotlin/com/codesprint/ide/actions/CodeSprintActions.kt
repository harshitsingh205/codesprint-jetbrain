package com.codesprint.ide.actions

import com.codesprint.ide.core.SessionMode
import com.codesprint.ide.core.TimerStatus
import com.codesprint.ide.service.MotivationService
import com.codesprint.ide.service.Notifier
import com.codesprint.ide.service.TimerService
import com.codesprint.ide.settings.CodeSprintConfigurable
import com.codesprint.ide.state.CodeSprintSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.wm.ToolWindowManager

const val TOOL_WINDOW_ID = "CodeSprint"

/**
 * Base for all CodeSprint actions. They read the in-memory timer state, so updates run
 * on the UI thread, and none of them need indexes (DumbAware).
 */
abstract class CodeSprintAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

/** An action that is only enabled and visible while the timer is in one of [statuses]. */
abstract class TimerStatusAction(private val statuses: Set<TimerStatus>) : CodeSprintAction() {
    override fun update(e: AnActionEvent) {
        val enabled = TimerService.getInstance().status in statuses
        e.presentation.isEnabledAndVisible = enabled
    }
}

class StartTimerAction : TimerStatusAction(setOf(TimerStatus.IDLE)) {
    override fun actionPerformed(e: AnActionEvent) = TimerService.getInstance().start()
}

class PauseTimerAction : TimerStatusAction(setOf(TimerStatus.RUNNING)) {
    override fun actionPerformed(e: AnActionEvent) = TimerService.getInstance().pause()
}

class ResumeTimerAction : TimerStatusAction(setOf(TimerStatus.PAUSED)) {
    override fun actionPerformed(e: AnActionEvent) = TimerService.getInstance().resume()
}

class ResetTimerAction : TimerStatusAction(setOf(TimerStatus.RUNNING, TimerStatus.PAUSED)) {
    override fun actionPerformed(e: AnActionEvent) = TimerService.getInstance().reset()
}

/** One action for a shortcut or toolbar button: starts, pauses or resumes depending on the state. */
class ToggleTimerAction : CodeSprintAction() {
    override fun update(e: AnActionEvent) {
        val (text, icon) = when (TimerService.getInstance().status) {
            TimerStatus.RUNNING -> "Pause Timer" to AllIcons.Actions.Pause
            TimerStatus.PAUSED -> "Resume Timer" to AllIcons.Actions.Execute
            TimerStatus.IDLE -> "Start Timer" to AllIcons.Actions.Execute
        }
        e.presentation.text = text
        e.presentation.icon = icon
    }

    override fun actionPerformed(e: AnActionEvent) = TimerService.getInstance().toggle()
}

class SwitchSessionTypeAction : CodeSprintAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val settings = CodeSprintSettings.getInstance().get()
        val minutes = mapOf(
            SessionMode.FOCUS to settings.focusMinutes,
            SessionMode.SHORT_BREAK to settings.shortBreakMinutes,
            SessionMode.LONG_BREAK to settings.longBreakMinutes,
        )
        val step = object : BaseListPopupStep<SessionMode>("Choose a Session Type", SessionMode.values().toList()) {
            override fun getTextFor(value: SessionMode): String = "${value.label} (${minutes[value]} min)"

            override fun onChosen(selectedValue: SessionMode, finalChoice: Boolean): PopupStep<*>? {
                ModeSwitcher.switchTo(project, selectedValue)
                return PopupStep.FINAL_CHOICE
            }
        }
        val popup = JBPopupFactory.getInstance().createListPopup(step)
        if (project != null) popup.showCenteredInCurrentWindow(project) else popup.showInFocusCenter()
    }
}

class ShowMotivationAction : CodeSprintAction() {
    override fun actionPerformed(e: AnActionEvent) = MotivationService.getInstance().showRandomQuote(manual = true)
}

class OpenDashboardAction : CodeSprintAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)?.activate(null)
    }
}

class OpenSettingsAction : CodeSprintAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.project, CodeSprintConfigurable::class.java)
    }
}

class ClearDataAction : CodeSprintAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val confirmed = MessageDialogBuilder
            .yesNo("Clear CodeSprint Data", "Delete all CodeSprint sessions, statistics and streaks? This cannot be undone.")
            .yesText("Delete Data")
            .noText("Cancel")
            .asWarning()
            .ask(e.project)
        if (confirmed) {
            TimerService.getInstance().clearAllData()
            Notifier.info("CodeSprint data has been cleared.", e.project)
        }
    }
}
