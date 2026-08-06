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
            _fileSizeInfo.value = reportRepository.calculateFileSizes(photos)
        }
    }

    fun exportProject(projectId: Long, projectName: String, format: String, quality: Int, language: String) {
        reportRepository.enqueueExportWork(projectId, projectName, format, quality, language)
    }
}
