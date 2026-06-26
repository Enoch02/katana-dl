package com.enoch02

import kotlin.test.Test
import kotlin.test.assertEquals

class MainTest {

    @Test
    fun testParseIndicesSingleValue() {
        val maxIndex = 10
        val result = parseIndices("3", maxIndex)
        assertEquals(setOf(3), result)
    }

    @Test
    fun testParseIndicesMultipleValues() {
        val maxIndex = 10
        val result = parseIndices("1, 3, 5", maxIndex)
        assertEquals(setOf(1, 3, 5), result)
    }

    @Test
    fun testParseIndicesRange() {
        val maxIndex = 10
        val result = parseIndices("2-5", maxIndex)
        assertEquals(setOf(2, 3, 4, 5), result)
    }

    @Test
    fun testParseIndicesMixed() {
        val maxIndex = 10
        val result = parseIndices("1, 3-5, 8", maxIndex)
        assertEquals(setOf(1, 3, 4, 5, 8), result)
    }

    @Test
    fun testParseIndicesAll() {
        val maxIndex = 5
        val result = parseIndices("all", maxIndex)
        assertEquals(setOf(1, 2, 3, 4, 5), result)
    }

    @Test
    fun testParseIndicesAllCaseInsensitive() {
        val maxIndex = 3
        val result = parseIndices("ALL", maxIndex)
        assertEquals(setOf(1, 2, 3), result)
    }

    @Test
    fun testParseIndicesOutOfBounds() {
        val maxIndex = 5
        val result = parseIndices("0, 6, 2", maxIndex)
        assertEquals(setOf(2), result) // 0 and 6 should be filtered out
    }

    @Test
    fun testParseIndicesOutOfBoundsRange() {
        val maxIndex = 5
        val result = parseIndices("3-10", maxIndex)
        assertEquals(setOf(3, 4, 5), result) // Range is coerced to maxIndex
    }

    @Test
    fun testParseIndicesReverseRange() {
        val maxIndex = 10
        val result = parseIndices("5-2", maxIndex)
        assertEquals(setOf(2, 3, 4, 5), result)
    }

    @Test
    fun testParseIndicesEmptyAndInvalid() {
        val maxIndex = 10
        val result = parseIndices("", maxIndex)
        assertEquals(emptySet(), result)

        val invalidResult = parseIndices("abc, 1-x, -5", maxIndex)
        assertEquals(emptySet(), invalidResult)
    }
}
