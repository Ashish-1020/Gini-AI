package com.aashu.offlinechatbot.di

import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import com.aashu.offlinechatbot.data.llm.LiteRtLlmEngine
import com.aashu.offlinechatbot.data.llm.LlmEngine
import com.aashu.offlinechatbot.data.repository.DownloadManagerModelRepository
import com.aashu.offlinechatbot.data.repository.ModelRepository
import com.aashu.offlinechatbot.data.voice.AndroidSpeaker
import com.aashu.offlinechatbot.data.voice.AndroidSpeechToText
import com.aashu.offlinechatbot.data.voice.Speaker
import com.aashu.offlinechatbot.data.voice.SpeechToText
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDownloadManager(@ApplicationContext context: Context): DownloadManager =
        context.getSystemService(DownloadManager::class.java)

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("offline_chat_prefs", Context.MODE_PRIVATE)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: DownloadManagerModelRepository): ModelRepository

    @Binds
    @Singleton
    abstract fun bindLlmEngine(impl: LiteRtLlmEngine): LlmEngine

    @Binds
    abstract fun bindSpeechToText(impl: AndroidSpeechToText): SpeechToText

    @Binds
    @Singleton
    abstract fun bindSpeaker(impl: AndroidSpeaker): Speaker
}
