package com.nova.assistant

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class NovaAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: NovaAccessibilityService? = null
    }

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun find(n: AccessibilityNodeInfo, t: String): AccessibilityNodeInfo? {
        val a = n.text?.toString()?.lowercase() ?: ""
        val b = n.contentDescription?.toString()?.lowercase() ?: ""
        if (a.contains(t) || b.contains(t)) return n
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            val r = find(c, t)
            if (r != null) return r
        }
        return null
    }

    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        var node: AccessibilityNodeInfo? = find(root, text.lowercase())
        while (node != null) {
            if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node = node.parent
        }
        return false
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }
}
