package com.scryme.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scryme.notes.R
import com.scryme.notes.ui.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark by viewModel.isDarkMode.collectAsState()
    val selectedFontPref by viewModel.fontFamilyPreference.collectAsState()
    val activeAccentColorVal by viewModel.accentColorVal.collectAsState()
    val markdownEnabled by viewModel.markdownEnabled.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsState()
    val dailyReminderTime by viewModel.dailyReminderTime.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val accentColors =
        listOf(
            Pair("Blue", 0xFF1B63C2.toInt()),
            Pair("Purple", 0xFF7C4DFF.toInt()),
            Pair("Green", 0xFF2E7D32.toInt()),
            Pair("Orange", 0xFFFF6D00.toInt()),
            Pair("Red", 0xFFD11A2A.toInt()),
        )

    val fontFamilies = listOf("Sans-Serif", "Serif", "Monospace")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Section 0: Profile Settings
            SettingsSection(title = "Profile Settings") {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "User Name",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Customize your name for personalized greetings",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    var nameInput by remember(userName) { mutableStateOf(userName) }
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            viewModel.setUserName(it)
                        },
                        singleLine = true,
                        placeholder = { Text("Enter your name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                }
            }

            // Section 1: Appearance
            SettingsSection(title = "Appearance") {
                // Theme Toggle row
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Dark Mode",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Enjoy writing in a dark interface",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.setDarkMode(it) },
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Accent color row
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Accent Color",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Choose your workspace highlight color",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        accentColors.forEach { (name, colorVal) ->
                            val isSelected = activeAccentColorVal == colorVal
                            Box(
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .background(Color(colorVal), CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape,
                                        )
                                        .clickable { viewModel.setAccentColorVal(colorVal) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 1.5: Notifications & Reminders
            SettingsSection(title = "Notifications & Reminders") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Notes Reminder",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Receive a daily reminder notification to write notes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = dailyReminderEnabled,
                        onCheckedChange = { viewModel.setDailyReminderEnabled(it) },
                    )
                }

                if (dailyReminderEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Reminder Time",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Set the time when you want to be reminded daily",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        var showTimeDialog by remember { mutableStateOf(false) }
                        TextButton(onClick = { showTimeDialog = true }) {
                            Text(dailyReminderTime, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        if (showTimeDialog) {
                            val initialParts = dailyReminderTime.split(":")
                            var hour by remember { mutableStateOf(initialParts.firstOrNull()?.toIntOrNull() ?: 9) }
                            var minute by remember { mutableStateOf(initialParts.lastOrNull()?.toIntOrNull() ?: 0) }

                            AlertDialog(
                                onDismissRequest = { showTimeDialog = false },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val timeStr = String.format("%02d:%02d", hour, minute)
                                            viewModel.setDailyReminderTime(timeStr)
                                            showTimeDialog = false
                                        },
                                    ) {
                                        Text("Set Time", fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showTimeDialog = false }) {
                                        Text("Cancel")
                                    }
                                },
                                title = { Text("Set Reminder Time") },
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Hour: ", fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { hour = (hour + 23) % 24 }) {
                                            Icon(Icons.Default.Remove, "minus")
                                        }
                                        Text(String.format("%02d", hour), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        IconButton(onClick = { hour = (hour + 1) % 24 }) {
                                            Icon(Icons.Default.Add, "plus")
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("Minute: ", fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { minute = (minute + 55) % 60 }) {
                                            Icon(Icons.Default.Remove, "minus")
                                        }
                                        Text(String.format("%02d", minute), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        IconButton(onClick = { minute = (minute + 5) % 60 }) {
                                            Icon(Icons.Default.Add, "plus")
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // Section 2: Editor Settings
            SettingsSection(title = "Editor Settings") {
                // Font Family Options
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Font Family",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Choose the writing typeface for your blocks",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        fontFamilies.forEach { font ->
                            val isSelected = selectedFontPref == font
                            Card(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setFontFamilyPreference(font) },
                                shape = RoundedCornerShape(8.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            },
                                    ),
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = font,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Markdown switch
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Markdown Shortcuts",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Auto-convert prefixes like #, -, or []",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = markdownEnabled,
                        onCheckedChange = { viewModel.setMarkdownEnabled(it) },
                    )
                }
            }

            // Section 3: Data & Storage
            SettingsSection(title = "Data & Storage") {
                // Export Notes Mock Card
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { /* mock action */ }
                            .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.SaveAlt,
                        contentDescription = "Export Data",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Backup / Export Notes",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Download all your local notes as Markdown files",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Clear Notes row (dangerous)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { showClearConfirmDialog = true }
                            .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Clear database",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clear All Data",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "Permanently delete all notes, blocks, and sub-pages",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Section 4: About
            SettingsSection(title = "About Scryme Notes") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Scryme Notes Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Scryme Notes v1.0.0",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "A modern Notion-like block editor featuring sub-notebook hierarchy, markdown shortcuts, rich inline text formatting, Obsidian-style organization, and collaborative high-fidelity UI avatars.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Built with Jetpack Compose & SQLite.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    // Confirmation Dialog for Clearing All Notes
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllNotes()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Are you absolutely sure?") },
            text = { Text("This will permanently delete all your notes. This action is irreversible.") },
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
