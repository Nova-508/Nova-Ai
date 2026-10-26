package com.nova.assistant

import android.content.SharedPreferences
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

object VoiceConfig {
    /** language name -> BCP-47 tag */
    val LANGUAGES = linkedMapOf(
        "English (US)" to "en-US", "English (UK)" to "en-GB", "English (India)" to "en-IN", "Hinglish (Hindi + English)" to "hinglish",
        "Hindi" to "hi-IN", "Tamil" to "ta-IN", "Telugu" to "te-IN", "Bengali" to "bn-IN",
        "Marathi" to "mr-IN", "Spanish" to "es-ES", "French" to "fr-FR", "German" to "de-DE",
        "Italian" to "it-IT", "Portuguese" to "pt-BR", "Arabic" to "ar-SA", "Japanese" to "ja-JP",
        "Korean" to "ko-KR", "Chinese" to "zh-CN", "Russian" to "ru-RU"
    )

    val STYLES = linkedMapOf(
        "Default" to "default",
        "Human female" to "female",
        "Human male" to "male",
        "Robot" to "robot",
        "Jarvis (deep, calm)" to "jarvis",
        "Custom (pitch, speed, voice pack)" to "custom"
    )

    fun localeOf(tag: String): Locale = if (tag == "hinglish") Locale("en", "IN") else Locale.forLanguageTag(tag)
    fun speechTag(tag: String): String = if (tag == "hinglish") "en-IN" else tag
    fun replyLanguage(tag: String): String =
        if (tag == "hinglish") "Hinglish (Hindi written in English letters, mixed naturally with English words)"
        else Locale.forLanguageTag(tag).displayLanguage

    private fun pick(voices: List<Voice>, female: Boolean?): Voice? {
        if (female == null) return null
        return voices.firstOrNull {
            val n = it.name.lowercase()
            if (female) n.contains("female") else (n.contains("male") && !n.contains("female"))
        }
    }

    fun apply(tts: TextToSpeech, p: SharedPreferences) {
        val locale = localeOf(p.getString("lang", "en-US") ?: "en-US")
        tts.setLanguage(locale)
        val style = p.getString("voice_style", "default") ?: "default"
        var pitch = 1f
        var rate = 1f
        when (style) {
            "robot" -> { pitch = 0.5f; rate = 0.9f }
            "jarvis" -> { pitch = 0.8f; rate = 0.95f }
            "custom" -> { pitch = p.getFloat("pitch", 1f); rate = p.getFloat("rate", 1f) }
        }
        tts.setPitch(pitch)
        tts.setSpeechRate(rate)

        val all: List<Voice> = try { (tts.voices ?: emptySet()).toList() } catch (e: Exception) { emptyList() }
        val same = all.filter { it.locale.language == locale.language }
        var chosen: Voice? = null
        when (style) {
            "female" -> chosen = pick(same, true)
            "male", "jarvis" -> chosen = pick(same, false)
            "custom" -> {
                val name = p.getString("voice_name", "") ?: ""
                if (name.isNotBlank()) chosen = all.firstOrNull { it.name == name }
            }
        }
        if (chosen != null) tts.setVoice(chosen)
    }

    fun voicesFor(tts: TextToSpeech, langTag: String): List<Voice> {
        val lang = localeOf(langTag).language
        return try { (tts.voices ?: emptySet()).filter { it.locale.language == lang }.sortedBy { it.name } }
        catch (e: Exception) { emptyList() }
    }
}
