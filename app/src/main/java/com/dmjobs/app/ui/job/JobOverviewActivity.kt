package com.dmjobs.app.ui.job

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dmjobs.app.data.SessionManager
import com.dmjobs.app.databinding.ActivityJobOverviewBinding

class JobOverviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJobOverviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJobOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = SessionManager.activeSession ?: run { finish(); return }
        val job = session.job
        val contacts = session.contacts

        val estimatedMinutes = ((contacts.size * job.rateLimitSeconds) / 60.0).toInt()
        val totalPay = contacts.size * job.payPerDm

        binding.tvJobTitle.text = job.title
        binding.tvMessage.text = job.message
        binding.tvContacts.text = "${contacts.size} contacts"
        binding.tvPayPerDm.text = "₦${job.payPerDm}/DM"
        binding.tvTotalPay.text = "Up to ₦${String.format("%,.0f", totalPay)}"
        binding.tvEstTime.text = "~$estimatedMinutes min/day"
        binding.tvDailyLimit.text = "${job.maxPerDay} DMs/day"
        binding.tvDelay.text = "${job.rateLimitSeconds}s between messages"

        binding.btnAccept.setOnClickListener {
            startActivity(Intent(this, PermissionActivity::class.java))
        }

        binding.btnReject.setOnClickListener {
            SessionManager.activeSession = null
            finish()
        }
    }
}

