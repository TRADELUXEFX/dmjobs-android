package com.dmjobs.app.ui.job

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dmjobs.app.data.SessionManager
import com.dmjobs.app.databinding.ActivityMessagingProgressBinding
import com.dmjobs.app.service.MessagingService

class MessagingProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessagingProgressBinding

    companion object {
        const val ACTION_PROGRESS = "com.dmjobs.PROGRESS"
        const val EXTRA_SENT = "sent"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_EARNINGS = "earnings"
        const val EXTRA_DAILY_LIMIT_HIT = "daily_limit_hit"
        const val EXTRA_JOB_COMPLETE = "job_complete"
    }

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val sent = intent.getIntExtra(EXTRA_SENT, 0)
            val total = intent.getIntExtra(EXTRA_TOTAL, 0)
            val earnings = intent.getDoubleExtra(EXTRA_EARNINGS, 0.0)
            val dailyLimitHit = intent.getBooleanExtra(EXTRA_DAILY_LIMIT_HIT, false)
            val jobComplete = intent.getBooleanExtra(EXTRA_JOB_COMPLETE, false)

            updateProgress(sent, total, earnings)

            when {
                jobComplete -> {
                    startActivity(Intent(this@MessagingProgressActivity, JobCompleteActivity::class.java))
                    finish()
                }
                dailyLimitHit -> showDailyLimitDialog(sent, total, earnings)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagingProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = SessionManager.activeSession ?: run { finish(); return }

        updateProgress(session.totalDmsSent, session.contacts.size, session.totalDmsSent * session.job.payPerDm)

        // Start foreground service
        val serviceIntent = Intent(this, MessagingService::class.java)
        startForegroundService(serviceIntent)

        binding.btnPause.setOnClickListener {
            sendBroadcast(Intent(MessagingService.ACTION_PAUSE))
            binding.btnPause.visibility = View.GONE
            binding.btnResume.visibility = View.VISIBLE
            binding.tvStatus.text = "Paused"
        }

        binding.btnResume.setOnClickListener {
            sendBroadcast(Intent(MessagingService.ACTION_RESUME))
            binding.btnResume.visibility = View.GONE
            binding.btnPause.visibility = View.VISIBLE
            binding.tvStatus.text = "Sending messages…"
        }

        binding.btnCancel.setOnClickListener { confirmCancel() }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(progressReceiver, IntentFilter(ACTION_PROGRESS))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver)
    }

    private fun updateProgress(sent: Int, total: Int, earnings: Double) {
        binding.tvSent.text = "$sent"
        binding.tvTotal.text = "of $total"
        binding.tvEarnings.text = "₦${String.format("%,.0f", earnings)}"
        val pct = if (total > 0) (sent * 100 / total) else 0
        binding.progressBar.progress = pct
        binding.tvPercent.text = "$pct%"
        binding.tvStatus.text = "Sending messages…"
    }

    private fun confirmCancel() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Job?")
            .setMessage("Are you sure you want to cancel? You'll need to submit a reason.")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                sendBroadcast(Intent(MessagingService.ACTION_STOP))
                startActivity(Intent(this, CancellationActivity::class.java))
                finish()
            }
            .setNegativeButton("Keep Going", null)
            .show()
    }

    private fun showDailyLimitDialog(sent: Int, total: Int, earnings: Double) {
        AlertDialog.Builder(this)
            .setTitle("Daily Limit Reached")
            .setMessage("You've sent $sent DMs today. Come back tomorrow to continue.\n\nEarnings so far: ₦${String.format("%,.0f", earnings)}")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onBackPressed() {
        // Prevent accidental back press stopping the job
        AlertDialog.Builder(this)
            .setTitle("Leave screen?")
            .setMessage("Messaging will continue in the background.")
            .setPositiveButton("Leave") { _, _ -> super.onBackPressed() }
            .setNegativeButton("Stay", null)
            .show()
    }
}

