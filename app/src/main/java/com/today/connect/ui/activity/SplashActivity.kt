package com.today.connect.ui.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.today.connect.R
import com.today.connect.ui.activity.adapter.ViewPagerAdapter
import com.today.connect.databinding.ActivityLoginBinding
import com.today.connect.databinding.ActivitySplashBinding
import java.util.Timer
import java.util.TimerTask

class SplashActivity : AppCompatActivity() {

    lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.activitySplashViewpager.adapter = ViewPagerAdapter()
        binding.activitySplashViewpager.offscreenPageLimit = 1
        binding.activitySplashViewpager.getChildAt(0).overScrollMode = ViewPager2.OVER_SCROLL_NEVER

        animPager()
        startChengePager()

    }

    private fun startChengePager() {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    binding.activitySplashViewpager.currentItem =
                        (binding.activitySplashViewpager.currentItem + 1) % binding.activitySplashViewpager.adapter?.itemCount!!
                }
            }
        }, 3000, 3000)
    }

    private fun animPager() {
        binding.activitySplashViewpager.setPageTransformer { page, position ->
            page.alpha = 0f
            page.visibility = View.VISIBLE
            page.alpha = 1f
        }
    }
}