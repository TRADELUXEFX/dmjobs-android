package com.dmjobs.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dmjobs.app.data.SessionManager
import com.dmjobs.app.data.repository.AuthRepository
import com.dmjobs.app.databinding.ActivitySignupBinding
import com.dmjobs.app.ui.job.JobCodeActivity
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener { attemptSignUp() }
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun attemptSignUp() {
        val username  = binding.etUsername.text.toString().trim()
        val fullName  = binding.etFullName.text.toString().trim()
        val phone     = binding.etPhone.text.toString().trim()
        val password  = binding.etPassword.text.toString()
        val confirm   = binding.etConfirmPassword.text.toString()

        if (username.isEmpty() || fullName.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields"); return
        }
        if (password.length < 6) {
            showError("Password must be at least 6 characters"); return
        }
        if (password != confirm) {
            showError("Passwords do not match"); return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = AuthRepository.signUp(username, fullName, phone, password)
            setLoading(false)
            result.fold(
                onSuccess = { user ->
                    SessionManager.saveWorker(this@SignUpActivity, user.id, user.fullName, user.username)
                    startActivity(Intent(this@SignUpActivity, JobCodeActivity::class.java))
                    finishAffinity()
                },
                onFailure = { showError(it.message ?: "Sign up failed") }
            )
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignup.isEnabled = !loading
        binding.btnSignup.text = if (loading) "Creating account…" else "Create Account"
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.tvError.visibility = View.GONE
    }
}

