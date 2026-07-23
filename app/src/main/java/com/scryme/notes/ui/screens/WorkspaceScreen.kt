package com.scryme.notes.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scryme.notes.domain.model.Note
import com.scryme.notes.ui.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: NoteViewModel,
    onNoteSelected: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val allNotes by viewModel.allNotes.collectAsState()
    val activeNote by viewModel.activeNote.collectAsState()
    val expandedNoteIds by viewModel.expandedNoteIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    // Group notes into tree hierarchy
    val rootNotes = remember(allNotes, searchQuery) {
        val roots = allNotes.filter { it.parentId == null }
        if (searchQuery.isBlank()) {
            roots
        } else {
            // If searching, flat filter list of matching notes
            allNotes.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(260.dp)
    ) {
        // Workspace / Brand Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Workspace",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Personal Plan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search pages...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .height(48.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Create New Root Page Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clickable { viewModel.createRootNote() },
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add page",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add a page",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Notes Hierarchy List
        Text(
            text = "PRIVATE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(rootNotes, key = { it.id }) { note ->
                HierarchyNode(
                    note = note,
                    allNotes = allNotes,
                    activeNote = activeNote,
                    expandedNoteIds = expandedNoteIds,
                    onNoteClick = {
                        viewModel.selectNote(it.id)
                        onNoteSelected(it)
                    },
                    onToggleExpand = { viewModel.toggleNoteExpanded(it.id) },
                    onCreateChild = { parentId -> viewModel.createChildNote(parentId) },
                    onDelete = { viewModel.deleteNote(it.id) },
                    indentationLevel = 0,
                    searchActive = searchQuery.isNotBlank()
                )
            }
        }
    }

    // Settings / Info Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done")
                }
            },
            title = { Text("Notion Notes") },
            text = {
                Column {
                    Text("Build powerful block-based notes organized in hierarchies.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Local SQLite Storage (Room)", fontWeight = FontWeight.Medium)
                    Text("• Fully-Recursive Parent-Child Tree", fontWeight = FontWeight.Medium)
                    Text("• Notion Slash Commands (/) & Toolbar formatting", fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
fun HierarchyNode(
    note: Note,
    allNotes: List<Note>,
    activeNote: Note?,
    expandedNoteIds: Set<String>,
    onNoteClick: (Note) -> Unit,
    onToggleExpand: (Note) -> Unit,
    onCreateChild: (String) -> Unit,
    onDelete: (Note) -> Unit,
    indentationLevel: Int,
    searchActive: Boolean
) {
    val isSelected = activeNote?.id == note.id
    val isExpanded = expandedNoteIds.contains(note.id)

    // Find matching child notes from flat list
    val children = remember(allNotes, note.id) {
        allNotes.filter { it.parentId == note.id }
    }
    val hasChildren = children.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (indentationLevel * 12).dp)
                .clickable { onNoteClick(note) }
                .padding(vertical = 4.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/Collapse arrow
            if (hasChildren && !searchActive) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Toggle nested",
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onToggleExpand(note) },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.width(18.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Page Icon (Notion style)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = "Page Icon",
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Note Title
            Text(
                text = if (note.title.isBlank()) "Untitled" else note.title,
                fontSize = 13.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Add Child Page & Delete Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onCreateChild(note.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add child page",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onDelete(note) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete page",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Sub-nodes listing
        if (hasChildren && isExpanded && !searchActive) {
            Column(modifier = Modifier.fillMaxWidth()) {
                children.forEach { child ->
                    HierarchyNode(
                        note = child,
                        allNotes = allNotes,
                        activeNote = activeNote,
                        expandedNoteIds = expandedNoteIds,
                        onNoteClick = onNoteClick,
                        onToggleExpand = onToggleExpand,
                        onCreateChild = onCreateChild,
                        onDelete = onDelete,
                        indentationLevel = indentationLevel + 1,
                        searchActive = searchActive
                    )
                }
            }
        }
    }
}
