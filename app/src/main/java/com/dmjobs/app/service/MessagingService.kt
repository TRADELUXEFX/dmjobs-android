package com.dmjobs.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dmjobs.app.DMJobsApp
import com.dmjobs.app.R
import com.dmjobs.app.data.SessionManager
import com.dmjobs.app.data.model.MessageLog
import com.dmjobs.app.data.repository.JobRepository
import com.dmjobs.app.ui.job.MessagingProgressActivity
import com.dmjobs.app.ui.job.MessagingProgressActivity.Companion.ACTION_PROGRESS
import com.dmjobs.app.ui.job.MessagingProgressActivity.Companion.EXTRA_DAILY_LIMIT_HIT
import com.dmjobs.app.ui.job.MessagingProgressActivity.Companion.EXTRA_EARNINGS
import com.dmjobs.app.ui.job.MessagingProgressActivity.Companion.EXTRA_JOB_COMPLETE
import com.dmjobs.app.ui.job.MessagingProgressActivity.Companion.EXTRA_SENT
import com.dmjobs.app.ui.job.MessagingProgressActivity.Companion.EXTRA_TOTAL
import kotlinx.coroutines.*

class MessagingService : Service() {

    companion object {
        const val NOTIF_ID = 1001
        const val ACTION_PAUSE = "com.dmjobs.PAUSE"
        const val ACTION_RESUME = "com.dmjobs.RESUME"
        const val ACTION_STOP = "com.dmjobs.STOP"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isPaused = false
    private var isStopped = false

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PAUSE  -> isPaused = true
                ACTION_RESUME -> isPaused = false
                ACTION_STOP   -> { isStopped = true; stopSelf() }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(ACTION_PAUSE)
            addAction(ACTION_RESUME)
            addAction(ACTION_STOP)
        }
        registerReceiver(controlReceiver, filter)
        startForeground(NOTIF_ID, buildNotification("Starting…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { runMessagingLoop() }
        return START_NOT_STICKY
    }

    private suspend fun runMessagingLoop() {
        val session = SessionManager.activeSession ?: run { stopSelf(); return }
        val job = session.job
        val contacts = session.contacts
        val workerId = SessionManager.getWorkerId(this) ?: run { stopSelf(); return }

        while (session.currentIndex < contacts.size && !isStopped) {
            // Pause check
            while (isPaused && !isStopped) delay(500)
            if (isStopped) break

            // Daily limit check
            if (session.dmsSentToday >= job.maxPerDay) {
                broadcastProgress(session.totalDmsSent, contacts.size,
                    session.totalDmsSent * job.payPerDm, dailyLimitHit = true)
                stopSelf()
                return
            }

            val contact = contacts[session.currentIndex]
            val encodedMsg = Uri.encode(job.message)
            val phone = contact.phoneNumber.replace(Regex("[^0-9+]"), "")

            // Open WhatsApp via deep link
            val waIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/$phone?text=$encodedMsg")).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            var sendSuccess = false
            var failReason: String? = null

            try {
                // Tell accessibility service to watch for send button
                WhatsAppAccessibilityService.waitingForSend = true
                var sendConfirmed = false
                WhatsAppAccessibilityService.onSendTapped = { sendConfirmed = true }

                startActivity(waIntent)

                // Wait up to 8 seconds for WhatsApp to open and send button to be tapped
                val waitStart = System.currentTimeMillis()
                while (!sendConfirmed && System.currentTimeMillis() - waitStart < 8000) {
                    delay(200)
                }

                if (sendConfirmed) {
                    sendSuccess = true
                } else {
                    WhatsAppAccessibilityService.waitingForSend = false
                    failReason = "Send button not found within timeout"
                }
            } catch (e: Exception) {
                failReason = e.message ?: "Unknown error"
                WhatsAppAccessibilityService.waitingForSend = false
            }

            // Log to Supabase
            JobRepository.logMessage(
                MessageLog(
                    jobId = job.id,
                    workerId = workerId,
                    contactId = contact.id,
                    success = sendSuccess,
                    failureReason = failReason
                )
            )

            session.currentIndex++
            if (sendSuccess) {
                session.dmsSentToday++
                session.totalDmsSent++
            }

            val earnings = session.totalDmsSent * job.payPerDm
            broadcastProgress(session.totalDmsSent, contacts.size, earnings)
            updateNotification("${session.totalDmsSent}/${contacts.size} sent · ₦${earnings.toLong()}")

            // Wait between DMs (only after a real send attempt)
            if (session.currentIndex < contacts.size && !isStopped) {
                delay(job.rateLimitSeconds * 1000L)
            }
        }

        if (!isStopped) {
            // All contacts done
            broadcastProgress(
                session.totalDmsSent, contacts.size,
                session.totalDmsSent * job.payPerDm, jobComplete = true
            )
        }
        stopSelf()
    }

    private fun broadcastProgress(
        sent: Int, total: Int, earnings: Double,
        dailyLimitHit: Boolean = false, jobComplete: Boolean = false
    ) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(ACTION_PROGRESS).apply {
                putExtra(EXTRA_SENT, sent)
                putExtra(EXTRA_TOTAL, total)
                putExtra(EXTRA_EARNINGS, earnings)
                putExtra(EXTRA_DAILY_LIMIT_HIT, dailyLimitHit)
                putExtra(EXTRA_JOB_COMPLETE, jobComplete)
            }
        )
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MessagingProgressActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, DMJobsApp.CHANNEL_ID)
            .setContentTitle("DM Jobs — Sending messages")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        unregisterReceiver(controlReceiver)
        WhatsAppAccessibilityService.waitingForSend = false
        WhatsAppAccessibilityService.onSendTapped = null
    }
}

