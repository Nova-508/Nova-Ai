package com.nova.assistant

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import org.json.JSONObject
import java.net.URLEncoder

object Actions {
    fun run(ctx: Context, plan: JSONObject, say: (String) -> Unit) {
        val speech = plan.optString("speech")
        if (speech.isNotBlank()) say(speech)
        val steps = plan.optJSONArray("steps") ?: return
        for (i in 0 until steps.length()) {
            val s = steps.optJSONObject(i) ?: continue
            try { exec(ctx, s) } catch (e: Exception) { }
            Thread.sleep(1800)
        }
    }

    private fun launch(ctx: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val c: Context = NovaAccessibilityService.instance ?: ctx
        c.startActivity(intent)
    }

    private fun exec(ctx: Context, s: JSONObject) {
        val a11y = NovaAccessibilityService.instance
        when (s.optString("do")) {
            "open_app" -> openApp(ctx, s.optString("app"))
            "send_message" -> sendMessage(ctx, s)
            "search" -> search(ctx, s)
            "call" -> {
                val n = numberFor(ctx, s.optString("to"))
                if (n != null) launch(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n")))
            }
            "click" -> a11y?.clickText(s.optString("text"))
            "type" -> a11y?.typeText(s.optString("text"))
            "back" -> a11y?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            "home" -> a11y?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            "wait" -> Thread.sleep(s.optLong("ms", 1500))
        }
    }

    private fun openApp(ctx: Context, name: String) {
        val pm = ctx.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(q, 0).firstOrNull {
            it.loadLabel(pm).toString().lowercase().contains(name.lowercase())
        } ?: return
        val li = pm.getLaunchIntentForPackage(match.activityInfo.packageName) ?: return
        launch(ctx, li)
    }

    private fun numberFor(ctx: Context, who: String): String? {
        if (who.isBlank()) return null
        if (who.count { it.isDigit() } >= 6) return who.filter { it.isDigit() || it == '+' }
        if (ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val col = ContactsContract.CommonDataKinds.Phone.NUMBER
        val cur = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(col),
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?", arrayOf("%$who%"), null
        )
        cur?.use {
            if (it.moveToFirst()) return it.getString(0).filter { ch -> ch.isDigit() || ch == '+' }
        }
        return null
    }

    private fun sendMessage(ctx: Context, s: JSONObject) {
        val app = s.optString("app", "sms").lowercase()
        val msg = s.optString("message")
        val num = numberFor(ctx, s.optString("to"))
        if (app.contains("whatsapp")) {
            val u = "https://wa.me/" + (num?.removePrefix("+") ?: "") + "?text=" + URLEncoder.encode(msg, "UTF-8")
            launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(u)))
        } else {
            val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + (num ?: ""))).putExtra("sms_body", msg)
            launch(ctx, i)
        }
        Thread.sleep(3000)
        NovaAccessibilityService.instance?.clickText("send")
    }

    private fun search(ctx: Context, s: JSONObject) {
        val q = URLEncoder.encode(s.optString("query"), "UTF-8")
        val url = when (s.optString("app")) {
            "youtube" -> "https://www.youtube.com/results?search_query=$q"
            "maps" -> "https://www.google.com/maps/search/$q"
            else -> "https://www.google.com/search?q=$q"
        }
        launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
