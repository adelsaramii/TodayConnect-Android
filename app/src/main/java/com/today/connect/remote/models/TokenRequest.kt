package com.today.connect.remote.models


import com.google.gson.annotations.SerializedName

data class TokenRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("client_secret")
    val clientSecret: String,
    @SerializedName("grant_type")
    val grantType: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("scopes")
    val scopes: String,
    @SerializedName("username")
    val username: String
)