package com.today.connect

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.webkit.JavascriptInterface
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.today.connect.ui.activity.MainActivity
import com.today.connect.ui.activity.PermissionManager
import com.today.connect.state.GlobalState
import com.today.connect.utils.FileUtils
import java.net.URI
import java.net.URL
import java.util.LinkedList
import java.util.Locale
import java.util.Queue


class JavaScriptInterface internal constructor(
    activity: MainActivity, mPermissionManager: PermissionManager, webViewManager: WebViewManager,
    mFirebaseMessaging: FirebaseMessaging,
    cacheManager: CacheManager
) {
    private val mMainActivity: MainActivity
    private val mWebViewManager: WebViewManager
    private val mCacheManager: CacheManager
    private var mOnFirebaseNewTokenCallBack: String? = null
    private var mOnNewIntentCallBack: String? = null
    private var mIntent: String? = null
    private var mFirebaseToken: String? = null
    private val firebaseMessaging: FirebaseMessaging
    private val permissionManager: PermissionManager
    private val intentCallbackQueue: Queue<String>
    private var setCaptionForNewNativeUploadMethod: String? = null
    private var mPrepareMediaMessageAndMakeTempIDCallBack: String? = null
    private var mInformUIAboutUploadEventsCallBack: String? = null
    private var downloadStatusCallBack: String? = null
    private var serviceRunID = 0

    init {
        mMainActivity = activity
        mCacheManager = cacheManager
        mWebViewManager = webViewManager
        intentCallbackQueue = LinkedList()
        inspectingFiles = HashMap()
        firebaseMessaging = mFirebaseMessaging
        permissionManager = mPermissionManager
    }

    @JavascriptInterface
    fun openMediaSelectionDialog(request: String?) {
        val jsonParser = JsonParser()
        val jo = jsonParser.parse(request) as JsonObject
        mMainActivity.openNativeFileChooser(
            jo["to"].asString,
            jo["mediaType"].asString,
            jo["replyToInfo"].asString
        )
    }

    @JavascriptInterface
    fun deleteAppCache() {
        mCacheManager.deleteAppFiles()
    }

    @JavascriptInterface
    fun deleteAssetCache() {
        mCacheManager.deleteAssetFiles()
    }

    @JavascriptInterface
    fun setStatusBarColor(statusColor: String?) {
        mMainActivity.setStatusBarColor(Color.parseColor(statusColor))
    }

    @JavascriptInterface
    fun setPreferredLang(preferredLang: String) {
        GlobalState.preferredLang = preferredLang.lowercase(Locale.ENGLISH)
        mMainActivity.setLocale(preferredLang.lowercase(Locale.ENGLISH))
    }

    @JavascriptInterface
    fun setUsername(username: String) {
        GlobalState.username = username
    }

    @JavascriptInterface
    fun setDisplayName(displayName: String) {
        GlobalState.displayName = displayName
    }

    @JavascriptInterface
    fun setNavigationColor(navigationColor: String?) {
        mMainActivity.setNavigationColor(Color.parseColor(navigationColor))
    }

    @JavascriptInterface
    fun resetStatusBarColor() {
        mMainActivity.setStatusBarColor(
            mMainActivity.getResources().getColor(R.color.white)
        )
    }

    @JavascriptInterface
    fun setFirebaseOnNewTokenCallBack(method: String?) {
        mOnFirebaseNewTokenCallBack = method
        fireNewFirebaseTokenCallBack()
    }

    @JavascriptInterface
    fun requestAndroidNotification() {
        firebaseMessaging.init(this, permissionManager)
    }

    @JavascriptInterface
    fun setBackCallBack(method: String?) {
        mWebViewManager.setBackCallBack(method!!)
    }

    @JavascriptInterface
    fun setNewIntentCallBack(method: String?) {
        mOnNewIntentCallBack = method
        clearJSIntentQueue()
    }

    @JavascriptInterface
    fun setCaptionForNewNativeUploadCallBack(method: String) {
        setCaptionForNewNativeUploadMethod = method
    }

    @JavascriptInterface
    fun setInformUIAboutUploadEventsCallBack(method: String) {
        mInformUIAboutUploadEventsCallBack = method
    }

    @JavascriptInterface
    fun informNativeCodeAboutNewUploadCaption(message: String?) {
        mMainActivity.informNativeCodeAboutNewUploadCaption(message!!)
    }

    @JavascriptInterface
    fun talkToUploadService(message: String?) {
        mMainActivity.talkToUploadService(message!!)
    }

    @JavascriptInterface
    fun getActiveUploads() {
        mMainActivity.getActiveUploads()
    }

    @JavascriptInterface
    fun setPrepareMediaMessageAndMakeTempIDCallBack(method: String) {
        mPrepareMediaMessageAndMakeTempIDCallBack = method
    }

    @JavascriptInterface
    fun getAndroidVersionCallback(method: String) {
        try {
            val pInfo: PackageInfo =
                mMainActivity.packageManager.getPackageInfo(mMainActivity.packageName, 0)
            val version = "" + pInfo.versionCode
            mWebViewManager.runScriptNoReturn("$method($version)")
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }
    
    @JavascriptInterface
    fun contactLoadedCallBack(contact: String?) {
        firebaseMessaging.cancelContactNotifications(
            mMainActivity.applicationContext,
            contact!!
        )
    }

    @JavascriptInterface
    fun listFiles(): String {
        return mMainActivity.filesDir.list()?.joinToString(separator = "\n") ?: ""
    }

    @JavascriptInterface
    fun deleteFirebaseToken() {
        firebaseMessaging.deleteToken()
    }

    @JavascriptInterface
    fun setDownloadStatusCallBack(method: String) {
        downloadStatusCallBack = method
    }

    @JavascriptInterface
    fun setInspectionUrlDownload(mixed: String) {
        val array = mixed.split("\\\$D@RK00B\\$".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (array.size == 2) {
            val id = array[0].toInt()
            var u = array[1]
            try {
                val url = URL(u)
                val uri = URI(
                    url.protocol,
                    url.userInfo,
                    url.host,
                    url.port,
                    url.path,
                    url.query,
                    url.ref
                )
                u = uri.toASCIIString()
            } catch (e: java.lang.Exception) {
                Log.e("cannot Encode URL", e.toString())
            }
            inspectingFiles[u] = id
            // 1- check with disk manager for downloaded or not
            try {
                val fu = FileUtils(mMainActivity, u, null, null)
                fu.generateFilePathFromURL()
                if (fu.checkFileExists()) {
                    mWebViewManager.runScriptNoReturn("$downloadStatusCallBack($id, 1)")
                } else {
                    mWebViewManager.runScriptNoReturn("$downloadStatusCallBack($id, 3)")
                }
                if (fu.checkIfDownloading(serviceRunID)) {
                    mWebViewManager.runScriptNoReturn("$downloadStatusCallBack($id, 2)")
                }
            } catch (e: java.lang.Exception) {
                Log.e("BAD URL!!!!", e.toString())
            }
        }
    }

    @JavascriptInterface
    fun notifyVideoEnd() // Must match Javascript interface method of VideoEnabledWebChromeClient
    {
        Log.d("___", "GOT IT")
        // This code is not executed in the UI thread, so we must force that to happen
        Handler(Looper.getMainLooper()).post { mWebViewManager.onHideCustomView() }
    }

    fun setIntent(intent: String?) {
        if(intent != null && intent.indexOf("redirect=msgapp://") > -1){
            val url = intent.replace("redirect=msgapp://", "https://")
            mWebViewManager.runScriptNoReturn("window.location.replace('$url')")
            return
        }
        mIntent = intent
        fireNewIntentCallBack()
    }

    fun setToken(token: String?) {
        mFirebaseToken = token
        fireNewFirebaseTokenCallBack()
    }

    private fun fireNewFirebaseTokenCallBack() {
        Log.e("mohsen", "fuckkkkkkkkkkk" )
        if (mOnFirebaseNewTokenCallBack == null || mFirebaseToken == null) return
        try {
            mWebViewManager.runScriptNoReturn("$mOnFirebaseNewTokenCallBack('$mFirebaseToken')")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchJSIntentCallback(mIntent: String) {
        try {
            mWebViewManager.runScriptNoReturn("$mOnNewIntentCallBack('$mIntent')")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fireNewIntentCallBack() {
        if (mIntent == null) return
        if (mOnNewIntentCallBack == null) {
            intentCallbackQueue.add(mIntent)
            return
        }
        launchJSIntentCallback(mIntent!!)
    }

    private fun clearJSIntentQueue() {
        for (iteratorValue in intentCallbackQueue) {
            launchJSIntentCallback(iteratorValue)
        }
        intentCallbackQueue.clear()
    }

    @JavascriptInterface
    fun resumeActualUpload(message: String?) {
        mMainActivity.resumeActualUpload(message!!)
    }

    companion object {
        lateinit var inspectingFiles: HashMap<String, Int>
    }

    fun orderUiToProvideCaptionForNewUpload(message: String) {
        try {
            mWebViewManager.runScriptNoReturn(
                "$setCaptionForNewNativeUploadMethod('" + message.replace(
                    "[\\n\t ]".toRegex(),
                    " "
                ) + "')"
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun commandUIToPrepareMediaMessageAndMakeTempID(message: String) {
        try {
            mWebViewManager.runScriptNoReturn(
                "$mPrepareMediaMessageAndMakeTempIDCallBack('" + message.replace(
                    "[\\n\t ]".toRegex(),
                    " "
                ) + "')"
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun informUIAboutUploadEvents(message: String) {
        try {
            mWebViewManager.runScriptNoReturn(
                "$mInformUIAboutUploadEventsCallBack('" + message.replace(
                    "[\\n\t ]".toRegex(),
                    " "
                ) + "')"
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun handleMessage(msg: Message) {
        val mixed = msg.obj as String
        if (mixed.contains("currentRunID")) {
            val array = mixed.split("\\\$D@RK00B\\$".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (array.size == 2) {
                serviceRunID = array[0].toInt()
            }
        }
        val array = mixed.split("\\\$D@RK00B\\$".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        if (array.size == 2) {
            val u = array[0].split("\\?".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            var url = array[0]
            if (u.size == 2) url = u[0]
            val Status = array[1]
            val id = inspectingFiles[url]
            if (id != null) {
                mWebViewManager.runScriptNoReturn("$downloadStatusCallBack($id, $Status)")
            }
        }
    }
}
