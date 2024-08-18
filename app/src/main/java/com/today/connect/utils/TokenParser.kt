package com.today.connect.utils

import com.auth0.android.jwt.JWT

class TokenParser(val token: String) {

    fun parseTokenGetUsername():String{
        val jwt = JWT(token)
        val username = jwt.getClaim("name").asString()
        return username!!
    }

    fun parseTokenGetPhoneNumbers():String{
        val jwt = JWT(token)
        val phoneNumbers = jwt.getClaim("phone_numbers").asString()
        return phoneNumbers!!
    }
}