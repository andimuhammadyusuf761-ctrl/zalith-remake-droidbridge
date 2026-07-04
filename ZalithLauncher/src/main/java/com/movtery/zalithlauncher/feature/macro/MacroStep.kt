package com.movtery.zalithlauncher.feature.macro

/**
 * One step inside a macro sequence.
 *
 * - [type] decides what we send to the JVM via CallbackBridge.
 * - [keycode] is a GLFW keycode (used by [MacroStepType.KEY_TAP] / KEY_HOLD / KEY_RELEASE).
 * - [delayMs] is how long we wait AFTER firing this step before moving to the next.
 *
 * Mouse buttons reuse fixed [MacroStepType] entries (LEFT/RIGHT/MIDDLE) so the editor
 * stays simple and the user does not need to memorise GLFW button numbers.
 */
data class MacroStep(
    var type: MacroStepType = MacroStepType.KEY_TAP,
    var keycode: Int = 0,
    var delayMs: Long = 20L
) {
    /** Build a new step that is the deep copy of this one (used by the editor when duplicating). */
    fun copyOf(): MacroStep = MacroStep(type, keycode, delayMs)
}

enum class MacroStepType {
    /** Tap a keyboard key: send DOWN then UP back-to-back. */
    KEY_TAP,
    /** Press the key down. Release happens later via [KEY_RELEASE] or when the macro stops. */
    KEY_HOLD,
    /** Release a previously held key. */
    KEY_RELEASE,
    /** Tap left mouse button. */
    MOUSE_LEFT,
    /** Tap right mouse button. */
    MOUSE_RIGHT,
    /** Tap middle mouse button. */
    MOUSE_MIDDLE,
    /** Pure delay step (no input). [MacroStep.delayMs] becomes the wait. */
    DELAY
}
