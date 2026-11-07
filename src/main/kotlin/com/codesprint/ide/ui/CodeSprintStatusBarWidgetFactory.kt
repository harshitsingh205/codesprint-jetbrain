package com.codesprint.ide.ui

import com.codesprint.ide.core.TimeUtil
import com.codesprint.ide.core.TimerStatus
import com.codesprint.ide.service.TimerService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.event.MouseEvent

/** Status bar entry showing the live timer; clicking toggles start, pause and resume. */
class CodeSprintStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = WIDGET_ID
    override fun getDisplayName(): String = "CodeSprint Timer"
    override fun isAvailable(project: Project): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = CodeSprintStatusBarWidget()
    override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)

    companion object {
        const val WIDGET_ID = "CodeSprint.StatusBar"
    }
}

private class CodeSprintStatusBarWidget : StatusBarWidget, StatusBarWidget.TextPresentation {
    private var statusBar: StatusBar? = null
    private val service = TimerService.getInstance()

    override fun ID(): String = CodeSprintStatusBarWidgetFactory.WIDGET_ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        service.addListener(this) { this.statusBar?.updateWidget(ID()) }
    }

    override fun dispose() {
        statusBar = null
    }

    override fun getText(): String {
        val t = service.snapshot()
        val clock = TimeUtil.formatClock(t.remainingMs)
        return when (t.status) {
            TimerStatus.RUNNING -> "$clock ${t.mode.label}"
            TimerStatus.PAUSED -> "$clock Paused"
            TimerStatus.IDLE -> "CodeSprint"
        }
    }

    override fun getTooltipText(): String {
        val t = service.snapshot()
        val clock = TimeUtil.formatClock(t.remainingMs)
        return when (t.status) {
            TimerStatus.RUNNING -> "CodeSprint: ${t.mode.label} running. Click to pause."
            TimerStatus.PAUSED -> "CodeSprint: ${t.mode.label} paused. Click to resume."
            TimerStatus.IDLE -> "CodeSprint: ${t.mode.label} ready ($clock). Click to start."
        }
    }

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer { service.toggle() }
}
