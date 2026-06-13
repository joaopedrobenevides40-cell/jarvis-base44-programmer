package com.jarvis.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.jarvis.android.core.JarvisCore
import com.jarvis.android.data.local.JarvisDatabase
import com.jarvis.android.data.remote.JarvisApiClient
import com.jarvis.android.memory.ContextMemoryManager

class JarvisApp : Application() {

    companion object {
        lateinit var instance: JarvisApp
            private set
        const val CHANNEL_ID = "jarvis_overlay"
        const val CHANNEL_NAME = "J.A.R.V.I.S Active"
    }

    lateinit var core: JarvisCore
    lateinit var memory: ContextMemoryManager
    lateinit var apiClient: JarvisApiClient
    lateinit var database: JarvisDatabase

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeCore()
        createNotificationChannel()
    }

    private fun initializeCore() {
        database = JarvisDatabase.getInstance(this)
        apiClient = JarvisApiClient.getInstance()
        memory = ContextMemoryManager(database, apiClient)
        core = JarvisCore(this, memory, apiClient)
        core.initialize()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "J.A.R.V.I.S está ativo e monitorando"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
