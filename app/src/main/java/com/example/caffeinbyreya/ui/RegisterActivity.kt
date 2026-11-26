package com.example.caffeinbyreya.ui

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.caffeinbyreya.R
import com.example.caffeinbyreya.databinding.ActivityRegisterBinding
import com.example.caffeinbyreya.viewmodel.MainViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val nama = binding.etNama.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (nama.isEmpty()) {
                binding.etNama.error = getString(R.string.err_empty_name)
                return@setOnClickListener
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = getString(R.string.err_invalid_email)
                return@setOnClickListener
            }
            if (password.length < 6) {
                binding.etPassword.error = getString(R.string.err_short_password)
                return@setOnClickListener
            }

            viewModel.register(nama, email, password) { success ->
                if (success) {
                    Toast.makeText(this, getString(R.string.msg_register_success), Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, getString(R.string.err_email_exists), Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.tvToLogin.setOnClickListener {
            finish()
        }
    }
}