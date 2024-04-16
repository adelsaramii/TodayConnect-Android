package ir.yekaan.darkoobnext.notification

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import ir.yekaan.darkoobnext.R
import ir.yekaan.darkoobnext.services.DownloadService
import ir.yekaan.darkoobnext.state.GlobalState
import ir.yekaan.darkoobnext.utils.FileUtils
import ir.yekaan.darkoobnext.utils.NotificationHelper
import java.io.File
import java.util.Objects


class DownloadNotification internal constructor() {
    private var mService: DownloadService? = null
    private var downloadId = 0
    private var notificationManager: NotificationManager? = null
    private var mBuilder: NotificationCompat.Builder? = null
    private var url: String = ""
    private var filePath: String = ""
    private var fileName: String = ""
    private var fileTempPath: String = ""
    private var foregroundNotification: Notification? = null

    fun setService(service: DownloadService?) {
        mService = service
    }

    fun setId(i: Int) {
        downloadId = i
    }

    fun setUrl(url: String) {
        this.url = url
    }

    fun setFilePath(filePath: String) {
        this.filePath = filePath
    }

    fun setFileName(fileName: String) {
        this.fileName = fileName
    }

    fun setFileTempPath(fileTempPath: String) {
        this.fileTempPath = fileTempPath
    }

    private fun prepareNotification(messageTitle: String?, messageBody: String?) {
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
            "Downloading...",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager!!.createNotificationChannel(channel)
        mBuilder!!.setChannelId(channelId)
        mBuilder?.setSmallIcon(R.drawable.ic_baseline_get_app_24)
            ?.setContentTitle(messageTitle)
            ?.setContentText(messageBody)
            ?.setAutoCancel(true)
            ?.setDefaults(Notification.DEFAULT_ALL)
            ?.setOnlyAlertOnce(true)?.color = Color.parseColor("#3F5996")
        foregroundNotification = mBuilder!!.build()
    }

    private fun setCancelIntent() {
        val cancelIntent = Intent(mService, DownloadService.DownloadServiceReceiver::class.java)
        cancelIntent.action = "ACTION_CANCEL"
        cancelIntent.putExtra("downloadId", downloadId)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            mService,
            downloadId,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        mBuilder!!.addAction(R.drawable.ic_baseline_close_24, "Cancel", cancelPendingIntent)
    }

    private fun setPauseIntent() {
        val pauseIntent = Intent(mService, DownloadService.DownloadServiceReceiver::class.java)
        pauseIntent.action = "ACTION_PAUSE"
        pauseIntent.putExtra("downloadId", downloadId)
        val pausePendingIntent = PendingIntent.getBroadcast(
            mService,
            downloadId,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        mBuilder!!.addAction(R.drawable.ic_baseline_pause_24, "Pause", pausePendingIntent)
    }

    private fun setResumeIntent() {
        val resumeIntent = Intent(mService, DownloadService.DownloadServiceReceiver::class.java)
        resumeIntent.action = "ACTION_RESUME"
        resumeIntent.putExtra("downloadId", downloadId)
        val resumePendingIntent = PendingIntent.getBroadcast(
            mService,
            downloadId,
            resumeIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        mBuilder!!.addAction(R.drawable.ic_baseline_play_arrow_24, "Resume", resumePendingIntent)
    }

    fun sendNotification(messageTitle: String?, messageBody: String?) {
        prepareNotification(messageTitle, messageBody)

        //Action Buttons & Intents
        setCancelIntent()
        setPauseIntent()
        notificationManager!!.notify(downloadId, foregroundNotification)
    }

    fun pause() {
        mBuilder!!.clearActions()

        //Action Buttons & Intents
        setCancelIntent()
        setResumeIntent()
        mBuilder!!.setContentText("Download Paused")
        notificationManager!!.notify(downloadId, mBuilder!!.build())
    }

    fun resume() {
        mBuilder!!.clearActions()

        //Action Buttons & Intents
        setCancelIntent()
        setPauseIntent()
        mBuilder!!.setContentText("Resuming...")
        notificationManager!!.notify(downloadId, mBuilder!!.build())
    }

    fun updateNotification(percent: Int, Eta: String, Speed: String) {
        mBuilder!!.setContentText("$Speed   $Eta").setProgress(100, percent, false)
        // Displays the progress bar for the first time.
        notificationManager!!.notify(downloadId, mBuilder!!.build())
    }

    private fun rename(from: File, to: File): Boolean {
        return Objects.requireNonNull(from.parentFile)
            .exists() && from.exists() && from.renameTo(to)
    }

    private fun checkIfAppIsInForeGround(): Boolean {
        val appProcessInfo = RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return !(appProcessInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND || appProcessInfo.importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE)
    }

    fun cancelSelf() {
        notificationManager!!.cancel(downloadId)
    }

    fun downloadCompleted() {
        notificationManager!!.cancel(downloadId)
        val i = Intent(mService?.applicationContext, DownloadService::class.java)
        i.putExtra("command", "done")
        i.putExtra("downloadId", downloadId)
        mService?.applicationContext?.startService(i)
        val currentFile = File(fileTempPath)
        val newFile = File(filePath)
        if (rename(currentFile, newFile)) {
            //Success
            Log.i(ContentValues.TAG, "Successfully downloaded and renamed")
        } else {
            //Fail
            Log.i(ContentValues.TAG, "Failed to download or rename")
        }
        val of = FileUtils(null, url, filePath, mService)
        val fileObject = File(filePath)
        if (fileObject.exists()) {
            of.addFileToGallery()
            GlobalState.setFileDownloadPath(url, filePath)
            if (checkIfAppIsInForeGround()) {
                val n = NotificationHelper()
                n.sendNotification(
                    mService?.applicationContext, fileName, "Download completed",
                    "ok$downloadId", of.intent
                )
            } else {
                of.openFile()
            }
        }
    }
}
