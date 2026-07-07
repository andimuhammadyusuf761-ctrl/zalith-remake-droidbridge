/*
 * Copyright (c) 2026 DNA Mobile Applications. All rights reserved.
 * DroidBridge project code — Device benchmark and auto-optimise.
 */
package ca.dnamobile.javalauncher.performance;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import ca.dnamobile.javalauncher.feature.log.Logging;
import ca.dnamobile.javalauncher.settings.MemoryAllocationUtils;

/**
 * Lightweight device benchmark for the DroidBridge Performance Center.
 *
 * <p>The benchmark performs three quick, synthetic workloads to estimate
 * relative device capability without needing OpenGL or a game session:
 * <ol>
 *   <li><b>CPU float</b> — 10 M floating-point multiply-add iterations.</li>
 *   <li><b>CPU int</b>  — 10 M integer operations (bitwise + shift).</li>
 *   <li><b>Mem bandwidth</b> — sequential read/write over a 4 MB int[].</li>
 * </ol>
 *
 * <p>The resulting score is a dimensionless integer (higher = faster).
 * Typical ranges:
 * <ul>
 *   <li>HIGH device  : 1400 – 2500+</li>
 *   <li>MID device   :  700 – 1400</li>
 *   <li>LOW device   :  300 –  700</li>
 * </ul>
 *
 * <p><b>Must be called on a background thread</b> — it blocks for ~300–600 ms.
 */
public final class DeviceBenchmark {

    private static final String TAG = "DeviceBenchmark";

    /** Benchmark result DTO. */
    public static final class Result {
        public final int score;
        public final int cpuScore;
        public final int memScore;
        public final long durationMs;
        /** Recommended preset based on this device's score. */
        @NonNull public final PerformancePreset recommendedPreset;

        Result(int score, int cpuScore, int memScore, long durationMs,
               @NonNull PerformancePreset recommendedPreset) {
            this.score             = score;
            this.cpuScore          = cpuScore;
            this.memScore          = memScore;
            this.durationMs        = durationMs;
            this.recommendedPreset = recommendedPreset;
        }

        @NonNull @Override
        public String toString() {
            return "BenchmarkResult{score=" + score + ", cpu=" + cpuScore
                + ", mem=" + memScore + ", ms=" + durationMs
                + ", preset=" + recommendedPreset.displayName + "}";
        }
    }

    private DeviceBenchmark() {}

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Runs the benchmark synchronously.  Duration ~300–600 ms.
     *
     * @param ctx Android context (used for RAM detection).
     * @return Benchmark result with composite score and recommended preset.
     */
    @WorkerThread
    @NonNull
    public static Result run(@NonNull Context ctx) {
        Logging.i(TAG, "Benchmark started");
        long t0 = SystemClock.elapsedRealtime();

        int cpuFloat = benchmarkCpuFloat();
        int cpuInt   = benchmarkCpuInt();
        int mem      = benchmarkMemBandwidth();
        int ramBonus = ramBonus(ctx);

        int cpuScore  = (cpuFloat + cpuInt) / 2;
        int memScore  = mem;
        int composite = cpuScore + memScore + ramBonus;

        long elapsed = SystemClock.elapsedRealtime() - t0;

        PerformancePreset recommended = recommend(composite);
        Result result = new Result(composite, cpuScore, memScore, elapsed, recommended);

        Logging.i(TAG, "Benchmark done: " + result);
        return result;
    }

    /**
     * Runs the benchmark asynchronously, saves the result to preferences,
     * and optionally applies the recommended preset.
     */
    public static void runAndAutoOptimise(
            @NonNull Context ctx,
            boolean applyPreset,
            @NonNull Callback callback
    ) {
        Thread t = new Thread(() -> {
            Result result = run(ctx);
            PerformanceCenterPreferences.saveBenchmarkResult(ctx, result.score);

            if (applyPreset) {
                PerformanceCenterPreferences.applyPreset(ctx, result.recommendedPreset);
                PerformanceCenterPreferences.setAutoOptimized(ctx, true);
            }
            callback.onComplete(result);
        }, "DB-Benchmark");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();
    }

