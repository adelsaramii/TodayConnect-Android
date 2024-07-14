package com.today.connect.utils

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import com.today.connect.MainActivity
import com.today.connect.services.DownloadService
import java.io.File
import java.io.FileInputStream
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder


class FileUtils(
    activity: MainActivity?,
    url: String,
    FilePath: String?,
    private var service: DownloadService?
) {
    private val mActivity: MainActivity? = activity
    private val mUrl: String = url
    private var mFilePath: String = ""
    private var file: File

    @Throws(MalformedURLException::class)
    fun generateFilePathFromURL() {
        var fileName: String? = getFileNameFromUrl(URL(mUrl))
        if (mUrl.contains("-YF-")) {
            var contentDisposition = mUrl.substring(mUrl.indexOf("-YF-") + 4)
            if (contentDisposition.contains("?")) {
                contentDisposition =
                    contentDisposition.substring(0, contentDisposition.indexOf("?"))
            }
            contentDisposition = "attachment; filename=\"$contentDisposition\""
            fileName = URLUtil.guessFileName(mUrl, contentDisposition, getMimeType(mUrl))
        }
        var downloadFolderPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        if (getMimeType(mUrl)!!.contains("image")) downloadFolderPath =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ).path else if (getMimeType(mUrl)!!.contains("video")) downloadFolderPath =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
            ).path
        try {
            mFilePath = downloadFolderPath + "/" + URLDecoder.decode(fileName, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }

    fun checkFileExists(): Boolean {
        return File(URLDecoder.decode(mFilePath)).exists()
    }

    fun checkIfDownloading(serviceRunID: Int): Boolean {
        return File(URLDecoder.decode(mFilePath) + serviceRunID + ".downloading").exists()
    }

    private fun getMimeType(url: String?): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }

    // run installer if it is apk
    val mimeType: String
        get() {
            var type = getMimeType(mUrl)
            if (type == null) type = "*/*"
            val extension = mUrl.substring(mUrl.lastIndexOf("."))
            if (extension.contains("apk")) type = "application/vnd.android.package-archive"
            return type
        }

    // run installer if it is apk
    private val contentUri: Uri?
        get() {
            if (service != null) {
                return FileProvider.getUriForFile(
                    service!!.applicationContext,
                    "${service!!.packageName}.provider",
                    file
                )
            }
            return FileProvider.getUriForFile(
                mActivity!!.applicationContext,
                "${mActivity.packageName}.provider",
                file
            )
        }

    val intent: Intent
        get() {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Important for starting from Service
            }
            // run installer if it is apk
            return intent
        }

    fun openFile(): Boolean {
        if (file.exists()) {
            try {
                mActivity?.startActivity(intent)
                service?.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                try {
                    Toast.makeText(
                        mActivity, "No application can handle this file type...",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (_: ActivityNotFoundException) {
                }
            }
        }
        return false
    }

    companion object {
        fun getFileNameFromUrl(url: URL): String {
            val urlString = url.file
            return urlString.substring(urlString.lastIndexOf('/') + 1).split("\\?".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()[0].split("#".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0]
        }
    }

    fun addFileToGallery() {
        var context: Context = if (mActivity != null) {
            mActivity.applicationContext
        } else {
            service!!.applicationContext
        }
        // Check if the file is an image or video (add other types if needed)
        val isImage =
            file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(".png")
        val isVideo = file.name.endsWith(".mp4") // Add other video formats if needed

        if (isImage || isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        if (isImage) "image/jpeg" else "video/mp4"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection =
                    if (isImage) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    else MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val item = context.contentResolver.insert(collection, values)

                item?.let { uri ->
                    context.contentResolver.openOutputStream(uri).use { outputStream ->
                        FileInputStream(file).copyTo(outputStream!!)
                    }

                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            } else {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    null
                ) { path, uri ->
                    Toast.makeText(
                        mActivity, "File added to gallery.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    init {
        if (FilePath != null) {
            mFilePath = FilePath
        }
        file = File(mFilePath)
    }
}