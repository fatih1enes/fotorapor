package com.fatihenes.photoreport.feature.backup.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import com.fatihenes.photoreport.core.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {

    val backupState = MutableStateFlow<OperationResult<Unit>?>(null)
    val restoreState = MutableStateFlow<OperationResult<Unit>?>(null)

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            backupRepository.createBackup(uri).collect { result ->
                backupState.value = result
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            backupRepository.restoreBackup(uri).collect { result ->
                restoreState.value = result
            }
        }
    }

    fun resetBackupState() {
        backupState.value = null
    }

    fun resetRestoreState() {
        restoreState.value = null
    }
}
