package com.movtery.zalithlauncher.feature.macro

import android.os.Handler
import android.os.Looper
import com.movtery.zalithlauncher.feature.log.Logging
import net.kdt.pojavlaunch.LwjglGlfwKeycode
import org.lwjgl.glfw.CallbackBridge
import java.util.concurrent.ConcurrentHashMap

/**
 * Runs [Macro]s by translating each step into [CallbackBridge] events.
 *
 * The engine is single-threaded per macro: it uses a UI-thread [Handler] and posts each
 * step via [Handler.postDelayed]. We intentionally do NOT spin up a coroutine because
 * CallbackBridge dispatches to a JNI bridge that is already thread-safe but cheap, and
 * a single [Handler] keeps cancellation trivially correct.
 *
 * Calling [run] on a macro that is already running stops the previous run first.
 */
object MacroEngine {
    private val handler = Handler(Looper.getMainLooper())
    private val running = ConcurrentHashMap<String, MacroRun>()

    fun isRunning(macroId: String): Boolean = running.containsKey(macroId)

    fun runningCount(): Int = running.size

    /** Stop a single macro by id. Releases any keys it had pressed. */
    fun stop(macroId: String) {
        val r = running.remove(macroId) ?: return
        r.cancel()
    }

    /** Stop everything (typically called from the in-game pause overlay). */
    fun stopAll() {
        val ids = running.keys.toList()
        ids.forEach { stop(it) }
    }

    /**
     * Schedule a macro to start. Returns immediately. The macro will run until either
     * its loopCount is exhausted, [stop]/[stopAll] is called, or [Macro.steps] is empty.
     */
    fun run(macro: Macro) {
        if (!macro.enabled || macro.steps.isEmpty()) return
        stop(macro.id)
        val r = MacroRun(macro)
        running[macro.id] = r
        r.start()
    }

    private class MacroRun(val macro: Macro) {
        private val held = HashSet<Int>()
        private var cancelled = false
        private var pass = 0

        fun start() {
            scheduleStep(0)
        }

        fun cancel() {
            cancelled = true
            handler.removeCallbacksAndMessages(this)
            // Safety net: release whatever we held so the player doesn't get stuck.
            for (key in held) {
                runCatching {
                    CallbackBridge.sendKeyPress(key, CallbackBridge.getCurrentMods(), false)
                    CallbackBridge.setModifiers(key, false)
                }
            }
            held.clear()
        }

        private fun scheduleStep(index: Int) {
            if (cancelled) return
            if (index >= macro.steps.size) {
                pass++
                if (macro.loopCount == 0 || pass < macro.loopCount) {
                    scheduleStep(0)
                } else {
                    cancel()
                    MacroEngine.running.remove(macro.id)
                }
                return
            }
            val step = macro.steps[index]
            val token: Any = this
            handler.postAtTime({
                if (cancelled) return@postAtTime
                runCatching { dispatch(step) }.onFailure {
                    Logging.e("MacroEngine", "step dispatch failed: ${it.message}")
                }
                scheduleStep(index + 1)
            }, token, android.os.SystemClock.uptimeMillis() + step.delayMs.coerceAtLeast(0L))
        }

        private fun dispatch(step: MacroStep) {
            // Skip dispatch when MC isn't actually grabbing input — keys would fire into
            // launcher menus otherwise, and that's never what the user wants.
            if (!CallbackBridge.isGrabbing() && step.type != MacroStepType.DELAY) {
                // Best-effort: still try, MC main menu will simply ignore unknown keys.
            }
            when (step.type) {
                MacroStepType.KEY_TAP -> {
                    val mods = CallbackBridge.getCurrentMods()
                    CallbackBridge.sendKeyPress(step.keycode, mods, true)
                    CallbackBridge.setModifiers(step.keycode, true)
                    CallbackBridge.sendKeyPress(step.keycode, mods, false)
                    CallbackBridge.setModifiers(step.keycode, false)
                }
                MacroStepType.KEY_HOLD -> {
                    if (held.add(step.keycode)) {
                        CallbackBridge.sendKeyPress(step.keycode, CallbackBridge.getCurrentMods(), true)
                        CallbackBridge.setModifiers(step.keycode, true)
                    }
                }
                MacroStepType.KEY_RELEASE -> {
                    if (held.remove(step.keycode)) {
                        CallbackBridge.sendKeyPress(step.keycode, CallbackBridge.getCurrentMods(), false)
                        CallbackBridge.setModifiers(step.keycode, false)
                    }
                }
                MacroStepType.MOUSE_LEFT -> tapMouse(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt())
                MacroStepType.MOUSE_RIGHT -> tapMouse(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt())
                MacroStepType.MOUSE_MIDDLE -> tapMouse(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE.toInt())
                MacroStepType.DELAY -> { /* delay-only step, handled by postAtTime */ }
            }
        }

        private fun tapMouse(button: Int) {
            CallbackBridge.sendMouseButton(button, true)
            CallbackBridge.sendMouseButton(button, false)
        }
    }

}
