package ir.yekaan.darkoobnext.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class SHA256 {
    companion object{
        fun hash(base: String): String? {
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
    }
}