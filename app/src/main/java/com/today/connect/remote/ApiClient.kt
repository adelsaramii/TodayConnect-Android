package com.today.connect.remote

import com.today.connect.remote.models.TokenRequest
import com.today.connect.remote.models.TokenResponse
import com.today.connect.utils.Contents
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiClient {

    @POST("connect/token")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @FormUrlEncoded
    fun getAccessToken(
        @Field("client_id") clientId: String = Contents.ClientID,
        @Field("client_secret") clientSecret: String = Contents.ClientSecret,
        @Field("grant_type") grantType: String = Contents.GrantType,
        @Field("password") password: String,
        @Field("scopes") scopes: String = Contents.Scopes,
        @Field("username") username: String
    ): Call<TokenResponse>
}


 fun main() {
    // Set up logging for Retrofit
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    // Initialize Retrofit
    val retrofit = Retrofit.Builder()
        .baseUrl(Contents.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiClient::class.java)

    try {
        val response = retrofit.getAccessToken(
            clientId = "today_connect_client_mobile",
            clientSecret = "iwsIgsQTsocWdzIGmHl5PC9L9SCmJLlS23QCFQejpROKpkaWGDW16jmWDmos0xYZ",
            grantType = "password",
            password = "Ali1234@$",
            scopes = "openid profile today_connect_scope today_calendar_scope offline_access",
            username = "14038121"
        )

        println("Scope: ${response.request()}")
    } catch (e: HttpException) {
        // Log the HTTP error details
        val errorBody = e.response()?.errorBody()?.string()
        println("HTTP error code: ${e.code()}")
        println("HTTP error body: $errorBody")
    } catch (e: Exception) {
        // Log any other exceptions
        println("An error occurred: ${e.message}")
        e.printStackTrace()
    }
}