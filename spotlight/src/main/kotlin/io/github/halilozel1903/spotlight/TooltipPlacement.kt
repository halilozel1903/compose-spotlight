package io.github.halilozel1903.spotlight

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure placement math for the tooltip, kept free of Compose types so it is easy to test.
 */
internal object TooltipPlacement {

    data class Result(val x: Int, val y: Int, val isAbove: Boolean)

    /**
     * Places a tooltip of `tooltipWidth × tooltipHeight` next to the anchor.
     *
     * Prefers below the anchor; goes above when it doesn't fit below and there is
     * more room above. Horizontally it centers on the anchor and is clamped so it
     * stays at least [margin] away from the container edges.
     */
    fun calculate(
        anchorLeft: Float,
        anchorTop: Float,
        anchorRight: Float,
        anchorBottom: Float,
        tooltipWidth: Int,
        tooltipHeight: Int,
        containerWidth: Int,
        containerHeight: Int,
        gap: Int,
        margin: Int,
    ): Result {
        val spaceBelow = containerHeight - anchorBottom - gap - margin
        val spaceAbove = anchorTop - gap - margin
        val isAbove = tooltipHeight > spaceBelow && spaceAbove > spaceBelow

        val preferredY = if (isAbove) anchorTop - gap - tooltipHeight else anchorBottom + gap
        val maxY = max(margin, containerHeight - margin - tooltipHeight)
        val y = preferredY.roundToInt().coerceIn(margin, maxY)

        val anchorCenterX = (anchorLeft + anchorRight) / 2f
        val maxX = max(margin, containerWidth - margin - tooltipWidth)
        val x = (anchorCenterX - tooltipWidth / 2f).roundToInt().coerceIn(margin, maxX)

        return Result(x = x, y = y, isAbove = isAbove)
    }
}
