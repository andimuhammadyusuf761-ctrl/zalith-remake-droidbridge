package com.movtery.zalithlauncher.feature.macro

import java.util.UUID

/**
 * A user-defined macro.
 *
 * @property id stable identifier (UUID string) so we can reference a macro from overlays / triggers.
 * @property name human-readable label shown in the launcher chip and the editor list.
 * @property steps ordered list of input events with delays.
 * @property loopCount how many times to run the whole sequence. 0 means "run forever until stopped".
 * @property enabled if false the macro is hidden from the in-game launcher overlay.
 */
data class Macro(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "Macro",
    var steps: MutableList<MacroStep> = mutableListOf(),
    var loopCount: Int = 1,
    var enabled: Boolean = true
) {
    fun deepCopy(): Macro = Macro(
        id = id,
        name = name,
        steps = steps.mapTo(mutableListOf()) { it.copyOf() },
        loopCount = loopCount,
        enabled = enabled
    )

    /** Total wall-clock time of one pass through the macro, used for UI summaries. */
    fun totalDelayMs(): Long = steps.sumOf { it.delayMs }
}
