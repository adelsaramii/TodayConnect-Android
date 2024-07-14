package com.today.connect.notification.lockScreen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.Person

class RingtoneService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    var url = ""
    var title = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        url = intent?.extras?.getString("URL").toString()
        title = intent?.extras?.getString("title").toString()
        showRingtoneFullScreenActivity()
        return START_STICKY
    }

    private fun showRingtoneFullScreenActivity() {
        val incomingCallIntent = Intent(
            this,
            RingtoneActivity::class.java
        )
        incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        incomingCallIntent.putExtra("URL" , url)
        incomingCallIntent.putExtra("title" , title)
        startActivity(incomingCallIntent)
    }

    private fun start() {
        val channelId = "full_screen_channel_id"

        val contentIntent = Intent(this, RingtoneActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create an Intent for the activity you want to start
        val fullScreenIntent = Intent(this, RingtoneActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val incomingCaller = Person.Builder()
            .setName("Jane Doe")
            .setImportant(true)
            .build()


        val hangUpIntent = Intent(
            this,
            RingtoneActivity::class.java
        )
        val hangUpPendingIntent = PendingIntent.getService(
            this,
            22,
            hangUpIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

//        val customView = RemoteViews("com.cave.todayconnectringtone", R.layout.cave_notification)
        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(androidx.core.R.drawable.notification_bg) // Replace with your icon
            .setContentTitle("Sticky Notification")
            .setContentText("This notification cannot be dismissed")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true) // Makes the notification sticky
            .setCategory(NotificationCompat.CATEGORY_CALL)
//            .build()
        // Show the notification
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    "MyRingtoneApp",
                    NotificationManager.IMPORTANCE_HIGH,
                )
            channel.description = "Caveeeeeee"
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)

            val notification = notificationBuilder.build()
            notification.flags = Notification.FLAG_INSISTENT

            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            channel.setSound(ringtoneUri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            startForeground(1, notification)


        }

    }


    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)

    }

    enum class Actions {
        START, STOP, ON_ACCEPT_CALL_BUTTON_CLICKED, ON_DECLINE_CALL_BUTTON_CLICKED,
        ON_BACK_BUTTON_CLICKED_FROM_RINGTONE_ACTIVITY, ON_RINGTONE_SCREEN_DISMISSED
    }

}