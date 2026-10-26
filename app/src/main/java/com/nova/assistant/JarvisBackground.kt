package com.nova.assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random

/** Animated Jarvis-style HUD background: glow, grid, rotating rings, drifting particles, scan line. */
class JarvisBackground(ctx: Context) : View(ctx) {
    private class P(var x: Float, var y: Float, val vx: Float, val vy: Float, val r: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val start = System.nanoTime()
    private var last = start
    private val cyan = Color.rgb(0, 229, 255)
    private val violet = Color.rgb(139, 92, 246)
    private val ps = List(55) {
        P(Random.nextFloat(), Random.nextFloat(),
            (Random.nextFloat() - 0.5f) * 0.03f, (Random.nextFloat() - 0.5f) * 0.03f,
            1.5f + Random.nextFloat() * 2.5f)
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return
        val now = System.nanoTime()
        val t = (now - start) / 1e9f
        val dt = ((now - last) / 1e9f).coerceAtMost(0.1f)
        last = now

        c.drawColor(Color.rgb(5, 7, 15))

        // soft glow behind the HUD
        val cx = w / 2f
        val cy = h * 0.28f
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, max(w, h) * 0.75f,
            intArrayOf(Color.argb(80, 139, 92, 246), Color.argb(30, 0, 229, 255), Color.argb(0, 0, 0, 0)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // grid
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.argb(22, 0, 229, 255)
        val step = 60f
        val off = (t * 12f) % step
        var x = -off
        while (x < w) { c.drawLine(x, 0f, x, h, paint); x += step }
        var y = off
        while (y < h) { c.drawLine(0f, y, w, y, paint); y += step }

        // rotating HUD rings
        val base = minOf(w, h) * 0.42f
        for (i in 0 until 4) {
            val r = base * (0.45f + i * 0.2f)
            rect.set(cx - r, cy - r, cx + r, cy + r)
            paint.strokeWidth = if (i % 2 == 0) 3f else 1.5f
            paint.color = if (i % 2 == 0) Color.argb(150, 0, 229, 255) else Color.argb(120, 139, 92, 246)
            val dir = if (i % 2 == 0) 1f else -1f
            val rot = t * (18f + i * 12f) * dir
            for (a in 0 until 3) {
                c.drawArc(rect, rot + a * 120f, 70f - i * 8f, false, paint)
            }
        }

        // drifting particles + links
        paint.style = Paint.Style.FILL
        for (p in ps) {
            p.x += p.vx * dt; p.y += p.vy * dt
            if (p.x < 0f) p.x = 1f; if (p.x > 1f) p.x = 0f
            if (p.y < 0f) p.y = 1f; if (p.y > 1f) p.y = 0f
        }
        paint.strokeWidth = 1f
        for (i in ps.indices) {
            for (j in i + 1 until ps.size) {
                val d = hypot((ps[i].x - ps[j].x) * w, (ps[i].y - ps[j].y) * h)
                if (d < 120f) {
                    paint.color = Color.argb(((1f - d / 120f) * 70).toInt(), 0, 229, 255)
                    c.drawLine(ps[i].x * w, ps[i].y * h, ps[j].x * w, ps[j].y * h, paint)
                }
            }
        }
        for (p in ps) {
            paint.color = Color.argb(170, 0, 229, 255)
            c.drawCircle(p.x * w, p.y * h, p.r, paint)
        }

        // scan line
        val sy = ((t * 0.12f) % 1f) * h
        paint.color = Color.argb(45, 0, 229, 255)
        c.drawRect(0f, sy - 2f, w, sy + 2f, paint)

        postInvalidateOnAnimation()
    }
}
