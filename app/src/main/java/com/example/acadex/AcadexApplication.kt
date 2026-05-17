package com.example.acadex

import android.app.Application
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
        appScope.launch {
            ProfileRepository.loadProfile()
            ResourceRepository.refreshFromSupabase()
        }
    }
}
