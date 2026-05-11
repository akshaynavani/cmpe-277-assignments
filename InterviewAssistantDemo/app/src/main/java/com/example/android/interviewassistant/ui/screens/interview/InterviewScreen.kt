package com.example.android.interviewassistant.ui.screens.interview

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android.interviewassistant.data.local.entity.MessageEntity
import com.example.android.interviewassistant.ui.theme.scoreColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewScreen(viewModel: InterviewViewModel) {
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.startError.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    when (val p = phase) {
        is InterviewPhase.Config -> ConfigPhase(
            config = config,
            snackbarHostState = snackbarHostState,
            onRoleChange = viewModel::updateRole,
            onLevelChange = viewModel::updateLevel,
            onDomainChange = viewModel::updateDomain,
            onStart = viewModel::startSession
        )
        is InterviewPhase.Starting -> StartingPhase()
        is InterviewPhase.Active -> ActivePhase(
            phase = p,
            messages = messages,
            onSendAnswer = viewModel::sendAnswer,
            onEndSession = viewModel::endSession,
            onClearError = viewModel::clearError
        )
        is InterviewPhase.Summary -> SummaryPhase(
            phase = p,
            onNewSession = viewModel::resetToConfig
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigPhase(
    config: ConfigState,
    snackbarHostState: SnackbarHostState,
    onRoleChange: (String) -> Unit,
    onLevelChange: (String) -> Unit,
    onDomainChange: (String) -> Unit,
    onStart: () -> Unit
) {
    val levels = listOf("junior", "mid", "senior")
    val domains = listOf("algorithms", "system-design", "behavioral", "android", "databases")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mock Interview") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Configure your session",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = config.role,
                onValueChange = onRoleChange,
                label = { Text("Role") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Level", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    levels.forEachIndexed { i, level ->
                        SegmentedButton(
                            selected = config.level == level,
                            onClick = { onLevelChange(level) },
                            shape = SegmentedButtonDefaults.itemShape(i, levels.size),
                            label = { Text(level.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Domain", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                domains.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { domain ->
                            FilterChip(
                                selected = config.domain == domain,
                                onClick = { onDomainChange(domain) },
                                label = { Text(domain.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onStart,
                enabled = config.role.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Interview")
            }
        }
    }
}

@Composable
private fun StartingPhase() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Preparing session...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivePhase(
    phase: InterviewPhase.Active,
    messages: List<MessageEntity>,
    onSendAnswer: (String) -> Unit,
    onEndSession: () -> Unit,
    onClearError: () -> Unit
) {
    var userAnswer by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showEndDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Auto-scroll to bottom on new messages or rubric update
    LaunchedEffect(messages.size, phase.lastRubric) {
        val itemCount = messages.size + (if (phase.lastRubric != null) 1 else 0)
        if (itemCount > 0) {
            scope.launch {
                listState.animateScrollToItem(itemCount - 1)
            }
        }
    }

    // Show error snackbar
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(phase.errorMessage) {
        phase.errorMessage?.let {
            snackbarHost.showSnackbar(it)
            onClearError()
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End Interview?") },
            text = { Text("This will save the session and generate a summary.") },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    onEndSession()
                }) { Text("End & Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("Continue") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interview in Progress") },
                actions = {
                    TextButton(
                        onClick = { showEndDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("End")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = userAnswer,
                        onValueChange = { userAnswer = it },
                        placeholder = { Text("Type your answer...") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        maxLines = 5,
                        enabled = !phase.isLoading
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (userAnswer.isNotBlank()) {
                                onSendAnswer(userAnswer.trim())
                                userAnswer = ""
                            }
                        },
                        enabled = userAnswer.isNotBlank() && !phase.isLoading
                    ) {
                        if (phase.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }
            if (phase.lastRubric != null) {
                item(key = "rubric") {
                    RubricCard(
                        rubric = phase.lastRubric,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            if (phase.isLoading) {
                item {
                    Row(modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Evaluating...", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: MessageEntity) {
    val isCandidate = message.role == "candidate"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCandidate) Arrangement.End else Arrangement.Start
    ) {
        if (!isCandidate) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp).align(Alignment.Top)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isCandidate) 16.dp else 4.dp,
                topEnd = if (isCandidate) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = if (isCandidate) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCandidate) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
            )
        }
        if (isCandidate) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp).align(Alignment.Top)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Face, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun RubricCard(rubric: RubricState, modifier: Modifier = Modifier) {
    val avg = rubric.average
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    rubric.topic,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${"%.0f".format(avg)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor(avg)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                "Clarity" to rubric.clarity,
                "Correctness" to rubric.correctness,
                "Communication" to rubric.communication,
                "Edge Cases" to rubric.edgeCases
            ).forEach { (label, score) ->
                ScoreRow(label = label, score = score)
            }
            if (rubric.feedback.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    rubric.feedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(100.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = scoreColor(score),
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "${"%.0f".format(score)}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(28.dp)
        )
    }
}

@Composable
private fun SummaryPhase(
    phase: InterviewPhase.Summary,
    onNewSession: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Session Summary",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            // Overall score gauge
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { phase.overallScore / 100f },
                            modifier = Modifier.size(100.dp),
                            strokeWidth = 8.dp,
                            color = scoreColor(phase.overallScore)
                        )
                        Text(
                            "${"%.0f".format(phase.overallScore)}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor(phase.overallScore)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Overall Score",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(phase.summary, style = MaterialTheme.typography.bodyMedium)
        }

        if (phase.strongAreas.isNotEmpty()) {
            item {
                SummarySection(title = "Strengths", items = phase.strongAreas,
                    color = MaterialTheme.colorScheme.secondaryContainer)
            }
        }

        if (phase.weakSpots.isNotEmpty()) {
            item {
                SummarySection(title = "Areas to Improve", items = phase.weakSpots,
                    color = MaterialTheme.colorScheme.errorContainer)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Next Focus", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(phase.nextFocus, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Button(
                onClick = onNewSession,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Session")
            }
        }
    }
}

@Composable
private fun SummarySection(title: String, items: List<String>, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("• ", style = MaterialTheme.typography.bodyMedium)
                    Text(item, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
