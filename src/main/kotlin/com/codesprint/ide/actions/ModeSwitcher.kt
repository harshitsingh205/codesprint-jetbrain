package com.codesprint.ide.actions

import com.codesprint.ide.core.SessionMode
import com.codesprint.ide.core.TimerStatus
import com.codesprint.ide.service.TimerService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder

/** Switches the session type, asking first when an unfinished session would be discarded. */
object ModeSwitcher {
    fun switchTo(project: Project?, mode: SessionMode) {
        val service = TimerService.getInstance()
        if (service.mode == mode) return
        if (service.status != TimerStatus.IDLE) {
            val confirmed = MessageDialogBuilder
                .yesNo("Switch Session Type", "Switching session type will discard the session in progress.")
                .yesText("Switch Session")
                .noText("Cancel")
                .ask(project)
            if (!confirmed) {
                service.refresh() // let the UI snap back to the real mode
                return
            }
        }
        service.setMode(mode)
    }
}
