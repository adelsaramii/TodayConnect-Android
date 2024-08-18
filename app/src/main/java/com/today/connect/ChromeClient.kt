package com.today.connect

import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.VideoView
import com.today.connect.ui.activity.MainActivity
import com.today.connect.ui.activity.PermissionManager


class ChromeClient internal constructor(
    activity: MainActivity,
    permissionManager: PermissionManager,
    uploadManager: UploadManager,
    geolocationManager: GeolocationManager,
    activityNonVideoView: View,
    activityVideoView: ViewGroup,
    loadingView: View?,
    webView: WebView?
) : WebChromeClient(),
    OnPreparedListener, OnCompletionListener, MediaPlayer.OnErrorListener {

    companion object {
        interface ToggledFullscreenCallback {
            fun toggledFullscreen(fullscreen: Boolean)
        }
    }

    private val mActivity: MainActivity
    private val mUploadManager: UploadManager
    private val mPermissionManager: PermissionManager
    private val mGeolocationManager: GeolocationManager
    private var mPermissionRequest: PermissionRequest? = null
    private val activityNonVideoView: View
    private val activityVideoView: ViewGroup
    private val loadingView: View?
    private val webView: WebView?

    /**
     * Indicates if the video is being displayed using a custom view (typically full-screen)
     * @return true it the video is being displayed using a custom view (typically full-screen)
     */
    var isVideoFullscreen // Indicates if the video is being displayed using a custom view (typically full-screen)
            : Boolean
        private set
    private var videoViewContainer: FrameLayout? = null
    private var videoViewCallback: CustomViewCallback? = null
    private var toggledFullscreenCallback: ToggledFullscreenCallback? = null
    override fun onPermissionRequest(request: PermissionRequest) {
        mActivity.runOnUiThread(Runnable {
            mPermissionRequest = request
            val res = mPermissionManager.requestWebViewPermissions()
            if (res == PermissionManager.PERMISSION_REQUEST_DENIED) {
                cancelAfterPermissionDenied()
            } else if (res == PermissionManager.PERMISSION_REQUEST_GRANTED) {
                resumeAfterPermissionAcquired()
            }
        })
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        mUploadManager.showFileChooser(filePathCallback, fileChooserParams)
        return true
    }

    fun resumeAfterPermissionAcquired() {
        if (mPermissionRequest == null) return
        mPermissionRequest!!.grant(mPermissionRequest!!.resources)
        mPermissionRequest = null
    }

    fun cancelAfterPermissionDenied() {
        mPermissionRequest = null
    }

    /**
     * Set a callback that will be fired when the video starts or finishes displaying using a custom view (typically full-screen)
     * @param callback A VideoEnabledWebChromeClient.ToggledFullscreenCallback callback
     */
    fun setOnToggledFullscreen(callback: ToggledFullscreenCallback?) {
        toggledFullscreenCallback = callback
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (view is FrameLayout) {
            // A video wants to be shown
            val frameLayout = view
            val focusedChild = frameLayout.focusedChild

            // Save video related variables
            isVideoFullscreen = true
            videoViewContainer = frameLayout
            videoViewCallback = callback

            // Hide the non-video view, add the video view, and show it
            activityNonVideoView.visibility = View.INVISIBLE
            activityVideoView.addView(
                videoViewContainer,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            activityVideoView.visibility = View.VISIBLE
            if (focusedChild is VideoView) {
                // android.widget.VideoView (typically API level <11)
                val videoView = focusedChild

                // Handle all the required events
                videoView.setOnPreparedListener(this)
                videoView.setOnCompletionListener(this)
                videoView.setOnErrorListener(this)
            } else {
                // Other classes, including:
                // - android.webkit.HTML5VideoFullScreen$VideoSurfaceView, which inherits from android.view.SurfaceView (typically API level 11-18)
                // - android.webkit.HTML5VideoFullScreen$VideoTextureView, which inherits from android.view.TextureView (typically API level 11-18)
                // - com.android.org.chromium.content.browser.ContentVideoView$VideoSurfaceView, which inherits from android.view.SurfaceView (typically API level 19+)

                // Handle HTML5 video ended event only if the class is a SurfaceView
                // Test case: TextureView of Sony Xperia T API level 16 doesn't work fullscreen when loading the javascript below
                if (webView != null && webView.settings.javaScriptEnabled && focusedChild is SurfaceView) {
                    // Run javascript code that detects the video end and notifies the Javascript interface
                    var js = "javascript:"
                    js += "var _ytrp_html5_video_last;"
                    js += "var _ytrp_html5_video = document.getElementsByTagName('video')[0];"
                    js += "if (_ytrp_html5_video != undefined && _ytrp_html5_video != _ytrp_html5_video_last) {"
                    run {
                        js += "_ytrp_html5_video_last = _ytrp_html5_video;"
                        js += "function _ytrp_html5_video_ended() {"
                        {
                            js += "_VideoEnabledWebView.notifyVideoEnd();" // Must match Javascript interface name and method of VideoEnableWebView
                        }
                        js += "}"
                        js += "_ytrp_html5_video.addEventListener('ended', _ytrp_html5_video_ended);"
                    }
                    js += "}"
                    webView.loadUrl(js)
                }
            }

            // Notify full-screen change
            if (toggledFullscreenCallback != null) {
                toggledFullscreenCallback!!.toggledFullscreen(true)
            }
        }
    }


    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        mGeolocationManager.checkPermissions(origin, callback)
    }

    override fun onShowCustomView(
        view: View,
        requestedOrientation: Int,
        callback: CustomViewCallback
    ) // Available in API level 14+, deprecated in API level 18+
    {
        onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        // This method should be manually called on video end in all cases because it's not always called automatically.
        // This method must be manually called on back key press (from this class' onBackPressed() method).
        if (isVideoFullscreen) {
            // Hide the video view, remove it, and show the non-video view
            activityVideoView.visibility = View.INVISIBLE
            activityVideoView.removeView(videoViewContainer)
            activityNonVideoView.visibility = View.VISIBLE

            // Call back (only in API level <19, because in API level 19+ with chromium webview it crashes)
            if (videoViewCallback != null && !videoViewCallback!!.javaClass.name.contains(".chromium.")) {
                videoViewCallback!!.onCustomViewHidden()
            }

            // Reset video related variables
            isVideoFullscreen = false
            videoViewContainer = null
            videoViewCallback = null

            // Notify full-screen change
            if (toggledFullscreenCallback != null) {
                toggledFullscreenCallback!!.toggledFullscreen(false)
            }
        }
    }

    override fun getVideoLoadingProgressView(): View? // Video will start loading
    {
        return if (loadingView != null) {
            loadingView.visibility = View.VISIBLE
            loadingView
        } else {
            super.getVideoLoadingProgressView()
        }
    }

    override fun onPrepared(mp: MediaPlayer) // Video will start playing, only called in the case of android.widget.VideoView (typically API level <11)
    {
        if (loadingView != null) {
            loadingView.visibility = View.GONE
        }
    }

    override fun onCompletion(mp: MediaPlayer) // Video finished playing, only called in the case of android.widget.VideoView (typically API level <11)
    {
        onHideCustomView()
    }

    override fun onError(
        mp: MediaPlayer,
        what: Int,
        extra: Int
    ): Boolean // Error while playing video, only called in the case of android.widget.VideoView (typically API level <11)
    {
        return false // By returning false, onCompletion() will be called
    }

    /**
     * Notifies the class that the back key has been pressed by the user.
     * This must be called from the Activity's onBackPressed(), and if it returns false, the activity itself should handle it. Otherwise don't do anything.
     * @return Returns true if the event was handled, and false if was not (video view is not visible)
     */
    fun onBackPressed(): Boolean {
        return if (isVideoFullscreen) {
            onHideCustomView()
            true
        } else {
            false
        }
    }

    init {
        mActivity = activity
        mPermissionManager = permissionManager
        mUploadManager = uploadManager
        mGeolocationManager = geolocationManager
        this.activityNonVideoView = activityNonVideoView
        this.activityVideoView = activityVideoView
        this.loadingView = loadingView
        isVideoFullscreen = false
        this.webView = webView
    }
}