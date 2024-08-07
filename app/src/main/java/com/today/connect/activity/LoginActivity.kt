package com.today.connect.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.today.connect.R
import com.today.connect.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}