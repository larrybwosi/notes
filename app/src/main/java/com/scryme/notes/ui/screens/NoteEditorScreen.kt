package com.scryme.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.scryme.notes.domain.model.Block
import com.scryme.notes.domain.model.BlockType
import com.scryme.notes.domain.model.StyleType
import com.scryme.notes.ui.components.RichTextTransformer
import com.scryme.notes.ui.viewmodel.NoteViewModel
import com.scryme.notes.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val RobotoFontFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_bold, FontWeight.Bold)
)

@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    onOpenSidebar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeNote by viewModel.activeNote.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val subNotes by viewModel.subNotes.collectAsState()
    val focusedBlockId by viewModel.focusedBlockId.collectAsState()

    val markdownEnabled by viewModel.markdownEnabled.collectAsState()
    val fontFamilyPref by viewModel.fontFamilyPreference.collectAsState()
    val selectedFontFamily =
        when (fontFamilyPref) {
            "Serif" -> FontFamily.Serif
            "Monospace" -> FontFamily.Monospace
            "Inter" -> InterFontFamily
            "Roboto" -> RobotoFontFamily
            else -> FontFamily.Default
        }

    val allNotes by viewModel.allNotes.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var activeSelection by remember { mutableStateOf<TextRange?>(null) }

    if (activeNote == null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val recentlyAdded =
            remember(allNotes) {
                val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                val pinnedSet = prefs.getStringSet("pinned_notes", emptySet()) ?: emptySet()
                allNotes.sortedWith(
                    compareByDescending<com.scryme.notes.domain.model.Note> { pinnedSet.contains(it.id) }
                        .thenByDescending { it.updatedAt }
                )
            }

        val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
        val greeting =
            when {
                hour < 12 -> "Good Morning"
                hour < 17 -> "Good Afternoon"
                else -> "Good Evening"
            }

        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "$greeting, $userName",
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 24.sp,
                            ),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Let's capture something new today.",
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            ),
                    )
                }

                IconButton(
                    onClick = { viewModel.createRootNote() },
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Create new note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "Recently Added",
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 28.sp,
                        ),
                )

                Text(
                    text = "${allNotes.size} Notes",
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                        ),
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recentlyAdded.isEmpty()) {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        ),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Empty Notes",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No notes captured yet",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the edit icon above to write your first beautiful note.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                val chunkedNotes = recentlyAdded.chunked(2)
                chunkedNotes.forEach { rowNotes ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        rowNotes.forEach { note ->
                            Box(modifier = Modifier.weight(1f)) {
                                NoteGridCard(note = note, viewModel = viewModel)
                            }
                        }
                        if (rowNotes.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        return
    }

    val note = activeNote!!

    // Lock Screen implementation
    val context = androidx.compose.ui.platform.LocalContext.current
    val isNoteLocked = remember(note.id) {
        val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
        prefs.getBoolean("locked_note_${note.id}", false)
    }
    var isUnlockedSession by remember(note.id) { mutableStateOf(false) }

    if (isNoteLocked && !isUnlockedSession) {
        var pinValue by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This Note is Locked",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please enter the 4-digit passcode to view and edit.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = pinValue,
                onValueChange = {
                    pinValue = it
                    isError = false
                },
                singleLine = true,
                placeholder = { Text("Enter PIN") },
                isError = isError,
                modifier = Modifier.width(150.dp),
            )

            if (isError) {
                Text(
                    text = "Incorrect PIN!",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                    val expectedPin = prefs.getString("lock_pin_note_${note.id}", "1234")
                    if (pinValue == expectedPin) {
                        isUnlockedSession = true
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.width(150.dp),
            ) {
                Text("Unlock")
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Notion Breadcrumbs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                breadcrumbs.forEachIndexed { idx, crumb ->
                    Text(
                        text = if (crumb.title.isBlank()) "Untitled" else crumb.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (idx == breadcrumbs.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { viewModel.selectNote(crumb.id) },
                    )
                    if (idx < breadcrumbs.lastIndex) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Separator",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emoji Thumbnail if exists
            val thumbnailEmoji = remember(note.id, note.updatedAt) {
                val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                prefs.getString("thumbnail_note_${note.id}", null)
            }
            if (thumbnailEmoji != null) {
                Text(
                    text = thumbnailEmoji,
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Large Borderless Title Editor
            var titleText by remember(note.id) { mutableStateOf(note.title) }
            BasicTextField(
                value = titleText,
                onValueChange = {
                    titleText = it
                    viewModel.updateActiveNoteTitle(it)
                },
                textStyle =
                    TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (titleText.isEmpty()) {
                        Text(
                            "Untitled",
                            style =
                                TextStyle(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                ),
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nested Sub-Pages listing
            if (subNotes.isNotEmpty()) {
                Text(
                    "Sub-pages",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                subNotes.forEach { sub ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectNote(sub.id) }
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Page Icon",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (sub.title.isBlank()) "Untitled" else sub.title,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Inline "+" subpage creator helper inside parent
            OutlinedButton(
                onClick = { viewModel.createChildNote(note.id) },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.align(Alignment.Start),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add subpage", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add a sub-page", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable list of rich text blocks
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                itemsIndexed(note.blocks, key = { _, block -> block.id }) { index, block ->
                    val sequenceNumber =
                        if (block.type == BlockType.NUMBERED_LIST_ITEM) {
                            var count = 0
                            for (i in 0..index) {
                                if (note.blocks[i].type == BlockType.NUMBERED_LIST_ITEM) {
                                    count++
                                } else {
                                    count = 0
                                }
                            }
                            count
                        } else {
                            1
                        }

                    BlockEditorItem(
                        block = block,
                        focusedBlockId = focusedBlockId,
                        sequenceNumber = sequenceNumber,
                        markdownEnabled = markdownEnabled,
                        selectedFontFamily = selectedFontFamily,
                        onFocusChanged = { focused ->
                            if (focused) viewModel.setFocusedBlock(block.id)
                        },
                        onTextChanged = { text ->
                            viewModel.updateBlockText(block.id, text)
                        },
                        onEnterPressed = { nextBlockText ->
                            val isListType =
                                block.type == BlockType.BULLETED_LIST_ITEM ||
                                    block.type == BlockType.NUMBERED_LIST_ITEM ||
                                    block.type == BlockType.TODO_LIST_ITEM
                            if (isListType && block.text.isEmpty()) {
                                viewModel.changeBlockType(block.id, BlockType.PARAGRAPH)
                            } else if (isListType) {
                                viewModel.insertBlockAfter(block.id, block.type, nextBlockText)
                            } else {
                                viewModel.insertBlockAfter(block.id, BlockType.PARAGRAPH, nextBlockText)
                            }
                        },
                        onBackspaceOnEmpty = {
                            val isListType =
                                block.type == BlockType.BULLETED_LIST_ITEM ||
                                    block.type == BlockType.NUMBERED_LIST_ITEM ||
                                    block.type == BlockType.TODO_LIST_ITEM
                            if (isListType) {
                                viewModel.changeBlockType(block.id, BlockType.PARAGRAPH)
                            } else {
                                viewModel.deleteBlock(block.id)
                            }
                        },
                        onToggleTodo = {
                            viewModel.toggleTodoBlockChecked(block.id)
                        },
                        onChangeType = { type ->
                            viewModel.changeBlockType(block.id, type)
                        },
                        onApplyStyle = { styleType, start, end ->
                            viewModel.applyStyleToSelection(block.id, styleType, start, end)
                        },
                        onDeleteBlock = {
                            viewModel.deleteBlock(block.id)
                        },
                        onSelectionChanged = { selection ->
                            if (focusedBlockId == block.id) {
                                activeSelection = selection
                            }
                        },
                    )
                }
            }
        }

        // Horizontal Scrollable Combined Unified Toolbar and Keyboard Accessory Bar
        if (focusedBlockId != null) {
            val focusedBlock = note.blocks.find { it.id == focusedBlockId }
            if (focusedBlock != null) {
                var showBlocksMenu by remember { mutableStateOf(false) }

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .height(52.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(26.dp),
                    border =
                        androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        ),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 1. Notion-Style Dropdown Block Selector
                        Box {
                            val currentTypeName =
                                when (focusedBlock.type) {
                                    BlockType.PARAGRAPH -> "Text"
                                    BlockType.HEADER_1 -> "Heading 1"
                                    BlockType.HEADER_2 -> "Heading 2"
                                    BlockType.HEADER_3 -> "Heading 3"
                                    BlockType.TODO_LIST_ITEM -> "To-do"
                                    BlockType.BULLETED_LIST_ITEM -> "Bullet List"
                                    BlockType.NUMBERED_LIST_ITEM -> "Numbered List"
                                    BlockType.QUOTE -> "Quote"
                                    BlockType.CALLOUT -> "Callout"
                                    BlockType.CODE_BLOCK -> "Code Block"
                                }

                            FilterChip(
                                selected = true,
                                onClick = { showBlocksMenu = true },
                                label = { Text(currentTypeName, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Type",
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                            )

                            DropdownMenu(
                                expanded = showBlocksMenu,
                                onDismissRequest = { showBlocksMenu = false },
                            ) {
                                val items =
                                    listOf(
                                        Pair("Text", BlockType.PARAGRAPH),
                                        Pair("Heading 1", BlockType.HEADER_1),
                                        Pair("Heading 2", BlockType.HEADER_2),
                                        Pair("Heading 3", BlockType.HEADER_3),
                                        Pair("To-do list", BlockType.TODO_LIST_ITEM),
                                        Pair("Bulleted list", BlockType.BULLETED_LIST_ITEM),
                                        Pair("Numbered list", BlockType.NUMBERED_LIST_ITEM),
                                        Pair("Quote", BlockType.QUOTE),
                                        Pair("Callout", BlockType.CALLOUT),
                                        Pair("Code block", BlockType.CODE_BLOCK),
                                    )
                                items.forEach { (label, type) ->
                                    DropdownMenuItem(
                                        text = { Text(label, fontSize = 13.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = getBlockIcon(type),
                                                contentDescription = label,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        },
                                        onClick = {
                                            viewModel.changeBlockType(focusedBlock.id, type)
                                            showBlocksMenu = false
                                        },
                                    )
                                }
                            }
                        }

                        VerticalDivider(modifier = Modifier.height(24.dp))

                        // 2. High-fidelity conversion shortcuts from 5-Tool keyboard bar
                        // Pen/Scribble: Converts to/from CALLOUT
                        IconButton(
                            onClick = {
                                val newType = if (focusedBlock.type == BlockType.CALLOUT) BlockType.PARAGRAPH else BlockType.CALLOUT
                                viewModel.changeBlockType(focusedBlock.id, newType)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Scribble/Callout",
                                tint = if (focusedBlock.type == BlockType.CALLOUT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        // Checklist/Todo: Converts to/from TODO_LIST_ITEM
                        IconButton(
                            onClick = {
                                val newType = if (focusedBlock.type == BlockType.TODO_LIST_ITEM) BlockType.PARAGRAPH else BlockType.TODO_LIST_ITEM
                                viewModel.changeBlockType(focusedBlock.id, newType)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = "Checklist/Todo",
                                tint = if (focusedBlock.type == BlockType.TODO_LIST_ITEM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        // Table/Grid: Converts to/from CODE_BLOCK
                        IconButton(
                            onClick = {
                                val newType = if (focusedBlock.type == BlockType.CODE_BLOCK) BlockType.PARAGRAPH else BlockType.CODE_BLOCK
                                viewModel.changeBlockType(focusedBlock.id, newType)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "Table/Code block",
                                tint = if (focusedBlock.type == BlockType.CODE_BLOCK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(24.dp))

                        // Formatting Actions
                        val textRange = activeSelection ?: TextRange(0, focusedBlock.text.length)
                        val start = textRange.start
                        val end = textRange.end

                        IconButton(
                            onClick = {
                                viewModel.applyStyleToSelection(focusedBlock.id, StyleType.BOLD, start, end)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.FormatBold, "Bold", modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                viewModel.applyStyleToSelection(focusedBlock.id, StyleType.ITALIC, start, end)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.FormatItalic, "Italic", modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                viewModel.applyStyleToSelection(focusedBlock.id, StyleType.UNDERLINE, start, end)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.FormatUnderlined, "Underline", modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                viewModel.applyStyleToSelection(focusedBlock.id, StyleType.STRIKETHROUGH, start, end)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.FormatStrikethrough, "Strikethrough", modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                viewModel.applyStyleToSelection(focusedBlock.id, StyleType.CODE, start, end)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.Code, "Code Span", modifier = Modifier.size(18.dp))
                        }

                        VerticalDivider(modifier = Modifier.height(24.dp))

                        // Text Colors
                        IconButton(
                            onClick = {
                                viewModel.applyStyleToSelection(focusedBlock.id, StyleType.TEXT_COLOR_RED, start, end)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.FormatColorText, "Red Text", modifier = Modifier.size(18.dp), tint = Color.Red)
                        }

                        IconButton(
                            onClick = {
                                viewModel.applyStyleToSelection(focusedBlock.id, StyleType.TEXT_COLOR_BLUE, start, end)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.FormatColorText,
                                "Blue Text",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF1B63C2),
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.applyStyleToSelection(focusedBlock.id, StyleType.TEXT_COLOR_GREEN, start, end)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.FormatColorText,
                                "Green Text",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF2E7D32),
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(24.dp))

                        // Highlights/Backgrounds
                        IconButton(
                            onClick = {
                                viewModel.applyStyleToSelection(focusedBlock.id, StyleType.BACKGROUND_COLOR_YELLOW, start, end)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.Highlight,
                                "Yellow Highlight",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFFFF9C4),
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.applyStyleToSelection(focusedBlock.id, StyleType.BACKGROUND_COLOR_LIGHT_GRAY, start, end)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.FormatColorFill,
                                "Light Gray Bg",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFEEEEEE),
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(24.dp))

                        // Add Block & Delete Block Actions
                        IconButton(
                            onClick = {
                                viewModel.insertBlockAfter(focusedBlock.id, BlockType.PARAGRAPH, "")
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.AddCircleOutline, "Add Block Below", modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = {
                                viewModel.deleteBlock(focusedBlock.id)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "Delete Block",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockEditorItem(
    block: Block,
    focusedBlockId: String?,
    onFocusChanged: (Boolean) -> Unit,
    onTextChanged: (String) -> Unit,
    onEnterPressed: (String) -> Unit,
    onBackspaceOnEmpty: () -> Unit,
    onToggleTodo: () -> Unit,
    onChangeType: (BlockType) -> Unit,
    onApplyStyle: (StyleType, Int, Int) -> Unit,
    onDeleteBlock: () -> Unit,
    onSelectionChanged: (TextRange) -> Unit,
    sequenceNumber: Int = 1,
    markdownEnabled: Boolean = true,
    selectedFontFamily: FontFamily = FontFamily.Default,
) {
    val isFocused = focusedBlockId == block.id
    val focusRequester = remember { FocusRequester() }

    var textFieldValue by remember(block.id) {
        mutableStateOf(
            TextFieldValue(
                annotatedString = RichTextTransformer.toAnnotatedString(block.text, block.inlineStyles),
                selection = TextRange(block.text.length),
            ),
        )
    }

    // Keep synchronization of external state changes (e.g. typing or format toggles)
    LaunchedEffect(block.text, block.inlineStyles) {
        val annotated = RichTextTransformer.toAnnotatedString(block.text, block.inlineStyles)
        if (textFieldValue.text != block.text || textFieldValue.annotatedString != annotated) {
            val length = block.text.length
            val newStart = textFieldValue.selection.start.coerceIn(0, length)
            val newEnd = textFieldValue.selection.end.coerceIn(0, length)
            textFieldValue =
                textFieldValue.copy(
                    annotatedString = annotated,
                    selection = TextRange(newStart, newEnd),
                )
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(isFocused, textFieldValue.selection) {
        if (isFocused) {
            onSelectionChanged(textFieldValue.selection)
        }
    }

    // Slash command / popup menu state
    var showSlashMenu by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
    ) {
        // Block Layout Container with indentations for professional list layouts
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        when (block.type) {
                            BlockType.BULLETED_LIST_ITEM -> Modifier.padding(start = 14.dp, end = 4.dp)
                            BlockType.NUMBERED_LIST_ITEM -> Modifier.padding(start = 14.dp, end = 4.dp)
                            BlockType.TODO_LIST_ITEM -> Modifier.padding(start = 8.dp, end = 4.dp)
                            BlockType.CALLOUT ->
                                Modifier
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            BlockType.QUOTE ->
                                Modifier
                                    .background(Color(0xFFF8FAFC))
                                    .padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
                            else -> Modifier
                        },
                    ),
            verticalAlignment = Alignment.Top,
        ) {
            // Decorators based on BlockType with polished alignment
            when (block.type) {
                BlockType.BULLETED_LIST_ITEM -> {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "•",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                BlockType.NUMBERED_LIST_ITEM -> {
                    Text(
                        "$sequenceNumber.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 10.dp, top = 0.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                BlockType.TODO_LIST_ITEM -> {
                    val isChecked = block.properties["checked"] == "true"
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleTodo() },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                BlockType.CALLOUT -> {
                    Text(
                        "💡",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                }
                BlockType.QUOTE -> {
                    // Styled as a vertical strip indicator on left
                    Box(
                        modifier =
                            Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(Color.LightGray)
                                .padding(end = 10.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                else -> {}
            }

            val textStyle =
                remember(block.type, selectedFontFamily) {
                    getBlockTextStyle(block.type).copy(fontFamily = if (block.type == BlockType.CODE_BLOCK) FontFamily.Monospace else selectedFontFamily)
                }

            // Main Core BasicTextField
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val oldText = textFieldValue.text
                    if (newValue.text.contains("\n")) {
                        val index = newValue.text.indexOf('\n')
                        val beforeText = newValue.text.substring(0, index)
                        val afterText = newValue.text.substring(index + 1)

                        // Immediately update state to prevent flicker
                        textFieldValue =
                            TextFieldValue(
                                annotatedString = RichTextTransformer.toAnnotatedString(beforeText, block.inlineStyles),
                                selection = TextRange(beforeText.length),
                            )
                        onTextChanged(beforeText)
                        onEnterPressed(afterText)
                    } else {
                        // Check for markdown shortcuts at the start of the block
                        val text = newValue.text
                        var matchedShortcut = false
                        var targetType: BlockType? = null
                        var prefixLength = 0

                        if (markdownEnabled && (text.startsWith("- ") || text.startsWith("* ") || text.startsWith("• "))) {
                            targetType = BlockType.BULLETED_LIST_ITEM
                            prefixLength = 2
                            matchedShortcut = true
                        } else if (markdownEnabled && (text.startsWith("1. ") || text.startsWith("1) "))) {
                            targetType = BlockType.NUMBERED_LIST_ITEM
                            prefixLength = 3
                            matchedShortcut = true
                        } else if (markdownEnabled && (text.startsWith("[] ") || text.startsWith("[ ] "))) {
                            targetType = BlockType.TODO_LIST_ITEM
                            prefixLength = if (text.startsWith("[] ")) 3 else 4
                            matchedShortcut = true
                        } else if (markdownEnabled && text.startsWith("# ")) {
                            targetType = BlockType.HEADER_1
                            prefixLength = 2
                            matchedShortcut = true
                        } else if (markdownEnabled && text.startsWith("## ")) {
                            targetType = BlockType.HEADER_2
                            prefixLength = 3
                            matchedShortcut = true
                        } else if (markdownEnabled && text.startsWith("### ")) {
                            targetType = BlockType.HEADER_3
                            prefixLength = 4
                            matchedShortcut = true
                        } else if (markdownEnabled && text.startsWith("> ")) {
                            targetType = BlockType.QUOTE
                            prefixLength = 2
                            matchedShortcut = true
                        }

                        if (matchedShortcut && targetType != null) {
                            val remainingText = text.substring(prefixLength)
                            val newAnnotated = RichTextTransformer.toAnnotatedString(remainingText, emptyList())
                            textFieldValue =
                                TextFieldValue(
                                    annotatedString = newAnnotated,
                                    selection = TextRange(remainingText.length),
                                )
                            onTextChanged(remainingText)
                            onChangeType(targetType)
                        } else {
                            textFieldValue = newValue
                            // Callback to trigger text updates
                            if (newValue.text != oldText) {
                                onTextChanged(newValue.text)

                                // Check if typed slash command "/"
                                if (newValue.text.endsWith("/")) {
                                    showSlashMenu = true
                                } else {
                                    showSlashMenu = false
                                }
                            }
                        }
                    }
                },
                textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { onFocusChanged(it.isFocused) }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.key == Key.Enter) {
                                    val selStart = textFieldValue.selection.start
                                    val text = textFieldValue.text
                                    val beforeText = text.substring(0, selStart)
                                    val afterText = text.substring(selStart)

                                    textFieldValue =
                                        TextFieldValue(
                                            annotatedString = RichTextTransformer.toAnnotatedString(beforeText, block.inlineStyles),
                                            selection = TextRange(beforeText.length),
                                        )
                                    onTextChanged(beforeText)
                                    onEnterPressed(afterText)
                                    true
                                } else if (keyEvent.key == Key.Backspace && textFieldValue.text.isEmpty()) {
                                    onBackspaceOnEmpty()
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        },
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = getPlaceholderText(block.type),
                                style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        // Inline Slash commands popover menu when typed "/"
        if (showSlashMenu && isFocused) {
            Popup(
                alignment = Alignment.BottomStart,
                onDismissRequest = { showSlashMenu = false },
            ) {
                Card(
                    modifier =
                        Modifier
                            .width(220.dp)
                            .heightIn(max = 250.dp)
                            .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            "Basic Blocks",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                        val items =
                            listOf(
                                Pair("Text", BlockType.PARAGRAPH),
                                Pair("Heading 1", BlockType.HEADER_1),
                                Pair("Heading 2", BlockType.HEADER_2),
                                Pair("Heading 3", BlockType.HEADER_3),
                                Pair("To-do list", BlockType.TODO_LIST_ITEM),
                                Pair("Bulleted list", BlockType.BULLETED_LIST_ITEM),
                                Pair("Numbered list", BlockType.NUMBERED_LIST_ITEM),
                                Pair("Quote", BlockType.QUOTE),
                                Pair("Callout", BlockType.CALLOUT),
                                Pair("Code block", BlockType.CODE_BLOCK),
                            )
                        LazyColumn {
                            items(items) { (label, type) ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onChangeType(type)
                                                // Strip out the last '/' slash symbol from input
                                                val textWithoutSlash = textFieldValue.text.removeSuffix("/")
                                                onTextChanged(textWithoutSlash)
                                                showSlashMenu = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = getBlockIcon(type),
                                        contentDescription = label,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Selection Style formatting toolbar (Bold, Italic, Color, Code, custom transformations)
        if (isFocused && textFieldValue.selection.length > 0) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-45).dp),
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.BOLD, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.FormatBold, "Bold", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.ITALIC, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.FormatItalic, "Italic", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.UNDERLINE, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.FormatUnderlined, "Underline", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.CODE, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.Code, "Code Span", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.TEXT_COLOR_RED, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.FormatColorText, "Red Text", modifier = Modifier.size(16.dp), tint = Color.Red)
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.TEXT_COLOR_BLUE, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.FormatColorText, "Blue Text", modifier = Modifier.size(16.dp), tint = Color(0xFF1B63C2))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.TEXT_COLOR_GREEN, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.FormatColorText, "Green Text", modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.STRIKETHROUGH, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.FormatStrikethrough, "Strikethrough", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(
                                    StyleType.BACKGROUND_COLOR_YELLOW,
                                    textFieldValue.selection.start,
                                    textFieldValue.selection.end,
                                )
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.Highlight, "Yellow Highlight", modifier = Modifier.size(16.dp), tint = Color(0xFFFFF9C4))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(
                                    StyleType.BACKGROUND_COLOR_LIGHT_GRAY,
                                    textFieldValue.selection.start,
                                    textFieldValue.selection.end,
                                )
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.FormatColorFill, "Light Gray Bg", modifier = Modifier.size(16.dp), tint = Color(0xFFEEEEEE))
                        }
                    }
                }
            }
        }
    }
}

private fun getBlockTextStyle(type: BlockType): TextStyle {
    return when (type) {
        BlockType.HEADER_1 -> TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
        BlockType.HEADER_2 -> TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
        BlockType.HEADER_3 -> TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold)
        BlockType.QUOTE -> TextStyle(fontSize = 14.sp, fontStyle = FontStyle.Italic)
        BlockType.CODE_BLOCK -> TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        else -> TextStyle(fontSize = 14.sp)
    }
}

private fun getPlaceholderText(type: BlockType): String {
    return when (type) {
        BlockType.HEADER_1 -> "Heading 1"
        BlockType.HEADER_2 -> "Heading 2"
        BlockType.HEADER_3 -> "Heading 3"
        BlockType.QUOTE -> "Empty Quote"
        BlockType.CODE_BLOCK -> "Type some code..."
        else -> "Type '/' for commands"
    }
}

private fun getBlockIcon(type: BlockType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type) {
        BlockType.HEADER_1 -> Icons.Default.LooksOne
        BlockType.HEADER_2 -> Icons.Default.LooksTwo
        BlockType.HEADER_3 -> Icons.Default.Looks3
        BlockType.TODO_LIST_ITEM -> Icons.Default.CheckBox
        BlockType.BULLETED_LIST_ITEM -> Icons.AutoMirrored.Filled.FormatListBulleted
        BlockType.NUMBERED_LIST_ITEM -> Icons.Default.FormatListNumbered
        BlockType.QUOTE -> Icons.Default.FormatQuote
        BlockType.CALLOUT -> Icons.Default.Lightbulb
        BlockType.CODE_BLOCK -> Icons.Default.Code
        else -> Icons.Default.Title
    }
}

@Composable
fun NoteGridCard(
    note: com.scryme.notes.domain.model.Note,
    viewModel: NoteViewModel,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tag =
        remember(note.id, note.updatedAt) {
            val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
            prefs.getString("label_note_${note.id}", null) ?: run {
                val tags = listOf("Random", "Work", "Goals", "Personal", "Journal")
                tags[kotlin.math.abs(note.id.hashCode()) % tags.size]
            }
        }

    val (tagBg, tagText) =
        when (tag) {
            "Work" -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1)) // Blue
            "Personal" -> Pair(Color(0xFFFCE7F3), Color(0xFFBE185D)) // Pink
            "Goals" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D)) // Green
            "Journal" -> Pair(Color(0xFFF3E8FF), Color(0xFF6B21A8)) // Purple
            else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569)) // Gray
        }

    val previewText =
        remember(note.blocks) {
            note.blocks.joinToString(" ") { it.text }.trim()
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { viewModel.selectNote(note.id) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border =
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val thumbnailEmoji = remember(note.id) {
                    val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.getString("thumbnail_note_${note.id}", null)
                }
                Surface(
                    color = tagBg,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = if (thumbnailEmoji != null) "$thumbnailEmoji $tag" else tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tagText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val isPinned = remember(note.id) {
                        val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                        val pinnedSet = prefs.getStringSet("pinned_notes", emptySet()) ?: emptySet()
                        pinnedSet.contains(note.id)
                    }
                    val isLocked = remember(note.id) {
                        val prefs = context.getSharedPreferences("notes_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.getBoolean("locked_note_${note.id}", false)
                    }
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.NorthEast,
                        contentDescription = "Open Note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tag == "Random" && note.title.contains("Pause", ignoreCase = true)) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val colors = listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF94A3B8))
                    colors.forEach { c ->
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 30.dp, height = 20.dp)
                                    .background(c, RoundedCornerShape(4.dp)),
                        )
                    }
                }
            } else if (tag == "Goals" && note.title.contains("Goals", ignoreCase = true)) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("☕", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            Text(
                text = if (note.title.isBlank()) "Untitled" else note.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (previewText.isEmpty()) "No content" else previewText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = getElapsedTimeString(note.updatedAt),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

fun getElapsedTimeString(updatedAt: Long): String {
    val diff = System.currentTimeMillis() - updatedAt
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
