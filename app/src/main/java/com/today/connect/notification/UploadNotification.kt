package com.today.connect.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.today.connect.R
import com.today.connect.services.UploadService
import com.today.connect.utils.NotificationHelper


class UploadNotification {
    private var mService: UploadService? = null
    private var uploadId = 0
    private var mFileSize: Long = 0
    private var notificationManager: NotificationManager? = null
    private var mBuilder: NotificationCompat.Builder? = null
    private var fileName: String? = null
    private var n: Notification? = null
    private var mFileId: String? = null
    fun setService(service: UploadService?) {
        mService = service
    }

    fun setId(i: Int) {
        uploadId = i
    }

    fun setFileId(fileId: String?) {
        mFileId = fileId
    }

    fun setFileSize(fileSize: Long) {
        mFileSize = fileSize
    }

    fun setFileName(fileName: String?) {
        this.fileName = fileName
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun prepareNotification(messageTitle: String?, messageBody: String?) {
        if (!shouldShowNotification()) return
        mBuilder = mService?.let {
            NotificationCompat.Builder(
                it.applicationContext, "notify_00"
            )
        }
        notificationManager = mService
            ?.applicationContext
            ?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        val channelId = "notify_00"
        val channel = NotificationChannel(
            channelId,
            "Uploading...",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager!!.createNotificationChannel(channel)
        mBuilder!!.setChannelId(channelId)
        mBuilder!!.setSmallIcon(R.drawable.ic_baseline_cloud_upload_24)
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setOnlyAlertOnce(true).color = Color.parseColor("#3F5996")
        n = mBuilder!!.build()
    }

    private fun setCancelIntent() {
        if (!shouldShowNotification()) return
        val cancelIntent = Intent(mService, UploadService.UploadServiceReceiver::class.java)
        cancelIntent.action = "ACTION_CANCEL"
        cancelIntent.putExtra("fileId", mFileId)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            mService,
            uploadId,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        mBuilder!!.addAction(R.drawable.ic_baseline_close_24, "Cancel", cancelPendingIntent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendNotification(messageTitle: String?, messageBody: String?) {
        if (!shouldShowNotification()) return
        prepareNotification(messageTitle, messageBody)

        //Action Buttons & Intents
        setCancelIntent()
        notificationManager!!.notify(uploadId, n)
    }

    private fun shouldShowNotification(): Boolean {
        return mFileSize > 5242880
    }

    fun updateNotification(percent: Int, body: String?) {
        if (!shouldShowNotification()) return
        mBuilder!!.setContentText(body).setProgress(100, percent, false)
        // Displays the progress bar for the first time.
        notificationManager!!.notify(uploadId, mBuilder!!.build())
    }

    fun cancelSelf() {
        notificationManager!!.cancel(uploadId)
    }

    fun uploadCompleted() {
        if (!shouldShowNotification()) return
        notificationManager!!.cancel(uploadId)
        val n = NotificationHelper()
        val i = Intent(mService?.applicationContext!!, UploadService::class.java)
        i.putExtra("command", "error")
        i.putExtra("uploadId", uploadId)
        n.sendNotification(
            mService!!.applicationContext,
            fileName!!,
            "Upload completed.",
            "" + uploadId,
            i,
            R.drawable.ic_baseline_check_24
        )
    }

    fun uploadError() {
        notificationManager!!.cancel(uploadId)
        val n = NotificationHelper()
        val i = Intent(mService?.applicationContext, UploadService::class.java)
        i.putExtra("command", "error")
        i.putExtra("uploadId", uploadId)
        n.sendNotification(
            mService!!.applicationContext,
            fileName!!,
            "Upload failed!",
            "" + uploadId,
            i,
            R.drawable.ic_baseline_cancel_schedule_send_24
        )
    }
}
