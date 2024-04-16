package ir.yekaan.darkoobnext.uploader

import com.google.gson.annotations.SerializedName

data class ServerResponse(
    @SerializedName(value = "msg", alternate = ["Msg"])
    val msg: String? = null,
    @SerializedName(value = "url", alternate = ["Url"])
    val url: String? = null,
    @SerializedName(value = "reason", alternate = ["Reason"])
    val reason: String? = null,
    @SerializedName(value = "offset", alternate = ["Offset"])
    val offset: String? = null,
)