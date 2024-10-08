package com.today.connect.ui.activity

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import com.today.connect.CacheManager
import com.today.connect.ChromeClient
import com.today.connect.DownloadManager
import com.today.connect.FirebaseMessaging
import com.today.connect.GeolocationManager
import com.today.connect.JavaScriptInterface
import com.today.connect.R
import com.today.connect.UploadManager
import com.today.connect.WebViewManager
import com.today.connect.data.uploader.NativeUploadManager
import com.today.connect.notification.MyObject
import com.today.connect.state.GlobalState
import com.today.connect.utils.BetterActivityResult
import com.today.connect.utils.TokenParser
import java.lang.reflect.Method
import java.util.Locale


class MainActivity : AppCompatActivity() {

    companion object {
        const val DOWNLOAD_PERMISSION_REQUEST_CODE = 1
        const val UPLOAD_PERMISSION_REQUEST_CODE = 2
        const val WEB_VIEW_PERMISSION_REQUEST_CODE = 3
        const val POST_NOTIFICATION_PERMISSION_REQUEST_CODE = 4
        const val GEOLOCATION_PERMISSION_REQUEST_CODE = 5
        const val STATE_NONE = 0
        const val STATE_LOADED = 1
        const val STATE_UPDATING = 2
        const val STATE_UPDATE_FAILED = 3
        const val CHOOSE_FILE_REQUEST = 1
        const val HANGUP_CALL = 2
        const val NATIVE_FILE_CHOOSER = 3
    }

    private var mState: Int = STATE_NONE
    private lateinit var mWebViewManager: WebViewManager
    private lateinit var mChromeClient: ChromeClient
    private lateinit var mCacheManager: CacheManager
    private lateinit var mDownloadManager: DownloadManager
    private lateinit var mUploadManager: UploadManager
    private lateinit var mJavaScriptInterface: JavaScriptInterface
    private lateinit var mNativeUploadManager: NativeUploadManager

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalState.init(this)
//        GlobalState.preferredLang = "en"
        setContentView(R.layout.activity_main)
        val webView: WebView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        Log.d("Token :", GlobalState.accessToken!!)

