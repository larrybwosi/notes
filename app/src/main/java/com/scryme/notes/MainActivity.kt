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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scryme.notes.domain.model.Note
import com.scryme.notes.ui.DatabaseProvider
import com.scryme.notes.ui.screens.NoteEditorScreen
import com.scryme.notes.ui.screens.WorkspaceScreen
import com.scryme.notes.ui.viewmodel.NoteViewModel
import com.scryme.notes.ui.viewmodel.NoteViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory(DatabaseProvider.getRepository(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    Row(modifier = Modifier.fillMaxSize()) {
        // Collapsible Sidebar (Workspace Screen)
        AnimatedVisibility(
            visible = sidebarVisible,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(260.dp),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                WorkspaceScreen(
                    viewModel = viewModel,
                    onNoteSelected = {
                        sidebarVisible = false
                    }
                )
            }
        }

        // Vertical divider if sidebar is open
        if (sidebarVisible) {
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        // Main Editor Area with Toolbar at the top to toggle sidebar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // High-fidelity top bar (Mockup inspired)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeNote != null) {
                    // Left back/navigation chevron to deselect and return to Welcome Screen
                    IconButton(
                        onClick = { viewModel.deselectActiveNote() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Left menu toggle if on Welcome Screen
                    IconButton(
                        onClick = { sidebarVisible = !sidebarVisible }
                    ) {
                        Icon(
                            imageVector = if (sidebarVisible) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                            contentDescription = "Toggle Sidebar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (activeNote != null) {
                    // Centered circular overlapping avatars (High-Fidelity)
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colors = listOf(Color(0xFFFFCDD2), Color(0xFFC8E6C9), Color(0xFFBBDEFB))
                        val letters = listOf("H", "J", "S")
                        Box(
                            modifier = Modifier.width(64.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Surface(
                                modifier = Modifier.size(26.dp),
                                shape = CircleShape,
                                color = colors[0],
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(letters[0], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Surface(
                                modifier = Modifier.size(26.dp).offset(x = 16.dp),
                                shape = CircleShape,
                                color = colors[1],
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(letters[1], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Surface(
                                modifier = Modifier.size(26.dp).offset(x = 32.dp),
                                shape = CircleShape,
                                color = colors[2],
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(letters[2], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    // Done checkmark action button
                    IconButton(
                        onClick = { viewModel.setFocusedBlock(null) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save/Done",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // More Options "..." button
                    IconButton(
                        onClick = { showBottomSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // Standard Scryme Notes Title if no note is active
                    Text(
                        text = "Scryme Notes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Editor Screen View
            NoteEditorScreen(
                viewModel = viewModel,
                onOpenSidebar = { sidebarVisible = true },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Modal Bottom Sheet (Mockup Design)
    if (showBottomSheet && activeNote != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            BottomSheetContent(
                note = activeNote!!,
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
                }
            )
        }
    }
}

@Composable
fun BottomSheetContent(
    note: Note,
    onDismiss: () -> Unit,
    onDeleteNote: () -> Unit,
    onDuplicateNote: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mockup Row of 3 Cards: Image, Voice, Share
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val cards = listOf(
                Triple("Image", Icons.Default.Image, {}),
                Triple("Voice", Icons.Default.Mic, {}),
                Triple("Share", Icons.Default.Share, {})
            )
            cards.forEach { (label, icon, onClick) ->
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clickable { onClick() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Vertical Option Rows
        val options = listOf(
            Triple("Pin", Icons.Default.PushPin, {}),
            Triple("Add Thumbnail", Icons.Default.AddPhotoAlternate, {}),
            Triple("Label", Icons.Default.Label, {}),
            Triple("Send", Icons.Default.Send, {}),
            Triple("Make a Copy", Icons.Default.ContentCopy, onDuplicateNote),
            Triple("Lock Note", Icons.Default.Lock, {}),
            Triple("Delete Note", Icons.Default.Delete, onDeleteNote)
        )

        options.forEach { (label, icon, onClick) ->
            val isDelete = label == "Delete Note"
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (label == "Label") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Work",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "label next",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
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
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
