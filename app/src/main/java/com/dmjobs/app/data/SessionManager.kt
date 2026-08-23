package com.dmjobs.app.data

import android.content.Context
import android.content.SharedPreferences
import com.dmjobs.app.data.model.JobSession

object SessionManager {

    private const val PREFS_NAME = "dmjobs_prefs"
    private const val KEY_WORKER_ID = "worker_id"
    private const val KEY_WORKER_NAME = "worker_name"
    private const val KEY_WORKER_USERNAME = "worker_username"

    // In-memory job session shared between activities and the messaging service
    var activeSession: JobSession? = null

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveWorker(ctx: Context, id: String, name: String, username: String) {
        prefs(ctx).edit()
            .putString(KEY_WORKER_ID, id)
            .putString(KEY_WORKER_NAME, name)
            .putString(KEY_WORKER_USERNAME, username)
            .apply()
    }

    fun getWorkerId(ctx: Context): String? = prefs(ctx).getString(KEY_WORKER_ID, null)
    fun getWorkerName(ctx: Context): String? = prefs(ctx).getString(KEY_WORKER_NAME, null)
    fun getWorkerUsername(ctx: Context): String? = prefs(ctx).getString(KEY_WORKER_USERNAME, null)

    fun isLoggedIn(ctx: Context): Boolean = getWorkerId(ctx) != null

    fun clearWorker(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}

