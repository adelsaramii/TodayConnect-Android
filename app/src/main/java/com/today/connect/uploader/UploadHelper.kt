package com.today.connect.data.uploader

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.google.gson.Gson
import com.today.connect.ui.activity.MainActivity
import com.today.connect.R
import com.today.connect.services.UploadService
import com.today.connect.utils.Singleton

class UploadHelper(
    private val mMainActivity: MainActivity
) {
    val context = HashMap<String, UploadObject>()

    fun requestNewUpload(
        filePaths: Array<String?>,
        to: String,
        replyToInfo: String
    ): Array<String>{
        var newUploadIds: Array<String> = arrayOf()
        var tempUploadObject: SingleFileUpload
        var ret: Array<String> = arrayOf()
        filePaths.forEach {
            val newUpload = UploadObject(it!!, to, replyToInfo)
            context[newUpload.getFileId()] = newUpload
            newUploadIds += newUpload.getFileId()
        }
        if(newUploadIds.size == 1){
            tempUploadObject = SingleFileUpload(
                "single",
                newUploadIds[0],
                context[newUploadIds[0]]?.getLocalFileName(),
                context[newUploadIds[0]]?.getFileType(),
                context[newUploadIds[0]]?.getFileBlob(),
            )
            ret += Gson().toJson(tempUploadObject)
            return ret
        }
        newUploadIds.forEach {
            tempUploadObject = SingleFileUpload(
                "multiple",
                it,
                context[it]?.getLocalFileName(),
                context[it]?.getFileType(),
                context[it]?.getFileBlob(),
            )
            ret += Gson().toJson(tempUploadObject)
        }
        return ret
    }

    fun informNativeCodeAboutNewUploadCaption(command: String){
        val (fileID, caption) = Gson().fromJson(
            command,
            NativeCodeUploadCaption::class.java
        )

        if(context.contains(fileID)){
            if(caption != "N/A")
                context[fileID]?.setCaption(caption)
            mMainActivity.commandUIToPrepareMediaMessageAndMakeTempID(Gson().toJson(
                PrepareMediaMessageAndMakeTempID(
                    fileID,
                    context[fileID]?.getLocalFileName(),
                    context[fileID]?.getFileType(),
                    context[fileID]?.getFileModifiedAt(),
                    context[fileID]?.getFileBlob(),
                    context[fileID]?.getTargetUser(),
                    context[fileID]?.getReplyToInfo(),
                    context[fileID]?.getCaption(),
                    "${context[fileID]?.getFileSize()}",
                )
            ))
        }
    }

    fun getActiveUploads(){
        val i = Intent(
            mMainActivity.applicationContext,
            UploadService::class.java
        )
        i.putExtra("command", "getActiveUploads")
        mMainActivity.applicationContext.startService(i)
    }

    fun talkToUploadService(message: String){
        val i = Intent(
            mMainActivity.applicationContext,
            UploadService::class.java
        )
        i.putExtra("command", "talkToUploadService")
        i.putExtra("payload", message)
        mMainActivity.applicationContext.startService(i)
    }

    fun resumeActualUpload(command: String){
        val (
            fileID,
            hash,
            restMessageSendHash,
            fullPath,
            filename,
            range,
            tmpMsgId,
            targetDomain,
            tmpContent,
            uiTempRawMessageKey,
            tmpRawLocalMediaMessageJsonString,
            from
        ) = Gson().fromJson(
            command,
            ResumeActualUpload::class.java
        )
        if(context.contains(fileID)){
            context[fileID]?.setHash(hash)
            context[fileID]?.setRestMessageSendHash(restMessageSendHash)
            context[fileID]?.setFullPath(fullPath)
            context[fileID]?.setServerSideFileName(filename)
            context[fileID]?.setTmpMsgId(tmpMsgId)
            context[fileID]?.setTargetDomain(targetDomain)
            context[fileID]?.setRange(range)
            context[fileID]?.setTmpContent(tmpContent)
            context[fileID]?.setUiMsgKey(uiTempRawMessageKey)
            context[fileID]?.setTmpRawLocalMediaMessageJsonString(tmpRawLocalMediaMessageJsonString)
            context[fileID]?.setFrom(from)
            startUploadByFileID(fileID)
        }
    }

    var handler: Handler = object : Handler(Looper.getMainLooper()) {
        // handle service messages
        override fun handleMessage(msg: Message) {
            mMainActivity.informUIAboutUploadEvents(msg.obj.toString())
        }
    }

    private fun startUploadByFileID(
        fileID: String
    ): Boolean{
        if(context.contains(fileID)){
            val i = Intent(
                mMainActivity.applicationContext,
                UploadService::class.java
            )

            i.putExtra("resumableUploadEndpoint", mMainActivity.resources.getString(R.string.upload_url))
            i.putExtra("localFilePath", context[fileID]?.getLocalFilePath())
            i.putExtra("fileId", fileID)
            i.putExtra("hash", context[fileID]?.getServerSideHash())
            i.putExtra("fullPath", context[fileID]?.getFullPath())
            i.putExtra("filename", context[fileID]?.getLocalFileName())
            i.putExtra("tmpMsgId", context[fileID]?.getServerSideTmpMessageId())
            i.putExtra("targetDomain", context[fileID]?.getTargetDomain())
            i.putExtra("targetUser", context[fileID]?.getTargetUser())
            i.putExtra("tmpContent", context[fileID]?.getTmpContent())
            i.putExtra("uiTempRawMessageKey", context[fileID]?.getUiMessageKey())
            i.putExtra("token", Singleton.instance?.uiToken)
            i.putExtra("serverSideFilename", context[fileID]?.getServerSideFileName())
            i.putExtra("contentType", context[fileID]?.getFileContentType())
            i.putExtra("modifiedAt", context[fileID]?.getFileModifiedAt())
            i.putExtra("fileSize", "" + context[fileID]?.getFileSize())
            i.putExtra("range", context[fileID]?.getRange())
            i.putExtra("contentType", context[fileID]?.getFileContentType())
            i.putExtra("isUploadNative", "Yes/Android")
            i.putExtra("messenger", Messenger(handler))
            i.putExtra("from", context[fileID]?.getFrom())
            i.putExtra("restMessageSendHash", context[fileID]?.getRestMessageSendHash())
            i.putExtra("caption", context[fileID]?.getCaption())
            i.putExtra("tmpRawLocalMediaMessageJsonString",
                context[fileID]?.getTmpRawLocalMediaMessageJsonString())
            i.putExtra("command", "start")
            mMainActivity.applicationContext.startService(i)
            return true
        }
        return false
    }

    private fun cancelUploadByFileID(
        fileID: String
    ): Boolean{
        if(context.contains(fileID)){
            val i = Intent(
                mMainActivity.applicationContext,
                UploadService::class.java
            )
            i.putExtra("fileId", fileID)
            i.putExtra("action", "stop")
            mMainActivity.applicationContext.startService(i)
            return true
        }
        return false
    }

    private fun pauseUploadByFileID(
        fileID: String
    ): Boolean{
        if(context.contains(fileID)){
            val i = Intent(
                mMainActivity.applicationContext,
                UploadService::class.java
            )
            i.putExtra("fileId", fileID)
            i.putExtra("action", "stop")
            mMainActivity.applicationContext.startService(i)
            return true
        }
        return false
    }

    private fun resumeUploadByFileID(
        fileID: String
    ): Boolean{
        if(context.contains(fileID)){
            val i = Intent(
                mMainActivity.applicationContext,
                UploadService::class.java
            )
            i.putExtra("fileId", fileID)
            i.putExtra("action", "stop")
            mMainActivity.applicationContext.startService(i)
            return true
        }
        return false
    }

}