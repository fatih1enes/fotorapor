package com.fatihenes.photoreport.repository

import android.content.Context
import com.fatihenes.photoreport.data.ProjectDao
import com.fatihenes.photoreport.data.ProjectEntity
import com.fatihenes.photoreport.widget.WidgetDataHelper
import com.fatihenes.photoreport.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ProjectRepository {
    fun getAllProjects(): Flow<List<ProjectEntity>>
    suspend fun insertProject(project: ProjectEntity): Long
    suspend fun deleteProjectById(projectId: Long)
    fun getProjectById(projectId: Long): Flow<ProjectEntity?>
    suspend fun getProjectByIdSuspend(projectId: Long): ProjectEntity?
    suspend fun getLatestProjectSuspend(): ProjectEntity?
    suspend fun refreshWidgetData()
}

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val projectDao: ProjectDao
) : ProjectRepository {

    override fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    override suspend fun insertProject(project: ProjectEntity): Long {
        val id = projectDao.insertProject(project)
        refreshWidgetData()
        return id
    }

    override suspend fun deleteProjectById(projectId: Long) {
        projectDao.softDeleteProjectById(projectId, System.currentTimeMillis())
        refreshWidgetData()
    }

    override fun getProjectById(projectId: Long): Flow<ProjectEntity?> = projectDao.getProjectById(projectId)

    override suspend fun getProjectByIdSuspend(projectId: Long): ProjectEntity? = projectDao.getProjectByIdSuspend(projectId)

    override suspend fun getLatestProjectSuspend(): ProjectEntity? = projectDao.getLatestProjectSuspend()

    override suspend fun refreshWidgetData() = withContext(Dispatchers.IO) {
        val latest = projectDao.getLatestProjectSuspend()
        if (latest != null) {
            WidgetDataHelper.saveLatestProject(appContext, latest.id, latest.name)
        } else {
            WidgetDataHelper.saveLatestProject(appContext, -1L, appContext.getString(R.string.empty_state_title))
        }
    }
}
