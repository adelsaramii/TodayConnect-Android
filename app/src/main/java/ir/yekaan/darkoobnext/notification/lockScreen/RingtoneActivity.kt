package ir.yekaan.darkoobnext.notification.lockScreen

import android.app.KeyguardManager
import android.app.KeyguardManager.KeyguardLock
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ir.yekaan.darkoobnext.MainActivity
import ir.yekaan.darkoobnext.R
import ir.yekaan.darkoobnext.notification.MyObject
import ir.yekaan.darkoobnext.notification.Ring


class RingtoneActivity : AppCompatActivity() {

    private var keyguardLock: KeyguardLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_ringtone)
        showWhenLockedAndTurnScreenOn()
        super.onCreate(savedInstanceState)
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardLock = keyguardManager.newKeyguardLock("TAG")
        keyguardLock!!.disableKeyguard()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        val caller: TextView = findViewById(R.id.callerName)
        val answer: ImageView = findViewById(R.id.answer)
        val reject: ImageView = findViewById(R.id.reject)

        caller.text = intent.extras?.getString("title") ?: "Unknown"

        answer.setOnClickListener {
            applicationContext.stopService(Intent(this, Ring::class.java))
            MyObject.ring?.runnable?.invoke()
            startActivity(
                Intent(this, MainActivity::class.java).putExtra(
                    "URL",
                    intent.extras?.getString("URL")
                )
            )
        }

        reject.setOnClickListener {
            applicationContext.stopService(Intent(this, Ring::class.java))
            MyObject.ring?.runnable?.invoke()
            finish()
        }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (keyguardLock != null) {
            keyguardLock!!.reenableKeyguard()
        }
    }

}