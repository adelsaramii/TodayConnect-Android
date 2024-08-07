package com.today.connect

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Messenger
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.Toast
import com.today.connect.activity.MainActivity
import com.today.connect.activity.PermissionManager
import com.today.connect.services.DownloadService
import com.today.connect.state.GlobalState
import com.today.connect.utils.FileUtils
import java.io.File
import java.net.URLDecoder


internal class DownloadManager(activity: MainActivity, permissionManager: PermissionManager) :
    DownloadListener {
    private val mMainActivity: MainActivity
    private val mPermissionManager: PermissionManager
    private var mUrl: String? = null
    private var mContentDisposition: String? = null
    private var mMimetype: String? = null
    override fun onDownloadStart(
        url: String, userAgent: String,
        contentDisposition: String, mimetype: String,
        contentLength: Long
    ) {
        mUrl = url
        mContentDisposition = contentDisposition
        mMimetype = mimetype

        if (Build.VERSION.SDK_INT >= 33) {
            resumeAfterPermissionAcquired()
        } else {
            val res = mPermissionManager.requestDownloadPermission()
            if (res == PermissionManager.PERMISSION_REQUEST_DENIED) {
                cancelAfterPermissionDenied()
            } else if (res == PermissionManager.PERMISSION_REQUEST_GRANTED) {
                resumeAfterPermissionAcquired()
            }
        }
    }

    private fun getMimeType(url: String?): String {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type!!
    }

    private fun getFlavorSpecificFolder(): String {
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val flavorFolder = File(downloadsFolder, mMainActivity.resources.getString(R.string.app_name))

        if (!flavorFolder.exists()) {
            flavorFolder.mkdirs() // Create the folder if it doesn't exist
        }

        return mMainActivity.resources.getString(R.string.app_name)
    }

    private fun generateFilePathFromURL(filename: String?): String {
        if (mUrl == null || filename == null) return ""
        val url = URLDecoder.decode(mUrl, "UTF-8")
        var fileName: String? = filename
        if (url.contains("-YF-")) {
            var contentDisposition = url.substring(url.indexOf("-YF-") + 4)
            if (contentDisposition.contains("?")) {
                contentDisposition =
                    contentDisposition.substring(0, contentDisposition.indexOf("?"))
            }
            contentDisposition = "attachment; filename=\"$contentDisposition\""
            fileName = URLUtil.guessFileName(mUrl, contentDisposition, getMimeType(mUrl))
        }
        val downloadFolderPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
        return "$downloadFolderPath/${getFlavorSpecificFolder()}/$fileName"
    }

    fun resumeAfterPermissionAcquired() {
        if (mUrl == null) return
        val url = mUrl!!
        val storedFilePath = GlobalState.getFileDownloadPath(url)
        if (storedFilePath != null) {
            val of = FileUtils(mMainActivity, url, storedFilePath, null)
            if(of.openFile())
                return
        }

        val fileName = URLUtil.guessFileName(mUrl, mContentDisposition, mMimetype)

        val filePath = generateFilePathFromURL(URLDecoder.decode(fileName, "UTF-8"))

        if (filePath == "") {
            Toast.makeText(
                mMainActivity, "Unable to Download! Sorry...",
                Toast.LENGTH_SHORT
            ).show()
        }

        Toast.makeText(
            mMainActivity, "Downloading...",
            Toast.LENGTH_SHORT
        ).show()

        val i = Intent(mMainActivity.applicationContext, DownloadService::class.java)
        i.putExtra("address", url)
        i.putExtra("destination", filePath)
        i.putExtra("filename", fileName)
        i.putExtra("messenger", Messenger(mMainActivity.handler))
        mMainActivity.applicationContext.startService(i)
    }

    fun cancelAfterPermissionDenied() {
        mUrl = null
        mContentDisposition = null
        mMimetype = null
    }

    init {
        mMainActivity = activity
        mPermissionManager = permissionManager
    }
}
