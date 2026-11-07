package com.codesprint.ide.ui

import com.codesprint.ide.actions.ModeSwitcher
import com.codesprint.ide.core.SessionMode
import com.codesprint.ide.core.Settings
import com.codesprint.ide.core.TimeUtil
import com.codesprint.ide.core.TimerStatus
import com.codesprint.ide.core.ViewModelBuilder
import com.codesprint.ide.settings.CodeSprintConfigurable
import com.codesprint.ide.service.MotivationService
import com.codesprint.ide.service.TimerService
import com.codesprint.ide.state.CodeSprintData
import com.codesprint.ide.state.CodeSprintSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridLayout
import java.awt.Rectangle
import java.awt.event.HierarchyEvent
import javax.swing.ButtonGroup
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JToggleButton
import javax.swing.Scrollable
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants

/** Vertical column that always matches the width of the tool window (no horizontal scrolling). */
private class ScrollableColumn : JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, JBUI.scale(10), true, false)), Scrollable {
    override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
    override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int = JBUI.scale(16)
    override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int): Int = JBUI.scale(80)
    override fun getScrollableTracksViewportWidth(): Boolean = true
    override fun getScrollableTracksViewportHeight(): Boolean = false
}

private fun iconButton(icon: Icon, tooltip: String, onClick: () -> Unit) = JButton(icon).apply {
    toolTipText = tooltip
    accessibleContext.accessibleName = tooltip
    isBorderPainted = false
    isContentAreaFilled = false
    border = JBUI.Borders.empty(4)
    addActionListener { onClick() }
}

/**
 * The CodeSprint dashboard: timer, daily goal, statistics, weekly chart and a daily
 * reminder. It holds no business logic. Everything shown comes from
 * [ViewModelBuilder], and every user action is forwarded to [TimerService].
 *
 * Threading: UI thread only.
 */
class DashboardPanel(private val project: Project) : Disposable {
    private val service = TimerService.getInstance()
    private val motivation = MotivationService.getInstance()
    private val settings = CodeSprintSettings.getInstance()
    private val data = CodeSprintData.getInstance()

    private var updating = false

    // header / welcome
    private val greeting = JBLabel("Welcome").apply { font = font.deriveFont(Font.BOLD, JBUI.scale(15).toFloat()) }
    private val welcomeSub = WrappedText(muted = true)

    // timer
    private val modeButtons = mapOf(
        SessionMode.FOCUS to JToggleButton("Focus"),
        SessionMode.SHORT_BREAK to JToggleButton("Short"),
        SessionMode.LONG_BREAK to JToggleButton("Long"),
    )
    private val ring = RingComponent()
    private val timerStatus = mutedLabel("Ready to start").apply { horizontalAlignment = SwingConstants.CENTER }
    private val toggleButton = JButton("Start", AllIcons.Actions.Execute)
    private val resetButton = JButton("Reset", AllIcons.Actions.Rollback)
    private val dots = DotsComponent()

    // goal
    private val goalDone = JBLabel("0m").apply { font = font.deriveFont(Font.BOLD) }
    private val goalTarget = mutedLabel("of 2h")
    private val goalPercent = JBLabel("0%").apply { font = font.deriveFont(Font.BOLD) }
    private val goalBar = BarComponent()
    private val goalStatus = mutedLabel("")
    private val goalSpinner = JSpinner(
        SpinnerNumberModel(120, Settings.GOAL_RANGE.first, Settings.GOAL_RANGE.last, 5),
    )

    // statistics
    private val statValues = linkedMapOf(
        "Today" to JBLabel("0m"),
        "Sessions" to JBLabel("0"),
        "This week" to JBLabel("0m"),
        "Goal" to JBLabel("0%"),
        "Streak" to JBLabel("0 days"),
        "Best streak" to JBLabel("0 days"),
    )

    // week and quote
    private val weekChart = WeekChart()
    private val weekSummary = mutedLabel("")
    private val quoteKind = mutedLabel("Daily reminder")
    private val quoteText = WrappedText()

    val component: JComponent

