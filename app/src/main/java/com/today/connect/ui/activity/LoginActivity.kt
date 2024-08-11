package com.today.connect.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.common.api.Response
import com.today.connect.R
import com.today.connect.databinding.ActivityLoginBinding
import com.today.connect.remote.ApiClient
import com.today.connect.remote.RetrofitInstant
import com.today.connect.remote.models.TokenRequest
import com.today.connect.remote.models.TokenResponse
import com.today.connect.state.GlobalState
import com.today.connect.ui.activity.adapter.ViewPagerAdapter
import com.today.connect.utils.Contents
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Timer
import java.util.TimerTask

class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setOnClickListener()
        // init shared preferences
        GlobalState.init(this)

        allViewPagerConfig()

        checkLogin()

    }

    private fun setOnClickListener() {
        binding.loginButton.setOnClickListener(onClickListenerLogin())
        binding.loginPasswordShow.setOnClickListener(onClickShowAndHidePassword())
    }

    private fun allViewPagerConfig() {
        startChengePager()
        animPager()
        configPager()
    }

    private fun checkLogin() {
        if (!GlobalState.accessToken.isNullOrBlank()) {
            goToMain()
        }
    }


    private fun onClickShowAndHidePassword(): View.OnClickListener {
        return View.OnClickListener {
            if (binding.loginPassword.inputType == 129) {
                binding.loginPassword.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.loginPasswordShow.setImageResource(R.drawable.round_visibility_24)
            } else {
                binding.loginPassword.inputType = 129
                binding.loginPasswordShow.setImageResource(R.drawable.baseline_visibility_off_24)
            }
            binding.loginPassword.setSelection(binding.loginPassword.text.length)
        }

    }

    private fun onClickListenerLogin(): View.OnClickListener {
        return View.OnClickListener {
            if (userAndPasswordIsNotNull()) {
                loading(true)
                RetrofitInstant.apiClient.getAccessToken(
                    password = binding.loginPassword.text.toString(),
                    username = binding.loginUsername.text.toString()
                ).enqueue(object : Callback<TokenResponse> {
                    override fun onResponse(
                        p0: Call<TokenResponse>,
                        p1: retrofit2.Response<TokenResponse>
                    ) {
                        if (p1.isSuccessful) {
                            val tokenResponse = p1.body()
                            if (tokenResponse != null) {
                                loginIsSuccessful(tokenResponse)
                                loading(false)
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Token response is null",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loading(false)
                            }
                        } else {
                            if (p1.code() == 400) {
                                Toast.makeText(
                                    this@LoginActivity,
                                    resources.getString(R.string.error_400),
                                    Toast.LENGTH_SHORT
                                ).show()
                                loading(false)
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    p1.errorBody()?.string(),
                                    Toast.LENGTH_SHORT
                                ).show()
                                loading(false)
                            }
                        }
                    }

                    override fun onFailure(p0: Call<TokenResponse>, p1: Throwable) {
                        Toast.makeText(
                            this@LoginActivity,
                            p1.message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                })
            } else {
                Toast.makeText(
                    this@LoginActivity,
                    "username or password is empty",
                    Toast.LENGTH_SHORT
                ).show()

                binding.loginPassword.error = "password is empty"
                binding.loginUsername.error = "username is empty"
            }
        }
    }

    private fun loginIsSuccessful(tokenResponse: TokenResponse) {
        GlobalState.init(this@LoginActivity)
        GlobalState.accessToken = tokenResponse.accessToken
        GlobalState.username = binding.loginUsername.text.toString()
        goToMain()
    }

    private fun loading(b: Boolean) {
        if (b) {
            binding.loginProgressBar.visibility = View.VISIBLE
            binding.loginButton.text = ""
        } else {
            binding.loginProgressBar.visibility = View.GONE
            binding.loginButton.text = "Login"
        }

    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun userAndPasswordIsNotNull(): Boolean {
        if (binding.loginUsername.text.toString()
                .isNotEmpty() && binding.loginPassword.text.toString().isNotEmpty()
        ) {
            return true
        } else {
            return false
        }
    }

    private fun startChengePager() {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    binding.activityLoginViewpager.currentItem =
                        (binding.activityLoginViewpager.currentItem + 1) % binding.activityLoginViewpager.adapter?.itemCount!!
                }
            }
        }, 3000, 3000)
    }

    private fun animPager() {
        binding.activityLoginViewpager.setPageTransformer { page, position ->
            page.alpha = 0f
            page.visibility = View.VISIBLE
            page.alpha = 1f
        }
    }

    private fun configPager() {
        binding.activityLoginViewpager.adapter = ViewPagerAdapter()
        binding.activityLoginViewpager.offscreenPageLimit = 1
        binding.activityLoginViewpager.getChildAt(0).overScrollMode = ViewPager2.OVER_SCROLL_NEVER

        val dotsIndicator = binding.dotsIndicator
        val viewPager = binding.activityLoginViewpager
        val adapter = ViewPagerAdapter()
        viewPager.adapter = adapter
        dotsIndicator.attachTo(viewPager)


    }
}


