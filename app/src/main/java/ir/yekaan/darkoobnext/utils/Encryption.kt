package ir.yekaan.darkoobnext.utils

import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


object Encryption {
    fun sha256(base: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(base.toByteArray(StandardCharsets.UTF_8))
            val hexString = StringBuilder()
            for (b in hash) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            hexString.toString()
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    fun encrypt(input: String, key: String): String? {
        try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keyByte = key.toByteArray(StandardCharsets.UTF_8)
            val secretKey = SecretKeySpec(keyByte, "AES/CBC/PKCS5Padding")
            val ivparameterspec = IvParameterSpec(keyByte)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivparameterspec)
            val cipherText = cipher.doFinal(input.toByteArray())
            return Base64.getEncoder().encodeToString(cipherText)
        } catch (e: Exception) {
            Log.e("UPLOAD_QUERY_ENCRYPTION", e.message!!)
        }
        return null
    }
}