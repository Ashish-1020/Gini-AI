package com.aashu.offlinechatbot.data.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

interface Speaker {
    val isSpeaking: StateFlow<Boolean>

    fun speak(text: String)

    fun stop()
}

@Singleton
class AndroidSpeaker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : Speaker {
    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    private val pending = mutableListOf<String>()
    private val queued = AtomicInteger(0)
    private val nextId = AtomicInteger(0)

    override fun speak(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        queued.incrementAndGet()
        _isSpeaking.value = true
        val engine = ensureTts()
        synchronized(pending) {
            if (!ready) {
                pending += clean
                return
            }
        }
        engine.speak(clean, TextToSpeech.QUEUE_ADD, null, "utt-${nextId.incrementAndGet()}")
    }

    override fun stop() {
        synchronized(pending) { pending.clear() }
        queued.set(0)
        _isSpeaking.value = false
        tts?.stop()
    }

    private fun ensureTts(): TextToSpeech = tts ?: TextToSpeech(context) { status ->
        if (status != TextToSpeech.SUCCESS) {
            synchronized(pending) { pending.clear() }
            queued.set(0)
            _isSpeaking.value = false
            return@TextToSpeech
        }
        val engine = tts ?: return@TextToSpeech
        val result = engine.setLanguage(Locale.getDefault())
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finished()

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finished()
            override fun onError(utteranceId: String?, errorCode: Int) = finished()
        })
        synchronized(pending) {
            ready = true
            pending.forEach { engine.speak(it, TextToSpeech.QUEUE_ADD, null, "utt-${nextId.incrementAndGet()}") }
            pending.clear()
        }
    }.also { tts = it }

    private fun finished() {
        if (queued.decrementAndGet() <= 0) {
            queued.set(0)
            _isSpeaking.value = false
        }
    }
}
