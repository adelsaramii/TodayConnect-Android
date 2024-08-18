package com.today.connect.remote

import com.today.connect.utils.Contents
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstant {
    val apiClient: ApiClient = Retrofit.Builder()
        .baseUrl(Contents.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiClient::class.java)
}