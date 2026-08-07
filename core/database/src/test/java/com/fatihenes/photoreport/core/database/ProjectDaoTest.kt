package com.fatihenes.photoreport.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProjectDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ProjectDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.projectDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetProject() = runTest {
        val project = ProjectEntity(name = "Test", colorHex = "#FFFFFF")
        val id = dao.insertProject(project)

        val result = dao.getProjectByIdSuspend(id)
        assertEquals("Test", result?.name)
    }

    @Test
    fun getAllProjects_excludesDeleted() = runTest {
        dao.insertProject(ProjectEntity(name = "Active", colorHex = "#000"))
        val deletedId = dao.insertProject(ProjectEntity(name = "Deleted", colorHex = "#111"))
        dao.softDeleteProjectById(deletedId, System.currentTimeMillis())

        val projects = dao.getAllProjects().first()
        assertEquals(1, projects.size)
        assertEquals("Active", projects[0].name)
    }

    @Test
    fun softDeleteAndRestore() = runTest {
        val id = dao.insertProject(ProjectEntity(name = "Test", colorHex = "#000"))

        dao.softDeleteProjectById(id, System.currentTimeMillis())
        assertNull(dao.getProjectByIdSuspend(id))

        dao.restoreProjectById(id)
        val restored = dao.getProjectByIdSuspend(id)
        assertEquals("Test", restored?.name)
    }

    @Test
    fun hardDelete_removesFromDb() = runTest {
        val id = dao.insertProject(ProjectEntity(name = "Test", colorHex = "#000"))
        dao.hardDeleteProjectById(id)
        assertNull(dao.getProjectByIdSuspend(id))
    }
}
