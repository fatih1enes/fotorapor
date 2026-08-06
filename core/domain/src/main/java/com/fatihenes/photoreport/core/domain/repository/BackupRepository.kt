package com.fatihenes.photoreport.core.domain.repository

import android.net.Uri
import com.fatihenes.photoreport.core.common.util.result.OperationResult
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    fun createBackup(destUri: Uri): Flow<OperationResult<Unit>>
    fun restoreBackup(sourceUri: Uri): Flow<OperationResult<Unit>>
}
