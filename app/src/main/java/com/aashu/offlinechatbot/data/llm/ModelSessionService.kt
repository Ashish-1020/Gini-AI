package com.aashu.offlinechatbot.data.llm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ModelSessionService : Service() {
    @Inject lateinit var llmEngine: LlmEngine

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        llmEngine.close()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            try {
                context.startService(Intent(context, ModelSessionService::class.java))
            } catch (e: IllegalStateException) {
                Log.w("ModelSessionService", "Couldn't start session watcher", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ModelSessionService::class.java))
        }
    }
}
