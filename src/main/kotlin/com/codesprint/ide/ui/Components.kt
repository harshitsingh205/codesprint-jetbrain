package com.codesprint.ide.ui

import com.codesprint.ide.core.TimeUtil
import com.codesprint.ide.core.WeekDay
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.SwingConstants

/** Shared colours. Every colour is a [JBColor], so light and dark IDE themes are handled by the platform. */
object Palette {
    val accent = JBColor(Color(0x2563EB), Color(0x4C8DFF))
    val accentSoft = JBColor(Color(0xBFD3FA), Color(0x2F4A7D))
    val track = JBColor(Color(0xE2E8F0), Color(0x3C4150))
    val cardFill = JBColor(Color(0xF6F8FB), Color(0x2B2D33))
    val cardLine = JBColor(Color(0xD8DEE8), Color(0x3E424C))
    val success = JBColor(Color(0x15803D), Color(0x57B77B))
    val muted: Color get() = UIUtil.getContextHelpForeground()
}

private fun Graphics.antialiased(): Graphics2D = (create() as Graphics2D).apply {
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
}

/** Rounded surface used for the dashboard sections. */
open class RoundedCard(layout: java.awt.LayoutManager = BorderLayout()) : JPanel(layout) {
    init {
        isOpaque = false
        border = JBUI.Borders.empty(10)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.antialiased()
        try {
            val arc = JBUI.scale(12).toDouble()
            val shape = RoundRectangle2D.Double(0.5, 0.5, width - 1.0, height - 1.0, arc, arc)
            g2.color = Palette.cardFill
            g2.fill(shape)
            g2.color = Palette.cardLine
            g2.draw(shape)
        } finally {
            g2.dispose()
        }
    }
}

/** Circular countdown with the remaining time in the middle. */
class RingComponent : JComponent() {
    var progress: Double = 0.0
        set(value) {
            val v = value.coerceIn(0.0, 1.0)
            if (v != field) { field = v; repaint() }
        }
    var timeText: String = "25:00"
        set(value) { if (value != field) { field = value; repaint() } }
    var subText: String = "Focus session"
        set(value) { if (value != field) { field = value; repaint() } }

    init {
        isOpaque = false
        accessibleContext.accessibleName = "Timer"
    }

    override fun getPreferredSize(): Dimension = Dimension(JBUI.scale(168), JBUI.scale(168))
    override fun getMinimumSize(): Dimension = Dimension(JBUI.scale(120), JBUI.scale(120))

    override fun paintComponent(g: Graphics) {
        val g2 = g.antialiased()
        try {
            val stroke = JBUI.scale(8).toFloat()
            val size = minOf(width, height) - stroke - 2
            val x = (width - size) / 2.0
            val y = (height - size) / 2.0
            g2.stroke = java.awt.BasicStroke(stroke, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
            g2.color = Palette.track
            g2.draw(Ellipse2D.Double(x, y, size, size))
            if (progress > 0.0) {
                g2.color = Palette.accent
                g2.draw(Arc2D.Double(x, y, size, size, 90.0, -360.0 * progress, Arc2D.OPEN))
            }

            val timeFont = JBUI.Fonts.label().deriveFont(Font.BOLD, JBUI.scale(30).toFloat())
            g2.font = timeFont
            g2.color = UIUtil.getLabelForeground()
            val tm: FontMetrics = g2.fontMetrics
            val timeX = (width - tm.stringWidth(timeText)) / 2
            val timeY = height / 2 + tm.ascent / 2 - JBUI.scale(6)
            g2.drawString(timeText, timeX, timeY)

            g2.font = JBUI.Fonts.smallFont()
            g2.color = Palette.muted
            val sm = g2.fontMetrics
            g2.drawString(subText, (width - sm.stringWidth(subText)) / 2, timeY + sm.height + JBUI.scale(4))
        } finally {
            g2.dispose()
        }
    }
}

/** Row of dots showing how many focus sessions of the current cycle are done. */
class DotsComponent : JComponent() {
    private var done = 0
    private var total = 4

