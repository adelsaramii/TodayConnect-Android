package com.today.connect

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonParser
import com.today.connect.activity.PermissionManager
import com.today.connect.notification.Ring
import com.today.connect.utils.NotificationHelper
import com.today.connect.utils.SHA256
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter


class FirebaseMessaging : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val notification: RemoteMessage.Notification? = remoteMessage.notification
        val data: Map<String, String?> = remoteMessage.data
        var title: String? = ""
        var body: String? = ""
        if (notification != null) {
            title = notification.title
            body = notification.body
        }
        if (data["title"] != null && data["body"] != null) {
            title = data["title"]
            body = data["body"]
        }
        val channelId: String = SHA256.hash(
            data["intent"].toString().replace("?cc=", "")
        ).toString()
        val n = NotificationHelper()

        if(title != null && body != null && body.startsWith("JSON:")){
            try {
                val jsonElement = JsonParser.parseString(body.replace("JSON:", ""))

                if (jsonElement.isJsonObject) {
                    val jsonObject = jsonElement.asJsonObject

                    if(jsonObject.has("mode") && jsonObject.has("address")) {
                        // Access properties dynamically
                        val mode = jsonObject.get("mode").toString().replace("\"", "");
                        val address = jsonObject.get("address").toString().replace("\"", "");
                        if(mode == "call") {
                            val serviceIntent = Intent(this, Ring::class.java).putExtra("address" , address).putExtra("title" , title)
                            startService(serviceIntent)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Notification Error!", e.toString());
            }
            return;
        }

        if (title != null && body != null){
            val id: Int = n.sendNotification(
                this.applicationContext, title, body, channelId, data["intent"] ?: ""
            )
            saveNotificationIDToFile(this.applicationContext, id, channelId)
        }
    }

    fun init(javaScriptInterface: JavaScriptInterface, permissionManager: PermissionManager) {
        if (Build.VERSION.SDK_INT >= 33) {
            val res = permissionManager.requestNotificationPermission13()
            if (res == PermissionManager.PERMISSION_REQUEST_GRANTED || res == PermissionManager.PERMISSION_REQUEST_PENDING) {
                requestNewToken(javaScriptInterface)
            }
        } else {
            requestNewToken(javaScriptInterface)
        }
    }

    private fun requestNewToken(javaScriptInterface: JavaScriptInterface) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnCompleteListener(object : OnCompleteListener<String?> {
                override fun onComplete(task: Task<String?>) {
                    if (!task.isSuccessful) {
                        Log.w(
                            "Firebase Messaging",
                            "Fetching FCM registration token failed",
                            task.exception
                        )
                        return
                    }

                    // Get new FCM registration token
                    val token: String? = task.result
                    javaScriptInterface.setToken(token)
                }
            })
    }

    fun deleteToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken()
    }

    private fun saveNotificationIDToFile(ctx: Context, id: Int, filename: String) {
        try {
            val outputStreamWriter = OutputStreamWriter(
                ctx.openFileOutput(
                    "$filename.ndb", Context.MODE_APPEND
                )
            )
            outputStreamWriter.write("$id\r\n")
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "write notification to db failed $e")
        }
    }

    private fun cancelNotification(ctx: Context, id: Int, tag: String?) {
        val nMgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nMgr.cancel(tag, id)
    }

    fun cancelContactNotifications(context: Context, contact: String) {
        try {
            SHA256.hash(contact).toString()
            val fos = context.openFileInput(SHA256.hash(contact).toString() + ".ndb")
            val reader = BufferedReader(InputStreamReader(fos))
            var line = reader.readLine()
            while (line != null) {
                cancelNotification(context, line.toInt(), SHA256.hash(contact).toString())
                line = reader.readLine()
            }
        } catch (e: IOException) {
            Log.e("Exception", "no notification db for $contact found! carry on...")
        }
    }
}