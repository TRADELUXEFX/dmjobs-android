package com.dmjobs.app.data.repository

import com.dmjobs.app.data.SupabaseClient.client
import com.dmjobs.app.data.model.WorkerUser
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AuthRepository {

    // Supabase auth uses email/password. We generate a fake email from username.
    private fun usernameToEmail(username: String) = "${username.lowercase().trim()}@dmjobs.internal"

    suspend fun signUp(
        username: String,
        fullName: String,
        phone: String,
        password: String
    ): Result<WorkerUser> = runCatching {
        // Check username not already taken
        val existing = client.postgrest["wdmj_users"]
            .select { filter { eq("username", username.lowercase().trim()) } }
            .decodeList<WorkerUser>()
        if (existing.isNotEmpty()) error("Username already taken")

        val email = usernameToEmail(username)

        // Create Supabase auth user
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }

        val userId = client.auth.currentUserOrNull()?.id ?: error("Signup failed")

        // Insert into wdmj_users
        val user = WorkerUser(
            id = userId,
            fullName = fullName,
            email = email,
            phone = phone,
            username = username.lowercase().trim()
        )
        client.postgrest["wdmj_users"].insert(user)
        user
    }

    suspend fun login(username: String, password: String): Result<WorkerUser> = runCatching {
        val email = usernameToEmail(username)
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = client.auth.currentUserOrNull()?.id ?: error("Login failed")

        client.postgrest["wdmj_users"]
            .select { filter { eq("id", userId) } }
            .decodeSingle<WorkerUser>()
    }

    suspend fun logout() {
        runCatching { client.auth.signOut() }
    }

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id
}

