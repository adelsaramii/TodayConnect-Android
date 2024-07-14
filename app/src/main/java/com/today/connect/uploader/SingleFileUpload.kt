package com.today.connect.uploader

data class SingleFileUpload(
    val action: String,
    val fileID: String,
    val fileName: String?,
    val fileType: String?,
    val fileBlob: String?
)