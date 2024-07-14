package com.today.connect.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import io.karn.notify.Notify
import com.today.connect.MainActivity
import com.today.connect.R

class NotificationHelper {
    private fun send(context: Context, t: String, te: String, k: String, notificationIntent: Intent, i: Int): Int {
        val rnd = (0..1000000).random()
        return Notify
            .with(context)
            .meta { // this: Payload.Meta
                // Launch the MainActivity once the notification is clicked.
                clickIntent = PendingIntent.getActivity(context,
                    rnd, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }
            .header {
                icon = i
            }
            .content { // this: Payload.Content.Default
                title = t
                text = te
            }
            // Define the notification as being stackable. This block should be the same for all
            // notifications which are to be grouped together.
            .stackable { // this: Payload.Stackable
                // In particular, this key should be the same. The properties of this stackable
                // notification as taken from the latest stackable notification's stackable block.
                key = k
                // This is the summary of this notification as it appears when it is as part of a
                // stacked notification. This String value is what is shown as a single line in the
                // stacked notification.
                summaryContent = te
                // The number of notifications with the same key is passed as the 'count' argument. We
                // happen not to use it, but it is there if needed.
                summaryTitle = { t }
                // ... here as well, but we instead choose to use to to update the summary for when the
                // notification is collapsed.
                summaryDescription = { count -> "$count new messages." }
                clickIntent = PendingIntent.getActivity(context,
                    rnd, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }
            .alerting("test"){
                lightColor = Color.rgb(119, 41, 83);
                //vibrationPattern = listOf(0, 200, 100, 300)
            }.show()
    }
    fun sendNotification(context: Context, t: String, te: String, k: String, intent: String) : Int {
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.putExtra("intent", intent)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return send(context, t, te, k, notificationIntent, R.mipmap.ic_launcher);
    }

    fun sendNotification(context: Context?, t: String?, te: String, k: String, intent: Intent) : Int? {
        return context?.let {
            if (t != null) {
                send(it, t, te, k, intent, R.mipmap.ic_launcher)
            } else {
                0
            }
        };
    }

    fun sendNotification(context: Context, t: String, te: String, k: String, intent: Intent, icon: Int) : Int {
        return send(context, t, te, k, intent, icon);
    }
}