package com.scryme.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scryme.notes.domain.model.Note
import com.scryme.notes.ui.DatabaseProvider
import com.scryme.notes.ui.screens.NoteEditorScreen
import com.scryme.notes.ui.screens.SettingsScreen
import com.scryme.notes.ui.screens.WorkspaceScreen
import com.scryme.notes.ui.viewmodel.NoteViewModel
import com.scryme.notes.ui.viewmodel.NoteViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(DatabaseProvider.getRepository(applicationContext), applicationContext)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        val launchNoteId = intent.getStringExtra("LAUNCH_NOTE_ID")
        if (!launchNoteId.isNullOrEmpty()) {
            viewModel.selectNote(launchNoteId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launchNoteId = intent?.getStringExtra("LAUNCH_NOTE_ID")
        if (!launchNoteId.isNullOrEmpty()) {
            viewModel.selectNote(launchNoteId)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            val accentColorVal by viewModel.accentColorVal.collectAsState()

            val accentColor = Color(accentColorVal)
            val colorScheme =
                if (isDark) {
                    darkColorScheme(
                        primary = accentColor,
                        secondary = accentColor.copy(alpha = 0.8f),
                        background = Color(0xFF121212),
                        surface = Color(0xFF1E1E1E),
                    )
                } else {
                    lightColorScheme(
                        primary = accentColor,
                        secondary = accentColor.copy(alpha = 0.8f),
                        background = Color(0xFFF8FAFC),
                        surface = Color.White,
                    )
                }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreenLayout(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenLayout(viewModel: NoteViewModel) {
    var sidebarVisible by remember { mutableStateOf(false) }
    val activeNote by viewModel.activeNote.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showSettingsPage by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Editor Area with Toolbar at the top to toggle sidebar
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            // High-fidelity top bar (Mockup inspired)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Menu button is always accessible on the left of the screen
                IconButton(
                    onClick = { sidebarVisible = !sidebarVisible },
                ) {
                    Icon(
                        imageVector = if (sidebarVisible) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                        contentDescription = "Toggle Sidebar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (activeNote != null) {
                    // Left back/navigation chevron next to menu button to return to Welcome Screen
                    IconButton(
                        onClick = { viewModel.deselectActiveNote() },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (activeNote != null) {
                    // Centered circular overlapping avatars (High-Fidelity)
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val colors = listOf(Color(0xFFFFCDD2), Color(0xFFC8E6C9), Color(0xFFBBDEFB))
                        val letters = listOf("H", "J", "S")
                        Box(
                            modifier = Modifier.width(64.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Surface(
                                modifier = Modifier.size(26.dp),
                                shape = CircleShape,
                                color = colors[0],
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        letters[0],
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.size(26.dp).offset(x = 16.dp),
                                shape = CircleShape,
                                color = colors[1],
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        letters[1],
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.size(26.dp).offset(x = 32.dp),
                                shape = CircleShape,
                                color = colors[2],
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        letters[2],
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    // Done checkmark action button
                    IconButton(
                        onClick = { viewModel.setFocusedBlock(null) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save/Done",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    // More Options "..." button
                    IconButton(
                        onClick = { showBottomSheet = true },
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else {
                    // Standard Scryme Notes Title if no note is active
                    Text(
                        text = "Scryme Notes",
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )

            // Editor Screen View
            NoteEditorScreen(
                viewModel = viewModel,
                onOpenSidebar = { sidebarVisible = true },
                modifier = Modifier.weight(1f),
            )
        }

        // Overlay Scrim for the sidebar drawer
        if (sidebarVisible) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { sidebarVisible = false },
            )
        }

        // Sidebar Navigation Overlay (Workspace Screen)
        AnimatedVisibility(
            visible = sidebarVisible,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .width(280.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                WorkspaceScreen(
                    viewModel = viewModel,
                    onNoteSelected = {
                        sidebarVisible = false
                    },
                    onOpenSettings = {
                        showSettingsPage = true
                        sidebarVisible = false
                    },
                )
            }
        }

        // Full Screen Slide-in Settings Page Overlay
        AnimatedVisibility(
            visible = showSettingsPage,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
            modifier = Modifier.fillMaxSize(),
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onClose = { showSettingsPage = false },
            )
        }
    }

    // Modal Bottom Sheet (Mockup Design)
    if (showBottomSheet && activeNote != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            BottomSheetContent(
                note = activeNote!!,
                viewModel = viewModel,
                onDismiss = { showBottomSheet = false },
                onDeleteNote = {
                    activeNote?.let {
                        viewModel.deleteNote(it.id)
                    }
                    showBottomSheet = false
                },
                onDuplicateNote = {
                    activeNote?.let {
                        viewModel.createRootNote(it.title + " (Copy)")
                    }
                    showBottomSheet = false
                },
            )
        }
    }
}

@Composable
fun BottomSheetContent(
    note: Note,
    viewModel: NoteViewModel,
    onDismiss: () -> Unit,
    onDeleteNote: () -> Unit,
    onDuplicateNote: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mockup Row of 3 Cards: Image, Voice, Share
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val cards =
                listOf(
                    Triple("Image", Icons.Default.Image, {}),
                    Triple("Voice", Icons.Default.Mic, {}),
                    Triple("Share", Icons.Default.Share, {}),
                )
            cards.forEach { (label, icon, onClick) ->
                Card(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clickable { onClick() },
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        var showReminderDialog by remember { mutableStateOf(false) }

        if (showReminderDialog) {
            val currentReminder = remember(note.id) { viewModel.getNoteReminder(note.id) }
            val sdf = remember { java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault()) }
            val currentReminderStr = if (currentReminder > 0) sdf.format(java.util.Date(currentReminder)) else "None"

            AlertDialog(
                onDismissRequest = { showReminderDialog = false },
                confirmButton = {
                    TextButton(onClick = { showReminderDialog = false }) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                },
                title = { Text("Set Reminder for Note") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Current Reminder: $currentReminderStr", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.setNoteReminder(note.id, note.title, System.currentTimeMillis() + 10_000L)
                                showReminderDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Text("In 10 Seconds (Fast Test)")
                        }

                        Button(
                            onClick = {
                                viewModel.setNoteReminder(note.id, note.title, System.currentTimeMillis() + 60_000L)
                                showReminderDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("In 1 Minute")
                        }

                        Button(
                            onClick = {
                                viewModel.setNoteReminder(note.id, note.title, System.currentTimeMillis() + 3600_000L)
                                showReminderDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("In 1 Hour")
                        }

                        Button(
                            onClick = {
                                viewModel.setNoteReminder(note.id, note.title, System.currentTimeMillis() + 24 * 3600_000L)
                                showReminderDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("In 24 Hours")
                        }

                        if (currentReminder > 0) {
                            Button(
                                onClick = {
                                    viewModel.cancelNoteReminder(note.id)
                                    showReminderDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                            ) {
                                Text("Cancel Reminder")
                            }
                        }
                    }
                }
            )
        }

        // Vertical Option Rows
        val options =
            listOf(
                Triple("Pin", Icons.Default.PushPin, {}),
                Triple("Set Reminder", Icons.Default.NotificationsActive, { showReminderDialog = true }),
                Triple("Add Thumbnail", Icons.Default.AddPhotoAlternate, {}),
                Triple("Label", Icons.Default.Label, {}),
                Triple("Send", Icons.Default.Send, {}),
                Triple("Make a Copy", Icons.Default.ContentCopy, onDuplicateNote),
                Triple("Lock Note", Icons.Default.Lock, {}),
                Triple("Delete Note", Icons.Default.Delete, onDeleteNote),
            )

        options.forEach { (label, icon, onClick) ->
            val isDelete = label == "Delete Note"
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
                        .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )

                if (label == "Label") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Work",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "label next",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic footer "Last Edited..."
        val sdf = remember { SimpleDateFormat("MMMM d, yyyy HH:mm", Locale.getDefault()) }
        val lastEditedStr = remember(note.updatedAt) { sdf.format(Date(note.updatedAt)) }
        Text(
            text = "Last Edited $lastEditedStr By Harrison..",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}
