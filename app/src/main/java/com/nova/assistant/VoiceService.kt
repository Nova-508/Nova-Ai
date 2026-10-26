package com.nova.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var awaitingUntil = 0L
    private var busy = false
    private var speaking = false
    private var running = true

    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        val ch = "nova"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(ch, "NOVA", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, ch)
            .setContentTitle("NOVA is listening")
            .setContentText("Say your wake word, then a command")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(1, n)

        tts = TextToSpeech(this) { st ->
            ttsReady = st == TextToSpeech.SUCCESS
            if (ttsReady) tts?.let { VoiceConfig.apply(it, getSharedPreferences("nova", MODE_PRIVATE)) }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) { resume() }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) { resume() }
            })
        }
        main.post { startListening() }
    }

    private fun resume() {
        speaking = false
        main.post { startListening() }
    }

    private fun say(text: String) {
        main.post {
            if (!ttsReady) return@post
            speaking = true
            recognizer?.cancel()
            tts?.let { VoiceConfig.apply(it, getSharedPreferences("nova", MODE_PRIVATE)) }
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "nova")
        }
    }

    private fun startListening() {
        if (!running || speaking || busy) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        recognizer?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
            override fun onError(e: Int) {
                val delay = if (e == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1500L else 500L
                main.postDelayed({ startListening() }, delay)
            }
            override fun onResults(b: Bundle?) {
                val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                handle(t)
            }
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, VoiceConfig.speechTag(getSharedPreferences("nova", MODE_PRIVATE).getString("lang", "en-US") ?: "en-US"))
        r.startListening(i)
    }

    private fun handle(text: String) {
        val prefs = getSharedPreferences("nova", MODE_PRIVATE)
        val wake = prefs.getString("wake", "nova")!!.lowercase().trim()
        val low = text.lowercase()
        if (System.currentTimeMillis() < awaitingUntil && text.isNotBlank()) {
            awaitingUntil = 0
            runCommand(text)
            return
        }
        val idx = low.indexOf(wake)
        if (idx < 0) { startListening(); return }
        val cmd = text.substring(idx + wake.length).trim(' ', ',', '.', '!', '?')
        if (cmd.isNotBlank()) runCommand(cmd)
        else {
            awaitingUntil = System.currentTimeMillis() + 8000
            say("Yes?")
        }
    }

    private fun runCommand(cmd: String) {
        busy = true
        val prefs = getSharedPreferences("nova", MODE_PRIVATE)
        val key = prefs.getString("api_key", "") ?: ""
        val persona = prefs.getString("persona", "") ?: ""
        val lang = VoiceConfig.replyLanguage(prefs.getString("lang", "en-US") ?: "en-US")
        Thread {
            if (key.isBlank()) {
                say("Please add your API key in the NOVA app.")
            } else {
                val plan = AiClient.ask(key, persona, cmd, lang)
                Actions.run(this, plan) { say(it) }
            }
            busy = false
            main.post { if (!speaking) startListening() }
        }.start()
    }

    override fun onDestroy() {
        running = false
        recognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}
