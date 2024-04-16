package ir.yekaan.darkoobnext

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


internal class CacheManager(activity: MainActivity) {
    private val mActivity: MainActivity
    fun tryHandle(request: WebResourceRequest): WebResourceResponse? {
        if (request.method != "GET") return null
        val url = request.url.toString()
        if (isNetworkOnly(url)) return null
        val mimetype = getMimeType(url)
        val fileName = URLUtil.guessFileName(url, null, mimetype)
        if (isNetworkStream(fileName)) return null
        return if (!isNetworkFirst(fileName)) {
            if (fileExists(fileName)) createWebResourceResponseFromFile(
                fileName,
                mimetype
            ) else createWebResourceResponse(request, fileName)
        } else {
            val response = createWebResourceResponse(request, fileName)
            if (response != null) return response
            if (fileExists(fileName)) createWebResourceResponseFromFile(
                fileName,
                mimetype
            ) else null
        }
    }

    private fun createWebResourceResponseFromFile(
        fileName: String,
        mimeType: String?
    ): WebResourceResponse? {
        val inputStream: InputStream = try {
            mActivity.openFileInput(fileName)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        }
        val encoding = "UTF-8"
        val statusCode = 200
        val reasonPhase = "OK"
        val responseHeaders: MutableMap<String, String> = HashMap()
        responseHeaders["Access-Control-Allow-Origin"] = "*"
        val metaFile = "$fileName.meta"
        if (fileExists(metaFile)) {
            responseHeaders["Content-Disposition"] = readFromFile(metaFile)
        }
        return WebResourceResponse(
            mimeType,
            encoding,
            statusCode,
            reasonPhase,
            responseHeaders,
            inputStream
        )
    }

    private fun createWebResourceResponse(
        request: WebResourceRequest,
        fileName: String
    ): WebResourceResponse? {
        return try {
            val url = URL(request.url.toString())
            val conn = url.openConnection() as HttpURLConnection
            val requestHeaders = request.requestHeaders
            for ((key, value) in requestHeaders) {
                conn.setRequestProperty(key, value)
            }
            val mimeType = conn.contentType
            val contentDisposition = conn.getHeaderField("Content-Disposition")
            val input: InputStream = BufferedInputStream(url.openStream())
            val output: FileOutputStream =
                mActivity.openFileOutput(fileName + TEMP_FILE_EXTENSION, Context.MODE_PRIVATE)
            if (contentDisposition != null) {
                writeToFile("$fileName.meta$TEMP_FILE_EXTENSION", contentDisposition)
            }
            val data = ByteArray(1024)
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                output.write(data, 0, count)
            }
            output.flush()
            output.close()
            input.close()
            renameFile(fileName + TEMP_FILE_EXTENSION, fileName)
            renameFile(
                "$fileName.meta$TEMP_FILE_EXTENSION",
                "$fileName.meta"
            )
            createWebResourceResponseFromFile(fileName, mimeType)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteAppFiles() {
        for (file in mActivity.filesDir.list()!!) {
            try {
                if (isAppFile(file) || isTempFile(file)) mActivity.deleteFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAssetFiles() {
        for (file in mActivity.filesDir.list()!!) {
            try {
                if (!isAppFile(file)) mActivity.deleteFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fileExists(fileName: String): Boolean {
        return File(mActivity.filesDir.toString() + "/" + fileName).exists()
    }

    val isAppCached: Boolean
        get() = fileExists("bundle.js")

    private fun renameFile(oldName: String, newName: String): Boolean {
        val root: String = mActivity.filesDir.toString() + "/"
        val source = File(root + oldName)
        val dest = File(root + newName)
        return source.renameTo(dest)
    }

    private fun writeToFile(fileName: String, data: String) {
        try {
            val outputStreamWriter = OutputStreamWriter(
                mActivity.openFileOutput(fileName, Context.MODE_PRIVATE)
            )
            outputStreamWriter.write(data)
            outputStreamWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun readFromFile(fileName: String): String {
        var ret = ""
        try {
            val inputStream: InputStream = mActivity.openFileInput(fileName)
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var receiveString: String?
            val stringBuilder = StringBuilder()
            while (bufferedReader.readLine().also { receiveString = it } != null) {
                stringBuilder.append(receiveString)
            }
            inputStream.close()
            ret = stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ret
    }

    companion object {
        private val NETWORK_ONLY_PATHS = arrayOf(
            "/TokenGenerator",
            "/PezhvaMessageBroker",
            "/NativeFileUploadMainActivity",
            "/RFU",
            "/AuthService",
            "/FileUpload",
            "/Admin",
            "/DynaRest",
            "hamayand",
            "call",
            "tile",
            "8081"
        )
        private val NETWORK_FIRST_EXTENSIONS = arrayOf(".ver", ".json")
        private val NETWORK_STREAM_EXTENSIONS = arrayOf(".mp3", ".opus")
        private val APP_FILE_EXTENSIONS =
            arrayOf(".js", ".css", ".html", ".ttf", ".eot", "woff", "woff2")
        private const val TEMP_FILE_EXTENSION = ".tmp00"
        private fun getMimeType(url: String): String? {
            var type: String? = null
            val extension = MimeTypeMap.getFileExtensionFromUrl(url)
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
            return type
        }

        private fun disableSSLCertificateChecking() {
            val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? {
                    return null
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(arg0: Array<X509Certificate>, arg1: String) {
                    // Not implemented
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(arg0: Array<X509Certificate>, arg1: String) {
                    // Not implemented
                }
            })
            try {
                val sc = SSLContext.getInstance("TLS")
                sc.init(null, trustAllCerts, SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            } catch (e: KeyManagementException) {
                e.printStackTrace()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
            }
        }

        private fun isNetworkFirst(fileName: String): Boolean {
            for (ext in NETWORK_FIRST_EXTENSIONS) {
                if (fileName.lowercase(Locale.getDefault()).endsWith(ext)) {
                    return true
                }
            }
            return false
        }

        private fun isNetworkStream(fileName: String): Boolean {
            for (ext in NETWORK_STREAM_EXTENSIONS) {
                if (fileName.lowercase(Locale.getDefault()).endsWith(ext)) {
                    return true
                }
            }
            return false
        }

        private fun isNetworkOnly(url: String): Boolean {
            for (path in NETWORK_ONLY_PATHS) {
                if (url.lowercase(Locale.getDefault())
                        .contains(path.lowercase(Locale.getDefault()))
                ) return true
            }
            return false
        }

        private fun isAppFile(fileName: String): Boolean {
            for (ext in APP_FILE_EXTENSIONS) {
                if (fileName.lowercase(Locale.getDefault()).endsWith(ext)) {
                    return true
                }
            }
            return false
        }

        private fun isTempFile(fileName: String): Boolean {
            return fileName.endsWith(TEMP_FILE_EXTENSION)
        }
    }

    init {
        mActivity = activity
        disableSSLCertificateChecking()
    }
}