    public interface Callback {
        void onComplete(@NonNull Result result);
    }

    // ── Workloads ─────────────────────────────────────────────────────────

    /** ~10 M float multiplications. Score = iterations per millisecond. */
    private static int benchmarkCpuFloat() {
        final int N = 10_000_000;
        long t0 = SystemClock.elapsedRealtime();
        double acc = 1.0;
        for (int i = 0; i < N; i++) {
            acc = acc * 1.000_001 + 0.000_001;
        }
        long ms = Math.max(1, SystemClock.elapsedRealtime() - t0);
        // Prevent dead-code elimination
        if (acc < 0) Logging.w(TAG, "float sink=" + acc);
        return (int) (N / ms);
    }

    /** ~10 M integer bitwise ops. Score = iterations per millisecond. */
    private static int benchmarkCpuInt() {
        final int N = 10_000_000;
        long t0 = SystemClock.elapsedRealtime();
        int acc = 0x5F3759DF;
        for (int i = 0; i < N; i++) {
            acc ^= (acc << 13);
            acc ^= (acc >>> 17);
            acc ^= (acc << 5);
        }
        long ms = Math.max(1, SystemClock.elapsedRealtime() - t0);
        if (acc == 0) Logging.w(TAG, "int sink=" + acc);
        return (int) (N / ms);
    }

    /** Sequential read+write over 4 MB. Score = MB/s (capped for comparability). */
    private static int benchmarkMemBandwidth() {
        final int LEN = 1024 * 1024; // 4 MB as int[]
        int[] buf = new int[LEN];
        long t0 = SystemClock.elapsedRealtime();
        // Write
        for (int i = 0; i < LEN; i++) buf[i] = i;
        // Read
        long sum = 0;
        for (int i = 0; i < LEN; i++) sum += buf[i];
        long ms = Math.max(1, SystemClock.elapsedRealtime() - t0);
        if (sum < 0) Logging.w(TAG, "mem sink=" + sum);
        // 4 MB read + 4 MB write = 8 MB total
        int mbPerSec = (int) (8000L / ms);
        return Math.min(mbPerSec, 600); // cap at 600 so it doesn't dominate
    }

    /** Small RAM bonus so higher-RAM devices rank better. */
    private static int ramBonus(@NonNull Context ctx) {
        int totalMb = MemoryAllocationUtils.getTotalMemoryMb(ctx);
        if (totalMb >= 8 * 1024) return 200;
        if (totalMb >= 6 * 1024) return 150;
        if (totalMb >= 4 * 1024) return 100;
        if (totalMb >= 3 * 1024) return 50;
        return 0;
    }

    // ── Preset recommendation ─────────────────────────────────────────────

    @NonNull
    static PerformancePreset recommend(int score) {
        if (score >= 2000) return PerformancePreset.ULTRA_FPS;
        if (score >= 1400) return PerformancePreset.MAXIMUM_FPS;
        if (score >= 900)  return PerformancePreset.BALANCED;
        if (score >= 500)  return PerformancePreset.LOW_END;
        return PerformancePreset.BATTERY_SAVER;
    }

    // ── Device info helpers ───────────────────────────────────────────────

    @NonNull
    public static String getDeviceLabel() {
        return Build.MANUFACTURER + " " + Build.MODEL
            + " (API " + Build.VERSION.SDK_INT + ")";
    }

    @NonNull
    public static String getCpuInfo() {
        int cores = Runtime.getRuntime().availableProcessors();
        String abi = Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "unknown";
        return cores + " cores · " + abi;
    }

    public static int getAvailableRamMb(@NonNull Context ctx) {
        return MemoryAllocationUtils.getAvailableMemoryMb(ctx);
    }

    public static int getTotalRamMb(@NonNull Context ctx) {
        return MemoryAllocationUtils.getTotalMemoryMb(ctx);
    }
}
