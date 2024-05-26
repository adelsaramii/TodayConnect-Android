package ir.yekaan.darkoobnext.notification.lockScreen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ir.yekaan.darkoobnext.MainActivity
import ir.yekaan.darkoobnext.R


class RingtoneActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_ringtone)
        showWhenLockedAndTurnScreenOn()
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        )
        val caller: TextView = findViewById(R.id.callerName)
        val answer: Button = findViewById(R.id.answer)
        val reject: Button = findViewById(R.id.reject)

        caller.text = intent.extras?.getString("title") ?: "Unknown"

        answer.setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java).putExtra(
                    "URL",
                    intent.extras?.getString("URL")
                )
            )
        }

        reject.setOnClickListener {
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
}