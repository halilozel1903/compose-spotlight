package io.github.halilozel1903.spotlight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates and remembers a [SpotlightState].
 */
@Composable
public fun rememberSpotlightState(): SpotlightState = remember { SpotlightState() }

/**
 * Drives a spotlight tour: which targets exist, which one is highlighted and
 * whether the tour is running.
 *
 * Targets register themselves with [Modifier.spotlightTarget][spotlightTarget];
 * steps are ordered by their `order` value.
 */
@Stable
public class SpotlightState {
    private val targets = mutableStateMapOf<String, SpotlightTarget>()

    /** `true` while the tour is on screen. */
    public var isActive: Boolean by mutableStateOf(false)
        private set

    /** Index of the highlighted step within [steps]. */
    public var currentIndex: Int by mutableIntStateOf(0)
        private set

    internal var onFinish: (completed: Boolean) -> Unit = {}

    /** Registered steps, in tour order. */
    public val steps: List<SpotlightStep>
        get() = orderedTargets().mapIndexed { index, target -> target.toStep(index, targets.size) }

    /** The highlighted step, or `null` when the tour is not running. */
    public val currentStep: SpotlightStep?
        get() = if (isActive) steps.getOrNull(currentIndex) else null

    internal val currentTarget: SpotlightTarget?
        get() = if (isActive) orderedTargets().getOrNull(currentIndex) else null

    private var pendingStartIndex: Int? = null

    /**
     * Starts the tour at [index].
     *
     * Safe to call before the targets have been laid out (for example from a
     * `LaunchedEffect` on the first frame): the tour then begins as soon as the
     * first target registers.
     */
    public fun start(index: Int = 0) {
        if (targets.isEmpty()) {
            pendingStartIndex = index.coerceAtLeast(0)
            return
        }
        pendingStartIndex = null
        currentIndex = index.coerceIn(0, targets.size - 1)
        isActive = true
    }

    /** Starts the tour at the step with [key], if it is registered. */
    public fun start(key: String) {
        val index = orderedTargets().indexOfFirst { it.key == key }
        if (index >= 0) start(index)
    }

    /** Moves to the next step, or finishes the tour after the last one. */
    public fun next() {
        if (!isActive) return
        if (currentIndex < targets.size - 1) currentIndex++ else finish(completed = true)
    }

    /** Moves back one step. */
    public fun previous() {
        if (isActive && currentIndex > 0) currentIndex--
    }

    /** Ends the tour early. */
    public fun skip() {
        if (isActive) finish(completed = false)
    }

    private fun finish(completed: Boolean) {
        pendingStartIndex = null
        isActive = false
        currentIndex = 0
        onFinish(completed)
    }

    private fun orderedTargets(): List<SpotlightTarget> =
        targets.values.sortedWith(compareBy<SpotlightTarget> { it.order }.thenBy { it.key })

    internal fun register(key: String, spec: SpotlightSpec, boundsInWindow: Rect) {
        val target = SpotlightTarget(key, spec.order, spec, boundsInWindow)
        if (targets[key] != target) targets[key] = target
        pendingStartIndex?.let { index ->
            // Other targets usually register in the same frame, so keep the requested index.
            pendingStartIndex = null
            currentIndex = index
            isActive = true
        }
    }

    internal fun unregister(key: String) {
        targets.remove(key)
        if (isActive) {
            if (targets.isEmpty()) {
                finish(completed = false)
            } else if (currentIndex >= targets.size) {
                currentIndex = targets.size - 1
            }
        }
    }
}

/** A step as exposed to tooltip content. */
@Immutable
public data class SpotlightStep(
    val key: String,
    val index: Int,
    val count: Int,
    val title: String,
    val description: String?,
) {
    public val isFirst: Boolean get() = index == 0
    public val isLast: Boolean get() = index == count - 1
}

/** The cut-out drawn around a target. */
@Immutable
public sealed interface SpotlightShape {
    /** A circle that encloses the target. Great for icon buttons and FABs. */
    public data object Circle : SpotlightShape

    /** A rounded rectangle around the target. */
    public data class RoundedRect(val cornerRadius: Dp = 16.dp) : SpotlightShape
}

@Immutable
internal data class SpotlightSpec(
    val order: Int,
    val title: String,
    val description: String?,
    val shape: SpotlightShape,
    val padding: Dp,
)

@Immutable
internal data class SpotlightTarget(
    val key: String,
    val order: Int,
    val spec: SpotlightSpec,
    val boundsInWindow: Rect,
) {
    fun toStep(index: Int, count: Int) = SpotlightStep(
        key = key,
        index = index,
        count = count,
        title = spec.title,
        description = spec.description,
    )
}
