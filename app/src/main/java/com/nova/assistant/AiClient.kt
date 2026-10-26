package com.nova.assistant

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {
    private const val MODEL = "claude-haiku-5-5"

    private val SYSTEM = """
You are NOVA, a hands-free voice assistant that operates an Android phone. Reply with ONLY a JSON object, no markdown:
{"speech":"short spoken reply, 1-3 sentences, no markdown","steps":[ ... ]}
Each step is an object with a "do" field:
- {"do":"open_app","app":"app name as shown on the home screen"}
- {"do":"send_message","app":"sms|whatsapp","to":"contact name or phone number","message":"full message text"}
- {"do":"search","app":"web|youtube|maps","query":"..."}
- {"do":"call","to":"contact name or number"}
- {"do":"click","text":"visible label or description of a button"}
- {"do":"type","text":"text to type in the focused field"}
- {"do":"back"}, {"do":"home"}, {"do":"wait","ms":1500}
For questions or information requests, answer in "speech" with an empty steps array. Only include steps the user asked for.
""".trimIndent()

    fun ask(apiKey: String, persona: String, command: String, language: String = "English"): JSONObject {
        try {
            val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.setRequestProperty("x-api-key", apiKey)
            c.setRequestProperty("anthropic-version", "2023-06-01")
            c.setRequestProperty("content-type", "application/json")
            c.doOutput = true
            c.connectTimeout = 15000
            c.readTimeout = 60000
            val sys = (if (persona.isBlank()) SYSTEM else SYSTEM + "\nPersonality: " + persona) +
                "\nAlways write the \"speech\" field in " + language + "."
            val body = JSONObject()
                .put("model", MODEL)
                .put("max_tokens", 1024)
                .put("system", sys)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", command)))
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream.bufferedReader().readText()
            if (code !in 200..299) return JSONObject().put("speech", "The AI service returned error $code.")
            val reply = JSONObject(text).getJSONArray("content").getJSONObject(0).getString("text")
            val s = reply.indexOf('{')
            val e = reply.lastIndexOf('}')
            return if (s >= 0 && e > s) JSONObject(reply.substring(s, e + 1)) else JSONObject().put("speech", reply)
        } catch (ex: Exception) {
            return JSONObject().put("speech", "I couldn't reach the AI service.")
        }
    }
}
