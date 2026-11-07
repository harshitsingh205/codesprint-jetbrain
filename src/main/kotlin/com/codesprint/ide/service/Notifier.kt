package com.codesprint.ide.service

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/** Thin wrapper over the "CodeSprint" notification group declared in plugin.xml. */
object Notifier {
    private const val GROUP_ID = "CodeSprint"
    private const val TITLE = "CodeSprint"

    fun info(content: String, project: Project? = null, configure: (Notification) -> Unit = {}) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(TITLE, content, NotificationType.INFORMATION)
        configure(notification)
        notification.notify(project)
    }
}