    init {
        isOpaque = false
    }

    fun update(done: Int, total: Int) {
        if (done == this.done && total == this.total) return
        this.done = done
        this.total = total
        accessibleContext.accessibleName = "$done of $total focus sessions completed in this cycle"
        toolTipText = accessibleContext.accessibleName
        revalidate()
        repaint()
    }

    override fun getPreferredSize(): Dimension = Dimension(JBUI.scale(16) * total, JBUI.scale(14))

    override fun paintComponent(g: Graphics) {
        val g2 = g.antialiased()
        try {
            val d = JBUI.scale(9).toDouble()
            val gap = JBUI.scale(7).toDouble()
            val totalWidth = total * d + (total - 1) * gap
            var x = (width - totalWidth) / 2
            val y = (height - d) / 2
            for (i in 0 until total) {
                val dot = Ellipse2D.Double(x, y, d, d)
                if (i < done) {
                    g2.color = Palette.accent
                    g2.fill(dot)
                } else {
                    g2.color = Palette.track
                    g2.fill(dot)
                }
                x += d + gap
            }
        } finally {
            g2.dispose()
        }
    }
}

/** Slim horizontal progress bar. */
class BarComponent : JComponent() {
    var fraction: Double = 0.0
        set(value) {
            val v = value.coerceIn(0.0, 1.0)
            if (v != field) { field = v; repaint() }
        }
    var complete: Boolean = false
        set(value) { if (value != field) { field = value; repaint() } }

    init {
        isOpaque = false
        accessibleContext.accessibleName = "Daily goal progress"
    }

    override fun getPreferredSize(): Dimension = Dimension(JBUI.scale(120), JBUI.scale(8))
    override fun getMinimumSize(): Dimension = Dimension(JBUI.scale(40), JBUI.scale(8))
    override fun getMaximumSize(): Dimension = Dimension(Int.MAX_VALUE, JBUI.scale(8))

    override fun paintComponent(g: Graphics) {
        val g2 = g.antialiased()
        try {
            val h = height.toDouble()
            g2.color = Palette.track
            g2.fill(RoundRectangle2D.Double(0.0, 0.0, width.toDouble(), h, h, h))
            val fillWidth = width * fraction
            if (fillWidth > 0) {
                g2.color = if (complete) Palette.success else Palette.accent
                g2.fill(RoundRectangle2D.Double(0.0, 0.0, maxOf(fillWidth, h), h, h, h))
            }
        } finally {
            g2.dispose()
        }
    }
}

/** Seven day bar chart. Hover a bar to see the exact focus time. */
class WeekChart : JComponent() {
    private var days: List<WeekDay> = emptyList()
    private var maxMs: Long = 1

    init {
        isOpaque = false
        toolTipText = ""
        accessibleContext.accessibleName = "Focus time for the last seven days"
    }

    fun update(days: List<WeekDay>, maxMs: Long) {
        if (days == this.days && maxMs == this.maxMs) return
        this.days = days
        this.maxMs = maxOf(1L, maxMs)
        accessibleContext.accessibleDescription = days.joinToString("; ") { describe(it) }
        repaint()
    }

    private fun describe(day: WeekDay) =
        day.fullLabel + (if (day.isToday) " (today)" else "") + ": " + TimeUtil.formatDuration(day.focusMs)

    override fun getPreferredSize(): Dimension = Dimension(JBUI.scale(160), JBUI.scale(96))
    override fun getMinimumSize(): Dimension = Dimension(JBUI.scale(100), JBUI.scale(80))

    private fun columnWidth(): Double = if (days.isEmpty()) 0.0 else width.toDouble() / days.size

