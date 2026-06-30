package com.elektrik.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProjectEntity::class, DailyLogEntity::class, PhotoEntity::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun photoDao(): PhotoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Versiyon 1'den 2'ye geÃƒÆ’Ã‚Â§iÃƒâ€¦Ã…Â¸ (ÃƒÆ’Ã¢â‚¬â€œrnek: FotoÃƒâ€Ã…Â¸raflara dÃƒÆ’Ã‚Â¶ndÃƒÆ’Ã‚Â¼rme ÃƒÆ’Ã‚Â¶zelliÃƒâ€Ã…Â¸i eklenmesi)
                db.execSQL("ALTER TABLE photos ADD COLUMN rotation REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Versiyon 2'den 3'e geçiş (Performans için index eklenmesi)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_logs_projectId` ON `daily_logs` (`projectId`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Versiyon 3'ten 4'e geçiş (Çöp Kutusu için isDeleted ve deletedAt alanları eklendi)
                db.execSQL("ALTER TABLE projects ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN deletedAt INTEGER")
                
                db.execSQL("ALTER TABLE photos ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE photos ADD COLUMN deletedAt INTEGER")
            }
        }
    }
}
