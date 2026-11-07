package com.codesprint.ide.state

import com.codesprint.ide.core.MotivationalDisplay
import com.codesprint.ide.core.Settings
import com.codesprint.ide.service.ChangeListeners
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * User-facing preferences, stored in codesprint-settings.xml and synchronised by
 * Settings Sync like any other IDE setting. Registered in plugin.xml.
 */
@State(name = "CodeSprintSettings", storages = [Storage("codesprint-settings.xml")])
class CodeSprintSettings : PersistentStateComponent<CodeSprintSettings.Bean> {

    /** Mutable bean used by the XML serializer and bound directly by the settings page. */
    class Bean {
        var focusMinutes: Int = 25
        var shortBreakMinutes: Int = 5
        var longBreakMinutes: Int = 15
        var sessionsBeforeLongBreak: Int = 4
        var dailyGoalMinutes: Int = 120
        var notifications: Boolean = true
        var autoBreakSuggestions: Boolean = true
        var motivationalIntervalMinutes: Int = 30
        var motivationalDisplay: String = MotivationalDisplay.STATUS_BAR.name
    }

    private var bean = Bean()
    private val listeners = ChangeListeners()

    override fun getState(): Bean = bean

    override fun loadState(state: Bean) {
        XmlSerializerUtil.copyBean(state, bean)
    }

    /** The bean the settings page edits. Call [notifyChanged] after modifying it. */
    val editable: Bean get() = bean

    /** Validated snapshot of the current settings. */
    fun get(): Settings = Settings.normalized(
        focusMinutes = bean.focusMinutes,
        shortBreakMinutes = bean.shortBreakMinutes,
        longBreakMinutes = bean.longBreakMinutes,
        sessionsBeforeLongBreak = bean.sessionsBeforeLongBreak,
        dailyGoalMinutes = bean.dailyGoalMinutes,
        notifications = bean.notifications,
        autoBreakSuggestions = bean.autoBreakSuggestions,
        motivationalIntervalMinutes = bean.motivationalIntervalMinutes,
        motivationalDisplay = bean.motivationalDisplay,
    )

    fun update(transform: (Settings) -> Settings) {
        val s = transform(get())
        bean.focusMinutes = s.focusMinutes
        bean.shortBreakMinutes = s.shortBreakMinutes
        bean.longBreakMinutes = s.longBreakMinutes
        bean.sessionsBeforeLongBreak = s.sessionsBeforeLongBreak
        bean.dailyGoalMinutes = s.dailyGoalMinutes
        bean.notifications = s.notifications
        bean.autoBreakSuggestions = s.autoBreakSuggestions
        bean.motivationalIntervalMinutes = s.motivationalIntervalMinutes
        bean.motivationalDisplay = s.motivationalDisplay.name
        notifyChanged()
    }

    fun notifyChanged() = listeners.fire()

    fun addListener(parent: Disposable, listener: () -> Unit) = listeners.add(parent, listener)

    companion object {
        fun getInstance(): CodeSprintSettings = service()
    }
}
