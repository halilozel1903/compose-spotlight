package io.github.halilozel1903.spotlight

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Hosts a spotlight tour on top of [content].
 *
 * Put it around the screen (or the whole app) that contains the targets, then
 * call [SpotlightState.start].
 *
 * @param onFinish Called when the tour ends; `completed` is `false` when it was skipped.
 * @param advanceOnScrimTap Tapping outside the tooltip moves to the next step.
 * @param tooltip The tooltip content. Defaults to a Material 3 card with Back / Next / Skip.
 */
@Composable
public fun SpotlightHost(
    state: SpotlightState,
    modifier: Modifier = Modifier,
    colors: SpotlightColors = SpotlightDefaults.colors(),
    advanceOnScrimTap: Boolean = true,
    onFinish: (completed: Boolean) -> Unit = {},
    tooltip: @Composable (step: SpotlightStep, state: SpotlightState) -> Unit = { step, spotlight ->
        SpotlightDefaults.Tooltip(step = step, state = spotlight, colors = colors)
    },
    content: @Composable () -> Unit,
) {
    val currentOnFinish by rememberUpdatedState(onFinish)
    SideEffect { state.onFinish = { completed -> currentOnFinish(completed) } }

    var hostOffset by remember { mutableStateOf(Offset.Zero) }

    // Keep the last target so the overlay can fade out on its final position.
    val target = state.currentTarget
    var lastTarget by remember { mutableStateOf<SpotlightTarget?>(null) }
    val step = state.currentStep
    var lastStep by remember { mutableStateOf<SpotlightStep?>(null) }
    if (target != null && step != null) {
        lastTarget = target
        lastStep = step
    }

    BackHandler(enabled = state.isActive) { state.skip() }

    Box(modifier.onGloballyPositioned { hostOffset = it.positionInWindow() }) {
        content()

        AnimatedVisibility(
            visible = target != null,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200)),
        ) {
            val shownTarget = lastTarget
            val shownStep = lastStep
            if (shownTarget != null && shownStep != null) {
                SpotlightOverlay(
                    target = shownTarget,
                    step = shownStep,
                    hostOffset = hostOffset,
                    state = state,
                    colors = colors,
                    advanceOnScrimTap = advanceOnScrimTap,
                    tooltip = tooltip,
                )
            }
        }
    }
}

@Composable
private fun SpotlightOverlay(
    target: SpotlightTarget,
    step: SpotlightStep,
    hostOffset: Offset,
    state: SpotlightState,
    colors: SpotlightColors,
    advanceOnScrimTap: Boolean,
    tooltip: @Composable (SpotlightStep, SpotlightState) -> Unit,
) {
    val density = LocalDensity.current
    val paddingPx = with(density) { target.spec.padding.toPx() }
    val localBounds = target.boundsInWindow.translate(-hostOffset).inflate(paddingPx)
    val cutout = when (target.spec.shape) {
        SpotlightShape.Circle -> {
            val radius = max(localBounds.width, localBounds.height) / 2f
            Rect(center = localBounds.center, radius = radius)
        }
        is SpotlightShape.RoundedRect -> localBounds
    }

    val animatedCutout by animateRectAsState(
        targetValue = cutout,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "spotlightCutout",
    )
    val pulse by rememberInfiniteTransition(label = "spotlightPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1600), RepeatMode.Restart),
        label = "spotlightPulseProgress",
    )
    val cornerRadiusPx = when (val shape = target.spec.shape) {
        SpotlightShape.Circle -> Float.MAX_VALUE
        is SpotlightShape.RoundedRect -> with(density) { shape.cornerRadius.toPx() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .semantics { paneTitle = "Tour, step ${step.index + 1} of ${step.count}" }
            .pointerInput(state, advanceOnScrimTap) {
                // Swallow every touch so the UI below is not clickable during the tour.
                detectTapGestures { if (advanceOnScrimTap) state.next() }
            },
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(colors.scrim)
            drawCutout(animatedCutout, cornerRadiusPx, Color.Black, blendMode = BlendMode.Clear)
            drawPulse(animatedCutout, cornerRadiusPx, pulse, colors.pulse)
        }

        TooltipLayout(anchor = animatedCutout) {
            tooltip(step, state)
        }
    }
}

private fun DrawScope.drawCutout(rect: Rect, cornerRadius: Float, color: Color, blendMode: BlendMode) {
    val radius = min(cornerRadius, min(rect.width, rect.height) / 2f)
    drawRoundRect(
        color = color,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(radius, radius),
        blendMode = blendMode,
    )
}

private fun DrawScope.drawPulse(rect: Rect, cornerRadius: Float, progress: Float, color: Color) {
    val grow = 18.dp.toPx() * progress
    val ring = rect.inflate(grow)
    val radius = min(cornerRadius + grow, min(ring.width, ring.height) / 2f)
    drawRoundRect(
        color = color.copy(alpha = color.alpha * (1f - progress)),
        topLeft = ring.topLeft,
        size = ring.size,
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = 2.dp.toPx()),
    )
}

@Composable
private fun TooltipLayout(anchor: Rect, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val margin = 16.dp.roundToPx()
        val gap = 12.dp.roundToPx()
        val maxWidth = min(constraints.maxWidth - 2 * margin, 360.dp.roundToPx()).coerceAtLeast(0)
        val placeables = measurables.map { it.measure(Constraints(maxWidth = maxWidth)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { placeable ->
                val position = TooltipPlacement.calculate(
                    anchorLeft = anchor.left,
                    anchorTop = anchor.top,
                    anchorRight = anchor.right,
                    anchorBottom = anchor.bottom,
                    tooltipWidth = placeable.width,
                    tooltipHeight = placeable.height,
                    containerWidth = constraints.maxWidth,
                    containerHeight = constraints.maxHeight,
                    gap = gap,
                    margin = margin,
                )
                placeable.place(position.x, position.y)
            }
        }
    }
}
