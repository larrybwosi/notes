package com.scryme.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scryme.notes.R
import com.scryme.notes.domain.model.Note
import com.scryme.notes.ui.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: NoteViewModel,
    onNoteSelected: (Note) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allNotes by viewModel.allNotes.collectAsState()
    val activeNote by viewModel.activeNote.collectAsState()
    val expandedNoteIds by viewModel.expandedNoteIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val pinnedNotes =
        remember(allNotes) {
            val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
            prefs.getStringSet("pinned_notes", emptySet()) ?: emptySet()
        }

    // Group notes into tree hierarchy
    val rootNotes =
        remember(allNotes, searchQuery, pinnedNotes) {
            val roots = allNotes.filter { it.parentId == null }
            val filtered =
                if (searchQuery.isBlank()) {
                    roots
                } else {
                    // If searching, flat filter list of matching notes
                    allNotes.filter { it.title.contains(searchQuery, ignoreCase = true) }
                }
            filtered.sortedWith(
                compareByDescending<com.scryme.notes.domain.model.Note> { pinnedNotes.contains(it.id) }
                    .thenByDescending { it.updatedAt },
            )
        }

    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .width(260.dp),
    ) {
        // Workspace / Brand Header
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Workspace",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Personal Plan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    modifier = Modifier.size(16.dp),
                )
            },
            trailingIcon =
                if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                } else {
                    null
                },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .height(48.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Create New Root Page Button
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clickable { viewModel.createRootNote() },
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(6.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add page",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add a page",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                    searchActive = searchQuery.isNotBlank(),
                )
            }
        }
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
    searchActive: Boolean,
) {
    val isSelected = activeNote?.id == note.id
    val isExpanded = expandedNoteIds.contains(note.id)

    // Find matching child notes from flat list
    val children =
        remember(allNotes, note.id) {
            allNotes.filter { it.parentId == note.id }
        }
    val hasChildren = children.isNotEmpty()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = (indentationLevel * 12).dp, end = 8.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onNoteClick(note) }
                    .padding(vertical = 6.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Expand/Collapse arrow
            if (hasChildren && !searchActive) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Toggle nested",
                    modifier =
                        Modifier
                            .size(18.dp)
                            .clickable { onToggleExpand(note) },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.width(8.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            val isPinned =
                remember(note.id) {
                    val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                    val pinnedSet = prefs.getStringSet("pinned_notes", emptySet()) ?: emptySet()
                    pinnedSet.contains(note.id)
                }
            val isLocked =
                remember(note.id) {
                    val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.getBoolean("locked_note_${note.id}", false)
                }

            // Note Title
            Text(
                text = if (note.title.isBlank()) "Untitled" else note.title,
                fontSize = 13.sp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            if (isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    modifier = Modifier.size(12.dp).padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(12.dp).padding(end = 4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            // Add Child Page & Delete Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = { onCreateChild(note.id) },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add child page",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = { onDelete(note) },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete page",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
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
                        searchActive = searchActive,
                    )
                }
            }
        }
    }
}
