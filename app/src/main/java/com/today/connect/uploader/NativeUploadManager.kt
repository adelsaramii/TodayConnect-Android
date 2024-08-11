package com.today.connect.data.uploader

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import com.today.connect.utils.FUtils
import com.today.connect.utils.RealPathUtil
import com.today.connect.JavaScriptInterface
import com.today.connect.ui.activity.MainActivity
import com.today.connect.ui.activity.PermissionManager

internal class NativeUploadManager(
    activity: MainActivity,
    permissionManager: PermissionManager,
    javaScriptInterface: JavaScriptInterface
) :
    PickiTCallbacks {
    private val mMainActivity: MainActivity
    private val mPermissionManager: PermissionManager
    private val mUploadHelper: UploadHelper
    private val mJavaScriptInterface: JavaScriptInterface
    private var uriFilter: String
    private var mTo: String = ""
    private var mReplyToInfo: String = ""
    private var pickIT: PickiT

    init {
        mMainActivity = activity
        mPermissionManager = permissionManager
        mJavaScriptInterface = javaScriptInterface
        mUploadHelper = UploadHelper(mMainActivity)
        uriFilter = ""
        pickIT = PickiT(mMainActivity.applicationContext, this, mMainActivity)
    }

    fun showFileChooser(
        to: String,
        mediaType: String,
        replyToInfo: String
    ) {
        mTo = to
        uriFilter = mediaType
        mReplyToInfo = replyToInfo
        if (uriFilter == "inApp" && Build.VERSION.SDK_INT >= 33) {
            uriFilter = "*"
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

    fun signalActivityResulted(resultCode: Int, data: Intent?) {
        var results: Array<String?>? = null
        var flag = true
        try {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val r: MutableList<String> = ArrayList()
                if (null != data.clipData) { // checking multiple selection or not
                    for (i in 0 until data.clipData!!.itemCount) {
                        val uri = data.clipData!!.getItemAt(i).uri
                        val f = FUtils(mMainActivity.applicationContext)
                        if (RealPathUtil.getRealPath(
                                mMainActivity.applicationContext,
                                uri
                            ) != null
                        ) RealPathUtil.getRealPath(
                            mMainActivity.applicationContext,
                            uri
                        )?.let {
                            r.add(
                                it
                            )
                        } else if (f.getPath(uri) != null) r.add(f.getPath(uri)!!) else flag =
                            false
                    }
                } else {
                    val uri = data.data
                    val f = FUtils(mMainActivity.applicationContext)
                    if (uri?.let {
                            RealPathUtil.getRealPath(
                                mMainActivity.applicationContext,
                                it
                            )
                        } != null
                    ) RealPathUtil.getRealPath(
                        mMainActivity.applicationContext,
                        uri
                    )?.let {
                        r.add(
                            it
                        )
                    } else if (uri?.let { f.getPath(it) } != null) f.getPath(uri)?.let { r.add(it) } else flag = false
                }
                results = r.toTypedArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (flag && results != null) {
            val newUploadRequests: Array<String> =
                mUploadHelper.requestNewUpload(results, mTo, mReplyToInfo)
            for (newUploadRequest in newUploadRequests) {
                mJavaScriptInterface.orderUiToProvideCaptionForNewUpload(newUploadRequest)
            }
        } else if (data != null) {
            val clipData = data.clipData
            if (clipData != null) {
                val numberOfFilesSelected = clipData.itemCount
                if (numberOfFilesSelected > 1) {
                    pickIT.getMultiplePaths(clipData)
                } else {
                    pickIT.getPath(clipData.getItemAt(0).uri, Build.VERSION.SDK_INT)
                }
            } else {
                pickIT.getPath(data.data, Build.VERSION.SDK_INT)
            }
        }
    }

    fun informNativeCodeAboutNewUploadCaption(message: String) {
        mUploadHelper.informNativeCodeAboutNewUploadCaption(message)
    }

    fun talkToUploadService(message: String) {
        mUploadHelper.talkToUploadService(message)
    }

    val activeUploads: Unit
        get() {
            mUploadHelper.getActiveUploads()
        }

    fun resumeActualUpload(command: String) {
        mUploadHelper.resumeActualUpload(command)
    }

    private fun resumeAfterPermissionAcquired() {
        try {
            var filter = "*/*"
            when (uriFilter) {
                "*" -> filter = "*/*"
                "gallery" -> filter = "image/*|video/*"
                "inApp" -> filter = "image/*|video/*"
                else -> {}
            }
            val pm: PackageManager = mMainActivity.applicationContext.packageManager
            val intents: MutableList<Intent> = ArrayList()
            val galleryIntent: Intent = if (filter == "*/*") Intent(Intent.ACTION_GET_CONTENT) else Intent(Intent.ACTION_PICK)
            galleryIntent.type = filter
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            for (ri in pm.queryIntentActivities(galleryIntent, PackageManager.MATCH_DEFAULT_ONLY)) {
                val packageName = ri.activityInfo.packageName
                val intent = Intent(galleryIntent)
                intent.component = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
                intent.setPackage(packageName)
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intents.add(intent)
            }
            val chooser = Intent.createChooser(galleryIntent, "Select File")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            mMainActivity.startActivityForResult(chooser, MainActivity.NATIVE_FILE_CHOOSER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelAfterPermissionDenied() {
        Toast.makeText(
            mMainActivity.applicationContext, "Upload process canceled! ",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun PickiTonUriReturned() {}
    override fun PickiTonStartListener() {}
    override fun PickiTonProgressUpdate(progress: Int) {}
    override fun PickiTonCompleteListener(
        path: String,
        wasDriveFile: Boolean,
        wasUnknownProvider: Boolean,
        wasSuccessful: Boolean,
        Reason: String?
    ) {
        val r: MutableList<String> = ArrayList()
        r.add(path)
        val newUploadRequests: Array<String> =
            mUploadHelper.requestNewUpload(r.toTypedArray(), mTo, mReplyToInfo)
        for (newUploadRequest in newUploadRequests) {
            mJavaScriptInterface.orderUiToProvideCaptionForNewUpload(newUploadRequest)
        }
    }

    override fun PickiTonMultipleCompleteListener(
        paths: ArrayList<String?>,
        wasSuccessful: Boolean,
        Reason: String?
    ) {
        val newUploadRequests: Array<String> =
            paths.toTypedArray().let { mUploadHelper.requestNewUpload(it, mTo, mReplyToInfo) }
        for (newUploadRequest in newUploadRequests) {
            mJavaScriptInterface.orderUiToProvideCaptionForNewUpload(newUploadRequest)
        }
    }
}