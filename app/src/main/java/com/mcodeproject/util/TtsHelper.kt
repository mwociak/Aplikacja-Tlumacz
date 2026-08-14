package com.mcodeproject.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in API 21+")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    Log.e("TtsHelper", "TTS error code: $errorCode")
                }
            })
        } else {
            Log.e("TtsHelper", "TTS Initialization failed")
        }
    }

    fun speak(text: String, languageCode: String) {
        if (!isInitialized || text.isBlank()) return

        val locale = if (languageCode.lowercase() == "pl") {
            Locale.forLanguageTag("pl-PL")
        } else {
            Locale.US
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TtsHelper", "Language $languageCode is not supported")
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_trans_id")
        }
    }

    /** Zatrzymuje bieżące odtwarzanie TTS i resetuje stan. */
    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        _isSpeaking.value = false
    }
}
