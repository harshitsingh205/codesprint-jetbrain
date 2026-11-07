package com.codesprint.ide.ui

import com.intellij.openapi.util.IconLoader

object CodeSprintIcons {
    /** 13x13 monochrome glyph for the tool window stripe (a `_dark` variant is picked up automatically). */
    @JvmField
    val ToolWindow = IconLoader.getIcon("/icons/codesprint.svg", CodeSprintIcons::class.java)

    /** Full-colour mark used in the dashboard header. */
    @JvmField
    val Logo = IconLoader.getIcon("/icons/logo.svg", CodeSprintIcons::class.java)
}
