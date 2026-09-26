package io.github.halilozel1903.spotlight

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Marks this element as a step of the spotlight tour driven by [state].
 *
 * ```kotlin
 * IconButton(
 *     onClick = { … },
 *     modifier = Modifier.spotlightTarget(
 *         state = spotlight,
 *         key = "search",
 *         order = 1,
 *         title = "Search everything",
 *         description = "Find notes, tags and people in one place.",
 *         shape = SpotlightShape.Circle,
 *     ),
 * )
 * ```
 *
 * @param key Unique id of the step.
 * @param order Position in the tour; lower comes first.
 * @param padding Extra space between the element and the cut-out.
 */
public fun Modifier.spotlightTarget(
    state: SpotlightState,
    key: String,
    order: Int,
    title: String,
    description: String? = null,
    shape: SpotlightShape = SpotlightShape.RoundedRect(),
    padding: Dp = 8.dp,
): Modifier = this then SpotlightTargetElement(
    state = state,
    key = key,
    spec = SpotlightSpec(order, title, description, shape, padding),
)

private data class SpotlightTargetElement(
    val state: SpotlightState,
    val key: String,
    val spec: SpotlightSpec,
) : ModifierNodeElement<SpotlightTargetNode>() {

    override fun create(): SpotlightTargetNode = SpotlightTargetNode(state, key, spec)

    override fun update(node: SpotlightTargetNode) {
        node.update(state, key, spec)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "spotlightTarget"
        properties["key"] = key
        properties["order"] = spec.order
        properties["title"] = spec.title
    }
}

private class SpotlightTargetNode(
    private var state: SpotlightState,
    private var key: String,
    private var spec: SpotlightSpec,
) : Modifier.Node(), GlobalPositionAwareModifierNode {

    private var lastCoordinates: LayoutCoordinates? = null

    fun update(state: SpotlightState, key: String, spec: SpotlightSpec) {
        if (state != this.state || key != this.key) {
            this.state.unregister(this.key)
        }
        this.state = state
        this.key = key
        this.spec = spec
        lastCoordinates?.takeIf { it.isAttached }?.let(::publish)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        lastCoordinates = coordinates
        publish(coordinates)
    }

    override fun onDetach() {
        lastCoordinates = null
        state.unregister(key)
    }

    private fun publish(coordinates: LayoutCoordinates) {
        state.register(key, spec, coordinates.boundsInWindow())
    }
}
