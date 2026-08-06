package com.fatihenes.photoreport.core.common.model

data class FileSizeInfo(
    val totalPhotoBytes: Long,
    val totalVideoBytes: Long,
    val photoCount: Int,
    val videoCount: Int,
    val estimatedQ100Bytes: Long,
    val estimatedQ85Bytes: Long,
    val estimatedQ75Bytes: Long
)
