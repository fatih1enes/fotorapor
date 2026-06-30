package com.elektrik.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.elektrik.data.PhotoEntity
import com.elektrik.data.ProjectEntity
import com.elektrik.ui.viewmodel.TrashViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val deletedProjects by viewModel.deletedProjects.collectAsState(initial = emptyList())
    val deletedPhotos by viewModel.deletedPhotos.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Çöp Kutusu (Son 30 Gün)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { padding ->
        if (deletedProjects.isEmpty() && deletedPhotos.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Çöp kutusu boş", fontSize = 18.sp, color = Color.Gray)
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
                        Text("Silinen Projeler", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(deletedProjects) { project ->
                        TrashProjectItem(
                            project = project,
                            onRestore = { scope.launch { viewModel.restoreProject(project.id) } },
                            onDelete = { scope.launch { viewModel.hardDeleteProject(context, project.id) } }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                if (deletedPhotos.isNotEmpty()) {
                    item {
                        Text("Silinen Fotoğraflar", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(deletedPhotos) { photo ->
                        TrashPhotoItem(
                            photo = photo,
                            onRestore = { scope.launch { viewModel.restorePhoto(photo.id) } },
                            onDelete = { scope.launch { viewModel.hardDeletePhoto(context, photo) } }
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
    val dateStr = project.deletedAt?.let { sdf.format(Date(it)) } ?: "Bilinmiyor"
    
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Color(android.graphics.Color.parseColor(project.colorHex)), androidx.compose.foundation.shape.CircleShape))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(project.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Silinme: $dateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, "Geri Yükle", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Kalıcı Olarak Sil", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TrashPhotoItem(photo: PhotoEntity, onRestore: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))
    val dateStr = photo.deletedAt?.let { sdf.format(Date(it)) } ?: "Bilinmiyor"
    
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
                Text("Fotoğraf", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Silinme: $dateStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, "Geri Yükle", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Kalıcı Olarak Sil", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
