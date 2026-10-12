package com.pixlo.app

import java.time.LocalDate
import kotlin.math.roundToInt

data class YearProgress(val total: Int, val filled: Int, val left: Int, val percentComplete: Int)
data class StreakState(val count: Int = 0, val lastDay: Long? = null)

object DailyRules {
    fun year(date: LocalDate): YearProgress {
        val total = date.lengthOfYear()
        val filled = date.dayOfYear
        return YearProgress(total, filled, total - filled, (filled * 100.0 / total).roundToInt())
    }
    fun visibleStreak(state: StreakState, today: Long): Int =
        if (state.lastDay == null || today - state.lastDay !in 0L..1L) 0 else state.count
    fun tap(state: StreakState, today: Long): StreakState = when {
        state.lastDay == today -> state
        state.lastDay == today - 1 -> StreakState(state.count + 1, today)
        else -> StreakState(1, today)
    }
    fun toggle(state: StreakState, previous: StreakState?, today: Long): StreakState =
        if (state.lastDay == today) {
            previous ?: if (state.count > 1) StreakState(state.count - 1, today - 1) else StreakState()
        } else tap(state, today)
    fun dailyIndex(date: LocalDate, count: Int): Int = if (count <= 0) 0 else Math.floorMod(date.toEpochDay(), count.toLong()).toInt()
}

object QuoteBank {
    val genres = listOf("Work", "Gym", "Time", "Study", "Heartbreak")
    private val quotes = mapOf(
        "Work" to listOf("Small steps. Real progress.", "Begin with one useful thing.", "Consistency makes quiet work count.", "Give the next hour a clear purpose.", "Progress starts with showing up.", "Finish a little. Build a lot.", "Focus on the work within your reach."),
        "Gym" to listOf("Show up. Move. Repeat.", "Stronger begins with today's effort.", "One more honest rep.", "Train for the person you are becoming.", "Rest well. Return stronger.", "Small sessions still count.", "Keep your promise to your body."),
        "Time" to listOf("This moment is yours to use.", "Make room for what matters.", "Start before the perfect moment arrives.", "Spend today with intention.", "Less rushing. More meaning.", "Protect an hour for your future.", "A little time used well adds up."),
        "Study" to listOf("One page closer.", "Understanding grows one question at a time.", "Begin with the part you can learn today.", "Practice makes knowledge your own.", "Stay curious. Keep learning.", "A focused hour beats a worried day.", "Learn slowly. Remember deeply."),
        "Heartbreak" to listOf("Healing has its own pace.", "You can miss someone and move forward.", "Be gentle with the person you are becoming.", "Today can hold a small new beginning.", "Your story still has room to grow.", "Let peace return one day at a time.", "You deserve kindness from yourself.")
    )
    fun today(genre: String, date: LocalDate = LocalDate.now()): String {
        val list = quotes[genre] ?: quotes.getValue("Work")
        return list[DailyRules.dailyIndex(date, list.size)]
    }
}
