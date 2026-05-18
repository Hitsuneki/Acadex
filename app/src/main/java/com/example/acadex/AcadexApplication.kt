package com.example.acadex

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.example.acadex.data.ResourceRepository
import com.example.acadex.data.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AcadexApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Post heavy initialization to run AFTER the main looper finishes the
        // current Application-bind message.  Without this delay, the coroutine
        // launch triggers class verification for Supabase/Ktor/kotlinx-serialization
        // on the main thread during Application.onCreate(), blocking it for 5+ seconds
        // and causing an ANR that force-closes the app before any Activity can start.
        Handler(Looper.getMainLooper()).post {
            appScope.launch {
                try {
                    ProfileRepository.loadProfile()
                } catch (_: Exception) {
                    // Profile load failure is non-fatal at startup;
                    // it will be retried when the user reaches a screen that needs it.
                }
                try {
                    ResourceRepository.refreshFromSupabase()
                } catch (_: Exception) {
                    // Same — non-fatal; the home screen will retry on its own.
                }
            }
        }
    }
}
