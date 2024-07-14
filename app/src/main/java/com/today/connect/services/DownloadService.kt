package com.today.connect.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import androidx.core.app.NotificationCompat
import com.today.connect.R
import com.today.connect.downloader.DownloadsList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadService : Service() {
    private var mReceiver: DownloadServiceReceiver? = null
    var currentRunID = -1
    private var firstDL = true
    private var messenger: Messenger? = null
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val address = intent.getStringExtra("address")
        val destination = intent.getStringExtra("destination")
        val fileName = intent.getStringExtra("filename")
        val command = intent.getStringExtra("command")
        val downloadId = intent.getIntExtra("downloadId", -1)
        if (currentRunID == -1) {
            val now = Date()
            currentRunID = SimpleDateFormat("ddHHmmss", Locale.US).format(now).toInt()
        }
        // get javascript messenger if it exists
        val extras = intent.extras
        if (intent.hasExtra("messenger")) {
            messenger = extras!!["messenger"] as Messenger?
            val msg = Message.obtain()
            msg.obj = "$currentRunID\$D@RK00B\$currentRunID"
            try {
                messenger!!.send(msg)
            } catch (e1: RemoteException) {
                Log.w(javaClass.name, "Exception sending message", e1)
            }
        }

        // check start logic
        if (command != null) {
            mDownloads.updateDownloadItemStatus(command, downloadId, messenger)
            if (mDownloads.downloadsCount() == 0) {
                stopSelf()
            }
        } else {
            // its not an update so lets begin new download
//            try {
                mDownloads.addDownloadItem(
                    this,
                    address!!,
                    destination!!,
                    fileName,
                    messenger,
                    currentRunID
                )
//            } catch (e: Exception) {
//                Log.e("Fuck Mohsen : ", e.toString())
//            }
            val NOTIFICATION_CHANNEL_ID = "background_download"
            val channelName = "Background Download"
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val chan = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    channelName,
                    NotificationManager.IMPORTANCE_NONE
                )
                chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                manager.createNotificationChannel(chan)
            }
            if (firstDL) {
                val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.logo_round)
                    .setContentTitle("App is running in background")
                    .setCategory(Notification.CATEGORY_SERVICE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) notificationBuilder.priority =
                    NotificationManager.IMPORTANCE_MIN
                val notification = notificationBuilder.build()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    startForeground(10, notification)
                } else {
                    startForeground(10, notification,
                        FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                }
                registerBroadcastReceiver()
                firstDL = false
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver)
        }
        super.onDestroy()
    }

    class DownloadServiceReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            //Updates tvTimer
            if (action != null) {
                val notificationMessage = Intent(context, DownloadService::class.java)
                notificationMessage.putExtra("downloadId", intent.getIntExtra("downloadId", -1))
                when (action) {
                    "ACTION_PAUSE" -> notificationMessage.putExtra("command", "pause")
                    "ACTION_RESUME" -> notificationMessage.putExtra("command", "resume")
                    "ACTION_CANCEL" -> notificationMessage.putExtra("command", "cancel")
                    "ACTION_DONE" -> notificationMessage.putExtra("command", "done")
                }
                context.startService(notificationMessage)
            }
        }
    }

    private fun registerBroadcastReceiver() {
        mReceiver = DownloadServiceReceiver()
        val statusIntentFilter = IntentFilter("TIMER_SERVICE_BROADCAST")
        statusIntentFilter.addAction("ACTION_CANCEL")
        statusIntentFilter.addAction("ACTION_PAUSE")
        statusIntentFilter.addAction("ACTION_RESUME")
        statusIntentFilter.addAction("ACTION_DONE")

        // Registers the DownloadStateReceiver and its intent filters
//        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mReceiver, statusIntentFilter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(mReceiver, statusIntentFilter)
            }


//        } catch (e: Exception) {
//            Log.e("Fuck Mohsen : ", e.toString())
//        }
    }

    companion object {
        private val mDownloads = DownloadsList()
    }
}