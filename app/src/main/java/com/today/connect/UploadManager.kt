package com.today.connect

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import com.today.connect.activity.MainActivity
import com.today.connect.activity.PermissionManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date


internal class UploadManager(activity: MainActivity, permissionManager: PermissionManager) {
    private val mActivity: MainActivity
    private val mPermissionManager: PermissionManager
    private lateinit var mValueCallback: ValueCallback<Array<Uri>>
    private var mFiles: Array<Uri?>? = null
    private var uriFilter: String

    fun setFiles(files: Array<Uri?>?) {
        mFiles = files
    }

    fun showFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ) {
        mValueCallback = filePathCallback
        uriFilter = ""
        for (mimeType in fileChooserParams.acceptTypes) {
            uriFilter += "$mimeType|"
        }
        uriFilter = if (uriFilter.isNotEmpty()) {
            uriFilter.substring(0, uriFilter.length - 1)
        } else {
            "*/*"
        }
        val res: Int = if (Build.VERSION.SDK_INT >= 33) {
            mPermissionManager.requestUploadPermission13()
        } else mPermissionManager.requestUploadPermission()
        if (res == PermissionManager.PERMISSION_REQUEST_DENIED) {
            cancelAfterPermissionDenied()
        } else if (res == PermissionManager.PERMISSION_REQUEST_GRANTED) {
            resumeAfterPermissionAcquired()
        }
    }

    fun generatePhotoUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
        val imageFileName = "XXX_$timeStamp.jpg"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val imageFile = File(storageDir, imageFileName)
        return Uri.fromFile(imageFile)
    }

    fun signalActivityResulted(resultCode: Int, data: Intent?) {
        try {
            if (resultCode != Activity.RESULT_OK) mValueCallback.onReceiveValue(null) else mValueCallback.onReceiveValue(
                arrayOf<Uri>(
                    data?.data!!
                )
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

     fun resumeAfterPermissionAcquired() {
        try {
            if (mFiles != null) {
                mValueCallback.onReceiveValue(mFiles as Array<Uri>)
                mFiles = null
                return
            }
            val pm: PackageManager = mActivity.applicationContext.packageManager
            val intents: MutableList<Intent> = ArrayList()
            val galleryIntent: Intent = if (uriFilter == "*/*") Intent(Intent.ACTION_GET_CONTENT) else Intent(Intent.ACTION_PICK)
            galleryIntent.type = uriFilter
            for (ri in pm.queryIntentActivities(galleryIntent, PackageManager.MATCH_DEFAULT_ONLY)) {
                val packageName = ri.activityInfo.packageName
                val intent = Intent(galleryIntent)
                intent.component = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
                intent.setPackage(packageName)
                intents.add(intent)
            }
            val chooser = Intent.createChooser(galleryIntent, "Select File")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            mActivity.startActivityForResult(chooser, MainActivity.CHOOSE_FILE_REQUEST)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mFiles = null
    }

    fun cancelAfterPermissionDenied() {
        mFiles = null
    }

    init {
        mActivity = activity
        mPermissionManager = permissionManager
        uriFilter = ""
    }
}
