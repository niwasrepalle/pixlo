package com.pixlo.app

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RotationRulesTest {
    @Test fun avoidsImmediateRepeat() {
        val random = Random(42)
        for (count in 2..50) for (previous in 0 until count) repeat(20) {
            val next = RotationRules.nextIndex(count, previous, random)
            assertTrue(next in 0 until count)
            assertNotEquals(previous, next)
        }
    }
    @Test fun emptyAndSingleLibraries() {
        assertEquals(0, RotationRules.nextIndex(0, -1))
        assertEquals(0, RotationRules.nextIndex(1, 0))
        assertTrue(RotationRules.nextIndex(5, -1, Random(1)) in 0..4)
    }
    @Test fun intervalLimitsAndPartialBlocks() {
        assertFalse(RotationRules.validSeconds(0)); assertFalse(RotationRules.validSeconds(86401))
        assertTrue(RotationRules.validSeconds(1)); assertTrue(RotationRules.validSeconds(86400))
        assertEquals(3, RotationRules.gridSize(101, 50))
        assertEquals(1, RotationRules.gridSize(1, 128))
    }
}
