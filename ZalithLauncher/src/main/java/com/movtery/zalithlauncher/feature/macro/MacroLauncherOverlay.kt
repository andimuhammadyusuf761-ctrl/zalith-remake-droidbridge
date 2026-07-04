package com.movtery.zalithlauncher.feature.macro

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.movtery.zalithlauncher.R

/**
 * Tiny draggable "M" chip + popup panel that shows enabled [Macro]s for one-tap activation
 * during gameplay.
 *
 * The overlay is intentionally implemented as raw views (no FloatingX dependency) because
 * it needs to be attached to MainActivity's content view as an aurora-styled chip and
 * survive across pause / resume cycles. The position is remembered in-process for the
 * activity's lifetime.
 *
 * It is meant to be installed once per MainActivity in onPostCreate via [install].
 */
object MacroLauncherOverlay {

    private const val TAG_OVERLAY = "aurora_macro_chip"
    private const val TAG_PANEL = "aurora_macro_panel"

    @JvmStatic
    @SuppressLint("ClickableViewAccessibility")
    fun install(activity: Activity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (root.findViewWithTag<View>(TAG_OVERLAY) != null) return

        val chip = makeChip(activity)
        chip.tag = TAG_OVERLAY

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            leftMargin = dp(activity, 12f)
            topMargin = dp(activity, 96f)
        }
        root.addView(chip, lp)

        var dragging = false
        var downX = 0f
        var downY = 0f
        var startLeft = 0
        var startTop = 0
        chip.setOnTouchListener(View.OnTouchListener { v, ev ->
            val params = v.layoutParams as FrameLayout.LayoutParams
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    startLeft = params.leftMargin
                    startTop = params.topMargin
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    if (!dragging && (dx * dx + dy * dy) > 64f) dragging = true
                    if (dragging) {
                        params.leftMargin = (startLeft + dx.toInt()).coerceIn(0, root.width - v.width)
                        params.topMargin = (startTop + dy.toInt()).coerceIn(0, root.height - v.height)
                        v.layoutParams = params
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!dragging) togglePanel(activity, v)
                    true
                }
                else -> false
            }
        })
    }

    private fun makeChip(context: Context): View {
        val tv = TextView(context).apply {
            text = "M"
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(dp(context, 14f), dp(context, 8f), dp(context, 14f), dp(context, 8f))
            background = chipBg(context)
            elevation = dp(context, 6f).toFloat()
        }
        return tv
    }

    private fun chipBg(context: Context): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 18f).toFloat()
            colors = intArrayOf(0xFF6366F1.toInt(), 0xFF8B5CF6.toInt(), 0xFFEC4899.toInt())
            orientation = GradientDrawable.Orientation.TL_BR
            setStroke(dp(context, 1f), 0x55FFFFFF)
        }
    }

    private fun togglePanel(activity: Activity, anchor: View) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val existing = root.findViewWithTag<View>(TAG_PANEL)
        if (existing != null) {
            root.removeView(existing)
            return
        }
        val macros = MacroStore.load(activity).filter { it.enabled }
        val panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = panelBg(activity)
            tag = TAG_PANEL
            setPadding(dp(activity, 10f), dp(activity, 8f), dp(activity, 10f), dp(activity, 8f))
            elevation = dp(activity, 8f).toFloat()
        }

        val header = TextView(activity).apply {
            text = activity.getString(R.string.macro_overlay_title)
            setTextColor(0xFFEDE9FE.toInt())
            textSize = 12f
            setPadding(0, 0, 0, dp(activity, 6f))
        }
        panel.addView(header)

        if (macros.isEmpty()) {
            panel.addView(TextView(activity).apply {
                text = activity.getString(R.string.macro_overlay_empty)
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(dp(activity, 4f), dp(activity, 4f), dp(activity, 4f), dp(activity, 4f))
            })
        } else {
            for (m in macros) {
                val running = MacroEngine.isRunning(m.id)
                val row = TextView(activity).apply {
                    text = (if (running) "■  " else "▶  ") + m.name
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    background = rowBg(activity, running)
                    setPadding(dp(activity, 12f), dp(activity, 8f), dp(activity, 12f), dp(activity, 8f))
                }
                val rowLp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(activity, 4f) }
                row.setOnClickListener {
                    if (MacroEngine.isRunning(m.id)) MacroEngine.stop(m.id)
                    else MacroEngine.run(m)
                    // Refresh: drop and reopen.
                    root.removeView(panel)
                    togglePanel(activity, anchor)
                }
                panel.addView(row, rowLp)
            }
            val stopAll = TextView(activity).apply {
                text = activity.getString(R.string.macro_overlay_stop_all)
                setTextColor(0xFFFCA5A5.toInt())
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dp(activity, 8f), dp(activity, 8f), dp(activity, 8f), dp(activity, 6f))
                setOnClickListener {
                    MacroEngine.stopAll()
                    root.removeView(panel)
                }
            }
            panel.addView(stopAll)
        }

        val anchorParams = anchor.layoutParams as FrameLayout.LayoutParams
        val panelLp = FrameLayout.LayoutParams(
            dp(activity, 220f),
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            leftMargin = (anchorParams.leftMargin + anchor.width + dp(activity, 6f))
                .coerceAtMost(root.width - dp(activity, 220f))
            topMargin = anchorParams.topMargin
        }
        root.addView(panel, panelLp)
    }

    private fun panelBg(context: Context): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 14f).toFloat()
            colors = intArrayOf(0xF01E1B4B.toInt(), 0xF02D2065.toInt())
            orientation = GradientDrawable.Orientation.TL_BR
            setStroke(dp(context, 1f), 0x66A5B4FC)
        }
    }

    private fun rowBg(context: Context, active: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 8f).toFloat()
            colorFilter = PorterDuffColorFilter(0x00000000, PorterDuff.Mode.SRC)
            colors = if (active) intArrayOf(0x33EC4899, 0x33A855F7)
                     else intArrayOf(0x33312E81, 0x33312E81)
            orientation = GradientDrawable.Orientation.LEFT_RIGHT
        }
    }

    private fun dp(context: Context, value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics
        ).toInt()
    }
}
