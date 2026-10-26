package com.nova.assistant

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("nova", MODE_PRIVATE)
        val pad = (20 * resources.displayMetrics.density).toInt()
        tts = TextToSpeech(this) { ttsReady = it == TextToSpeech.SUCCESS }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        fun label(t: String) = TextView(this).apply {
            text = t; setTextColor(Color.parseColor("#7F93B8")); textSize = 12f; setPadding(0, pad / 2, 0, 0)
        }
        fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }

        root.addView(TextView(this).apply {
            text = "SETTINGS"; setTextColor(Color.parseColor("#00E5FF")); textSize = 26f
        })

        root.addView(label("WAKE WORD (type it in the language you will speak)"))
        val wake = EditText(this).apply {
            setText(prefs.getString("wake", "nova")); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); hint = "nova"
        }
        root.addView(wake)

        root.addView(label("PERSONALITY (optional)"))
        val persona = EditText(this).apply {
            setText(prefs.getString("persona", "")); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            hint = "e.g. concise and friendly"
        }
        root.addView(persona)

        root.addView(label("LANGUAGE (listening, speaking and replies)"))
        val langNames = VoiceConfig.LANGUAGES.keys.toList()
        val langSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, langNames)
            val cur = prefs.getString("lang", "en-US")
            setSelection(VoiceConfig.LANGUAGES.values.indexOf(cur).coerceAtLeast(0))
        }
        root.addView(langSpinner)

        root.addView(label("VOICE STYLE"))
        val styleNames = VoiceConfig.STYLES.keys.toList()
        val styleSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item, styleNames)
            val cur = prefs.getString("voice_style", "default")
            setSelection(VoiceConfig.STYLES.values.indexOf(cur).coerceAtLeast(0))
        }
        root.addView(styleSpinner)

        val pitchLabel = label("")
        val pitch = SeekBar(this).apply { max = 150; progress = ((prefs.getFloat("pitch", 1f) - 0.5f) * 100).toInt() }
        val rateLabel = label("")
        val rate = SeekBar(this).apply { max = 150; progress = ((prefs.getFloat("rate", 1f) - 0.5f) * 100).toInt() }
        fun refresh() {
            pitchLabel.text = "CUSTOM PITCH: %.2f".format(0.5f + pitch.progress / 100f)
            rateLabel.text = "CUSTOM SPEED: %.2f".format(0.5f + rate.progress / 100f)
        }
        val watcher = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { refresh() }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }
        pitch.setOnSeekBarChangeListener(watcher)
        rate.setOnSeekBarChangeListener(watcher)
        refresh()
        root.addView(pitchLabel); root.addView(pitch)
        root.addView(rateLabel); root.addView(rate)

        fun saveAll() {
            prefs.edit()
                .putString("wake", wake.text.toString().lowercase().trim().ifBlank { "nova" })
                .putString("persona", persona.text.toString().trim())
                .putString("lang", VoiceConfig.LANGUAGES[langNames[langSpinner.selectedItemPosition]] ?: "en-US")
                .putString("voice_style", VoiceConfig.STYLES[styleNames[styleSpinner.selectedItemPosition]] ?: "default")
                .putFloat("pitch", 0.5f + pitch.progress / 100f)
                .putFloat("rate", 0.5f + rate.progress / 100f)
                .apply()
        }

        root.addView(btn("Choose an installed voice pack") {
            saveAll()
            val t = tts
            if (t == null || !ttsReady) { Toast.makeText(this, "Voice engine is still loading, try again", Toast.LENGTH_SHORT).show(); return@btn }
            val voices = VoiceConfig.voicesFor(t, prefs.getString("lang", "en-US") ?: "en-US")
            if (voices.isEmpty()) { Toast.makeText(this, "No voices found for this language", Toast.LENGTH_LONG).show(); return@btn }
            AlertDialog.Builder(this)
                .setTitle("Pick a voice (sets style to Custom)")
                .setItems(voices.map { it.name }.toTypedArray()) { _, i ->
                    prefs.edit().putString("voice_name", voices[i].name).putString("voice_style", "custom").apply()
                    styleSpinner.setSelection(VoiceConfig.STYLES.values.indexOf("custom"))
                }
                .show()
        })
        root.addView(btn("Download more voice packs") {
            try { startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
            catch (e: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        })
        root.addView(btn("Test voice") {
            saveAll()
            val t = tts
            if (t != null && ttsReady) {
                VoiceConfig.apply(t, prefs)
                t.speak("Hello, I am NOVA, your voice assistant.", TextToSpeech.QUEUE_FLUSH, null, "test")
            }
        })
        root.addView(btn("Save") {
            saveAll()
            Toast.makeText(this, "Saved. Restart NOVA from the main screen to apply the language to listening.", Toast.LENGTH_LONG).show()
            finish()
        })

        val frame = FrameLayout(this)
        frame.addView(JarvisBackground(this))
        frame.addView(ScrollView(this).apply { addView(root) })
        setContentView(frame)
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
