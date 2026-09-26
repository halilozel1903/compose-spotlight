package io.github.halilozel1903.spotlight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TooltipPlacementTest {

    private fun place(
        anchorTop: Float,
        anchorBottom: Float,
        anchorLeft: Float = 100f,
        anchorRight: Float = 200f,
        width: Int = 300,
        height: Int = 150,
    ) = TooltipPlacement.calculate(
        anchorLeft = anchorLeft,
        anchorTop = anchorTop,
        anchorRight = anchorRight,
        anchorBottom = anchorBottom,
        tooltipWidth = width,
        tooltipHeight = height,
        containerWidth = 1000,
        containerHeight = 2000,
        gap = 10,
        margin = 20,
    )

    @Test
    fun prefersBelowTheAnchor() {
        val result = place(anchorTop = 100f, anchorBottom = 200f)
        assertFalse(result.isAbove)
        assertEquals(210, result.y)
    }

    @Test
    fun goesAboveWhenThereIsNoRoomBelow() {
        val result = place(anchorTop = 1800f, anchorBottom = 1900f)
        assertTrue(result.isAbove)
        assertEquals(1800 - 10 - 150, result.y)
    }

    @Test
    fun centersHorizontallyOnTheAnchor() {
        val result = place(anchorTop = 100f, anchorBottom = 200f, anchorLeft = 400f, anchorRight = 600f)
        assertEquals(500 - 150, result.x)
    }

    @Test
    fun clampsToTheContainerEdges() {
        val left = place(anchorTop = 100f, anchorBottom = 200f, anchorLeft = 0f, anchorRight = 40f)
        assertEquals(20, left.x)

        val right = place(anchorTop = 100f, anchorBottom = 200f, anchorLeft = 960f, anchorRight = 1000f)
        assertEquals(1000 - 20 - 300, right.x)
    }

    @Test
    fun staysOnScreenForHugeAnchors() {
        val result = place(anchorTop = 0f, anchorBottom = 2000f, height = 400)
        assertTrue(result.y in 20..(2000 - 20 - 400))
    }
}
