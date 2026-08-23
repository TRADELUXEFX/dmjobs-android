package com.dmjobs.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dmjobs.app.data.SessionManager
import com.dmjobs.app.data.repository.AuthRepository
import com.dmjobs.app.databinding.ActivityLoginBinding
import com.dmjobs.app.ui.job.JobCodeActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            showError("Enter your username and password"); return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = AuthRepository.login(username, password)
            setLoading(false)
            result.fold(
                onSuccess = { user ->
                    SessionManager.saveWorker(this@LoginActivity, user.id, user.fullName, user.username)
                    startActivity(Intent(this@LoginActivity, JobCodeActivity::class.java))
                    finish()
                },
                onFailure = { showError("Invalid username or password") }
            )
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "Signing in…" else "Sign In"
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.tvError.visibility = View.GONE
    }
}