    init {
        component = buildUi()
        service.addListener(this) { refresh() }
        motivation.addListener(this) { refresh() }
        settings.addListener(this) { refresh() }
        component.addHierarchyListener { e ->
            if ((e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L && component.isShowing) refresh()
        }
        refresh()
    }

    // --- layout ---------------------------------------------------------------

    private fun buildUi(): JComponent {
        val column = ScrollableColumn().apply { border = JBUI.Borders.empty(10) }
        column.add(buildHeader())
        column.add(buildWelcome())
        column.add(buildTimerCard())
        column.add(section("goal", "Daily goal", buildGoalBody()))
        column.add(section("stats", "Statistics", buildStatsBody()))
        column.add(section("week", "Weekly overview", buildWeekBody()))
        column.add(buildQuoteCard())

        return JBScrollPane(column, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER).apply {
            border = JBUI.Borders.empty()
            accessibleContext.accessibleName = "CodeSprint dashboard"
        }
    }

    private fun buildHeader(): JComponent {
        val brand = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(JBLabel(CodeSprintIcons.Logo))
            add(JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, false, false)).apply {
                isOpaque = false
                add(JBLabel("CodeSprint").apply { font = font.deriveFont(Font.BOLD, JBUI.scale(14).toFloat()) })
                add(mutedLabel("Focus. Code. Improve."))
            })
        }
        val settingsButton = iconButton(AllIcons.General.GearPlain, "Settings") {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, CodeSprintConfigurable::class.java)
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            add(brand, BorderLayout.CENTER)
            add(settingsButton, BorderLayout.EAST)
        }
    }

    private fun buildWelcome(): JComponent = JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, JBUI.scale(2), true, false)).apply {
        isOpaque = false
        add(greeting)
        add(welcomeSub)
    }

    private fun buildTimerCard(): JComponent {
        val tabs = JPanel(GridLayout(1, 3, JBUI.scale(4), 0)).apply { isOpaque = false }
        val group = ButtonGroup()
        modeButtons.forEach { (mode, button) ->
            group.add(button)
            tabs.add(button)
            button.accessibleContext.accessibleName = mode.label
            button.addActionListener { if (!updating) ModeSwitcher.switchTo(project, mode) }
        }

        toggleButton.font = toggleButton.font.deriveFont(Font.BOLD)
        // Keep the button width stable while its label changes between Start, Pause and Resume.
        toggleButton.text = "Resume"
        val toggleSize = toggleButton.preferredSize
        toggleButton.text = "Start"
        toggleButton.preferredSize = toggleSize
        toggleButton.addActionListener { service.toggle() }
        resetButton.accessibleContext.accessibleName = "Reset timer"
        resetButton.addActionListener { service.reset() }
        val controls = JPanel(FlowLayout(FlowLayout.CENTER, JBUI.scale(8), 0)).apply {
            isOpaque = false
            add(toggleButton)
            add(resetButton)
        }

        val body = JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, JBUI.scale(8), true, false)).apply {
            isOpaque = false
            add(tabs)
            add(JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply { isOpaque = false; add(ring) })
            add(timerStatus)
            add(controls)
            add(dots)
        }
        return RoundedCard().apply { add(body, BorderLayout.CENTER) }
    }

    private fun section(id: String, title: String, body: JComponent): JComponent =
        CollapsibleSection(title, body, expanded = !data.isCollapsed(id)) { expanded ->
            data.setCollapsed(id, !expanded)
        }

    private fun buildGoalBody(): JComponent {
        val figures = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
            isOpaque = false
            add(goalDone)
            add(goalTarget)
        }
        val topRow = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(figures, BorderLayout.WEST)
            add(goalPercent, BorderLayout.EAST)
        }

        val chips = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
            isOpaque = false
            listOf("30m" to 30, "1h" to 60, "2h" to 120, "3h" to 180).forEach { (label, minutes) ->
                add(JButton(label).apply {
                    accessibleContext.accessibleName = "Set daily goal to $label"
                    addActionListener { settings.update { it.copy(dailyGoalMinutes = minutes) } }
                })
            }
        }

        goalSpinner.accessibleContext.accessibleName = "Custom daily goal in minutes"
        goalSpinner.addChangeListener {
            if (!updating) settings.update { it.copy(dailyGoalMinutes = (goalSpinner.value as Number).toInt()) }
        }
        val custom = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
            isOpaque = false
            add(mutedLabel("Custom target (minutes)"))
            add(goalSpinner)
        }

        return JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, JBUI.scale(6), true, false)).apply {
            isOpaque = false
            add(topRow)
            add(goalBar)
            add(goalStatus)
            add(chips)
            add(custom)
        }
    }

    private fun buildStatsBody(): JComponent {
        val grid = JPanel(GridLayout(0, 2, JBUI.scale(8), JBUI.scale(8))).apply { isOpaque = false }
        statValues.forEach { (name, value) ->
            value.font = value.font.deriveFont(Font.BOLD, JBUI.scale(13).toFloat())
            grid.add(JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false)).apply {
                isOpaque = false
                add(mutedLabel(name))
                add(value)
            })
        }
        return grid
    }

    private fun buildWeekBody(): JComponent =
        JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, JBUI.scale(6), true, false)).apply {
            isOpaque = false
            add(weekChart)
            add(weekSummary)
        }

    private fun buildQuoteCard(): JComponent {
        val body = JPanel(VerticalFlowLayout(VerticalFlowLayout.TOP, 0, JBUI.scale(2), true, false)).apply {
            isOpaque = false
            add(quoteKind)
            add(quoteText)
        }
        return RoundedCard().apply {
            accessibleContext.accessibleName = "Motivation"
            add(JBLabel(AllIcons.General.BalloonInformation).apply { verticalAlignment = SwingConstants.TOP }, BorderLayout.WEST)
            add(body, BorderLayout.CENTER)
        }
    }

    // --- rendering -------------------------------------------------------------

    fun refresh() {
        if (!component.isShowing && component.parent != null) return // re-rendered when shown again
        val now = System.currentTimeMillis()
        val vm = ViewModelBuilder.build(
            now = now,
            timer = service.snapshot(),
            settings = settings.get(),
            history = data.history,
            completionMessage = service.completionMessage,
            motivationalQuote = motivation.currentQuote,
        )
        updating = true
        try {
            render(vm)
        } finally {
            updating = false
        }
    }

    private fun render(vm: com.codesprint.ide.core.ViewModel) {
        val t = vm.timer
        val running = t.status == TimerStatus.RUNNING
        val paused = t.status == TimerStatus.PAUSED
        val clock = TimeUtil.formatClock(t.remainingMs)

        greeting.text = vm.greeting
        welcomeSub.text = vm.welcome

        modeButtons.forEach { (mode, button) -> button.isSelected = mode == t.mode }
        ring.progress = t.progress
        ring.timeText = clock
        ring.subText = if (t.mode == SessionMode.FOCUS) "Focus session" else t.mode.label
        ring.accessibleContext.accessibleName = "${t.mode.label}, $clock remaining"
        timerStatus.text = if (running) "Running" else if (paused) "Paused" else "Ready to start"
        toggleButton.text = if (running) "Pause" else if (paused) "Resume" else "Start"
        toggleButton.icon = if (running) AllIcons.Actions.Pause else AllIcons.Actions.Execute
        resetButton.isEnabled = t.status != TimerStatus.IDLE
        dots.update(t.cycleDone, t.cycleLength)

        val goal = vm.goal
        goalDone.text = TimeUtil.formatDuration(goal.todayMs)
        goalTarget.text = "of ${TimeUtil.formatDuration(goal.goalMs)}"
        goalPercent.text = "${goal.percent}%"
        goalBar.fraction = goal.percent / 100.0
        goalBar.complete = goal.met
        goalStatus.text = if (goal.met) "Daily goal reached" else "${TimeUtil.formatDuration(goal.remainingMs)} remaining"
        goalBar.accessibleContext.accessibleDescription = "${goal.percent} percent of the daily goal"
        if ((goalSpinner.value as Number).toInt() != vm.settings.dailyGoalMinutes) {
            goalSpinner.value = vm.settings.dailyGoalMinutes
        }

        val s = vm.stats
        statValues["Today"]!!.text = TimeUtil.formatDuration(s.todayFocusMs)
        statValues["Sessions"]!!.text = s.totalSessions.toString()
        statValues["This week"]!!.text = TimeUtil.formatDuration(s.weekFocusMs)
        statValues["Goal"]!!.text = "${s.goalPercent}%"
        statValues["Streak"]!!.text = days(s.currentStreak)
        statValues["Best streak"]!!.text = days(s.longestStreak)

        weekChart.update(s.week, s.weekMaxMs)
        weekSummary.text = "${s.weekActiveDays} of 7 days active this week"

        quoteKind.text = vm.messageKind.label
        quoteText.text = vm.message
    }

    private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

    override fun dispose() = Unit
}
