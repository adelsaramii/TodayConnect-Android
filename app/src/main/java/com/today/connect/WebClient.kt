package com.today.connect

import android.net.http.SslError
import android.util.Log
import android.webkit.*
import com.today.connect.helpers.CustomTabActivityHelper
import java.util.*


class WebClient internal constructor(
    activity: MainActivity,
    homeUrl: String,
    downloadUrl: String,
    cacheManager: CacheManager,
    downloadManager: DownloadManager
) :
    WebViewClient() {
    private val mMainActivity: MainActivity
    private val mCacheManager: CacheManager
    private val mHomeUrl: String
    private val mDownloadUrl: String
    private val mDownloadManager: DownloadManager
    override fun onPageFinished(view: WebView, url: String) {
        mMainActivity.signalWebViewLoaded()
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val response: WebResourceResponse? = mCacheManager.tryHandle(request)
        return response ?: super.shouldInterceptRequest(view, request)
        //return response;
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (url.lowercase(Locale.getDefault())
                .startsWith(mDownloadUrl) && !url.contains("forceOnBrowser")
        ) {
            if (url.contains("-YF-") && (url.contains("?download") || url.contains("&download"))) {
                var contentDisposition = url.substring(url.indexOf("-YF-") + 4)
                if (contentDisposition.contains("?")) {
                    contentDisposition =
                        contentDisposition.substring(0, contentDisposition.indexOf("?"))
                }
                contentDisposition = "attachment; filename=\"$contentDisposition\""
                mDownloadManager.onDownloadStart(url, "", contentDisposition, "", 0)
            } else {
                view.loadUrl(url)
            }
        } else if (url.lowercase(Locale.getDefault())
                .startsWith(mHomeUrl) && !url.contains("forceOnBrowser")
        ) {
            mMainActivity.signalWebViewLoading()
            view.loadUrl(url)
        } else {
            val helper = CustomTabActivityHelper()
            helper.openCustomTab(mMainActivity, url)
        }
        return true
    }

    override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String,
        failingUrl: String
    ) {
        if (mCacheManager.isAppCached) return
        Log.e("WEB_CLIENT_ERROR", description)
        if (errorCode == -11) return  //net::ERR_SSL_PROTOCOL_ERROR
        mMainActivity.signalWebViewFailed()
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        if (mCacheManager.isAppCached) return
        Log.e("WEB_CLIENT_ERROR", error.description.toString())
        if (error.errorCode == -11) return  //net::ERR_SSL_PROTOCOL_ERROR
        mMainActivity.signalWebViewFailed()
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.proceed()
    }

    init {
        mMainActivity = activity
        mCacheManager = cacheManager
        mHomeUrl = homeUrl
        mDownloadUrl = downloadUrl
        mDownloadManager = downloadManager
    }
}