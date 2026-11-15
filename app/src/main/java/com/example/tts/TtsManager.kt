package com.example.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val usResult = engine.setLanguage(Locale.US)
            if (usResult == TextToSpeech.LANG_MISSING_DATA || usResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.UK)
            }
            engine.setSpeechRate(0.95f)
            _isReady.value = true
        } else {
            _isReady.value = false
        }
    }

    fun speak(text: String): Boolean {
        val clean = text.trim()
        if (clean.isEmpty() || !_isReady.value) return false
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "eh_tts_${System.currentTimeMillis()}")
        return true
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
    }
}
