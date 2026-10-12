package com.pixlo.app

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WidgetToggleTest {
    @Test fun repeatedTapsRestoreYesterdayAcrossStoreRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = -2001
        val today = LocalDate.now().toEpochDay()
        val previous = StreakState(1, today - 1)
        val store = HabitStore(context)
        store.remove(id)
        store.save(id, Habit(name = "Toggle test", state = previous))
        try {
            repeat(20) { index ->
                HabitStore(context).tap(id)
                assertEquals(if (index % 2 == 0) StreakState(2, today) else previous, HabitStore(context).get(id).state)
            }
        } finally { store.remove(id) }
    }

    @Test fun missedDayUndoRestoresStoredHistory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = -2002
        val today = LocalDate.now().toEpochDay()
        val previous = StreakState(4, today - 5)
        val store = HabitStore(context)
        store.remove(id)
        store.save(id, Habit(name = "Missed day test", state = previous))
        try {
            store.tap(id)
            assertEquals(StreakState(1, today), HabitStore(context).get(id).state)
            HabitStore(context).tap(id)
            assertEquals(previous, HabitStore(context).get(id).state)
        } finally { store.remove(id) }
    }
}
