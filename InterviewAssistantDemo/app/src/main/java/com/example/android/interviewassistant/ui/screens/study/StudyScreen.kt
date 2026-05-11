package com.example.android.interviewassistant.ui.screens.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android.interviewassistant.data.local.entity.DailyFaqEntity
import com.example.android.interviewassistant.domain.AppRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(viewModel: StudyViewModel, repository: AppRepository) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dailyFaqs by viewModel.dailyFaqs.collectAsStateWithLifecycle()

    // Collect user profile for topic/role context in manual refresh
    val profile by repository.getProfile().collectAsStateWithLifecycle(initialValue = null)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error, state.faqFetchError) {
        (state.error ?: state.faqFetchError)?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Ask") },
                    icon = { Icon(Icons.Default.QuestionAnswer, contentDescription = null) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Generate") },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("Daily FAQs") },
                    icon = { Icon(Icons.Default.TravelExplore, contentDescription = null) }
                )
            }

            when (state.selectedTab) {
                0 -> AskTab(state = state, viewModel = viewModel)
                1 -> GenerateTab(state = state, viewModel = viewModel)
                2 -> DailyFaqTab(
                    faqs = dailyFaqs,
                    isFetching = state.isFetchingFaqs,
                    onRefresh = {
                        val p = profile
                        if (p != null) viewModel.refreshDailyFaqs(p.role, p.level)
                    },
                    onSaveAsFlashcard = viewModel::saveFaqAsFlashcard
                )
            }
        }
    }
}

// ── Ask Tab ──────────────────────────────────────────────────────────────────

@Composable
private fun AskTab(state: StudyUiState, viewModel: StudyViewModel) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.question,
            onValueChange = viewModel::updateQuestion,
            label = { Text("Ask a technical question...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            enabled = !state.isAsking
        )

        OutlinedTextField(
            value = state.topic,
            onValueChange = viewModel::updateTopic,
            label = { Text("Topic (optional)") },
            placeholder = { Text("e.g. algorithms, system design") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isAsking
        )

        Button(
            onClick = viewModel::askQuestion,
            enabled = state.question.isNotBlank() && !state.isAsking,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (state.isAsking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Asking...")
            } else {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ask")
            }
        }

        AnimatedVisibility(visible = state.answer.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Answer", style = MaterialTheme.typography.titleSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.savedAsFlashcard) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Saved", style = MaterialTheme.typography.labelSmall) },
                                    icon = {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                                            modifier = Modifier.size(14.dp))
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(state.answer))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy",
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.answer, style = MaterialTheme.typography.bodyMedium)

                    if (state.relatedTopics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Related Topics",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.relatedTopics.forEach { topic ->
                                AssistChip(
                                    onClick = { viewModel.updateQuestion("Tell me more about $topic") },
                                    label = { Text(topic, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Generate Tab ─────────────────────────────────────────────────────────────

@Composable
private fun GenerateTab(state: StudyUiState, viewModel: StudyViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.notesTopic,
                onValueChange = viewModel::updateNotesTopic,
                label = { Text("Topic") },
                placeholder = { Text("e.g. Dynamic Programming, Distributed Systems") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isGenerating
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Paste your study notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 140.dp),
                minLines = 6,
                enabled = !state.isGenerating
            )

            Button(
                onClick = viewModel::generateFlashcards,
                enabled = state.notesTopic.isNotBlank() && state.notes.isNotBlank() && !state.isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Flashcards")
                }
            }
        }

        if (state.generatedCards.isNotEmpty()) {
            HorizontalDivider()
            Text(
                "${state.generatedCards.size} cards generated",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(state.generatedCards, key = { it.id }) { card ->
                    GeneratedCardItem(card = card)
                }
            }
            Button(
                onClick = viewModel::saveGeneratedCards,
                enabled = !state.cardsSaved,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp)
            ) {
                Icon(
                    if (state.cardsSaved) Icons.Default.CheckCircle else Icons.Default.Save,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (state.cardsSaved) "Saved!" else "Save All Cards")
            }
        }
    }
}

@Composable
private fun GeneratedCardItem(card: com.example.android.interviewassistant.data.local.entity.FlashcardEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                card.question,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                card.answer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Daily FAQs Tab ───────────────────────────────────────────────────────────

@Composable
private fun DailyFaqTab(
    faqs: List<DailyFaqEntity>,
    isFetching: Boolean,
    onRefresh: () -> Unit,
    onSaveAsFlashcard: (DailyFaqEntity) -> Unit
) {
    // Group FAQs by calendar day
    val grouped = remember(faqs) {
        faqs.groupBy { faq ->
            val cal = Calendar.getInstance().apply { timeInMillis = faq.fetchedAt }
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.entries.sortedByDescending { it.key }
    }

    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header row with refresh button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Internet-sourced FAQs", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Auto-refreshed daily • Glassdoor, LeetCode & more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onRefresh,
                enabled = !isFetching
            ) {
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        HorizontalDivider()

        if (faqs.isEmpty() && !isFetching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.TravelExplore,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No FAQs yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap Refresh to fetch today's interview FAQs\nfrom Glassdoor, LeetCode & the web",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (dayMillis, dayFaqs) ->
                    item(key = "header_$dayMillis") {
                        Text(
                            text = dateFormat.format(Date(dayMillis)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(dayFaqs, key = { it.id }) { faq ->
                        DailyFaqCard(faq = faq, onSave = { onSaveAsFlashcard(faq) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyFaqCard(faq: DailyFaqEntity, onSave: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Source chip
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                faq.source,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(faq.question, style = MaterialTheme.typography.bodyMedium)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        faq.answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (faq.savedAsFlashcard) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Saved to flashcards", style = MaterialTheme.typography.labelSmall) },
                                icon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp)) }
                            )
                        } else {
                            OutlinedButton(onClick = onSave) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save as Flashcard", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
