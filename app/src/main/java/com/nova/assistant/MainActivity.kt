package com.nova.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("nova", MODE_PRIVATE)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        fun label(t: String) = TextView(this).apply {
            text = t; setTextColor(Color.parseColor("#7F93B8")); textSize = 12f; setPadding(0, pad / 2, 0, 0)
        }
        fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }

        root.addView(TextView(this).apply {
            text = "NOVA"; setTextColor(Color.parseColor("#00E5FF")); textSize = 34f
        })
        val status = TextView(this).apply { setTextColor(Color.WHITE); text = "Stopped" }
        root.addView(status)

        root.addView(label("ANTHROPIC API KEY"))
        val key = EditText(this).apply {
            hint = "sk-ant-..."; setText(prefs.getString("api_key", "") ?: "")
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(key)

        root.addView(btn("Settings (wake word, language, voice)") {
            startActivity(Intent(this, SettingsActivity::class.java))
        })
        root.addView(btn("1. Grant mic, contacts, notifications") {
            val p = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= 33) p.add(Manifest.permission.POST_NOTIFICATIONS)
            requestPermissions(p.toTypedArray(), 1)
        })
        root.addView(btn("2. Enable NOVA accessibility service") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(btn("3. Allow display over other apps") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(btn("4. Disable battery optimization") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        root.addView(btn("Save & start NOVA") {
            prefs.edit().putString("api_key", key.text.toString().trim()).apply()
            stopService(Intent(this, VoiceService::class.java))
            startForegroundService(Intent(this, VoiceService::class.java))
            status.text = "Running. Say \"" + prefs.getString("wake", "nova") + "\" then a command."
        })
        root.addView(btn("Stop NOVA") {
            stopService(Intent(this, VoiceService::class.java))
            status.text = "Stopped"
        })

        val frame = FrameLayout(this)
        frame.addView(JarvisBackground(this))
        frame.addView(ScrollView(this).apply { addView(root) })
        setContentView(frame)
    }
}
