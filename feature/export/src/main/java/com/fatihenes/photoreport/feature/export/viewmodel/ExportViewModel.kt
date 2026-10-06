package com.fatihenes.photoreport.feature.export.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.core.common.model.FileSizeInfo
import com.fatihenes.photoreport.core.domain.repository.ReportRepository
import com.fatihenes.photoreport.core.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _fileSizeInfo = MutableStateFlow<FileSizeInfo?>(null)
    val fileSizeInfo: StateFlow<FileSizeInfo?> = _fileSizeInfo.asStateFlow()

    fun calculateFileSizes(photos: List<Photo>) {
        viewModelScope.launch {
            try {
                _fileSizeInfo.value = reportRepository.calculateFileSizes(photos)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Hata yutulmasın diye logla; UI hesaplıyor göstergesinde takılı kalmaz:
                // boş liste için sıfır değer yayınla.
                android.util.Log.w("ExportViewModel", "calculateFileSizes failed", e)
                if (photos.isEmpty()) {
                    _fileSizeInfo.value = com.fatihenes.photoreport.core.model.FileSizeInfo(
                        totalPhotoBytes = 0L,
                        totalVideoBytes = 0L,
                        photoCount = 0,
                        videoCount = 0,
                        estimatedQ100Bytes = 0L,
                        estimatedQ85Bytes = 0L,
                        estimatedQ75Bytes = 0L
                    )
                }
            }
        }
    }

    fun exportProject(projectId: Long, projectName: String, format: String, quality: Int, language: String) {
        reportRepository.enqueueExportWork(projectId, projectName, format, quality, language)
    }
}
