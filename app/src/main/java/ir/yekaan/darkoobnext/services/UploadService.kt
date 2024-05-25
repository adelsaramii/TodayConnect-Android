package ir.yekaan.darkoobnext.services

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import ir.yekaan.darkoobnext.R
import ir.yekaan.darkoobnext.uploader.HTTPUploader
import ir.yekaan.darkoobnext.utils.Encryption
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


internal class HTTPUploaderEvents {
    var fileId: String? = null
    var command: String? = null
    var value: String? = null
    var caption: String? = null
    var messageId: String? = null
    var targetDomain: String? = null
    var restMessageSendHash: String? = null
    var from: String? = null
    var rfu: String? = null
}

internal class UploadsList {
    private val uploadList: MutableMap<String?, HTTPUploader?> = HashMap()
    private var mMessenger: Messenger? = null
    private var mEndService: Callback? = null
    private var mUploadService: UploadService? = null
    fun setMessenger(messenger: Messenger?) {
        mMessenger = messenger
    }

    private fun checkIfAppIsInBackGround(): Boolean {
        val appProcessInfo = RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return !(appProcessInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND || appProcessInfo.importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE)
    }

    fun addUploadItem(
        resumableUploadEndpoint: String?,
        localPath: String?,
        filename: String?,
        mimeType: String?,
        range: String?,
        fileId: String?,
        fullPath: String?,
        hash: String?,
        restMessageSendHash: String?,
        tmpMsgId: String?,
        targetDomain: String?,
        targetUser: String?,
        tmpContent: String?,
        uiTempRawMessageKey: String?,
        token: String?,
        serverSideFilename: String?,
        contentType: String?,
        modifiedAt: String?,
        fileSize: String?,
        isUploadNative: String?,
        from: String?,
        caption: String?,
        tmpRawLocalMediaMessageJsonString: String?,
        endService: Callback?,
        uploadService: UploadService
    ) {
        mEndService = endService
        mUploadService = uploadService
        uploadList[fileId] = HTTPUploader(
            uploadService,
            localPath,
            resumableUploadEndpoint,
            range,
            fileId,
            filename,
            fullPath,
            hash,
            serverSideFilename,
            tmpRawLocalMediaMessageJsonString,
            uiTempRawMessageKey,
            caption,
            restMessageSendHash,
            tmpMsgId,
            targetDomain,
            from
        ) { message: String -> uploadEventsHandler(message) }
    }

