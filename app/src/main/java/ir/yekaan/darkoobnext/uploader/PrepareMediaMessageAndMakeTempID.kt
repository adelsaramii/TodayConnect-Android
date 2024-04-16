package ir.yekaan.darkoobnext.uploader

import com.google.gson.annotations.SerializedName

data class PrepareMediaMessageAndMakeTempID(
    @SerializedName("fileID") var fileID: String,
    @SerializedName("fileName") var fileName: String?,
    @SerializedName("fileType") var fileType: String?,
    @SerializedName("modifiedAt") var modifiedAt: String?,
    @SerializedName("previewBlob") var previewBlob: String?,
    @SerializedName("targetUser") var targetUser: String?,
    @SerializedName("replyToInfo") var replyToInfo: String?,
    @SerializedName("caption") var caption: String?,
    @SerializedName("fileSize") var fileSize: String?
)