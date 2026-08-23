package com.dmjobs.app.data.repository

import com.dmjobs.app.data.SupabaseClient.client
import com.dmjobs.app.data.model.*
import io.github.jan.supabase.postgrest.postgrest

object JobRepository {

    suspend fun fetchJobByCode(code: String): Result<Job> = runCatching {
        val results = client.postgrest["wdmj_jobs"]
            .select { filter { eq("job_code", code.uppercase().trim()) } }
            .decodeList<Job>()
        results.firstOrNull() ?: error("Job code not found")
    }

    suspend fun fetchContacts(jobId: String): Result<List<Contact>> = runCatching {
        client.postgrest["wdmj_contacts"]
            .select { filter { eq("job_id", jobId) } }
            .decodeList<Contact>()
    }

    suspend fun logMessage(log: MessageLog): Result<Unit> = runCatching {
        client.postgrest["wdmj_message_logs"].insert(log)
    }

    suspend fun submitWithdrawal(request: WithdrawalRequest): Result<Unit> = runCatching {
        client.postgrest["wdmj_withdrawals"].insert(request)
        // Notify admin
        client.postgrest["wdmj_notifications"].insert(
            Notification(
                relatedJobId = request.jobId,
                relatedUserId = request.workerId,
                type = "withdrawal",
                message = "Worker requested withdrawal of ₦${request.amount.toLong()} for job."
            )
        )
    }

    suspend fun submitCancellation(cancellation: Cancellation): Result<Unit> = runCatching {
        client.postgrest["wdmj_cancellations"].insert(cancellation)
        // Notify admin
        client.postgrest["wdmj_notifications"].insert(
            Notification(
                relatedJobId = cancellation.jobId,
                relatedUserId = cancellation.workerId,
                type = "cancellation",
                message = "Worker cancelled job after sending ${cancellation.dmsSentBeforeCancel} DMs. Reason: ${cancellation.reason}"
            )
        )
    }

    suspend fun notifyJobComplete(workerId: String, jobId: String, dmsSent: Int): Result<Unit> = runCatching {
        client.postgrest["wdmj_notifications"].insert(
            Notification(
                relatedJobId = jobId,
                relatedUserId = workerId,
                type = "completion",
                message = "Worker completed job — $dmsSent DMs sent."
            )
        )
    }
}
