package com.today.connect

import android.webkit.GeolocationPermissions
import android.widget.Toast

class GeolocationManager internal constructor(
    activity: MainActivity,
    permissionManager: PermissionManager
) {
    private val mActivity: MainActivity
    private val mPermissionManager: PermissionManager
    private var mValueCallback: GeolocationPermissions.Callback? = null
    private var mOrigin: String? = null

    init {
        mActivity = activity
        mPermissionManager = permissionManager
    }

    fun checkPermissions(origin: String?, callback: GeolocationPermissions.Callback?) {
        mValueCallback = callback
        mOrigin = origin
        val res = mPermissionManager.requestGeolocationPermission()
        if (res == PermissionManager.PERMISSION_REQUEST_DENIED) {
            cancelAfterPermissionDenied()
        } else if (res == PermissionManager.PERMISSION_REQUEST_GRANTED || res == PermissionManager.PERMISSION_REQUEST_PENDING) {
            resumeAfterPermissionAcquired()
        }
    }

    private fun resumeAfterPermissionAcquired() {
        mValueCallback!!.invoke(mOrigin, true, false)
    }

    private fun cancelAfterPermissionDenied() {
        Toast.makeText(
            mActivity, "Denied : ",
            Toast.LENGTH_LONG
        ).show()
        mValueCallback!!.invoke(mOrigin, false, false)
    }
}