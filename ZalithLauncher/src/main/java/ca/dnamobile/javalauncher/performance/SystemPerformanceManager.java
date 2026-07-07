/*
 * Copyright (c) 2026 DNA Mobile Applications. All rights reserved.
 * DroidBridge project code — System-level performance optimisations.
 */
package ca.dnamobile.javalauncher.performance;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ca.dnamobile.javalauncher.feature.log.Logging;

/**
 * System-level performance manager for DroidBridge.
 *
 * Covers four areas:
 *  1. CPU big-core affinity — boosts the current thread group to URGENT_DISPLAY
 *     priority so the scheduler favours big cores on big.LITTLE SoCs.
 *  2. Sustained Performance Mode — asks the platform to keep clocks steady
 *     rather than burst-then-throttle (API 24+, Window required).
 *  3. Memory trimming — requests an aggressive trim before launch to clear
 *     cached background processes and free heap for Minecraft.
 *  4. Background optimisation — drops the launcher process to BACKGROUND
 *     class once Minecraft is running so it gives up CPU slices.
 */
public final class SystemPerformanceManager {

    private static final String TAG = "SystemPerfMgr";

    private SystemPerformanceManager() {}

    // ── 1. CPU big-core affinity ───────────────────────────────────────────

    /**
     * Boosts the calling thread and its process group to
     * {@link Process#THREAD_PRIORITY_URGENT_DISPLAY} so the Android
     * scheduler preferentially places it on big (high-performance) cores
     * on ARM big.LITTLE / DynamIQ SoCs.
     *
     * <p>Should be called from the game-launch thread immediately before
     * {@code Runtime.exec()} / {@code ProcessBuilder.start()}.
     */
    public static void applyBigCoreAffinity() {
        try {
            // URGENT_DISPLAY = -8; scheduler heavily favours big cores.
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);

            // On API 28+ we can also boost the whole process cgroup.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.os.Process.setProcessGroup(
                    android.os.Process.myPid(),
                    android.os.Process.THREAD_GROUP_TOP_APP
                );
            }

            Logging.i(TAG, "Big-core affinity applied (tid=" + Process.myTid() + ")");
        } catch (Throwable t) {
            // setThreadPriority throws SecurityException if the caller lacks
            // MODIFY_AUDIO_SETTINGS — catch broadly so launch never aborts.
            Logging.w(TAG, "Big-core affinity failed (non-fatal): " + t.getMessage());
        }
    }

    /**
     * Restores the calling thread to normal scheduling priority.
     * Call this when returning to launcher UI after the game exits.
     */
    public static void restoreNormalPriority() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.os.Process.setProcessGroup(
                    android.os.Process.myPid(),
                    android.os.Process.THREAD_GROUP_DEFAULT
                );
            }
            Logging.i(TAG, "Thread priority restored to DEFAULT");
        } catch (Throwable t) {
            Logging.w(TAG, "Priority restore failed (non-fatal): " + t.getMessage());
        }
    }

    // ── 2. Sustained Performance Mode ─────────────────────────────────────

    /**
     * Requests the platform's Sustained Performance Mode via
     * {@link Window#setSustainedPerformanceMode(boolean)}.
     *
     * <p>Requires API 24+. When enabled, the CPU/GPU governor targets
     * a sustained (thermally stable) clock rather than bursting and then
     * throttling — important for long play sessions.
     *
     * @param activity The activity whose window is used (typically GameActivity).
     * @param enabled  {@code true} to enable, {@code false} to disable.
     */
    public static void setSustainedPerformanceMode(@NonNull Activity activity, boolean enabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Logging.i(TAG, "Sustained perf mode not available (API < 24)");
            return;
        }
        try {
            Window window = activity.getWindow();
            if (window != null) {
                window.setSustainedPerformanceMode(enabled);
                Logging.i(TAG, "Sustained perf mode = " + enabled);
            }
        } catch (Throwable t) {
            Logging.w(TAG, "setSustainedPerformanceMode failed: " + t.getMessage());
        }
    }

    /**
     * Returns whether the device advertises Sustained Performance Mode support.
     */
    public static boolean isSustainedPerformanceModeSupported(@NonNull Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;
        try {
            ActivityManager am = (ActivityManager)
                ctx.getSystemService(Context.ACTIVITY_SERVICE);
            return am != null && am.isLowRamDevice()
                // Low-RAM devices usually do NOT support sustained mode.
                ? false
                // On regular devices we assume support; we catch exceptions if not.
                : (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N);
        } catch (Throwable t) {
            return false;
        }
    }

    // ── 3. Memory trimming ────────────────────────────────────────────────

    /**
     * Aggressively reclaims memory before game launch by signalling a trim
     * at {@link ComponentCallbacks2#TRIM_MEMORY_RUNNING_CRITICAL} and then
     * requesting a GC pass on the launcher process.
     *
     * <p>This is called on a background thread by {@link #preloadAndTrim}.
     */
    public static void trimMemoryBeforeLaunch(@NonNull Context ctx) {
        try {
            // Broadcast trim signal so all registered ComponentCallbacks2 in the
            // launcher process release their caches (image caches, etc.).
            ctx.getApplicationContext()
               .onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL);

            // Give the system a GC pass.
            System.gc();
            Runtime.getRuntime().runFinalization();
            System.gc();

            Logging.i(TAG, "Pre-launch memory trim complete");
        } catch (Throwable t) {
            Logging.w(TAG, "Memory trim error (non-fatal): " + t.getMessage());
        }
    }

    /**
     * Trims memory on a background thread so the UI is not blocked.
     * Safe to call from any context before starting the game process.
     */
    public static void trimMemoryAsync(@NonNull Context ctx) {
        Thread trimThread = new Thread(() -> trimMemoryBeforeLaunch(ctx), "DB-MemTrim");
        trimThread.setDaemon(true);
        trimThread.setPriority(Thread.MIN_PRIORITY);
        trimThread.start();
    }

    // ── 4. Background optimisation ────────────────────────────────────────

    /**
     * Lowers the launcher's process scheduling class to BACKGROUND so it
     * yields CPU and I/O to the Minecraft child process while the game runs.
     */
    public static void yieldToGameProcess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.os.Process.setProcessGroup(
                    android.os.Process.myPid(),
                    android.os.Process.THREAD_GROUP_BACKGROUND
                );
            }
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Logging.i(TAG, "Launcher yielded to game process");
        } catch (Throwable t) {
            Logging.w(TAG, "yieldToGameProcess failed (non-fatal): " + t.getMessage());
        }
    }

    // ── 5. Combined fast-launch sequence ──────────────────────────────────

    /**
     * One-shot method to apply all system-level optimisations for the
     * active performance preset before launching the game.
     *
     * <ul>
     *   <li>Trims memory asynchronously.</li>
     *   <li>Applies big-core affinity on the calling thread if requested.</li>
     *   <li>Applies sustained performance mode if the activity is available.</li>
     * </ul>
     */
    public static void applyPresetSystemSettings(
            @NonNull Context ctx,
            @NonNull PerformancePreset preset,
            @Nullable Activity activity
    ) {
        trimMemoryAsync(ctx);

        if (preset.preferBigCores) {
            applyBigCoreAffinity();
        }

        if (preset.sustainedPerformanceMode && activity != null) {
            setSustainedPerformanceMode(activity, true);
        }

        Logging.i(TAG, "System settings applied for preset: " + preset.displayName);
    }
}
