package com.scryme.notes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.scryme.notes.domain.model.Block
import com.scryme.notes.domain.model.BlockType
import com.scryme.notes.domain.model.Note
import com.scryme.notes.domain.model.StyleType
import com.scryme.notes.ui.components.RichTextTransformer
import com.scryme.notes.ui.viewmodel.NoteViewModel

@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val activeNote by viewModel.activeNote.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val subNotes by viewModel.subNotes.collectAsState()
    val focusedBlockId by viewModel.focusedBlockId.collectAsState()

    if (activeNote == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Select a page or create a new one to start writing",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }

    val note = activeNote!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Notion Breadcrumbs
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            breadcrumbs.forEachIndexed { idx, crumb ->
                Text(
                    text = if (crumb.title.isBlank()) "Untitled" else crumb.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (idx == breadcrumbs.lastIndex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { viewModel.selectNote(crumb.id) }
                )
                if (idx < breadcrumbs.lastIndex) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Separator",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Borderless Title Editor
        var titleText by remember(note.id) { mutableStateOf(note.title) }
        BasicTextField(
            value = titleText,
            onValueChange = {
                titleText = it
                viewModel.updateActiveNoteTitle(it)
            },
            textStyle = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                if (titleText.isEmpty()) {
                    Text(
                        "Untitled",
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                }
                innerTextField()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nested Sub-Pages listing
        if (subNotes.isNotEmpty()) {
            Text(
                "Sub-pages",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            subNotes.forEach { sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectNote(sub.id) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Page Icon",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (sub.title.isBlank()) "Untitled" else sub.title,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
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
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add subpage", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add a sub-page", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable list of rich text blocks
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(note.blocks, key = { _, block -> block.id }) { index, block ->
                BlockEditorItem(
                    block = block,
                    focusedBlockId = focusedBlockId,
                    onFocusChanged = { focused ->
                        if (focused) viewModel.setFocusedBlock(block.id)
                    },
                    onTextChanged = { text ->
                        viewModel.updateBlockText(block.id, text)
                    },
                    onEnterPressed = { nextBlockText ->
                        viewModel.insertBlockAfter(block.id, BlockType.PARAGRAPH, nextBlockText)
                    },
                    onBackspaceOnEmpty = {
                        viewModel.deleteBlock(block.id)
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
                    onLanguageChanged = { lang ->
                        viewModel.updateCodeBlockLanguage(block.id, lang)
                    },
                    onDeleteBlock = {
                        viewModel.deleteBlock(block.id)
                    }
                )
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
    onLanguageChanged: (String) -> Unit,
    onDeleteBlock: () -> Unit
) {
    val isFocused = focusedBlockId == block.id
    val focusRequester = remember { FocusRequester() }

    var textFieldValue by remember(block.id) {
        mutableStateOf(
            TextFieldValue(
                annotatedString = RichTextTransformer.toAnnotatedString(block.text, block.inlineStyles),
                selection = TextRange(block.text.length)
            )
        )
    }

    // Keep synchronization of external state changes (e.g. typing or format toggles)
    LaunchedEffect(block.text, block.inlineStyles) {
        val annotated = RichTextTransformer.toAnnotatedString(block.text, block.inlineStyles)
        if (textFieldValue.text != block.text || textFieldValue.annotatedString != annotated) {
            val length = block.text.length
            val newStart = textFieldValue.selection.start.coerceIn(0, length)
            val newEnd = textFieldValue.selection.end.coerceIn(0, length)
            textFieldValue = textFieldValue.copy(
                annotatedString = annotated,
                selection = TextRange(newStart, newEnd)
            )
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }

    // Slash command / popup menu state
    var showSlashMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Drag / Block Action Handles (Notion Style)
            if (isFocused) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showTypeMenu by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showTypeMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Change block type",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteBlock,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete block",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        val items = listOf(
                            Pair("Text", BlockType.PARAGRAPH),
                            Pair("Heading 1", BlockType.HEADER_1),
                            Pair("Heading 2", BlockType.HEADER_2),
                            Pair("Heading 3", BlockType.HEADER_3),
                            Pair("To-do list", BlockType.TODO_LIST_ITEM),
                            Pair("Bulleted list", BlockType.BULLETED_LIST_ITEM),
                            Pair("Numbered list", BlockType.NUMBERED_LIST_ITEM),
                            Pair("Quote", BlockType.QUOTE),
                            Pair("Callout", BlockType.CALLOUT),
                            Pair("Code block", BlockType.CODE_BLOCK)
                        )
                        items.forEach { (label, type) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getBlockIcon(type),
                                        contentDescription = label,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    onChangeType(type)
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Block Layout Container
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (block.type == BlockType.CALLOUT) {
                            Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        } else if (block.type == BlockType.QUOTE) {
                            Modifier
                                .background(Color(0xFFF8FAFC))
                                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
                        } else {
                            Modifier
                        }
                    ),
                verticalAlignment = Alignment.Top
            ) {
                // Decorators based on BlockType
                when (block.type) {
                    BlockType.BULLETED_LIST_ITEM -> {
                        Text(
                            "•",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp, top = 2.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    BlockType.NUMBERED_LIST_ITEM -> {
                        Text(
                            "1.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 8.dp, top = 2.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    BlockType.TODO_LIST_ITEM -> {
                        val isChecked = block.properties["checked"] == "true"
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleTodo() },
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    BlockType.CALLOUT -> {
                        Text(
                            "💡",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                    }
                    BlockType.QUOTE -> {
                        // Styled as a vertical strip indicator on left
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(Color.LightGray)
                                .padding(end = 10.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    else -> {}
                }

                val textStyle = remember(block.type) {
                    getBlockTextStyle(block.type)
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
                            textFieldValue = TextFieldValue(
                                annotatedString = RichTextTransformer.toAnnotatedString(beforeText, block.inlineStyles),
                                selection = TextRange(beforeText.length)
                            )
                            onTextChanged(beforeText)
                            onEnterPressed(afterText)
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
                    },
                    textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { onFocusChanged(it.isFocused) }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                if (keyEvent.key == Key.Enter) {
                                    onEnterPressed("")
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
                                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        // Inline Slash commands popover menu when typed "/"
        if (showSlashMenu && isFocused) {
            Popup(
                alignment = Alignment.BottomStart,
                onDismissRequest = { showSlashMenu = false }
            ) {
                Card(
                    modifier = Modifier
                        .width(220.dp)
                        .heightIn(max = 250.dp)
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            "Basic Blocks",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        val items = listOf(
                            Pair("Text", BlockType.PARAGRAPH),
                            Pair("Heading 1", BlockType.HEADER_1),
                            Pair("Heading 2", BlockType.HEADER_2),
                            Pair("Heading 3", BlockType.HEADER_3),
                            Pair("To-do list", BlockType.TODO_LIST_ITEM),
                            Pair("Bulleted list", BlockType.BULLETED_LIST_ITEM),
                            Pair("Numbered list", BlockType.NUMBERED_LIST_ITEM),
                            Pair("Quote", BlockType.QUOTE),
                            Pair("Callout", BlockType.CALLOUT),
                            Pair("Code block", BlockType.CODE_BLOCK)
                        )
                        LazyColumn {
                            items(items) { (label, type) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onChangeType(type)
                                            // Strip out the last '/' slash symbol from input
                                            val textWithoutSlash = textFieldValue.text.removeSuffix("/")
                                            onTextChanged(textWithoutSlash)
                                            showSlashMenu = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getBlockIcon(type),
                                        contentDescription = label,
                                        modifier = Modifier.size(16.dp)
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
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-45).dp)
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.BOLD, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FormatBold, "Bold", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.ITALIC, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FormatItalic, "Italic", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.UNDERLINE, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FormatUnderlined, "Underline", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.CODE, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Code, "Code Span", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.TEXT_COLOR_RED, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FormatColorText, "Red Text", modifier = Modifier.size(16.dp), tint = Color.Red)
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.TEXT_COLOR_BLUE, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FormatColorText, "Blue Text", modifier = Modifier.size(16.dp), tint = Color(0xFF1B63C2))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.TEXT_COLOR_GREEN, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FormatColorText, "Green Text", modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.STRIKETHROUGH, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FormatStrikethrough, "Strikethrough", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.BACKGROUND_COLOR_YELLOW, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Highlight, "Yellow Highlight", modifier = Modifier.size(16.dp), tint = Color(0xFFFFF9C4))
                        }
                        IconButton(
                            onClick = {
                                onApplyStyle(StyleType.BACKGROUND_COLOR_LIGHT_GRAY, textFieldValue.selection.start, textFieldValue.selection.end)
                            },
                            modifier = Modifier.size(32.dp)
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
        BlockType.BULLETED_LIST_ITEM -> Icons.Default.FormatListBulleted
        BlockType.NUMBERED_LIST_ITEM -> Icons.Default.FormatListNumbered
        BlockType.QUOTE -> Icons.Default.FormatQuote
        BlockType.CALLOUT -> Icons.Default.Lightbulb
        BlockType.CODE_BLOCK -> Icons.Default.Code
        else -> Icons.Default.Title
    }
}
