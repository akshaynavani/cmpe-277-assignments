package com.example.android.interviewassistant.ui.screens.onboarding

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    val levels = listOf("junior", "mid", "senior")
    val commonRoles = listOf(
        "Software Engineer", "Backend Developer", "Frontend Developer",
        "Full Stack Developer", "ML Engineer", "Data Scientist", "Mobile Developer"
    )
    var roleExpanded by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Up Your Profile") },
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tell us about yourself so we can personalise your interview prep.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Name
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("Your name *") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Role dropdown
            ExposedDropdownMenuBox(
                expanded = roleExpanded,
                onExpandedChange = { roleExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.role,
                    onValueChange = viewModel::updateRole,
                    label = { Text("Target role") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = roleExpanded,
                    onDismissRequest = { roleExpanded = false }
                ) {
                    commonRoles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role) },
                            onClick = {
                                viewModel.updateRole(role)
                                roleExpanded = false
                            }
                        )
                    }
                }
            }

            // Level
            Text(
                text = "Experience level",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                levels.forEachIndexed { index, level ->
                    SegmentedButton(
                        selected = state.level == level,
                        onClick = { viewModel.updateLevel(level) },
                        shape = SegmentedButtonDefaults.itemShape(index, levels.size),
                        label = { Text(level.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Target company
            OutlinedTextField(
                value = state.targetCompany,
                onValueChange = viewModel::updateTargetCompany,
                label = { Text("Target company (optional)") },
                placeholder = { Text("e.g. Google, Meta, Startup") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Interview date
            val calendar = remember { Calendar.getInstance() }
            OutlinedTextField(
                value = dateFormatter.format(Date(state.interviewDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Target interview date") },
                trailingIcon = {
                    TextButton(onClick = {
                        calendar.timeInMillis = state.interviewDate
                        val dialog = DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                calendar.set(year, month, day)
                                viewModel.updateInterviewDate(calendar.timeInMillis)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        dialog.datePicker.minDate = System.currentTimeMillis()
                        dialog.show()
                    }) { Text("Pick") }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::submit,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Let's Prepare", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
