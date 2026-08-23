package com.dmjobs.app.ui.job

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dmjobs.app.data.SessionManager
import com.dmjobs.app.data.model.JobSession
import com.dmjobs.app.data.repository.AuthRepository
import com.dmjobs.app.data.repository.JobRepository
import com.dmjobs.app.databinding.ActivityJobCodeBinding
import com.dmjobs.app.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class JobCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = SessionManager.getWorkerName(this) ?: "Worker"
        binding.tvGreeting.text = "Hi, ${name.split(" ").first()}"

        binding.btnEnter.setOnClickListener { lookupCode() }
        binding.tvLogout.setOnClickListener { logout() }
    }

    private fun lookupCode() {
        val code = binding.etJobCode.text.toString().trim().uppercase()
        if (code.length < 4) {
            showError("Enter a valid job code"); return
        }

        setLoading(true)
        lifecycleScope.launch {
            val jobResult = JobRepository.fetchJobByCode(code)
            jobResult.fold(
                onSuccess = { job ->
                    if (job.status != "active") {
                        setLoading(false)
                        showError("This job is no longer active (${job.status})")
                        return@fold
                    }
                    val contactsResult = JobRepository.fetchContacts(job.id)
                    contactsResult.fold(
                        onSuccess = { contacts ->
                            if (contacts.isEmpty()) {
                                setLoading(false)
                                showError("This job has no contacts")
                                return@fold
                            }
                            SessionManager.activeSession = JobSession(
                                job = job,
                                contacts = contacts
                            )
                            setLoading(false)
                            startActivity(Intent(this@JobCodeActivity, JobOverviewActivity::class.java))
                        },
                        onFailure = {
                            setLoading(false)
                            showError("Failed to load contacts")
                        }
                    )
                },
                onFailure = {
                    setLoading(false)
                    showError("Job code not found")
                }
            )
        }
    }

    private fun logout() {
        lifecycleScope.launch {
            AuthRepository.logout()
            SessionManager.clearWorker(this@JobCodeActivity)
            startActivity(Intent(this@JobCodeActivity, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnEnter.isEnabled = !loading
        binding.btnEnter.text = if (loading) "Looking up…" else "Enter Job"
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.tvError.visibility = View.GONE
    }
}

