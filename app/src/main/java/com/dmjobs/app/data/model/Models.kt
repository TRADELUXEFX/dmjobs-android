package com.dmjobs.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Job(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    @SerialName("rate_limit_seconds") val rateLimitSeconds: Int = 10,
    @SerialName("max_per_day") val maxPerDay: Int = 30,
    @SerialName("pay_per_dm") val payPerDm: Double = 0.0,
    @SerialName("total_contacts") val totalContacts: Int = 0,
    val status: String = "active",
    @SerialName("job_code") val jobCode: String = "",
    @SerialName("dms_sent") val dmsSent: Int = 0,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Contact(
    val id: String = "",
    @SerialName("job_id") val jobId: String = "",
    @SerialName("phone_number") val phoneNumber: String = ""
)

@Serializable
data class WorkerUser(
    val id: String = "",
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val username: String = ""
)

@Serializable
data class MessageLog(
    @SerialName("job_id") val jobId: String,
    @SerialName("worker_id") val workerId: String,
    @SerialName("contact_id") val contactId: String,
    val success: Boolean,
    @SerialName("failure_reason") val failureReason: String? = null
)

@Serializable
data class WithdrawalRequest(
    @SerialName("worker_id") val workerId: String,
    @SerialName("job_id") val jobId: String,
    val amount: Double,
    @SerialName("dms_sent") val dmsSent: Int,
    @SerialName("bank_name") val bankName: String,
    @SerialName("account_number") val accountNumber: String,
    @SerialName("account_name") val accountName: String,
    val status: String = "pending"
)

@Serializable
data class Cancellation(
    @SerialName("worker_id") val workerId: String,
    @SerialName("job_id") val jobId: String,
    val reason: String,
    @SerialName("dms_sent_before_cancel") val dmsSentBeforeCancel: Int
)

@Serializable
data class Notification(
    @SerialName("related_job_id") val relatedJobId: String? = null,
    @SerialName("related_user_id") val relatedUserId: String? = null,
    val type: String,
    val message: String,
    @SerialName("is_read") val isRead: Boolean = false
)

// Session data held in memory across activities
data class JobSession(
    val job: Job,
    val contacts: List<Contact>,
    var currentIndex: Int = 0,
    var dmsSentToday: Int = 0,
    var totalDmsSent: Int = 0
)

