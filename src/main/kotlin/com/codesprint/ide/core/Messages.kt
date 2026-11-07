package com.codesprint.ide.core

import kotlin.random.Random

/** Motivational copy. Plain text only: the interface never uses emojis. */
object Messages {
    val DAILY: List<String> = listOf(
        "Every expert was once a beginner.",
        "Consistency creates progress.",
        "Focus on improvement, not perfection.",
        "Every problem solved makes you better.",
        "Small sessions add up to real skill.",
        "Show up today and the results will follow.",
        "Clear code starts with a clear mind.",
        "Progress is built one focused session at a time.",
        "Debugging is how you learn how things really work.",
        "Start with the next small step.",
        "Steady practice beats occasional intensity.",
        "You do not need to be perfect to move forward.",
    )

    val COMPLETION: List<String> = listOf(
        "Session complete. Steady work like this builds real skill.",
        "Well done. Consistency creates progress.",
        "Another focused session logged. Keep the momentum.",
        "Good work. Every problem solved makes you better.",
        "Focus session finished. Small wins compound.",
        "Nice work. Rest, then continue.",
    )

    const val GOAL_REACHED = "Daily goal reached. Excellent consistency today."

    val BREAK: List<String> = listOf(
        "Look away from the screen and rest your eyes.",
        "Stand up, stretch and drink some water.",
        "Rest is part of the work.",
    )

    val QUOTES: List<String> = listOf(
        "Focus on one problem at a time. Quality over speed.",
        "Small, consistent sessions build lasting engineering skill.",
        "The best way to understand complex systems is piece by piece.",
        "Deep work is where real problem-solving happens.",
        "Write code for the developer who has to maintain it tomorrow.",
        "Clear logic comes from unbroken concentration.",
        "Patience in debugging turns confusion into mastery.",
        "Simplicity is the prerequisite for reliability.",
        "Keep your workspace clear and your focus sharp.",
        "Great software is iterative. Refine step by step.",
        "Every edge case handled is a defect prevented.",
        "First make it work, then make it right, then make it clean.",
        "Discipline turns occasional talent into steady craftsmanship.",
        "Good code is its own documentation; strong focus is its foundation.",
        "One clean abstraction is worth a thousand lines of patchwork.",
        "Progress is rarely instant, but consistency makes it inevitable.",
        "Stay curious, stay focused, and keep learning.",
        "Every senior engineer was once stuck on the exact same error.",
        "Break large challenges into smaller, verifiable components.",
        "Thoughtful pauses prevent rushed mistakes.",
        "Craftsmanship is caring about the parts nobody sees.",
        "Take pride in your craft; precision matters.",
        "A steady pace wins the marathon of software development.",
        "Solve the problem first, then write the code.",
        "A quiet mind produces elegant architecture.",
        "Focus is the art of knowing what to ignore.",
        "Code with intention. Every line should have a purpose.",
        "Refactoring is continuous refinement, not a chore.",
        "Embrace difficult challenges: hard problems build sharp minds.",
        "Mastery is the accumulation of small, disciplined efforts.",
        "Test early, test often, and trust your verified progress.",
        "Reduce cognitive load by writing concise, readable functions.",
        "Tenacity and careful thought untangle the hardest bugs.",
        "Continuous improvement always outperforms sporadic bursts.",
        "Protect your focus time; it is your most valuable asset.",
    )

    /** Stable string hash so the daily message does not depend on JVM hashing details. */
    private fun hash(text: String): Long {
        var h = 0L
        for (ch in text) h = (h * 31 + ch.code) and 0xFFFFFFFFL
        return h
    }

    /** Same message all day, different message on different days. */
    fun daily(dayKey: String): String = DAILY[(hash(dayKey) % DAILY.size).toInt()]

    fun breakMessage(dayKey: String): String = BREAK[(hash(dayKey) % BREAK.size).toInt()]

    /** [sessionsToday] is the 1-based count of completed focus sessions today. */
    fun completion(sessionsToday: Int): String =
        COMPLETION[maxOf(0, sessionsToday - 1) % COMPLETION.size]

    /** Picks a random quote, avoiding the previously shown index. Returns (quote, index). */
    fun randomQuote(excludeIndex: Int = -1, random: Random = Random.Default): Pair<String, Int> {
        if (QUOTES.size == 1) return QUOTES[0] to 0
        var index: Int
        do {
            index = random.nextInt(QUOTES.size)
        } while (index == excludeIndex)
        return QUOTES[index] to index
    }
}