        webViewRepair(
            webView,
            "https://todayconnect.yekaan.ir?username=${TokenParser(GlobalState.accessToken!!).parseTokenGetUsername()}&token=${GlobalState.accessToken}",
            "https://todayconnect.yekaan.ir?username=${TokenParser(GlobalState.accessToken!!).parseTokenGetUsername()}&token=${GlobalState.accessToken}"
        )
    }

    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(this)) {
            Handler().postDelayed({
                Toast.makeText(this, "لطفا دسترسی های مورد نیاز را تایید کنید", Toast.LENGTH_LONG)
                    .show()
            }, 2000)

            Handler().postDelayed({
                if (!Settings.canDrawOverlays(this)) {
                    // Request the permission
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, 85)
                }
            }, 3000)
        } else {
            if (!isShowOnLockScreenPermissionEnable()) {
                Handler().postDelayed({
                    if (Build.MANUFACTURER.equals("Xiaomi", true)) {
                        if (Locale.getDefault().language == "fe") {
                            Toast.makeText(
                                this,
                                "سایز مجور ها -> نمایش بر روی صفحه قفل -> اجازه دادن",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Other permissions -> Show on Lock screen -> Allow",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                        intent.setClassName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.permissions.PermissionsEditorActivity"
                        )
                        intent.putExtra("extra_pkgname", packageName)
                        startActivity(intent)
                    }
                }, 2000)
            }
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun isShowOnLockScreenPermissionEnable(): Boolean {
        return try {
            val manager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val method: Method = AppOpsManager::class.java.getDeclaredMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result =
                method.invoke(manager, 10020, Binder.getCallingUid(), packageName) as Int
            AppOpsManager.MODE_ALLOWED == result
        } catch (e: Exception) {
            false
        }
    }

    override fun onBackPressed() {
        if (!mWebViewManager.goBack()) super.onBackPressed()
    }

    private fun webViewRepair(
        webView: WebView,
        homeUrl: String,
        downloadUrl: String
    ) {
        val mPermissionManager = PermissionManager(this)
        mDownloadManager = DownloadManager(this, mPermissionManager)
        mUploadManager = UploadManager(this, mPermissionManager)
        val mGeoLocationManager = GeolocationManager(this, mPermissionManager)
        mCacheManager = CacheManager(this)
        val mFirebaseMessaging = FirebaseMessaging()
        // Initialize the VideoEnabledWebChromeClient and set event handlers
        val nonVideoLayout =
            findViewById<View>(R.id.nonVideoLayout) // Your own view, read class comments
        val videoLayout =
            findViewById<ViewGroup>(R.id.videoLayout) // Your own view, read class comments
        val loadingView: View = layoutInflater.inflate(
            R.layout.view_loading_video,
            null
        ) // Your own view, read class comments
        mChromeClient = ChromeClient(
            this,
            mPermissionManager,
            mUploadManager,
            mGeoLocationManager,
            nonVideoLayout,
            videoLayout,
            loadingView,
            webView
        )
        mChromeClient.setOnToggledFullscreen(object :
            ChromeClient.Companion.ToggledFullscreenCallback {
            override fun toggledFullscreen(fullscreen: Boolean) {
                val attrs = window.attributes
                if (fullscreen) {
                    attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
                    attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    window.attributes = attrs
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE
                } else {
                    attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
                    attrs.flags =
                        attrs.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
                    window.attributes = attrs
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        })
        mWebViewManager = WebViewManager(
            this, webView, mChromeClient,
            homeUrl,
            downloadUrl,
            mCacheManager, mDownloadManager
        )
        mJavaScriptInterface = JavaScriptInterface(
            this,
            mPermissionManager,
            mWebViewManager,
            mFirebaseMessaging,
            mCacheManager
        )
        mWebViewManager.addJavascriptInterface(mJavaScriptInterface, "YekaanInterface")
        handleCallIntent()
        mJavaScriptInterface.setIntent(processIntent(intent))
        mNativeUploadManager = NativeUploadManager(this, mPermissionManager, mJavaScriptInterface)
        setUiHandlers()
        mWebViewManager.load()
    }

    private fun setUiHandlers() {
        val clickButton = findViewById<Button>(R.id.button)
        clickButton.setOnClickListener { mWebViewManager.load() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCallIntent()
        mJavaScriptInterface.setIntent(processIntent(intent))
    }

    private fun handleCallIntent() {
        MyObject.ring?.runnable?.invoke()
        intent.getStringExtra("URL")?.let { url ->
            val displayName = GlobalState.displayName!!
            val pName = GlobalState.username!!
            val tUrlString = url.replace("_displayName", displayName).replace("_pName", pName)
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tUrlString))
            startActivity(browserIntent)
        }
    }

    fun signalWebViewLoaded() {
        if (mState == STATE_UPDATE_FAILED) return
        mState = STATE_LOADED
        findViewById<View>(R.id.LOGO).visibility = View.INVISIBLE
        findViewById<View>(R.id.textView).visibility = View.INVISIBLE
        findViewById<View>(R.id.button).visibility = View.INVISIBLE
        mWebViewManager.show()
    }

    fun signalWebViewLoading() {
        mState = STATE_UPDATING
        if (!mCacheManager.isAppCached) {
            (findViewById<View>(R.id.textView) as TextView).setText(R.string.label_text_updating)
        }
        findViewById<View>(R.id.LOGO).visibility = View.VISIBLE
        findViewById<View>(R.id.textView).visibility = View.VISIBLE
        findViewById<View>(R.id.button).visibility = View.INVISIBLE
        mWebViewManager.hide()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CHOOSE_FILE_REQUEST -> mUploadManager.signalActivityResulted(
                resultCode,
                data
            )
            //ToDo: HANGUP_CALL -> mJavaScriptInterface.hangCall()
            NATIVE_FILE_CHOOSER -> mNativeUploadManager.signalActivityResulted(
                resultCode,
                data
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var granted = true
        for (grantResult in grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                granted = false
                break
            }
        }
        when (requestCode) {
            DOWNLOAD_PERMISSION_REQUEST_CODE -> if (granted) {
                mDownloadManager.resumeAfterPermissionAcquired()
            } else {
                mDownloadManager.cancelAfterPermissionDenied()
            }

            UPLOAD_PERMISSION_REQUEST_CODE -> if (granted) {
                mUploadManager.resumeAfterPermissionAcquired()
            } else {
                mUploadManager.cancelAfterPermissionDenied()
            }

            WEB_VIEW_PERMISSION_REQUEST_CODE -> if (granted) {
                mChromeClient.resumeAfterPermissionAcquired()
            } else {
                mChromeClient.cancelAfterPermissionDenied()
            }
        }
    }

    private fun processIntent(intent: Intent): String? {
        val bundle = intent.extras
        if (bundle != null) {
            val keys = bundle.keySet()
            val it: Iterator<String> = keys.iterator()
            Log.e("IntentLog", "Dumping Intent start")
            while (it.hasNext()) {
                val key = it.next()
                Log.e("IntentLog", "[" + key + "=" + bundle.getString(key) + "]")
            }
            Log.e("IntentLog", "Dumping Intent end")
        }
        val action = intent.action
        val type = intent.type
        var strIntent: String? = ""
        try {
            if (intent.hasExtra("intent")) {
                strIntent = intent.extras?.getString("intent")
            } else if (Intent.ACTION_VIEW == action) {
                strIntent = "redirect=" + intent.dataString
            } else if (Intent.ACTION_SEND == action && type != null) {
                if ("text/plain" == type) {
                    strIntent = "sendText=" + intent.getStringExtra(Intent.EXTRA_TEXT)
                } else {
                    val uri = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    if (uri != null) {
                        strIntent = "uploadFile"
                        mUploadManager.setFiles(arrayOf(uri))
                    }
                }
            } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
                val uris = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (uris != null && uris.isNotEmpty()) {
                    strIntent = "uploadFile"
                    mUploadManager.setFiles(uris.toTypedArray())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strIntent
    }

    fun signalWebViewFailed() {
        if (mState != STATE_UPDATING) return
        mState = STATE_UPDATE_FAILED
        (findViewById<View>(R.id.textView) as TextView).setText(R.string.label_text_update_failed)
        findViewById<View>(R.id.button).visibility = View.VISIBLE
    }

    fun activityLauncher(): BetterActivityResult<Intent, ActivityResult> {
        return BetterActivityResult.registerActivityForResult(this)
    }

    fun openNativeFileChooser(
        to: String,
        mediaType: String,
        replyToInfo: String
    ) {
        mNativeUploadManager.showFileChooser(to, mediaType, replyToInfo)
    }

    fun informNativeCodeAboutNewUploadCaption(
        message: String
    ) {
        mNativeUploadManager.informNativeCodeAboutNewUploadCaption(message)
    }

    fun talkToUploadService(
        message: String
    ) {
        mNativeUploadManager.talkToUploadService(message)
    }

    fun getActiveUploads() {
        mNativeUploadManager.activeUploads
    }

    fun commandUIToPrepareMediaMessageAndMakeTempID(
        message: String
    ) {
        mJavaScriptInterface.commandUIToPrepareMediaMessageAndMakeTempID(message)
    }

    fun informUIAboutUploadEvents(
        message: String
    ) {
        mJavaScriptInterface.informUIAboutUploadEvents(message)
    }

    fun resumeActualUpload(
        command: String
    ) {
        mNativeUploadManager.resumeActualUpload(command)
    }

    var handler: Handler = object : Handler(Looper.getMainLooper()) {
        // handle service messages
        override fun handleMessage(msg: Message) {
            mJavaScriptInterface.handleMessage(msg)
        }
    }

    fun setStatusBarColor(statusColor: Int) {
        runOnUiThread { window.statusBarColor = statusColor }
    }

    fun setNavigationColor(navigationColor: Int) {
        runOnUiThread { window.navigationBarColor = navigationColor }
    }

    fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = this.resources
        val config = resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            config.locale = locale
        }

        resources.updateConfiguration(config, resources.displayMetrics)
    }
}