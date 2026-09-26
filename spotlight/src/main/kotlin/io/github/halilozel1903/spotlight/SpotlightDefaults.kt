package io.github.halilozel1903.spotlight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Colors used by [SpotlightHost]. */
@Immutable
public data class SpotlightColors(
    val scrim: Color,
    val pulse: Color,
    val tooltipContainer: Color,
    val tooltipContent: Color,
    val accent: Color,
)

/** Defaults for [SpotlightHost]. */
public object SpotlightDefaults {

    /** Colors derived from the current [MaterialTheme]. */
    @Composable
    public fun colors(
        scrim: Color = Color.Black.copy(alpha = 0.72f),
        pulse: Color = MaterialTheme.colorScheme.primary,
        tooltipContainer: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tooltipContent: Color = MaterialTheme.colorScheme.onSurface,
        accent: Color = MaterialTheme.colorScheme.primary,
    ): SpotlightColors = SpotlightColors(scrim, pulse, tooltipContainer, tooltipContent, accent)

    /**
     * The default tooltip: step counter, title, description, progress dots and
     * Skip / Back / Next buttons.
     */
    @Composable
    public fun Tooltip(
        step: SpotlightStep,
        state: SpotlightState,
        colors: SpotlightColors = SpotlightDefaults.colors(),
        modifier: Modifier = Modifier,
        skipLabel: String = "Skip",
        backLabel: String = "Back",
        nextLabel: String = "Next",
        doneLabel: String = "Got it",
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            color = colors.tooltipContainer,
            contentColor = colors.tooltipContent,
            shadowElevation = 12.dp,
            tonalElevation = 3.dp,
        ) {
            Column(Modifier.padding(start = 20.dp, top = 18.dp, end = 12.dp, bottom = 10.dp)) {
                Text(
                    text = "${step.index + 1} / ${step.count}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                )
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp),
                )
                step.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.tooltipContent.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp, end = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgressDots(step = step, accent = colors.accent)
                    Spacer(Modifier.weight(1f))
                    if (!step.isLast) {
                        TextButton(onClick = state::skip) { Text(skipLabel) }
                    }
                    if (!step.isFirst) {
                        TextButton(onClick = state::previous) { Text(backLabel) }
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = state::next) { Text(if (step.isLast) doneLabel else nextLabel) }
                }
            }
        }
    }

    @Composable
    private fun ProgressDots(step: SpotlightStep, accent: Color) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(step.count) { index ->
                val active = index == step.index
                Box(
                    Modifier
                        .size(if (active) 8.dp else 6.dp)
                        .background(accent.copy(alpha = if (active) 1f else 0.3f), CircleShape),
                )
            }
        }
    }
}
