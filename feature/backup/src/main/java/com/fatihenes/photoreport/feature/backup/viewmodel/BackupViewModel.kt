package com.fatihenes.photoreport.feature.backup.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import com.fatihenes.photoreport.core.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _backupState = MutableStateFlow<OperationResult<Unit>?>(null)
    val backupState: StateFlow<OperationResult<Unit>?> = _backupState.asStateFlow()

    private val _restoreState = MutableStateFlow<OperationResult<Unit>?>(null)
    val restoreState: StateFlow<OperationResult<Unit>?> = _restoreState.asStateFlow()

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            backupRepository.createBackup(uri).collect { result ->
                _backupState.value = result
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            backupRepository.restoreBackup(uri).collect { result ->
                _restoreState.value = result
            }
        }
    }

    fun resetBackupState() {
        _backupState.value = null
    }

    fun resetRestoreState() {
        _restoreState.value = null
    }
}
