package ir.yekaan.darkoobnext.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationManagerCompat
import ir.yekaan.darkoobnext.MainActivity
import ir.yekaan.darkoobnext.R
import ir.yekaan.darkoobnext.notification.lockScreen.RingtoneActivity


class Ring : Service() {

    private var ringtone: Ringtone? = null

    companion object {
        const val CHANNEL_ID = "CALL_NOTIFICATION_CHANNEL"
        const val NOTIFICATION_ID = 101
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Handler().postDelayed({
            ring(
                intent?.getStringExtra("address").toString(),
                intent?.getStringExtra("title").toString()
            )
        }, 5000)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun ring(url: String, name: String) {
        setupRingtone()
        showIncomingNotification(name, url)
        Handler(Looper.getMainLooper()).postDelayed({
            stopRingtone()
        }, 30000)
    }

    private fun setupRingtone() {
        ringtone = RingtoneManager.getRingtone(
            this,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        ringtone?.play()
    }

    private fun stopRingtone() {
        try {
            ringtone?.stop()
            NotificationManagerCompat.from(this).cancel("IncomingCall", 1124)
        } catch (e: Exception) {
            Log.e("stopRingtone", e.toString())
            Log.e("stopRingtone", e.message.toString())
        }
    }

    private fun showRingtoneFullScreenActivity(url: String, title: String) {
        try {
            val incomingCallIntent = Intent(
                this,
                RingtoneActivity::class.java
            )
            incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            incomingCallIntent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            incomingCallIntent.setData(Uri.parse("package:$packageName"));
            incomingCallIntent.putExtra("URL", url)
            incomingCallIntent.putExtra("title", title)
            startActivity(incomingCallIntent)
        } catch (e: Exception) {
        }
    }

    private fun showIncomingNotification(
        name: String, url: String
    ) {
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isInteractive) {
            val intent = Intent(this, MainActivity::class.java)
            intent.action = "voip"
            val builder: Notification.Builder = Notification.Builder(this)
                .setContentTitle(
                    "Call"
                )
                .setSmallIcon(R.drawable.call)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                val attrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_RING)
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .build()
                val chan = NotificationChannel(
                    "IncomingCall",
                    "IncomingCall",
                    NotificationManager.IMPORTANCE_HIGH
                )
                try {
                    chan.setSound(null, attrs)
                } catch (e: java.lang.Exception) {
                }
                chan.description = "My Description"
                chan.enableVibration(false)
                chan.enableLights(false)
                chan.setBypassDnd(true)
                builder.setChannelId("IncomingCall")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setSound(null)
            }
            val intentFlags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
            val rejectIntent = Intent(this, RejectReceiver::class.java)
            val rejectPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(this, 1, rejectIntent, intentFlags)

            val answerIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("URL", url)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val answerPendingIntent =
                PendingIntent.getActivity(this, 0, answerIntent, PendingIntent.FLAG_IMMUTABLE)
            builder.setPriority(Notification.PRIORITY_HIGH)
            builder.setVisibility(Notification.VISIBILITY_PUBLIC)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                builder.setShowWhen(false)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setColor(-0xd35a20)
                builder.setVibrate(LongArray(0))
                builder.setCategory(Notification.CATEGORY_CALL)
                builder.setFullScreenIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_MUTABLE
                    ), true
                )
            }
            val incomingNotification: Notification
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationChannel = NotificationChannel(
                    "IncomingCall",
                    "IncomingCall", NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(notificationChannel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationChannel = NotificationChannel(
                    "IncomingCall",
                    "IncomingCall", NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(notificationChannel)
                val avatar: Bitmap = BitmapFactory.decodeResource(
                    this.resources,
                    R.drawable.icon_unkown_caller_small
                )
                val person: Person = Person.Builder()
                    .setName(name)
                    .setIcon(Icon.createWithAdaptiveBitmap(avatar)).build()
                val notificationStyle = Notification.CallStyle.forIncomingCall(
                    person,
                    rejectPendingIntent,
                    answerPendingIntent
                )
                val testIntent: PendingIntent =
                    PendingIntent.getActivity(this, 0, Intent(), intentFlags)
                builder.style = notificationStyle
                builder.setOngoing(true)
                builder.setDefaults(Notification.DEFAULT_ALL)
                builder.setAutoCancel(false);
                builder.setOngoing(true);
                builder.setFullScreenIntent(testIntent, true)
                builder.setTimeoutAfter(100000)
                incomingNotification = builder.build()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.addAction(R.drawable.call, "reject", rejectPendingIntent)
                builder.addAction(R.drawable.call, "accept", answerPendingIntent)
                builder.setContentText(name)
                val customView = RemoteViews(
                    packageName,
                    R.layout.custom_call_notification
                )
                customView.setTextViewText(R.id.name, name)
                customView.setTextViewText(
                    R.id.btnAnswer,
                    "Answer"
                )
                customView.setTextViewText(
                    R.id.btnDecline,
                    "Decline"
                )
                customView.setOnClickPendingIntent(R.id.btnAnswer, answerPendingIntent)
                customView.setOnClickPendingIntent(R.id.btnDecline, rejectPendingIntent)
                builder.setCustomHeadsUpContentView(customView)
                builder.setCustomContentView(customView)
                builder.setCustomBigContentView(customView)

                builder.setLargeIcon(
                    BitmapFactory.decodeResource(
                        this.resources,
                        R.drawable.icon_unkown_caller_small
                    )
                )
                incomingNotification = builder.notification
                incomingNotification.bigContentView = customView
                incomingNotification.headsUpContentView = incomingNotification.bigContentView
            } else {
                builder.setContentText(name)
                builder.addAction(R.drawable.call, "reject", rejectPendingIntent)
                builder.addAction(R.drawable.call, "answer", answerPendingIntent)
                incomingNotification = builder.notification
            }
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationChannel = NotificationChannel(
                    "IncomingCall",
                    "IncomingCall", NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(notificationChannel)
            }
            startForeground(202, incomingNotification)
        } else {
            try {
                showRingtoneFullScreenActivity(url, name)

                val intent = Intent(this, MainActivity::class.java)
                intent.action = "voip"
                val builder: Notification.Builder = Notification.Builder(this)
                    .setContentTitle(
                        "Call"
                    )
                    .setSmallIcon(R.drawable.icon_unkown_caller_small)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    val attrs = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .build()
                    val chan = NotificationChannel(
                        "IncomingCall",
                        "IncomingCall",
                        NotificationManager.IMPORTANCE_LOW
                    )
                    try {
                        chan.setSound(null, attrs)
                    } catch (e: java.lang.Exception) {
                    }
                    chan.description = "My Description"
                    chan.enableVibration(false)
                    chan.enableLights(false)
                    chan.setBypassDnd(true)
                    builder.setChannelId("IncomingCall")
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder.setSound(null)
                }
                val intentFlags =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
                val rejectIntent = Intent(this, RejectReceiver::class.java)
                val rejectPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(this, 1, rejectIntent, intentFlags)

                val answerIntent = Intent(this, MainActivity::class.java).apply {
                    putExtra("URL", url)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val answerPendingIntent =
                    PendingIntent.getActivity(this, 0, answerIntent, PendingIntent.FLAG_IMMUTABLE)
                builder.setPriority(Notification.PRIORITY_HIGH)
                builder.setVisibility(Notification.VISIBILITY_PUBLIC)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    builder.setShowWhen(false)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder.setColor(-0xd35a20)
                    builder.setVibrate(LongArray(0))
                    builder.setCategory(Notification.CATEGORY_CALL)
                    builder.setFullScreenIntent(
                        PendingIntent.getActivity(
                            this,
                            0,
                            intent,
                            PendingIntent.FLAG_MUTABLE
                        ), true
                    )
                }
                val incomingNotification: Notification
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notificationChannel = NotificationChannel(
                        "IncomingCall",
                        "IncomingCall", NotificationManager.IMPORTANCE_HIGH
                    )
                    notificationManager.createNotificationChannel(notificationChannel)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notificationChannel = NotificationChannel(
                        "IncomingCall",
                        "IncomingCall", NotificationManager.IMPORTANCE_LOW
                    )
                    notificationManager.createNotificationChannel(notificationChannel)
                    val avatar: Bitmap = BitmapFactory.decodeResource(
                        this.resources,
                        R.drawable.icon_unkown_caller_small
                    )
                    val person: Person = Person.Builder()
                        .setName(name)
                        .setIcon(Icon.createWithAdaptiveBitmap(avatar)).build()
                    val notificationStyle = Notification.CallStyle.forIncomingCall(
                        person,
                        rejectPendingIntent,
                        answerPendingIntent
                    )
                    val testIntent: PendingIntent =
                        PendingIntent.getActivity(this, 0, Intent(), intentFlags)
                    builder.style = notificationStyle
                    builder.setOngoing(true)
                    builder.setDefaults(Notification.DEFAULT_ALL)
                    builder.setAutoCancel(false);
                    builder.setOngoing(true);
                    builder.setFullScreenIntent(testIntent, true)
                    builder.setTimeoutAfter(100000)
                    incomingNotification = builder.build()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder.addAction(R.drawable.call, "reject", rejectPendingIntent)
                    builder.addAction(R.drawable.call, "accept", answerPendingIntent)
                    builder.setContentText(name)
                    val customView = RemoteViews(
                        packageName,
                        R.layout.custom_call_notification
                    )
                    customView.setTextViewText(R.id.name, name)
                    customView.setTextViewText(
                        R.id.btnAnswer,
                        "Answer"
                    )
                    customView.setTextViewText(
                        R.id.btnDecline,
                        "Decline"
                    )
                    customView.setOnClickPendingIntent(R.id.btnAnswer, answerPendingIntent)
                    customView.setOnClickPendingIntent(R.id.btnDecline, rejectPendingIntent)
                    builder.setCustomHeadsUpContentView(customView)
                    builder.setCustomContentView(customView)
                    builder.setCustomBigContentView(customView)

                    builder.setLargeIcon(
                        BitmapFactory.decodeResource(
                            this.resources,
                            R.drawable.icon_unkown_caller_small
                        )
                    )
                    incomingNotification = builder.notification
                    incomingNotification.bigContentView = customView
                    incomingNotification.headsUpContentView = incomingNotification.bigContentView
                } else {
                    builder.setContentText(name)
                    builder.addAction(R.drawable.call, "reject", rejectPendingIntent)
                    builder.addAction(R.drawable.call, "answer", answerPendingIntent)
                    incomingNotification = builder.notification
                }
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val notificationChannel = NotificationChannel(
                        "IncomingCall",
                        "IncomingCall", NotificationManager.IMPORTANCE_LOW
                    )
                    notificationManager.createNotificationChannel(notificationChannel)
                }
                startForeground(202, incomingNotification)
            } catch (e: Exception) {
            }

        }
    }

    init {
        MyObject.ring = this
    }

    var runnable = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
        ringtone?.stop()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}

class RejectReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MyObject.ring?.runnable?.invoke()
    }
}