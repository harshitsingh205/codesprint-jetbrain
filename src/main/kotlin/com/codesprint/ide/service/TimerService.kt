package com.codesprint.ide.service

import com.codesprint.ide.core.Clock
import com.codesprint.ide.core.Completion
import com.codesprint.ide.core.Messages
import com.codesprint.ide.core.SessionMode
import com.codesprint.ide.core.SystemClock
import com.codesprint.ide.core.TimeUtil
import com.codesprint.ide.core.TimerEngine
import com.codesprint.ide.core.TimerSnapshot
import com.codesprint.ide.core.TimerStatus
import com.codesprint.ide.state.CodeSprintData
import com.codesprint.ide.state.CodeSprintSettings
import com.intellij.notification.NotificationAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import javax.swing.Timer

/**
 * Owns the single timer instance for the whole IDE (all project windows share it).
 * The one-second ticker exists only while a session is running; all time maths is
 * delegated to the timestamp-based [TimerEngine], so the ticker is just a
 * "check and refresh" heartbeat.
 *
 * Threading: every method must be called on the UI thread. The ticker is a Swing
 * timer, so it already runs there. Registered as an application service in plugin.xml.
 */
class TimerService : Disposable {
    private val settings: CodeSprintSettings = CodeSprintSettings.getInstance()
    private val data: CodeSprintData = CodeSprintData.getInstance()
    private val clock: Clock = SystemClock
    private val listeners = ChangeListeners()
    private val engine: TimerEngine
    private var ticker: Timer? = null
    private var initialized = false
    private var disposed = false

    /** Message to show after the most recent completed focus session, until the next start. */
    var completionMessage: String? = null
        private set

    init {
        val config = { settings.get().toEngineConfig() }
        engine = TimerEngine(config, clock, data.loadTimer(config(), clock.now()))
        settings.addListener(this) {
            if (engine.syncDurations()) persist()
            listeners.fire()
        }
    }

    val status: TimerStatus get() = engine.status
    val mode: SessionMode get() = engine.mode

    fun snapshot(): TimerSnapshot = engine.snapshot()

    fun addListener(parent: Disposable, listener: () -> Unit) = listeners.add(parent, listener)

    /** Asks every view to re-render from the current state (no state change). */
    fun refresh() = listeners.fire()

    /** Restores state after an IDE restart: credits a session that finished while closed, then resumes ticking. */
    fun initialize() {
        if (initialized) return
        initialized = true
        syncCompletion()
        afterChange()
    }

    fun start() {
        syncCompletion()
        if (engine.start()) {
            completionMessage = null
            afterChange()
        }
    }

    fun pause() {
        syncCompletion()
        if (engine.pause()) afterChange()
    }

    fun resume() {
        syncCompletion()
        if (engine.resume()) afterChange()
    }

    fun toggle() = when (engine.status) {
        TimerStatus.RUNNING -> pause()
        TimerStatus.PAUSED -> resume()
        TimerStatus.IDLE -> start()
    }

    fun reset() {
        syncCompletion()
        if (engine.reset()) afterChange()
    }

    /** Switches session type. Callers are expected to confirm first when a session is in progress. */
    fun setMode(mode: SessionMode) {
        syncCompletion()
        if (engine.mode == mode) return
        engine.setMode(mode)
        completionMessage = null
        afterChange()
    }

    /** Removes all stored productivity data and resets the timer. */
    fun clearAllData() {
        data.clearAll()
        engine.restart()
        completionMessage = null
        afterChange()
    }

    override fun dispose() {
        disposed = true
        stopTicker()
    }

    // --- internals -----------------------------------------------------------

    private fun syncCompletion() {
        engine.checkCompletion()?.let { handleCompletion(it) }
    }

    private fun onTick() {
        if (disposed) return
        val completion = engine.checkCompletion()
        if (completion != null) {
            handleCompletion(completion)
            afterChange()
            return
        }
        listeners.fire()
    }

    private fun handleCompletion(completion: Completion) {
        val session = completion.session
        // Recording is idempotent by session id, so a crash between the two writes
        // cannot lose or duplicate the session.
        data.recordSession(session)
        persist()

        if (session.mode == SessionMode.FOCUS) {
            val today = data.history.days[TimeUtil.dayKey(session.endedAt)]
            val goalMs = settings.get().dailyGoalMinutes * TimeUtil.MINUTE_MS
            val justReachedGoal = today != null && today.focusMs >= goalMs && today.focusMs - session.plannedMs < goalMs
            completionMessage = if (justReachedGoal) Messages.GOAL_REACHED else Messages.completion(today?.sessions ?: 1)
        } else {
            completionMessage = null
        }
        notifyCompletion(completion)
    }

    private fun notifyCompletion(completion: Completion) {
        if (!settings.get().notifications) return
        val session = completion.session
        val nextMode = completion.nextMode
        val minutes = maxOf(1L, Math.round(session.plannedMs.toDouble() / TimeUtil.MINUTE_MS))

        val text: String
        val actionLabel: String?
        if (session.mode == SessionMode.FOCUS) {
            text = "Focus session complete. $minutes ${if (minutes == 1L) "minute" else "minutes"} logged."
            actionLabel = if (nextMode == SessionMode.FOCUS) null else "Start ${nextMode.label}"
        } else {
            text = "${session.mode.label} finished. Ready for the next focus session?"
            actionLabel = "Start Focus"
        }

        Notifier.info(text) { notification ->
            if (actionLabel != null) {
                notification.addAction(NotificationAction.createSimpleExpiring(actionLabel) {
                    // Only act if the user has not changed anything in the meantime.
                    if (engine.status == TimerStatus.IDLE && engine.mode == nextMode) start()
                })
            }
        }
    }

    private fun persist() {
        data.saveTimer(engine.state)
        // Flush right away so a crash cannot lose a running session or a recorded one.
        ApplicationManager.getApplication().saveSettings()
    }

    private fun afterChange() {
        persist()
        updateTicker()
        listeners.fire()
    }

    /** Ensures exactly one ticker exists while running, and none otherwise. */
    private fun updateTicker() {
        if (disposed) return
        if (engine.status == TimerStatus.RUNNING) {
            if (ticker == null) {
                ticker = Timer(1000) { onTick() }.apply { isRepeats = true; start() }
            }
        } else {
            stopTicker()
        }
    }

    private fun stopTicker() {
        ticker?.stop()
        ticker = null
    }

    companion object {
        fun getInstance(): TimerService = service()
    }
}
