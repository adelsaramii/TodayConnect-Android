package com.today.connect.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.today.connect.R
import com.today.connect.databinding.ActivityWebViewBinding
import com.today.connect.state.GlobalState

class WebViewActivity : AppCompatActivity() {
    lateinit var binding: ActivityWebViewBinding
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GlobalState.init(this)
        Log.d(
            "onCreate: ",
            "https://todayconnect.yekaan.ir/?username=${GlobalState.username}&token=${GlobalState.accessToken}"
        )
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.webViewClient = WebViewClient()
        binding.webView.loadUrl("https://todayconnect.yekaan.ir?username=${GlobalState.username}&token=${GlobalState.accessToken}")
    }
}