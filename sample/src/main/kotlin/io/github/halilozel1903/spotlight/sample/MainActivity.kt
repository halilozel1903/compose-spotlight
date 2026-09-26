package io.github.halilozel1903.spotlight.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.spotlight.SpotlightHost
import io.github.halilozel1903.spotlight.SpotlightShape
import io.github.halilozel1903.spotlight.rememberSpotlightState
import io.github.halilozel1903.spotlight.spotlightTarget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                // `tourStep` is used by scripts/screenshots.sh to open the tour on a given step.
                NotesScreen(startStep = intent.getIntExtra("tourStep", 0))
            }
        }
    }
}

private val notes = listOf(
    "Groceries" to "Oat milk, blueberries, sourdough",
    "Weekend trip" to "Book the cabin before Friday",
    "Talk ideas" to "Coach marks that don't annoy people",
    "Reading list" to "Designing Data-Intensive Applications",
    "Workout" to "5 km easy run + mobility",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesScreen(startStep: Int = 0) {
    val spotlight = rememberSpotlightState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var onlyPinned by remember { mutableStateOf(false) }
    var tourSeen by rememberSaveable { mutableStateOf(false) }

    // Start the tour on first launch. In a real app, persist `tourSeen` in DataStore.
    LaunchedEffect(Unit) {
        if (!tourSeen) spotlight.start(startStep)
    }

    SpotlightHost(
        state = spotlight,
        onFinish = { completed ->
            tourSeen = true
            scope.launch { snackbar.showSnackbar(if (completed) "Tour completed 🎉" else "Tour skipped") }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Notes") },
                    actions = {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.spotlightTarget(
                                state = spotlight,
                                key = "search",
                                order = 1,
                                title = "Search everything",
                                description = "Find notes, tags and checklists in one place.",
                                shape = SpotlightShape.Circle,
                                padding = 4.dp,
                            ),
                        ) {
                            Icon(painterResource(R.drawable.ic_search), contentDescription = "Search")
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier.spotlightTarget(
                                state = spotlight,
                                key = "profile",
                                order = 4,
                                title = "Your profile",
                                description = "Sync, themes and backups live here.",
                                shape = SpotlightShape.Circle,
                                padding = 4.dp,
                            ),
                        ) {
                            Icon(painterResource(R.drawable.ic_person), contentDescription = "Profile")
                        }
                        IconButton(onClick = { spotlight.start() }) {
                            Icon(painterResource(R.drawable.ic_help), contentDescription = "Replay tour")
                        }
                    },
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {},
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text("New note") },
                    modifier = Modifier.spotlightTarget(
                        state = spotlight,
                        key = "new-note",
                        order = 3,
                        title = "Capture an idea",
                        description = "Tap here to write a new note. Long-press for a checklist.",
                    ),
                )
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = onlyPinned,
                            onClick = { onlyPinned = !onlyPinned },
                            label = { Text("Pinned") },
                            modifier = Modifier.spotlightTarget(
                                state = spotlight,
                                key = "filters",
                                order = 2,
                                title = "Filter in one tap",
                                description = "Show only pinned notes, or combine filters.",
                                padding = 6.dp,
                            ),
                        )
                        FilterChip(selected = false, onClick = {}, label = { Text("Shared") })
                    }
                }
                items(if (onlyPinned) notes.take(2) else notes) { (title, body) ->
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                        )
                        Text(
                            body,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
