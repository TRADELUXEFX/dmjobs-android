package com.dmjobs.app.ui.job

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dmjobs.app.data.SessionManager
import com.dmjobs.app.data.model.WithdrawalRequest
import com.dmjobs.app.data.repository.JobRepository
import com.dmjobs.app.databinding.ActivityJobCompleteBinding
import kotlinx.coroutines.launch

class JobCompleteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobCompleteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = SessionManager.activeSession
        val job = session?.job
        val dmsSent = session?.totalDmsSent ?: 0
        val earnings = dmsSent * (job?.payPerDm ?: 0.0)

        binding.tvDmsSent.text = "$dmsSent DMs sent"
        binding.tvEarnings.text = "₦${String.format("%,.0f", earnings)}"
        if (job != null) binding.tvJobTitle.text = job.title

        binding.btnSubmitWithdrawal.setOnClickListener { submitWithdrawal(dmsSent, earnings) }
    }

    private fun submitWithdrawal(dmsSent: Int, earnings: Double) {
        val bankName = binding.etBankName.text.toString().trim()
        val accountNumber = binding.etAccountNumber.text.toString().trim()
        val accountName = binding.etAccountName.text.toString().trim()

        if (bankName.isEmpty() || accountNumber.isEmpty() || accountName.isEmpty()) {
            binding.tvError.text = "Please fill in all bank details"
            binding.tvError.visibility = View.VISIBLE
            return
        }

        val workerId = SessionManager.getWorkerId(this) ?: return
        val jobId = SessionManager.activeSession?.job?.id ?: return

        binding.btnSubmitWithdrawal.isEnabled = false
        binding.btnSubmitWithdrawal.text = "Submitting…"

        lifecycleScope.launch {
            val result = JobRepository.submitWithdrawal(
                WithdrawalRequest(
                    workerId = workerId,
                    jobId = jobId,
                    amount = earnings,
                    dmsSent = dmsSent,
                    bankName = bankName,
                    accountNumber = accountNumber,
                    accountName = accountName
                )
            )
            result.fold(
                onSuccess = {
                    JobRepository.notifyJobComplete(workerId, jobId, dmsSent)
                    SessionManager.activeSession = null
                    binding.withdrawalForm.visibility = View.GONE
                    binding.tvSuccessMsg.visibility = View.VISIBLE
                    binding.btnDone.visibility = View.VISIBLE
                },
                onFailure = {
                    binding.tvError.text = "Failed to submit. Please try again."
                    binding.tvError.visibility = View.VISIBLE
                    binding.btnSubmitWithdrawal.isEnabled = true
                    binding.btnSubmitWithdrawal.text = "Submit Withdrawal Request"
                }
            )
        }
    }

    fun onDoneClick(view: View) {
        startActivity(Intent(this, JobCodeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        })
        finish()
    }
}

