package ir.yekaan.darkoobnext.notification

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.yekaan.darkoobnext.MainActivity
import ir.yekaan.darkoobnext.R

class Ring private constructor(private val context: Context) {

    private var ringtone: Ringtone? = null

    companion object {
        private var instance: Ring? = null
        const val CHANNEL_ID = "CALL_NOTIFICATION_CHANNEL"
        const val NOTIFICATION_ID = 101

        fun getInstance(context: Context): Ring {
            return instance ?: synchronized(this) {
                instance ?: Ring(context).also { instance = it }
            }
        }
    }

    fun ring(url: String, name: String) {
        setupRingtone()
        createNotification(url, name)

        // Automatically stop the ringtone after 30 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            stopRingtone()
        }, 30000)
    }

    private fun setupRingtone() {
        ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
        ringtone?.play()
    }

    fun stopRingtone() {
        ringtone?.stop()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun createNotification(url: String, name: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        val intentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0

        val answerIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("URL", url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val answerPendingIntent = PendingIntent.getActivity(context, 0, answerIntent, PendingIntent.FLAG_IMMUTABLE)

        val rejectIntent = Intent(context, RejectReceiver::class.java)
        val rejectPendingIntent: PendingIntent = PendingIntent.getBroadcast(context, 1, rejectIntent, intentFlags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(context.getString(R.string.incoming_call, name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_call, "Answer", answerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reject", rejectPendingIntent)
            .setDeleteIntent(rejectPendingIntent)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("Ring class", "Permission Denied")
            return
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channelName = "Incoming Call Channel"
        val channelDescription = "Notification channel for incoming calls"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
            description = channelDescription
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    init {
        createNotificationChannel()
    }
}

class RejectReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Ring.getInstance(context).stopRingtone()
    }
}