package com.fatihenes.photoreport.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fatihenes.photoreport.core.database.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(testDb, 1)
        // Insert some data in V1 schema here if needed
        db.close()

        // Re-open the database with version 2 and provide MIGRATION_1_2
        db = helper.runMigrationsAndValidate(testDb, 2, true, AppDatabase.MIGRATION_1_2)

        // Assert rotation column exists
        val cursor = db.query("SELECT rotation FROM photos")
        assert(cursor.columnCount == 1)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        val db = helper.createDatabase(testDb, 2)
        db.close()

        helper.runMigrationsAndValidate(testDb, 3, true, AppDatabase.MIGRATION_2_3).close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To4() {
        var db = helper.createDatabase(testDb, 3)
        db.close()

        db = helper.runMigrationsAndValidate(testDb, 4, true, AppDatabase.MIGRATION_3_4)

        // Assert isDeleted and deletedAt exist in projects and photos
        var cursor = db.query("SELECT isDeleted, deletedAt FROM projects")
        assert(cursor.columnCount == 2)
        cursor.close()

        cursor = db.query("SELECT isDeleted, deletedAt FROM photos")
        assert(cursor.columnCount == 2)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        val db = helper.createDatabase(testDb, 4)
        db.close()

        helper.runMigrationsAndValidate(testDb, 5, true, AppDatabase.MIGRATION_4_5).close()
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest version of the database.
        helper.createDatabase(testDb, 1).apply {
            close()
        }

        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        val db = helper.runMigrationsAndValidate(
            testDb,
            5,
            true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5
        )
        db.close()
    }
}
