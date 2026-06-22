package com.myschedule.id

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage

object SupabaseInstance {
    val client = createSupabaseClient(
        supabaseUrl = "https://jymozvmymdrepizcilfy.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imp5bW96dm15bWRyZXBpemNpbGZ5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcxNzc3MDcsImV4cCI6MjA5Mjc1MzcwN30.EILxkICi48FK9X6yey8YcvXwBcx2raXgX_mvd9Y9iFk"
    ) {
        install(Storage)
    }
}