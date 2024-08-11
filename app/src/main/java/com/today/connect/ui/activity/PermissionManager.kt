package com.today.connect.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

class PermissionManager(private val mActivity: MainActivity) {
    companion object {
        val PERMISSION_REQUEST_GRANTED = 1
        val PERMISSION_REQUEST_DENIED = 2
        val PERMISSION_REQUEST_PENDING = 3
    }

    fun requestDownloadPermission(): Int {
        return requestPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            MainActivity.DOWNLOAD_PERMISSION_REQUEST_CODE
        )
    }

    fun requestUploadPermission(): Int {
        return requestPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            MainActivity.UPLOAD_PERMISSION_REQUEST_CODE
        )
    }

    fun requestUploadPermission13(): Int {
        return requestPermission(
            arrayOf<String>(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            ), MainActivity.UPLOAD_PERMISSION_REQUEST_CODE
        )
    }

    fun requestNotificationPermission13(): Int {
        return requestPermission(
            arrayOf<String>(
                Manifest.permission.POST_NOTIFICATIONS
            ), MainActivity.POST_NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    }

    fun requestGeolocationPermission(): Int {
        return requestPermission(
            arrayOf<String>(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), MainActivity.GEOLOCATION_PERMISSION_REQUEST_CODE
        )
    }


    fun requestWebViewPermissions(): Int {
        return requestPermission(
            arrayOf(
                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
            ),
            MainActivity.WEB_VIEW_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestPermission(permission: String, requestCode: Int): Int {
        return requestPermission(arrayOf(permission), requestCode)
    }

    private fun requestPermission(permissions: Array<String>, requestCode: Int): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return PERMISSION_REQUEST_GRANTED
        val requests = ArrayList<String>()
        for (permission in permissions) {
            if (mActivity?.checkSelfPermission(permission) === PackageManager.PERMISSION_GRANTED) continue

//            if (!mActivity.shouldShowRequestPermissionRationale(permission)) {
//                String msg = mActivity.getResources().getString(R.string.toast_permission_request_failed);
//                Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show();
//                return PERMISSION_REQUEST_DENIED;
//            }
            requests.add(permission)
        }
        if (!requests.isEmpty()) {
            mActivity?.requestPermissions(requests.toTypedArray(), requestCode)
            return PERMISSION_REQUEST_PENDING
        }
        return PERMISSION_REQUEST_GRANTED
    }
}