    private fun calcContent(caption: String?, fileName: String?): String? {
        try {
            return if (caption != null &&
                caption.lowercase() != "n/a" &&
                caption.lowercase() != ""
            ) {
                JSONObject()
                    .put("fileName", fileName)
                    .put("caption", caption)
                    .toString()
            } else JSONObject()
                .put("fileName", fileName)
                .put("caption", caption)
                .toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return fileName
    }

    private fun uploadEventsHandler(message: String) {
        var shouldCheckForServiceEnd = false
        var shouldUploadGetRemovedFromList = false
        val event = Gson().fromJson(message, HTTPUploaderEvents::class.java)
        when (event.command) {
            "UPLOAD_SUCCESS" -> {
                if (checkIfAppIsInBackGround()) {
                    try {
                        val url = URL(
                            event.rfu + "H?q=" + Encryption.encrypt(
                                JSONObject()
                                    .put("messageId", event.messageId)
                                    .put("content", calcContent(event.caption, event.value))
                                    .put("domain", event.targetDomain)
                                    .put("from", event.from)
                                    .put("hash", event.restMessageSendHash)
                                    .toString(),
                                "!QAZ1qaz@WSX2wsx"
                            )
                        )
                        val con = url.openConnection() as HttpURLConnection
                        con.requestMethod = "GET"
                        con.setRequestProperty("User-Agent", "DARKOOB")
                        val responseCode = con.responseCode
                        println("GET Response Code :: $responseCode")
                        if (responseCode == HttpURLConnection.HTTP_OK) { // success
                            val `in` = BufferedReader(
                                InputStreamReader(
                                    con.inputStream
                                )
                            )
                            var inputLine: String?
                            val response = StringBuilder()
                            while (`in`.readLine().also { inputLine = it } != null) {
                                response.append(inputLine)
                            }
                            `in`.close()

                            // print result
                            println(response.toString())
                        } else {
                            println("GET request not worked")
                        }
                    } catch (protocolException: JSONException) {
                        protocolException.printStackTrace()
                    } catch (protocolException: IOException) {
                        protocolException.printStackTrace()
                    }
                }
                shouldCheckForServiceEnd = true
                shouldUploadGetRemovedFromList = true
            }

            "UPLOAD_UPDATE_PROGRESS" -> {}
            "UPLOAD_ERROR", "SERVER_ERROR", "UPLOAD_EXCEPTION", "UPLOAD_CANCELED" -> {
                shouldCheckForServiceEnd = true
                shouldUploadGetRemovedFromList = true
            }
        }
        if (shouldCheckForServiceEnd && uploadList.size == 1) {
            uploadList.remove(event.fileId)
            mEndService!!.call()
        } else if (shouldUploadGetRemovedFromList) {
            uploadList.remove(event.fileId)
        }
        val msg = Message()
        msg.obj = message
        try {
            mMessenger!!.send(msg)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun uploadsCount(): Int {
        return uploadList.size
    }

    fun updateUploadItemStatus(command: String?, fileId: String?) {
        val u: HTTPUploader? = uploadList.getOrDefault(fileId, null)
        if (command == "getActiveUploads") {
            Thread(Runnable {
                if (uploadsCount() == 0 && mEndService != null) mEndService!!.call() else if (uploadsCount() == 0) return@Runnable
                uploadList.forEach { (k, v) ->
                    try {
                        val rawMsg: String = v?.mTmpRawLocalMediaMessageJsonString!!
                        val msg = Message()
                        msg.obj = JSONObject()
                            .put("fileId", k)
                            .put("command", "FIY")
                            .put("rawMsg", rawMsg)
                            .toString()
                        mMessenger!!.send(msg)
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
            }).start()
            return
        }
        if (u != null) {
            when (command) {
                "pause" -> u.pauseUpload()
                "start", "resume" -> {
                    Toast.makeText(mUploadService, "uploadStarted", Toast.LENGTH_SHORT).show()
                    Thread(u::resumeUpload).start()
                }

                "cancel" -> u.cancelUpload()
                "cancelByTempKey" -> {
                    uploadList.forEach { (_, v) ->
                        if (v != null && v.mUiTempRawMessageKey == fileId) {
                            uploadList.getOrDefault(fileId, null)?.cancelUpload()
                        }
                    }
                }

                "talkToUploadService" -> {}
                else -> try {
                    val msg = Message()
                    msg.obj = JSONObject()
                        .put("fileId", fileId)
                        .put("command", "UNKNOWN_COMMAND")
                        .put("value", "N/A")
                        .toString()
                    mMessenger!!.send(msg)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }
}


internal interface Callback {
    fun call()
}

class UploadService : Service() {
    private var mReceiver: UploadServiceReceiver? = null
    var currentRunID = -1
    private var firstDL = true
    private var messenger: Messenger? = null
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val fileId = intent?.getStringExtra("fileId") ?: return START_NOT_STICKY
        val command = intent.getStringExtra("command")
        if (command != null && command == "start") {
            val resumableUploadEndpoint = intent.getStringExtra("resumableUploadEndpoint")
            val localPath = intent?.getStringExtra("localFilePath")
            val hash = intent?.getStringExtra("hash")
            val fullPath = intent?.getStringExtra("fullPath")
            val filename = intent?.getStringExtra("filename")
            val tmpMsgId = intent?.getStringExtra("tmpMsgId")
            val targetDomain = intent?.getStringExtra("targetDomain")
            val targetUser = intent?.getStringExtra("targetUser")
            val tmpContent = intent?.getStringExtra("tmpContent")
            val uiTempRawMessageKey = intent?.getStringExtra("uiTempRawMessageKey")
            val from = intent?.getStringExtra("from")
            val token = intent?.getStringExtra("token")
            val serverSideFilename = intent?.getStringExtra("serverSideFilename")
            val contentType = intent?.getStringExtra("contentType")
            val modifiedAt = intent?.getStringExtra("modifiedAt")
            val fileSize = intent?.getStringExtra("fileSize")
            val isUploadNative = intent?.getStringExtra("isUploadNative")
            val range = intent?.getStringExtra("range")
            val mimeType = intent?.getStringExtra("contentType")
            val caption = intent?.getStringExtra("caption")
            val restMessageSendHash = intent?.getStringExtra("restMessageSendHash")
            val tmpRawLocalMediaMessageJsonString = intent?.getStringExtra(
                "tmpRawLocalMediaMessageJsonString"
            )
            if (intent != null) {
                messenger = intent.extras?.get("messenger") as Messenger?
            }
            mUploads.setMessenger(messenger)
            val this2 = this
            mUploads.addUploadItem(
                resumableUploadEndpoint,
                localPath,
                filename,
                mimeType,
                range,
                fileId,
                fullPath,
                hash,
                restMessageSendHash,  // here
                tmpMsgId,
                targetDomain,
                targetUser,
                tmpContent,
                uiTempRawMessageKey,
                token,
                serverSideFilename,
                contentType,
                modifiedAt,
                fileSize,
                isUploadNative,
                from,
                caption,
                tmpRawLocalMediaMessageJsonString, object : Callback {
                    override fun call() {
                        this2.stopSelf()
                    }
                },
                this
            )
            val NOTIFICATION_CHANNEL_ID = "background_upload"
            val channelName = "Background Upload"
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val chan = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_NONE
            )
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            manager.createNotificationChannel(chan)
            if (firstDL) {
                val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.logo_round)
                    .setContentTitle("App is running in background")
                    .setCategory(Notification.CATEGORY_SERVICE)
                notificationBuilder.priority = NotificationManager.IMPORTANCE_MIN
                val notification = notificationBuilder.build()
                startForeground(10, notification)
                registerBroadcastReceiver()
                firstDL = false
            }
        } else if (command != null && command == "talkToUploadService") {
            val payload = intent.getStringExtra("payload")
            val event = Gson().fromJson(payload, HTTPUploaderEvents::class.java)
            mUploads.updateUploadItemStatus(event.command, event.fileId)
        }
        if (command != null) {
            mUploads.updateUploadItemStatus(command, fileId)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver)
        }
        super.onDestroy()
    }

    class UploadServiceReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            //Updates tvTimer
            if (action != null) {
                val notificationMessage = Intent(context, UploadService::class.java)
                notificationMessage.putExtra("fileId", intent.getStringExtra("fileId"))
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
        mReceiver = UploadServiceReceiver()
        val statusIntentFilter = IntentFilter("TIMER_SERVICE_BROADCAST")
        statusIntentFilter.addAction("ACTION_CANCEL")
        statusIntentFilter.addAction("ACTION_PAUSE")
        statusIntentFilter.addAction("ACTION_RESUME")
        statusIntentFilter.addAction("ACTION_DONE")

        // Registers the UploadStateReceiver and its intent filters

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mReceiver, statusIntentFilter, RECEIVER_EXPORTED)
            } else {
                registerReceiver(mReceiver, statusIntentFilter)
            }
        } catch (e: Exception) {
            Log.e("Fuck Mohsen : ", e.toString())
        }
    }

    companion object {
        private val mUploads = UploadsList()
    }
}