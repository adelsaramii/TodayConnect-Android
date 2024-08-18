package com.today.connect.data.uploader

import com.google.gson.annotations.SerializedName

data class NativeCodeUploadCaption(
    @SerializedName("fileID") var fileID: String,
    @SerializedName("caption") var caption: String,
)