package com.fatihenes.photoreport.core.domain.repository

import com.fatihenes.photoreport.core.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getAllProjects(): Flow<List<Project>>
    suspend fun insertProject(name: String, colorHex: String): Long
    suspend fun deleteProjectById(projectId: Long)
    fun getProjectById(projectId: Long): Flow<Project?>
    suspend fun getProjectByIdSuspend(projectId: Long): Project?
    suspend fun getLatestProjectSuspend(): Project?
}
