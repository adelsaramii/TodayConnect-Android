package com.today.connect.uploader

import com.google.gson.annotations.SerializedName

data class JsonUploadRequestData(
    // local upload parameters
    @SerializedName("fileId") var fileId: String,
    @SerializedName("contentType") var contentType: String,
    @SerializedName("filename") var filename: String,
    @SerializedName("blob") var blob: String,
    @SerializedName("caption") var caption: String,
    @SerializedName("fileSize") var fileSize: Long,
    @SerializedName("to") var to: String,
    @SerializedName("origin") var origin: String,
    @SerializedName("replyToInfo") var replyToInfo: String,
    @SerializedName("uiMsgKey") var uiMsgKey: String,
    @SerializedName("tmpRawLocalMediaMessageJsonString") var tmpRawLocalMediaMessageJsonString: String,
    @SerializedName("from") var from: String,
    @SerializedName("filePath") var filePath: String,
    @SerializedName("status") var status: Status,
    @SerializedName("progress") var progress: Int,
    @SerializedName("modifiedAt") var modifiedAt: String,
    // server upload parameters
    @SerializedName("hash") var hash: String,
    @SerializedName("restMessageSendHash") var restMessageSendHash: String,
    @SerializedName("fullPath") var fullPath: String,
    @SerializedName("serverSideFileName") var serverSideFileName: String,
    @SerializedName("range") var range: String,
    @SerializedName("tmpMsgId") var tmpMsgId: String,
    @SerializedName("tmpContent") var tmpContent: String,
    @SerializedName("targetDomain") var targetDomain: String,
)