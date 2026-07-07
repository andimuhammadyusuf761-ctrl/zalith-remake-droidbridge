package com.movtery.zalithlauncher.feature.touchbooster

import android.os.Process
import com.movtery.zalithlauncher.feature.log.Logging

/**
 * DroidBridge Touch Booster — raises the UI thread priority while the player
 * is in-game (pointer grabbed) to cut input-to-render latency.
 *
 * Sensitivity (1–10, from LauncherPreferences) controls the boost strength:
 *   1–3  → no boost (THREAD_PRIORITY_DEFAULT)
 *   4–6  → light boost (THREAD_PRIORITY_DISPLAY)
 *   7–10 → full boost (THREAD_PRIORITY_URGENT_DISPLAY)
 *
 * Call [boost] with the current sensitivity when the game grabs the pointer.
 * Call [restore] when the pointer is released.
 */
object TouchBooster {
    private const val TAG = "TouchBooster"

    @Volatile private var mBoosted = false
    @Volatile private var mSavedPriority = Process.THREAD_PRIORITY_DEFAULT

    /**
     * Boost using a stored sensitivity level (1–10).
     * Sensitivity ≤ 3 skips the boost entirely.
     */
    fun boost(sensitivity: Int = 5) {
        if (mBoosted) return
        val targetPriority = when {
            sensitivity <= 3 -> return
            sensitivity <= 6 -> Process.THREAD_PRIORITY_DISPLAY
            else             -> Process.THREAD_PRIORITY_URGENT_DISPLAY
        }
        try {
            mSavedPriority = Process.getThreadPriority(Process.myTid())
            Process.setThreadPriority(targetPriority)
            mBoosted = true
            Logging.i(TAG, "Touch boosted (sensitivity=$sensitivity): $mSavedPriority → $targetPriority")
        } catch (e: Exception) {
            Logging.w(TAG, "Could not boost touch thread priority", e)
        }
    }

    fun restore() {
        if (!mBoosted) return
        try {
            Process.setThreadPriority(mSavedPriority)
            mBoosted = false
            Logging.i(TAG, "Touch thread priority restored to $mSavedPriority")
        } catch (e: Exception) {
            Logging.w(TAG, "Could not restore touch thread priority", e)
        }
    }

    val isBoosted: Boolean get() = mBoosted
}
