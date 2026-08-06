package com.fatihenes.photoreport.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@androidx.annotation.Keep
@androidx.compose.runtime.Stable
@Entity(
    tableName = "projects",
    indices = [
        androidx.room.Index(value = ["isDeleted"]),
        androidx.room.Index(value = ["deletedAt"])
    ]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@androidx.annotation.Keep
@androidx.compose.runtime.Stable
@Entity(
    tableName = "daily_logs",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["projectId"]),
        androidx.room.Index(value = ["date"])
    ]
)
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val date: Long, // Epoch millis
    val note: String
)

@androidx.annotation.Keep
@androidx.compose.runtime.Stable
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = DailyLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["logId"]),
        androidx.room.Index(value = ["isDeleted"]),
        androidx.room.Index(value = ["deletedAt"])
    ]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logId: Long,
    val filePath: String,
    val rotation: Float = 0f,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@androidx.annotation.Keep
@androidx.compose.runtime.Stable
data class LogWithPhotos(
    @androidx.room.Embedded val log: DailyLogEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "logId"
    )
    val photos: List<PhotoEntity>
)
