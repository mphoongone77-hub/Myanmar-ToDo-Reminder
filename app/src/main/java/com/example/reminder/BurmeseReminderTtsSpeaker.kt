package com.example.reminder

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object BurmeseReminderTtsSpeaker {

    /**
     * Reads out task reminder details in Burmese when a notification triggers.
     */
    fun speakReminder(context: Context, title: String, notes: String, category: String) {
        val appContext = context.applicationContext

        // Burmese speech formatting
        val spokenText = buildString {
            append("သတိပေးချက်။ ")
            if (category.isNotBlank() && category != "အထွေထွေ") {
                append("$category။ ")
            }
            append("$title။ ")
            if (notes.isNotBlank()) {
                append("$notes။ ")
            }
            append("လုပ်ဆောင်ရန် အချိန်ရောက်ရှိပါပြီ။")
        }

        try {
            var tts: TextToSpeech? = null
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val myanmarLocale = Locale.forLanguageTag("my-MM")
                    val result = tts?.setLanguage(myanmarLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.language = Locale.getDefault()
                    }

                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d("BurmeseReminderTts", "Started speaking reminder: $utteranceId")
                        }

                        override fun onDone(utteranceId: String?) {
                            Log.d("BurmeseReminderTts", "Finished speaking reminder: $utteranceId")
                            try {
                                tts?.shutdown()
                            } catch (e: Exception) {
                                Log.e("BurmeseReminderTts", "Error shutting down TTS", e)
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            Log.e("BurmeseReminderTts", "TTS Error for utterance: $utteranceId")
                            try {
                                tts?.shutdown()
                            } catch (e: Exception) {
                                Log.e("BurmeseReminderTts", "Error shutting down TTS", e)
                            }
                        }
                    })

                    val params = Bundle().apply {
                        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                    }

                    tts?.speak(
                        spokenText,
                        TextToSpeech.QUEUE_FLUSH,
                        params,
                        "REMINDER_TTS_${System.currentTimeMillis()}"
                    )
                } else {
                    Log.e("BurmeseReminderTts", "Failed to initialize TextToSpeech, status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e("BurmeseReminderTts", "Error setting up TextToSpeech", e)
        }
    }
}
