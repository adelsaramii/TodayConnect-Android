package ir.yekaan.darkoobnext

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView


internal class WebViewManager(
    activity: MainActivity, webView: WebView, chromeClient: ChromeClient,
    homeUrl: String, downloadUrl: String, cacheManager: CacheManager,
    downloadManager: DownloadManager
) {
    private val mActivity: MainActivity
    private val mWebView: WebView
    private val mChromeClient: ChromeClient
    private val mDownloadManager: DownloadManager
    private val mCacheManager: CacheManager
    private val mHomeUrl: String
    private val mDownloadUrl: String
    private var mBackCallback: String

    init {
        mActivity = activity
        mWebView = webView
        mChromeClient = chromeClient
        mHomeUrl = homeUrl
        mDownloadUrl = downloadUrl
        mDownloadManager = downloadManager
        mCacheManager = cacheManager
        mBackCallback = ""
        init()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun init() {
        mWebView.webViewClient =
            WebClient(mActivity, mHomeUrl, mDownloadUrl, mCacheManager, mDownloadManager)
        mWebView.webChromeClient = mChromeClient
        mWebView.setDownloadListener(mDownloadManager)
        val webSettings = mWebView.settings
        mWebView.setInitialScale(0)
        mWebView.isVerticalScrollBarEnabled = false
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.allowContentAccess = true

        mWebView.requestFocusFromTouch()
        mWebView.setBackgroundColor(0)
        mWebView.setBackgroundResource(0)
        mWebView.overScrollMode = View.OVER_SCROLL_NEVER
        mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webSettings.databaseEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.setGeolocationEnabled(true)
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        val userAgent = webSettings.userAgentString
        webSettings.userAgentString = "$userAgent YekaanAndroid/1.0"
        val appInfo: ApplicationInfo = mActivity.applicationInfo
        if (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    @SuppressLint("JavascriptInterface")
    fun addJavascriptInterface(obj: Any?, interfaceName: String?) {
        mWebView.addJavascriptInterface(obj!!, interfaceName!!)
    }

    fun load() {
        mActivity.signalWebViewLoading()
        mWebView.loadUrl(mHomeUrl + "index.html")
    }

    fun show() {
        mWebView.visibility = View.VISIBLE
    }

    fun hide() {
        mWebView.visibility = View.INVISIBLE
    }

    fun runScriptNoReturn(script: String?) {
        mActivity.runOnUiThread(Runnable { mWebView.evaluateJavascript(script!!, null) })
    }

    fun pause() {
        mWebView.onPause()
    }

    fun resume() {
        mWebView.onResume()
    }

    fun onHideCustomView() {
        mChromeClient.onHideCustomView()
    }

    fun setBackCallBack(method: String) {
        mBackCallback = method
    }

    fun goBack(): Boolean {
        if (mBackCallback != "") {
            mWebView.evaluateJavascript(
                "$mBackCallback();"
            ) { s ->
                if (!s.contains("success")) {
                    val homeIntent = Intent(Intent.ACTION_MAIN)
                    homeIntent.addCategory(Intent.CATEGORY_HOME)
                    homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    mActivity.startActivity(homeIntent)
                }
            }
            return true
        }
        return false
    }
}