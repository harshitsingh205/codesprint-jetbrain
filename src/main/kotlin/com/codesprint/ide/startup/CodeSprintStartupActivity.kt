package com.codesprint.ide.startup

import com.codesprint.ide.service.MotivationService
import com.codesprint.ide.service.TimerService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Runs once a project has opened. Restores the saved timer (crediting a session that
 * finished while the IDE was closed) and starts the reminder schedule. Both calls are
 * idempotent, so opening several projects is harmless.
 */
class CodeSprintStartupActivity : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            TimerService.getInstance().initialize()
            MotivationService.getInstance().start()
        }
    }
}
