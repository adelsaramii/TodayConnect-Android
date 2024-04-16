package ir.yekaan.darkoobnext.downloader

import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import ir.yekaan.darkoobnext.services.DownloadService

class DownloadsList {
    private val mDownloads: HashMap<Int, FetchDownloader> = HashMap()
    private val mOnGoingDownloads: HashMap<String?, FetchDownloader> = HashMap()

    private fun sendMsgToMainThread(str: String, messenger: Messenger?) {
        val msg = Message.obtain()
        msg.obj = str
        try {
            messenger!!.send(msg)
        } catch (e1: RemoteException) {
            Log.w(javaClass.name, "Exception sending message", e1)
        }
    }

    fun addDownloadItem(
        s: DownloadService?,
        address: String,
        destination: String,
        fileName: String?,
        messenger: Messenger?,
        currentRunID: Int
    ) {
        val downloadExists: FetchDownloader? = mOnGoingDownloads[address]
        if (downloadExists == null) {
            val newDownload = FetchDownloader(s!!, address, destination, fileName, currentRunID)
            mDownloads[newDownload.downloadID] = newDownload
            mOnGoingDownloads[address] = newDownload
            // update ui that its downloading
            sendMsgToMainThread("$address\$D@RK00B\$2", messenger)
        }
    }

    private fun getDownloadObject(downloadId: Int): FetchDownloader? {
        return if (downloadId != -1) {
            mDownloads[downloadId]
        } else null
    }

    fun updateDownloadItemStatus(command: String?, downloadId: Int, messenger: Messenger?) {
        val dlObject: FetchDownloader? = getDownloadObject(downloadId)
        if (dlObject != null) {
            when (command) {
                "pause" -> dlObject.pauseDownload()
                "resume" -> dlObject.resumeDownload()
                "cancel" -> {
                    mOnGoingDownloads.remove(dlObject.downloadAddress)
                    dlObject.cancelDownload()
                    mDownloads.remove(downloadId)
                    sendMsgToMainThread(
                        dlObject.downloadAddress + "\$D@RK00B$" + "3",
                        messenger
                    )
                }

                "done" -> {
                    mOnGoingDownloads.remove(dlObject.downloadAddress)
                    mDownloads.remove(downloadId)
                    // update ui that its downloading
                    sendMsgToMainThread(
                        dlObject.downloadAddress + "\$D@RK00B$" + "1",
                        messenger
                    )
                }
            }
        }
    }

    fun downloadsCount(): Int {
        return mDownloads.size
    }
}