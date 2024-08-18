package com.today.connect.data.uploader

import android.util.Log
import com.google.gson.Gson
import com.today.connect.notification.UploadNotification
import com.today.connect.services.UploadService
import com.today.connect.utils.Encryption
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class HTTPUploader(
    s: UploadService,
    localPath: String?,
    resumableUploadEndpoint: String?,
    range: String?,
    fileId: String?,
    filename: String?,
    fullPath: String?,
    hash: String?,
    serverSideFilename: String?,
    tmpRawLocalMediaMessageJsonString: String?,
    uiTempRawMessageKey: String?,
    caption: String?,
    restMessageSendHash: String?,
    tmpMsgId: String?,
    targetDomain: String?,
    from: String?,
    uploadEventsCallback: (String) -> Unit
) {
    private var mErrorThershold = 0
    private var mMaxBufferSize = 1024 * 1024
    private var mFile: File
    private var mResumableUploadEndpoint: String?
    private var mRange: Long
    private var mFileId: String?
    private var mFullPath: String?
    private var mHash: String?
    private var mServerSideFilename: String?
    var mTmpRawLocalMediaMessageJsonString: String?
    var mUiTempRawMessageKey: String?
    private var mCaption: String?
    private var mFileSize: Long
    private var mCompletionPercentage = 0.0
    private var cancelUpload = false
    private var uploadInProgress = false
    private var mUploadEventsCallback: (String)->Unit
    private val note: UploadNotification
    private val mTmpMsgId: String?
    private val mTargetDomain: String?
    private val mFrom: String?
    private val mRestMessageSendHash: String?
    private val mUploadService: UploadService

    init {
        mFile = File(localPath)
        mFileSize = mFile.length()
        mResumableUploadEndpoint = resumableUploadEndpoint
        mRange = 0
        if (range != null) {
            mRange = range.toLong()
        }
        mFileId = fileId
        mFullPath = fullPath
        mHash = hash
        mServerSideFilename = serverSideFilename
        mUploadEventsCallback = uploadEventsCallback
        mUiTempRawMessageKey = uiTempRawMessageKey
        mCaption = caption
        mTmpRawLocalMediaMessageJsonString = tmpRawLocalMediaMessageJsonString
        mTmpMsgId = tmpMsgId
        mTargetDomain = targetDomain
        mFrom = from
        mRestMessageSendHash = restMessageSendHash
        mUploadService = s

        //notification
        note = UploadNotification()
        note.setService(s)
        note.setFileName(filename)
        note.setId((Math.random() * 65534).toInt())
        note.setFileId(fileId)
        note.setFileSize(mFile.length())
        note.sendNotification(filename, "Uploading...")
    }

    fun pauseUpload() {}
    fun cancelUpload() {
        cancelUpload = true
        if (!uploadInProgress) {
            try {
                mUploadEventsCallback.invoke(
                    JSONObject()
                        .put("fileId", mFileId)
                        .put("command", "UPLOAD_CANCELED")
                        .put("uiMsgKey", mUiTempRawMessageKey)
                        .put("value", mCompletionPercentage)
                        .toString()
                )
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    private fun setMRange() {
        try {
            val gson = Gson()
            val q = UploadQuery()
            q.fileId = mFileId
            q.range = "" + mRange
            q.filename = mServerSideFilename
            q.fullPath = mFullPath
            q.hash = mHash
            q.justGetOffset = true
            val url = URL(
                "$mResumableUploadEndpoint?q=" + Encryption.encrypt(
                    gson.toJson(q),
                    "!QAZ1qaz@WSX2wsx"
                )
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.useCaches = false
            connection.requestMethod = "POST"
            connection.connect()

            // Get Response
            val br: BufferedReader
            if (connection.responseCode == 200) {
                val result = StringBuilder()
                br = BufferedReader(InputStreamReader(connection.inputStream))
                var strCurrentLine: String?
                while (br.readLine().also { strCurrentLine = it } != null) {
                    result.append(strCurrentLine)
                }
                val srvResponse = Gson().fromJson(result.toString(), ServerResponse::class.java)
                if (srvResponse.msg == "INFO") {
                    mRange = srvResponse.offset!!.toLong()
                }
            }
        } catch (e: Exception) {
            Log.e("SET mRange Error", e.message!!)
            mRange = 0
        }
    }

    fun resumeUpload() {
        uploadInProgress = true
        var connection: HttpURLConnection? = null
        try {
            setMRange()
            val gson = Gson()
            val q = UploadQuery()
            q.fileId = mFileId
            q.range = "" + mRange
            q.filename = mServerSideFilename
            q.fullPath = mFullPath
            q.hash = mHash
            q.justGetOffset = false
            val url = URL(
                "$mResumableUploadEndpoint?q=" + Encryption.encrypt(
                    gson.toJson(q),
                    "!QAZ1qaz@WSX2wsx"
                )
            )
            connection = url.openConnection() as HttpURLConnection
            // Allow Inputs & Outputs
            connection.doInput = true
            connection.doOutput = true
            connection.useCaches = false
            // Enable POST method
            connection.requestMethod = "POST"
            connection.setFixedLengthStreamingMode(mFileSize - mRange)
            val outputStream = DataOutputStream(connection.outputStream)
            val fileInputStream = FileInputStream(mFile)
            fileInputStream.skip(mRange)
            var bytesAvailable = fileInputStream.available()
            var bufferSize = bytesAvailable.coerceAtMost(mMaxBufferSize)
            val buffer = ByteArray(bufferSize)
            var bytesRead = fileInputStream.read(buffer, 0, bufferSize)
            while (bytesRead > 0) {
                mErrorThershold = 0
                outputStream.write(buffer, 0, bufferSize)
                outputStream.flush()
                mRange += bytesRead.toLong()
                bytesAvailable = fileInputStream.available()
                bufferSize = bytesAvailable.coerceAtMost(mMaxBufferSize)
                bytesRead = fileInputStream.read(buffer, 0, bufferSize)
                mCompletionPercentage = mRange * 1.0 / mFileSize * 100
                mUploadEventsCallback.invoke(
                    JSONObject()
                        .put("fileId", mFileId)
                        .put("command", "UPLOAD_UPDATE_PROGRESS")
                        .put("uiMsgKey", mUiTempRawMessageKey)
                        .put("value", mCompletionPercentage)
                        .toString()
                )
                note.updateNotification(
                    java.lang.Double.valueOf(mRange * 1.0 / mFileSize * 100).toInt(), "Uploading..."
                )
                if (cancelUpload) {
                    try {
                        mUploadEventsCallback.invoke(
                            JSONObject()
                                .put("fileId", mFileId)
                                .put("command", "UPLOAD_CANCELED")
                                .put("uiMsgKey", mUiTempRawMessageKey)
                                .put("value", mCompletionPercentage)
                                .toString()
                        )
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    fileInputStream.close()
                    outputStream.close()
                    note.cancelSelf()
                    return
                }
            }
            fileInputStream.close()
            outputStream.close()
            val br: BufferedReader
            if (connection.responseCode == 200) {
                val result = StringBuilder()
                br = BufferedReader(InputStreamReader(connection.inputStream))
                var strCurrentLine: String?
                while (br.readLine().also { strCurrentLine = it } != null) {
                    result.append(strCurrentLine)
                }
                val srvResponse = Gson().fromJson(result.toString(), ServerResponse::class.java)
                if (srvResponse.msg == "DOWNLOAD_COMPLETED") {
                    mUploadEventsCallback.invoke(
                        JSONObject()
                            .put("fileId", mFileId)
                            .put("command", "UPLOAD_SUCCESS")
                            .put("rawMsg", mTmpRawLocalMediaMessageJsonString)
                            .put("uiMsgKey", mUiTempRawMessageKey)
                            .put("caption", mCaption)
                            .put("value", srvResponse.url)
                            .put("rfu", mResumableUploadEndpoint)
                            .put("tmpMsgId", mTmpMsgId)
                            .put("targetDomain", mTargetDomain)
                            .put("restMessageSendHash", mRestMessageSendHash)
                            .put("from", mFrom)
                            .toString()
                    )
                    note.uploadCompleted()
                } else if (srvResponse.msg == "ERROR") {
                    mUploadEventsCallback.invoke(
                        JSONObject()
                            .put("fileId", mFileId)
                            .put("command", "UPLOAD_ERROR")
                            .put("uiMsgKey", mUiTempRawMessageKey)
                            .put("value", srvResponse.reason)
                            .toString()
                    )
                    note.uploadError()
                }
            } else {
                Log.e(
                    "UPLOAD SOFT EXCEPTION",
                    "UPLOAD SOFT EXCEPTION " + connection.responseCode + mErrorThershold
                )
                if (!cancelUpload && mErrorThershold < 15) {
                    mErrorThershold++
                    mRange = 0
                    try {
                        TimeUnit.SECONDS.sleep(5)
                    } catch (ex: InterruptedException) {
                        Log.e("UPLOAD EXCEPTION", "UPLOAD SOFT EXCEPTION UNKNOWN")
                        ex.printStackTrace()
                    }
                    resumeUpload()
                    return
                }
                val errResult = StringBuilder()
                br = BufferedReader(InputStreamReader(connection.errorStream))
                var strCurrentLine: String?
                while (br.readLine().also { strCurrentLine = it } != null) {
                    errResult.append(strCurrentLine)
                }
                mUploadEventsCallback.invoke(
                    JSONObject()
                        .put("fileId", mFileId)
                        .put("command", "SERVER_ERROR")
                        .put("uiMsgKey", mUiTempRawMessageKey)
                        .put("value", errResult)
                        .toString()
                )
                note.uploadError()
            }
        } catch (e: Exception) {
            try {
                connection?.disconnect()
            } catch (er: Exception) {
                Log.e("Upload Service Error", er.message!!)
            }
            Log.e("UPLOAD EXCEPTION", "Protocol ERROR " + mErrorThershold + e.message)
            if (!cancelUpload && mErrorThershold < 15) {
                mRange = 0
                mErrorThershold++
                try {
                    TimeUnit.SECONDS.sleep(5)
                } catch (ex: InterruptedException) {
                    Log.e("UPLOAD EXCEPTION", "UNKNOWN PROTOCOL ERROR")
                    ex.printStackTrace()
                }
                resumeUpload()
                return
            }
            try {
                mUploadEventsCallback.invoke(
                    JSONObject()
                        .put("fileId", mFileId)
                        .put("command", "UPLOAD_EXCEPTION")
                        .put("uiMsgKey", mUiTempRawMessageKey)
                        .put("value", e.message)
                        .toString()
                )
            } catch (ex: JSONException) {
                ex.printStackTrace()
            }
            note.uploadError()
        }
        uploadInProgress = false
    }
}