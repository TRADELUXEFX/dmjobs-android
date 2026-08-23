package com.dmjobs.app.ui.job

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dmjobs.app.data.SessionManager
import com.dmjobs.app.data.model.Cancellation
import com.dmjobs.app.data.repository.JobRepository
import com.dmjobs.app.databinding.ActivityCancellationBinding
import kotlinx.coroutines.launch

class CancellationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCancellationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCancellationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = SessionManager.activeSession
        binding.tvDmsSentSoFar.text = "DMs sent before cancelling: ${session?.totalDmsSent ?: 0}"

        binding.btnSubmitCancel.setOnClickListener { submitCancellation() }
    }

    private fun submitCancellation() {
        val reason = binding.etReason.text.toString().trim()
        if (reason.isEmpty()) {
            binding.tvError.text = "Please enter a reason"
            binding.tvError.visibility = View.VISIBLE
            return
        }

        val workerId = SessionManager.getWorkerId(this) ?: return
        val session = SessionManager.activeSession ?: return
        val jobId = session.job.id
        val dmsSent = session.totalDmsSent

        binding.btnSubmitCancel.isEnabled = false
        binding.btnSubmitCancel.text = "Submitting…"

        lifecycleScope.launch {
            JobRepository.submitCancellation(
                Cancellation(
                    workerId = workerId,
                    jobId = jobId,
                    reason = reason,
                    dmsSentBeforeCancel = dmsSent
                )
            )
            SessionManager.activeSession = null
            startActivity(Intent(this@CancellationActivity, JobCodeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            finish()
        }
    }
}

