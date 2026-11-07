package com.codesprint.ide.service

import com.codesprint.ide.core.MotivationalDisplay
import com.codesprint.ide.core.Messages
import com.codesprint.ide.core.TimeUtil
import com.codesprint.ide.state.CodeSprintSettings
import com.intellij.notification.NotificationAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import java.awt.datatransfer.StringSelection
import javax.swing.Timer

/**
 * Periodically shows a short, professional developer quote, either in the status bar,
 * as a notification, or both. Can also be triggered on demand from the Tools menu.
 * UI thread only. Registered as an application service in plugin.xml.
 */
class MotivationService : Disposable {
    private val settings = CodeSprintSettings.getInstance()
    private val listeners = ChangeListeners()
    private var timer: Timer? = null
    private var lastIndex = -1
    private var started = false

    var currentQuote: String? = null
        private set

    fun addListener(parent: Disposable, listener: () -> Unit) = listeners.add(parent, listener)

    fun start() {
        if (started) return
        started = true
        settings.addListener(this) { schedule() }
        schedule()
    }

    /** Re-creates the interval timer. An interval of 0 disables automatic quotes. */
    private fun schedule() {
        timer?.stop()
        timer = null
        val minutes = settings.get().motivationalIntervalMinutes
        if (minutes <= 0) return
        val intervalMs = (minutes * TimeUtil.MINUTE_MS).toInt()
        timer = Timer(intervalMs) { showRandomQuote(manual = false) }.apply { isRepeats = true; start() }
    }

    /**
     * Picks a random quote and shows it as configured. When [manual] is true (the user
     * asked for it) it is always shown in the status bar and as a notification.
     */
    fun showRandomQuote(manual: Boolean) {
        val (quote, index) = Messages.randomQuote(lastIndex)
        lastIndex = index
        currentQuote = quote
        listeners.fire()

        val display = settings.get().motivationalDisplay
        if (manual || display == MotivationalDisplay.STATUS_BAR || display == MotivationalDisplay.BOTH) {
            showInStatusBar(quote)
        }
        if (manual || display == MotivationalDisplay.NOTIFICATION || display == MotivationalDisplay.BOTH) {
            Notifier.info(quote) { notification ->
                notification.addAction(NotificationAction.createSimpleExpiring("Copy quote") {
                    CopyPasteManager.getInstance().setContents(StringSelection(quote))
                })
            }
        }
    }

    /** Shows the quote in every open project's status bar and clears it after ten seconds. */
    private fun showInStatusBar(quote: String) {
        val bars = ProjectManager.getInstance().openProjects
            .filter { !it.isDisposed }
            .mapNotNull { WindowManager.getInstance().getStatusBar(it) }
        bars.forEach { it.info = quote }
        Timer(STATUS_BAR_MS) {
            bars.forEach { bar -> if (bar.info == quote) bar.info = "" }
        }.apply { isRepeats = false; start() }
    }

    override fun dispose() {
        timer?.stop()
        timer = null
    }

    companion object {
        private const val STATUS_BAR_MS = 10_000
        fun getInstance(): MotivationService = service()
    }
}
