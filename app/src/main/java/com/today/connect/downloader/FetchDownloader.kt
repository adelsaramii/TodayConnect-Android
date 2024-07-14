package com.today.connect.downloader


import android.content.Context
import android.util.Log
import com.tonyodev.fetch2.AbstractFetchListener
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.FetchObserver
import com.tonyodev.fetch2core.Reason
import com.today.connect.R
import com.today.connect.notification.DownloadNotification
import com.today.connect.services.DownloadService
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat


class FetchDownloader(
    s: DownloadService,
    address: String,
    destination: String,
    fileName: String?,
    currentRunID: Int
) :
    FetchObserver<Download?> {
    var mRequest: Request
    private val mFetch: Fetch
    private val note: DownloadNotification
    private val service: DownloadService
    val downloadAddress: String

    init {
        service = s
        downloadAddress = address
        val tempPath = "$destination$currentRunID.downloading"
        val fetchConfiguration: FetchConfiguration = FetchConfiguration.Builder(service)
            .setDownloadConcurrentLimit(20)
            .build()
        mFetch = Fetch.Impl.getInstance(fetchConfiguration)
        mRequest = Request(address, tempPath)
        mRequest.priority = Priority.HIGH
        mRequest.networkType = NetworkType.ALL
        note = DownloadNotification()
        note.setService(service)
        note.setUrl(address)
        note.setFilePath(destination)
        note.setFileTempPath(tempPath)
        note.setId(mRequest.id)
        val fetchListener: FetchListener = object : AbstractFetchListener() {
            override fun onProgress(
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long
            ) {
                super.onProgress(download, etaInMilliSeconds, downloadedBytesPerSecond)
                if (mRequest.id == download.id) {
                    updateProgress(
                        download.progress,
                        download.etaInMilliSeconds,
                        download.downloadedBytesPerSecond
                    )
                }
            }

            override fun onCompleted(download: Download) {
                super.onCompleted(download)
                if (mRequest.id == download.id) {
                    note.downloadCompleted()
                    removeFromList()
                }
            }
        }
        mFetch.addListener(fetchListener)
        mFetch.enableLogging(true).enqueue(mRequest, { request1 -> }) { error -> Log.e("download manager", error.toString())}
        try {
            val decodedName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name())
            note.setFileName(decodedName)
            note.sendNotification(decodedName, "Please Wait....")
        } catch (e: UnsupportedEncodingException) {
            // not going to happen - value came from JDK's own StandardCharsets
        }
    }

    val downloadID: Int
        get() = mRequest.id

    override fun onChanged(data: Download?, reason: Reason) {}
    fun removeFromList() {
        mFetch.remove(mRequest.id)
    }

    fun updateProgress(Progress: Int, Eta: Long, Speed: Long) {
        note.updateNotification(
            Progress,
            getETAString(service, Eta),
            getDownloadSpeedString(service, Speed)
        )
    }

    private fun getETAString(context: Context, etaInMilliSeconds: Long): String {
        if (etaInMilliSeconds < 0) {
            return ""
        }
        var seconds = (etaInMilliSeconds / 1000).toInt()
        val hours = (seconds / 3600).toLong()
        seconds -= (hours * 3600).toInt()
        val minutes = (seconds / 60).toLong()
        seconds -= (minutes * 60).toInt()
        return if (hours > 0) {
            context.getString(R.string.download_eta_hrs, hours, minutes, seconds)
        } else if (minutes > 0) {
            context.getString(R.string.download_eta_min, minutes, seconds)
        } else {
            context.getString(R.string.download_eta_sec, seconds)
        }
    }

    private fun getDownloadSpeedString(context: Context, downloadedBytesPerSecond: Long): String {
        if (downloadedBytesPerSecond < 0) {
            return ""
        }
        val kb = downloadedBytesPerSecond.toDouble() / 1000.0
        val mb = kb / 1000.0
        val decimalFormat = DecimalFormat(".##")
        return if (mb >= 1) {
            context.getString(R.string.download_speed_mb, decimalFormat.format(mb))
        } else if (kb >= 1) {
            context.getString(R.string.download_speed_kb, decimalFormat.format(kb))
        } else {
            context.getString(R.string.download_speed_bytes, downloadedBytesPerSecond)
        }
    }

    fun pauseDownload() {
        mFetch.pause(mRequest.id)
        note.pause()
    }

    fun cancelDownload() {
        note.cancelSelf()
        mFetch.delete(mRequest.id)
    }

    fun resumeDownload() {
        mFetch.resume(mRequest.id)
        note.resume()
    }
}
