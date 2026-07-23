package com.scryme.notes.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.scryme.notes.domain.model.Block
import com.scryme.notes.domain.model.BlockType
import com.scryme.notes.domain.model.StyleType
import com.scryme.notes.ui.components.RichTextTransformer
import com.scryme.notes.ui.viewmodel.NoteViewModel

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
    val selectedFontFamily = when (fontFamilyPref) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    var activeSelection by remember { mutableStateOf<TextRange?>(null) }

    if (activeNote == null) {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "Premium Book Icon",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome to Scryme Notes",
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A beautiful block-based workspace with nested notebooks, markdown shortcuts, rich text formatting, and obsidian style organization.",
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { viewModel.createRootNote("My First Note") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create page")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create a New Note", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onOpenSidebar,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = "Browse folders")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse Notes", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        return
    }

    val note = activeNote!!

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
                    val sequenceNumber = if (block.type == BlockType.NUMBERED_LIST_ITEM) {
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
                            val currentTypeName = when (focusedBlock.type) {
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
                                colors = FilterChipDefaults.filterChipColors(
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
                    Text(
                        "•",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp, top = 0.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                BlockType.NUMBERED_LIST_ITEM -> {
                    Text(
                        "$sequenceNumber.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 10.dp, top = 2.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                BlockType.TODO_LIST_ITEM -> {
                    val isChecked = block.properties["checked"] == "true"
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleTodo() },
                        modifier =
                            Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                    )
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
                            textFieldValue = TextFieldValue(
                                annotatedString = newAnnotated,
                                selection = TextRange(remainingText.length)
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

                                    textFieldValue = TextFieldValue(
                                        annotatedString = RichTextTransformer.toAnnotatedString(beforeText, block.inlineStyles),
                                        selection = TextRange(beforeText.length)
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
