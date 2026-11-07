package com.codesprint.ide.state

import com.codesprint.ide.core.CompletedSession
import com.codesprint.ide.core.EngineConfig
import com.codesprint.ide.core.History
import com.codesprint.ide.core.HistoryBean
import com.codesprint.ide.core.StorageCodec
import com.codesprint.ide.core.TimerBean
import com.codesprint.ide.core.TimerState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

/**
 * Timer state, session history and small UI flags. Stored locally in
 * codesprint-data.xml; roaming is disabled on purpose so progress on one machine is
 * never merged with another. No network access, no telemetry. Registered in plugin.xml.
 */
@State(
    name = "CodeSprintData",
    storages = [Storage(value = "codesprint-data.xml", roamingType = RoamingType.DISABLED)],
)
class CodeSprintData : PersistentStateComponent<CodeSprintData.Bean> {

    class Bean {
        var timer: TimerBean? = null
        var history: HistoryBean = HistoryBean()
        var collapsed: MutableList<String> = mutableListOf()
    }

    private var timerBean: TimerBean? = null
    private var historyCache: History = History()
    private val collapsedIds: MutableSet<String> = linkedSetOf()

    /** Live history. Read and modified on the UI thread only. */
    val history: History get() = historyCache

    override fun getState(): Bean = Bean().also {
        it.timer = timerBean
        it.history = StorageCodec.historyToBean(historyCache)
        it.collapsed = collapsedIds.toMutableList()
    }

    override fun loadState(state: Bean) {
        timerBean = state.timer
        historyCache = StorageCodec.restoreHistory(state.history)
        collapsedIds.clear()
        collapsedIds.addAll(state.collapsed)
    }

    fun loadTimer(config: EngineConfig, now: Long): TimerState? = StorageCodec.restoreTimer(timerBean, now, config)

    fun saveTimer(state: TimerState) {
        timerBean = TimerBean.from(state)
    }

    /** Persists a completed session. Returns false when it was already recorded. */
    fun recordSession(session: CompletedSession): Boolean = historyCache.addSession(session)

    fun isCollapsed(id: String): Boolean = id in collapsedIds

    fun setCollapsed(id: String, collapsed: Boolean) {
        if (collapsed) collapsedIds.add(id) else collapsedIds.remove(id)
    }

    fun clearAll() {
        timerBean = null
        historyCache = History()
        collapsedIds.clear()
    }

    companion object {
        fun getInstance(): CodeSprintData = service()
    }
}
