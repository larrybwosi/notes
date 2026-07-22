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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scryme.notes.ui.DatabaseProvider
import com.scryme.notes.ui.screens.NoteEditorScreen
import com.scryme.notes.ui.screens.WorkspaceScreen
import com.scryme.notes.ui.viewmodel.NoteViewModel
import com.scryme.notes.ui.viewmodel.NoteViewModelFactory

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
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreenLayout(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreenLayout(viewModel: NoteViewModel) {
    var sidebarVisible by remember { mutableStateOf(true) }

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
                        // Optionally close sidebar on narrow screens when note selected
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Editor Screen View
            NoteEditorScreen(
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}