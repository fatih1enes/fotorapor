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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.data.PhotoEntity
import com.fatihenes.photoreport.data.ProjectEntity
import com.fatihenes.photoreport.ui.viewmodel.TrashViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val deletedProjects by viewModel.deletedProjects.collectAsState(initial = emptyList())
    val deletedPhotos by viewModel.deletedPhotos.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

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
                Text(stringResource(R.string.trash_empty), fontSize = 18.sp, color = Color.Gray)
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
                            onRestore = { scope.launch { viewModel.restoreProject(project.id) } },
                            onDelete = { scope.launch { viewModel.hardDeleteProject(project.id) } }
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
                            onRestore = { scope.launch { viewModel.restorePhoto(photo.id) } },
                            onDelete = { scope.launch { viewModel.hardDeletePhoto(photo) } }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TrashProjectItem(project: ProjectEntity, onRestore: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))
    val dateStr = project.deletedAt?.let { sdf.format(Date(it)) } ?: stringResource(R.string.unknown_date)

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Color(android.graphics.Color.parseColor(project.colorHex)), androidx.compose.foundation.shape.CircleShape))
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
fun TrashPhotoItem(photo: PhotoEntity, onRestore: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))
    val dateStr = photo.deletedAt?.let { sdf.format(Date(it)) } ?: stringResource(R.string.unknown_date)

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = photo.filePath,
                contentDescription = null,
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
