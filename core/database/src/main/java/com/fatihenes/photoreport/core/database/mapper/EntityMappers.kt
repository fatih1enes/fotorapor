package com.fatihenes.photoreport.core.database.mapper

import com.fatihenes.photoreport.core.database.DailyLogEntity
import com.fatihenes.photoreport.core.database.LogWithPhotos
import com.fatihenes.photoreport.core.database.PhotoEntity
import com.fatihenes.photoreport.core.database.ProjectEntity
import com.fatihenes.photoreport.core.model.DailyLog
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.model.Project

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = name,
    colorHex = colorHex,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun DailyLogEntity.toDomain(): DailyLog = DailyLog(
    id = id,
    projectId = projectId,
    date = date,
    note = note
)

fun DailyLog.toEntity(): DailyLogEntity = DailyLogEntity(
    id = id,
    projectId = projectId,
    date = date,
    note = note
)

fun PhotoEntity.toDomain(): Photo = Photo(
    id = id,
    logId = logId,
    filePath = filePath,
    rotation = rotation,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun Photo.toEntity(): PhotoEntity = PhotoEntity(
    id = id,
    logId = logId,
    filePath = filePath,
    rotation = rotation,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun LogWithPhotos.toDomain(): DailyLogWithPhotos = DailyLogWithPhotos(
    log = log.toDomain(),
    photos = photos.map { it.toDomain() }
)
