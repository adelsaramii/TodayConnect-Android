package ir.yekaan.darkoobnext.uploader

data class SingleFileUpload(
    val action: String,
    val fileID: String,
    val fileName: String?,
    val fileType: String?,
    val fileBlob: String?
)