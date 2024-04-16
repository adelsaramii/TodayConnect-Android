package ir.yekaan.darkoobnext.uploader

import com.google.gson.annotations.SerializedName

data class ResumeActualUpload(
    @SerializedName("fileID") var fileID: String,
    @SerializedName("hash") var hash: String?,
    @SerializedName("restMessageSendHash") var restMessageSendHash: String?,
    @SerializedName("fullPath") var fullPath: String?,
    @SerializedName("filename") var filename: String?,
    @SerializedName("range") var range: String?,
    @SerializedName("tmpMsgId") var tmpMsgId: String?,
    @SerializedName("targetDomain") var targetDomain: String?,
    @SerializedName("tmpContent") var tmpContent: String?,
    @SerializedName("uiTempRawMessageKey") var uiTempRawMessageKey: String?,
    @SerializedName("tmpRawLocalMediaMessageJsonString") var tmpRawLocalMediaMessageJsonString: String?,
    @SerializedName("from") var from: String?
)