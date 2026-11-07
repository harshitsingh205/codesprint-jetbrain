package com.codesprint.ide.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import java.util.concurrent.CopyOnWriteArrayList

/** Tiny listener registry whose subscriptions are tied to a [Disposable] parent. */
class ChangeListeners {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun add(parent: Disposable, listener: () -> Unit) {
        listeners.add(listener)
        Disposer.register(parent, Disposable { listeners.remove(listener) })
    }

    fun fire() {
        for (listener in listeners) {
            try {
                listener()
            } catch (e: Exception) {
                LOG.warn("CodeSprint listener failed", e)
            }
        }
    }

    private companion object {
        val LOG = Logger.getInstance(ChangeListeners::class.java)
    }
}
