package com.fatihenes.photoreport.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.data.ProjectEntity
import com.fatihenes.photoreport.ui.viewmodel.TrashViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    language: String = "tr",
    viewModel: TrashViewModel = hiltViewModel()
) {
    val deletedProjects by viewModel.deletedProjects.collectAsState(initial = emptyList())
    val deletedPhotos by viewModel.deletedPhotos.collectAsState(initial = emptyList())
    
    var showDeleteConfirmProject by remember { mutableStateOf<Long?>(null) }
    var showDeleteConfirmPhoto by remember { mutableStateOf<PhotoEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.back_label))
                    }
                }
            )
        }
    ) { padding ->
        if (deletedProjects.isEmpty() && deletedPhotos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { isVisible = true }
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.8f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.acc_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.trash_empty),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (deletedProjects.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.trash_deleted_projects), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(deletedProjects, key = { it.id }) { project ->
                        TrashProjectItem(
                            project = project,
                            language = language,
                            onRestore = { viewModel.restoreProject(project.id) },
                            onDelete = { showDeleteConfirmProject = project.id }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                if (deletedPhotos.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.trash_deleted_photos), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(deletedPhotos, key = { it.id }) { photo ->
                        TrashPhotoItem(
                            photo = photo,
                            language = language,
                            onRestore = { viewModel.restorePhoto(photo.id) },
                            onDelete = { showDeleteConfirmPhoto = photo }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirmProject != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmProject = null },
            title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text("Bu projeyi tamamen silmek istediğinize emin misiniz? Bu işlem geri alınamaz.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmProject?.let { id ->
                            viewModel.hardDeleteProject(id)
                        }
                        showDeleteConfirmProject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.delete_confirm_btn), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmProject = null }) { Text(stringResource(R.string.cancel_btn)) }
            }
        )
    }

    if (showDeleteConfirmPhoto != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmPhoto = null },
            title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text(stringResource(R.string.delete_photo_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmPhoto?.let { photo ->
                            viewModel.hardDeletePhoto(photo)
                        }
                        showDeleteConfirmPhoto = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.delete_confirm_btn), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmPhoto = null }) { Text(stringResource(R.string.cancel_btn)) }
            }
        )
    }
}

@Composable
fun TrashProjectItem(project: ProjectEntity, language: String, onRestore: () -> Unit, onDelete: () -> Unit) {
    val dateStr = project.deletedAt?.let { com.fatihenes.photoreport.util.DateUtils.formatDateTime(it, language) } ?: stringResource(R.string.unknown_date)
    val projectColor = remember(project.colorHex) {
        runCatching { Color(project.colorHex.toColorInt()) }.getOrDefault(Color.Gray)
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(projectColor, androidx.compose.foundation.shape.CircleShape))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(project.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(stringResource(R.string.trash_deleted_at, dateStr), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, stringResource(R.string.restore_btn), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.hard_delete_btn), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TrashPhotoItem(photo: PhotoEntity, language: String, onRestore: () -> Unit, onDelete: () -> Unit) {
    val dateStr = photo.deletedAt?.let { com.fatihenes.photoreport.util.DateUtils.formatDateTime(it, language) } ?: stringResource(R.string.unknown_date)

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = photo.filePath,
                contentDescription = stringResource(R.string.photo_label),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(60.dp).background(Color.Gray)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.photo_label), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(stringResource(R.string.trash_deleted_at, dateStr), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, stringResource(R.string.restore_btn), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.hard_delete_btn), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
