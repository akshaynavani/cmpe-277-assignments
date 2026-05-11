package com.example.android.interviewassistant.ui.screens.flashcards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android.interviewassistant.data.local.entity.FlashcardEntity
import com.example.android.interviewassistant.domain.AppRepository
import com.example.android.interviewassistant.ui.theme.ScoreHigh
import com.example.android.interviewassistant.ui.theme.ScoreLow
import com.example.android.interviewassistant.ui.theme.ScoreMid
import com.example.android.interviewassistant.domain.Result
import kotlinx.coroutines.launch

private data class GradeButton(val grade: Int, val label: String)

private val gradeButtons = listOf(
    GradeButton(0, "Again"),
    GradeButton(2, "Hard"),
    GradeButton(3, "Good"),
    GradeButton(5, "Easy")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(repository: AppRepository) {
    var filterDueOnly by remember { mutableStateOf(true) }
    val allCards by repository.getAllFlashcards().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var dueCards by remember { mutableStateOf(emptyList<FlashcardEntity>()) }
    LaunchedEffect(filterDueOnly, allCards) {
        dueCards = if (filterDueOnly) repository.getDueFlashcards() else allCards
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    // Reset index when cards list changes
    LaunchedEffect(dueCards) {
        currentIndex = 0
        isFlipped = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flashcards") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Filter toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = filterDueOnly,
                    onClick = { filterDueOnly = true; currentIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    label = { Text("Due Today") }
                )
                SegmentedButton(
                    selected = !filterDueOnly,
                    onClick = { filterDueOnly = false; currentIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    label = { Text("All Cards") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (dueCards.isEmpty()) {
                // Empty state
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = ScoreHigh
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (filterDueOnly) "All caught up for today!" else "No flashcards yet.",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (filterDueOnly) "Come back tomorrow or review all cards."
                            else "Generate cards from your study notes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
                        )
                    }
                }
            } else if (filterDueOnly && currentIndex >= dueCards.size) {
                // Completed
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Session Complete!",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "You reviewed ${dueCards.size} card${if (dueCards.size > 1) "s" else ""}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = { currentIndex = 0; isFlipped = false }) {
                            Text("Review Again")
                        }
                    }
                }
            } else {
                val card = dueCards[currentIndex]

                // Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${currentIndex + 1} / ${dueCards.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(card.topic, style = MaterialTheme.typography.labelSmall) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { currentIndex.toFloat() / dueCards.size.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Flip card
                FlashCard(
                    card = card,
                    isFlipped = isFlipped,
                    onFlip = { isFlipped = !isFlipped },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isFlipped) {
                    OutlinedButton(
                        onClick = { isFlipped = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Answer")
                    }
                } else if (filterDueOnly) {
                    // Due Today mode: grade buttons advance the queue
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gradeButtons.forEach { btn ->
                            val containerColor = when (btn.grade) {
                                0 -> ScoreLow.copy(alpha = 0.15f)
                                2 -> ScoreMid.copy(alpha = 0.15f)
                                3 -> MaterialTheme.colorScheme.primaryContainer
                                5 -> ScoreHigh.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val contentColor = when (btn.grade) {
                                0 -> ScoreLow
                                2 -> ScoreMid
                                3 -> MaterialTheme.colorScheme.onPrimaryContainer
                                5 -> ScoreHigh
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        when (val r = repository.reviewCard(card, btn.grade)) {
                                            is Result.Success -> { currentIndex++; isFlipped = false }
                                            is Result.Error   -> snackbarHostState.showSnackbar("Save failed: ${r.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = containerColor,
                                    contentColor = contentColor
                                )
                            ) {
                                Text(btn.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // All Cards mode: prev/next navigation
                if (!filterDueOnly) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { currentIndex--; isFlipped = false },
                            enabled = currentIndex > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Previous")
                        }
                        Button(
                            onClick = { currentIndex++; isFlipped = false },
                            enabled = currentIndex < dueCards.size - 1,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FlashCard(
    card: FlashcardEntity,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "card_flip"
    )

    Card(
        modifier = modifier
            .clickable(onClick = onFlip)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rotation <= 90f) MaterialTheme.colorScheme.surface
                             else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front — question
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        card.question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Back — answer (unmirrored)
                Box(
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            card.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
