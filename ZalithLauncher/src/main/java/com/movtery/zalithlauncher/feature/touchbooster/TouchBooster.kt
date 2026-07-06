package com.movtery.zalithlauncher.feature.touchbooster

import android.os.Process
import com.movtery.zalithlauncher.feature.log.Logging

/**
 * DroidBridge Touch Booster — raises the UI thread priority to
 * THREAD_PRIORITY_URGENT_DISPLAY while the player is in-game (pointer grabbed).
 *
 * This cuts input-to-render latency by ensuring Android's scheduler
 * gives the touch-dispatch thread a higher time slice, reducing stutter
 * and improving camera responsiveness in Minecraft 1.21+.
 *
 * Call [boost] when the game grabs the pointer (player enters world).
 * Call [restore] when the pointer is released (menus, pause screen).
 */
object TouchBooster {
    private const val TAG = "TouchBooster"

    @Volatile private var mBoosted = false
    @Volatile private var mSavedPriority = Process.THREAD_PRIORITY_DEFAULT

    fun boost() {
        if (mBoosted) return
        try {
            mSavedPriority = Process.getThreadPriority(Process.myTid())
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            mBoosted = true
            Logging.i(TAG, "Touch thread boosted: $mSavedPriority → ${Process.THREAD_PRIORITY_URGENT_DISPLAY}")
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
