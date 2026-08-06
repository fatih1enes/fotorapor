package com.fatihenes.photoreport.core.domain.usecase

import android.net.Uri
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import com.fatihenes.photoreport.core.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BackupProjectUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    operator fun invoke(destUri: Uri): Flow<OperationResult<Unit>> = repository.createBackup(destUri)
}

class RestoreProjectBackupUseCase @Inject constructor(
    private val repository: BackupRepository
) {
    operator fun invoke(sourceUri: Uri): Flow<OperationResult<Unit>> = repository.restoreBackup(sourceUri)
}
