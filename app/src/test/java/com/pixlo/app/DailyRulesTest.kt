package com.pixlo.app
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DailyRulesTest {
    @Test fun firstDayRepeatedTapsToggleBetweenZeroAndOne() {
        val previous = StreakState()
        var state = previous
        repeat(20) { index ->
            state = DailyRules.toggle(state, previous, 100)
            assertEquals(if (index % 2 == 0) StreakState(1, 100) else previous, state)
        }
    }
    @Test fun secondDayRepeatedTapsToggleBetweenOneAndTwo() {
        val previous = StreakState(1, 100)
        var state = previous
        repeat(20) { index ->
            state = DailyRules.toggle(state, previous, 101)
            assertEquals(if (index % 2 == 0) StreakState(2, 101) else previous, state)
        }
    }
    @Test fun undoAfterMissedDaysRestoresExactPreviousState() {
        val previous = StreakState(5, 90)
        val checked = DailyRules.toggle(previous, previous, 100)
        assertEquals(StreakState(1, 100), checked)
        val undone = DailyRules.toggle(checked, previous, 100)
        assertEquals(previous, undone)
        assertEquals(0, DailyRules.visibleStreak(undone, 100))
    }
    @Test fun existingWidgetWithoutSnapshotCanUndoTodaysCheckin() {
        assertEquals(StreakState(), DailyRules.toggle(StreakState(1, 100), null, 100))
        assertEquals(StreakState(1, 99), DailyRules.toggle(StreakState(2, 100), null, 100))
    }
    @Test fun sameDayTapIsIdempotent() {
        val first = DailyRules.tap(StreakState(), 100)
        assertEquals(StreakState(1, 100), first)
        assertEquals(first, DailyRules.tap(first, 100))
    }
    @Test fun consecutiveTapAndMissedDayReset() {
        val two = DailyRules.tap(DailyRules.tap(StreakState(), 100), 101)
        assertEquals(2, two.count)
        assertEquals(2, DailyRules.visibleStreak(two, 102))
        assertEquals(0, DailyRules.visibleStreak(two, 103))
        assertEquals(StreakState(1, 103), DailyRules.tap(two, 103))
    }
    @Test fun independentHabitsAndClockChange() {
        val study = DailyRules.tap(StreakState(), 10)
        val gym = DailyRules.tap(DailyRules.tap(StreakState(), 9), 10)
        assertEquals(1, study.count); assertEquals(2, gym.count)
        assertEquals(StreakState(1, 8), DailyRules.tap(gym, 8))
    }
    @Test fun yearBoundariesAndLeapDay() {
        val start = DailyRules.year(LocalDate.of(2026, 1, 1)); assertEquals(365, start.total); assertEquals(1, start.filled); assertEquals(364, start.left)
        val end = DailyRules.year(LocalDate.of(2026, 12, 31)); assertEquals(0, end.left); assertEquals(100, end.percentComplete)
        val leap = DailyRules.year(LocalDate.of(2028, 2, 29)); assertEquals(366, leap.total); assertEquals(60, leap.filled)
        val example = DailyRules.year(LocalDate.of(2026, 10, 22)); assertEquals(70, example.left); assertEquals(81, example.percentComplete)
    }
    @Test fun dailySelectionIsStableAndChangesNextDay() {
        val date = LocalDate.of(2026, 10, 9)
        assertEquals(DailyRules.dailyIndex(date, 5), DailyRules.dailyIndex(date, 5))
        assertNotEquals(DailyRules.dailyIndex(date, 5), DailyRules.dailyIndex(date.plusDays(1), 5))
        assertEquals(0, DailyRules.dailyIndex(date, 0))
        QuoteBank.genres.forEach { assertTrue(QuoteBank.today(it, date).isNotBlank()); assertNotEquals(QuoteBank.today(it, date), QuoteBank.today(it, date.plusDays(1))) }
    }
}