    override fun getToolTipText(event: MouseEvent): String? {
        if (days.isEmpty()) return null
        val index = (event.x / columnWidth()).toInt().coerceIn(0, days.size - 1)
        return describe(days[index])
    }

    override fun paintComponent(g: Graphics) {
        if (days.isEmpty()) return
        val g2 = g.antialiased()
        try {
            g2.font = JBUI.Fonts.smallFont()
            val fm = g2.fontMetrics
            val labelHeight = fm.height + JBUI.scale(2)
            val chartHeight = height - labelHeight - JBUI.scale(4)
            val col = columnWidth()
            val barWidth = minOf(col * 0.55, JBUI.scale(18).toDouble())

            days.forEachIndexed { i, day ->
                val cx = col * i + col / 2
                val fraction = day.focusMs.toDouble() / maxMs
                var barHeight = chartHeight * fraction
                if (day.active) barHeight = maxOf(barHeight, JBUI.scale(3).toDouble())
                val top = chartHeight - barHeight
                val radius = JBUI.scale(4).toDouble()

                g2.color = Palette.track
                g2.fill(RoundRectangle2D.Double(cx - barWidth / 2, 0.0, barWidth, chartHeight.toDouble(), radius, radius))
                if (barHeight > 0) {
                    g2.color = if (day.isToday) Palette.accent else Palette.accentSoft
                    g2.fill(RoundRectangle2D.Double(cx - barWidth / 2, top, barWidth, barHeight, radius, radius))
                }
                g2.color = if (day.isToday) UIUtil.getLabelForeground() else Palette.muted
                g2.drawString(day.label, (cx - fm.stringWidth(day.label) / 2).toFloat(), (height - fm.descent - 1).toFloat())
            }
        } finally {
            g2.dispose()
        }
    }
}

/**
 * Read-only, word-wrapping text. A plain [JLabel] cannot wrap inside vertical flow
 * layouts, so this re-validates once its real width is known.
 */
class WrappedText(text: String = "", muted: Boolean = false, bold: Boolean = false) : JTextArea(text) {
    init {
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
        isFocusable = false
        isOpaque = false
        border = BorderFactory.createEmptyBorder()
        font = if (bold) UIUtil.getLabelFont().deriveFont(Font.BOLD) else UIUtil.getLabelFont()
        foreground = if (muted) Palette.muted else UIUtil.getLabelForeground()
        addComponentListener(object : ComponentAdapter() {
            private var lastWidth = -1
            override fun componentResized(e: ComponentEvent) {
                if (width != lastWidth) {
                    lastWidth = width
                    revalidate()
                }
            }
        })
    }

    override fun setText(t: String?) {
        if (t != text) {
            super.setText(t)
            revalidate()
        }
    }
}

/** Section header that shows or hides its body; keyboard operable because it is a real button. */
class CollapsibleSection(
    title: String,
    private val body: JComponent,
    expanded: Boolean,
    private val onToggle: (Boolean) -> Unit,
) : RoundedCard(BorderLayout(0, JBUI.scale(8))) {

    private val header = JButton(title).apply {
        horizontalAlignment = SwingConstants.LEFT
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusPainted = true
        border = JBUI.Borders.empty(2, 0)
        font = font.deriveFont(Font.BOLD)
        iconTextGap = JBUI.scale(6)
    }
    private var expanded = expanded

    init {
        add(header, BorderLayout.NORTH)
        add(body, BorderLayout.CENTER)
        header.addActionListener {
            this.expanded = !this.expanded
            apply()
            onToggle(this.expanded)
        }
        apply()
    }

    private fun apply() {
        body.isVisible = expanded
        header.icon = if (expanded) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
        header.accessibleContext.accessibleDescription = if (expanded) "Expanded" else "Collapsed"
        revalidate()
        repaint()
    }
}

fun mutedLabel(text: String = ""): JBLabel = JBLabel(text).apply {
    foreground = Palette.muted
    font = JBUI.Fonts.smallFont()
}
