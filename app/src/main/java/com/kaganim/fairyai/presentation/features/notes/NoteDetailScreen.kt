package com.kaganim.fairyai.presentation.features.notes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun NoteDetailScreen(
    onSaveSuccess: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onSaveSuccess()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::saveNote) {
                Icon(Icons.Default.Save, contentDescription = "Save Note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.content,
                onValueChange = viewModel::onContentChange,
                label = { Text("Content") },
                modifier = Modifier.fillWeight(1f).fillMaxWidth()
            )
        }
    }
}

private fun Modifier.fillWeight(weight: Float): Modifier = this.then(Modifier) // Helper for layout
