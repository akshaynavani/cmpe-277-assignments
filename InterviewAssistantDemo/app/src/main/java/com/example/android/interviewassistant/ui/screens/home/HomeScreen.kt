package com.example.android.interviewassistant.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.android.interviewassistant.domain.AppRepository
import com.example.android.interviewassistant.ui.Screen
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    repository: AppRepository,
    onNavigateTo: (Screen) -> Unit
) {
    val profile by repository.getProfile().collectAsStateWithLifecycle(null)
    val sessions by repository.getAllSessions().collectAsStateWithLifecycle(emptyList())
    val scores by repository.getAllScores().collectAsStateWithLifecycle(emptyList())
    val dueCount by repository.getDueCardCount().collectAsStateWithLifecycle(0)

    val completedSessions = remember(sessions) {
        sessions.filter { it.status == "completed" }
    }

    val avgScore = remember(scores) {
        if (scores.isEmpty()) null
        else scores.map { it.average }.average().toFloat()
    }

    val daysUntil = remember(profile) {
        profile?.let {
            val diff = it.interviewDate - System.currentTimeMillis()
            TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
        } ?: 0L
    }

    val topWeakSpots = remember(sessions) {
        sessions
            .filter { it.status == "completed" && !it.weakSpots.isNullOrBlank() }
            .flatMap { repository.parseWeakSpots(it.weakSpots) }
            .groupBy { it }
            .entries
            .sortedByDescending { it.value.size }
            .take(3)
            .map { it.key }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero card
        item {
            HeroCard(
                name = profile?.name ?: "",
                role = profile?.role ?: "Software Engineer",
                daysUntil = daysUntil
            )
        }

        // Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "$dueCount",
                    label = "Due Cards",
                    icon = Icons.Default.Style,
                    highlight = dueCount > 0
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = avgScore?.let { "%.0f%%".format(it) } ?: "--",
                    label = "Avg Score",
                    icon = Icons.Default.TrendingUp
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "${completedSessions.size}",
                    label = "Sessions",
                    icon = Icons.Default.CheckCircle
                )
            }
        }

        // Today's plan
        item {
            TodaysPlanCard(
                dueCount = dueCount,
                weakSpots = topWeakSpots,
                hasCompletedSessions = completedSessions.isNotEmpty()
            )
        }

        // Quick actions
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Mic,
                    label = "Mock\nInterview",
                    onClick = { onNavigateTo(Screen.Interview) }
                )
                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.MenuBook,
                    label = "Study\nQ&A",
                    onClick = { onNavigateTo(Screen.Study) }
                )
                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Style,
                    label = "Flash\nCards",
                    onClick = { onNavigateTo(Screen.Flashcards) }
                )
                QuickActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.BarChart,
                    label = "Progress",
                    onClick = { onNavigateTo(Screen.Progress) }
                )
            }
        }
    }
}

@Composable
private fun HeroCard(name: String, role: String, daysUntil: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column {
            if (name.isNotBlank()) {
                Text(
                    text = "Hey, $name!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = "$role Interview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$daysUntil",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (daysUntil == 1L) "day\nuntil interview" else "days\nuntil interview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight)
                MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight)
                    MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodaysPlanCard(
    dueCount: Int,
    weakSpots: List<String>,
    hasCompletedSessions: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Plan",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (dueCount > 0) {
                PlanItem(
                    icon = Icons.Default.Style,
                    text = "Review $dueCount due flashcard${if (dueCount > 1) "s" else ""}"
                )
            }

            weakSpots.forEach { spot ->
                PlanItem(
                    icon = Icons.Default.FitnessCenter,
                    text = "Practice: $spot"
                )
            }

            if (!hasCompletedSessions) {
                PlanItem(
                    icon = Icons.Default.Mic,
                    text = "Start a mock interview to build your baseline"
                )
            } else {
                PlanItem(
                    icon = Icons.Default.Mic,
                    text = "Run a new mock interview session"
                )
            }
        }
    }
}

@Composable
private fun PlanItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        contentPadding = PaddingValues(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
