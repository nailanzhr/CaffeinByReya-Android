package com.example.caffeinbyreya.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.caffeinbyreya.MainActivity
import com.example.caffeinbyreya.R
import com.example.caffeinbyreya.data.SessionManager
import com.example.caffeinbyreya.databinding.ActivityLoginBinding
import com.example.caffeinbyreya.viewmodel.MainViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)

        if (session.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = getString(R.string.err_invalid_email)
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.etPassword.error = getString(R.string.err_short_password)
                return@setOnClickListener
            }

            viewModel.login(email, password) { user ->
                if (user != null) {
                    session.saveSession(user.nama, user.email)
                    Toast.makeText(this, getString(R.string.msg_login_success), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.err_login_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}