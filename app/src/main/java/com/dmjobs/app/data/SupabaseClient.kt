package com.dmjobs.app.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {
    const val SUPABASE_URL = "https://urvqtpmijkkofuetfeug.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_uxxL8wfW8sJ4SVApvlPxew_7KFuV4IC"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }
}

