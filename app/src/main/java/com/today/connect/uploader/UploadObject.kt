package com.today.connect.data.uploader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.text.TextUtils
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64.getEncoder
import kotlin.math.max

@RequiresApi(Build.VERSION_CODES.O)
class UploadObject(
    filePath: String,
    to: String,
    replyToInfo: String
) {
    private val file = File(filePath)
    private var fileId: String = "N/A"
    private var contentType: String = "N/A"
    private var filename: String = "N/A"
    private var blob: String = "N/A"
    private var fileSize: Long = -1

    private var jsonUploadRequestObj: JsonUploadRequestData = Gson().fromJson("{}", JsonUploadRequestData::class.java)

    private fun getMimeType(filePath: String): String {
        val extension = getSuffix(filePath)
        if (!TextUtils.isEmpty(extension)) {
            val mime = MimeTypeMap.getSingleton()
            val type: String? = mime.getMimeTypeFromExtension(extension)
            if(type != null)
                return type
        }
        return "file/*"
    }

    private fun getSuffix(filePath: String): String? {
        if (TextUtils.isEmpty(filePath)) {
            return null
        }
        val dotPos = filePath.lastIndexOf('.')
        return if (dotPos >= 0 && dotPos < filePath.length - 1) {
            filePath.substring(dotPos + 1)
        } else null
    }

    init{
        if (file.exists()) {
            filename = file.name
            fileSize = file.length()
            fileId = (file.lastModified() + file.length()).toString()

            contentType = getMimeType(filePath)

            if(contentType.contains("image/") || contentType.contains("video/")){

                val bitmap: Bitmap = if(contentType.contains("image/")){
                    BitmapFactory.decodeFile(filePath)
                } else {
                    ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND)!!
                }

                var ratio = max(bitmap.width, bitmap.height) / 320.0
                if(ratio <= 0){
                    ratio = 1.0
                }

                val bmp = Bitmap.createScaledBitmap(bitmap, (bitmap.width/ratio).toInt(), (bitmap.height/ratio).toInt(), false)
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, stream)
                val image = stream.toByteArray()
                blob = getEncoder().encodeToString(image)

                // make json string answer
                // make a list about all uploads
                // make json string answer
                // make a list about all uploads
            }
            jsonUploadRequestObj.blob = blob
            jsonUploadRequestObj.modifiedAt = "${file.lastModified()}"
            jsonUploadRequestObj.to = to
            jsonUploadRequestObj.replyToInfo = replyToInfo
            jsonUploadRequestObj.fileId = fileId
            jsonUploadRequestObj.fileSize = fileSize
            jsonUploadRequestObj.contentType = contentType
            jsonUploadRequestObj.filename = filename
            jsonUploadRequestObj.uiMsgKey = "N/A"
            jsonUploadRequestObj.tmpRawLocalMediaMessageJsonString = "N/A"
            jsonUploadRequestObj.filePath = filePath
            jsonUploadRequestObj.status = Status.BOOT
            jsonUploadRequestObj.progress = 0
            jsonUploadRequestObj.caption = "N/A"
            jsonUploadRequestObj.hash = "N/A"
            jsonUploadRequestObj.restMessageSendHash = "N/A"
            jsonUploadRequestObj.fullPath = "N/A"
            jsonUploadRequestObj.range = "N/A"
            jsonUploadRequestObj.tmpMsgId = "N/A"
            jsonUploadRequestObj.tmpContent = "N/A"
            jsonUploadRequestObj.targetDomain = "N/A"
            jsonUploadRequestObj.serverSideFileName = "N/A"
            jsonUploadRequestObj.from = "N/A"
        }
    }

    // actions

    fun run(){
        jsonUploadRequestObj.status = Status.UPLOADING
    }


    fun pause(){
        jsonUploadRequestObj.status = Status.PAUSED
    }


    fun cancel(){
        jsonUploadRequestObj.status = Status.CANCELED
    }


    fun error(){
        jsonUploadRequestObj.status = Status.ERROR
    }


    fun resume(){
        jsonUploadRequestObj.status = Status.RESUMED
    }

    // setters

    fun setProgress(progress: Int?){
        if (progress != null) {
            jsonUploadRequestObj.progress = progress
        }
    }

    fun setRange(range: String?){
        if (range != null) {
            jsonUploadRequestObj.range = range
        }
    }

    fun setUiMsgKey(messageID: String?){
        if (messageID != null) {
            jsonUploadRequestObj.uiMsgKey = messageID
        }
    }

    fun setTmpRawLocalMediaMessageJsonString(rawMsg: String?){
        if (rawMsg != null) {
            jsonUploadRequestObj.tmpRawLocalMediaMessageJsonString = rawMsg
        }
    }

    fun setFrom(from: String?){
        if (from != null) {
            jsonUploadRequestObj.from = from
        }
    }

    fun setHash(hash: String?){
        if (hash != null) {
            jsonUploadRequestObj.hash = hash
        }
    }

    fun setRestMessageSendHash(restMessageSendHash: String?){
        if (restMessageSendHash != null) {
            jsonUploadRequestObj.restMessageSendHash = restMessageSendHash
        }
    }

    fun setFullPath(fullPath: String?){
        if (fullPath != null) {
            jsonUploadRequestObj.fullPath = fullPath
        }
    }

    fun setTmpMsgId(tmpMsgId: String?){
        if (tmpMsgId != null) {
            jsonUploadRequestObj.tmpMsgId = tmpMsgId
        }
    }

    fun setTargetDomain(targetDomain: String?){
        if (targetDomain != null) {
            jsonUploadRequestObj.targetDomain = targetDomain
        }
    }

    fun setTmpContent(tmpContent: String?){
        if (tmpContent != null) {
            jsonUploadRequestObj.tmpContent = tmpContent
        }
    }

    fun setServerSideFileName(serverSideFileName: String?){
        if (serverSideFileName != null) {
            jsonUploadRequestObj.serverSideFileName = serverSideFileName
        }
    }

    fun setCaption(caption: String?){
        if (caption != null) {
            jsonUploadRequestObj.caption = caption
        }
    }

    // getters

    fun getLocalFilePath(): String{
        return jsonUploadRequestObj.filePath
    }

    fun getFrom(): String{
        return jsonUploadRequestObj.from
    }

    fun getFullPath(): String{
        return jsonUploadRequestObj.fullPath
    }

    fun getLocalFileName(): String{
        return jsonUploadRequestObj.filename
    }

    fun getServerSideHash(): String{
        return jsonUploadRequestObj.hash
    }

    fun getRestMessageSendHash(): String{
        return jsonUploadRequestObj.restMessageSendHash
    }

    fun getTmpRawLocalMediaMessageJsonString(): String{
        return jsonUploadRequestObj.tmpRawLocalMediaMessageJsonString
    }

    fun getServerSideFileName(): String{
        return jsonUploadRequestObj.serverSideFileName
    }

    fun getFileContentType(): String{
        return jsonUploadRequestObj.contentType
    }

    fun getRange(): String{
        return jsonUploadRequestObj.range
    }

    fun getFileType(): String{
        return jsonUploadRequestObj.contentType
    }

    fun getFileModifiedAt(): String{
        return jsonUploadRequestObj.modifiedAt
    }

    fun getFileId(): String{
        return fileId
    }

    fun getFileBlob(): String{
        return jsonUploadRequestObj.blob
    }

    fun getTargetUser(): String{
        return jsonUploadRequestObj.to
    }

    fun getUiMessageKey(): String{
        return jsonUploadRequestObj.uiMsgKey
    }

    fun getTargetDomain(): String{
        return jsonUploadRequestObj.targetDomain
    }

    fun getTmpContent(): String{
        return jsonUploadRequestObj.tmpContent
    }

    fun getReplyToInfo(): String{
        return jsonUploadRequestObj.replyToInfo
    }

    fun getCaption(): String{
        return jsonUploadRequestObj.caption
    }

    fun getServerSideTmpMessageId(): String{
        return jsonUploadRequestObj.tmpMsgId
    }

    fun getFileSize(): Long{
        return jsonUploadRequestObj.fileSize
    }

    fun healthCheck(): Boolean{
        if(
            fileId == "N/A" ||
            contentType == "N/A" ||
            filename == "N/A" ||
            blob == "N/A"
        ) return false
        return true
    }

    fun getWVUploadInitRequest(): String{
        return Gson().toJson(jsonUploadRequestObj)
    }

}