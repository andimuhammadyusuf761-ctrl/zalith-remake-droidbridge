/*
 * Copyright (c) 2026 DNA Mobile Applications. All rights reserved.
 * DroidBridge project code — Performance preset definitions.
 */
package ca.dnamobile.javalauncher.performance;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One-tap performance presets for the DroidBridge Performance Center.
 *
 * Each preset carries:
 *   - A display name + description
 *   - JVM argument overlay (merged on top of FPSBoostConfig base)
 *   - GL/Gallium environment variable overlay
 *   - Resolution scale hint (100 = native, 75 = performance, etc.)
 *   - RAM allocation factor (multiplied against MemoryAllocationUtils default)
 *   - Whether to request Sustained Performance Mode
 *   - Whether to prefer big CPU cores (affinity hint)
 */
public enum PerformancePreset {

    // ─── Maximum FPS ──────────────────────────────────────────────────────────
    MAXIMUM_FPS(
        "Maximum FPS",
        "All throttles off — G1GC tuned for <2 ms pauses, big cores, full shader cache.",
        Arrays.asList(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=2",
            "-XX:G1NewSizePercent=20",
            "-XX:G1MaxNewSizePercent=60",
            "-XX:G1HeapRegionSize=16M",
            "-XX:InitiatingHeapOccupancyPercent=10",
            "-XX:G1MixedGCLiveThresholdPercent=90",
            "-XX:G1RSetUpdatingPauseTimePercent=5",
            "-XX:MaxTenuringThreshold=1",
            "-XX:+AlwaysPreTouch",
            "-XX:+DisableExplicitGC",
            "-XX:+ParallelRefProcEnabled",
            "-XX:ReservedCodeCacheSize=384M",
            "-XX:MaxInlineLevel=18",
            "-XX:CICompilerCount=4"
        ),
        buildMaxFpsEnv(),
        75,    // 75% res = more GPU headroom
        1.0f,  // full default RAM
        true,  // sustained perf mode
        true   // prefer big cores
    ),

    // ─── Ultra FPS (new preset) ────────────────────────────────────────────────
    ULTRA_FPS(
        "Ultra FPS ⚡",
        "Maximum FPS + MESA GL threading + shader batching + VBO/VAO/FastMath pipeline.",
        Arrays.asList(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=1",
            "-XX:G1NewSizePercent=20",
            "-XX:G1MaxNewSizePercent=60",
            "-XX:G1HeapRegionSize=16M",
            "-XX:InitiatingHeapOccupancyPercent=8",
            "-XX:G1MixedGCLiveThresholdPercent=90",
            "-XX:G1RSetUpdatingPauseTimePercent=3",
            "-XX:MaxTenuringThreshold=1",
            "-XX:+AlwaysPreTouch",
            "-XX:+DisableExplicitGC",
            "-XX:+ParallelRefProcEnabled",
            "-XX:+UseAdaptiveGCBoundary",
            "-XX:ReservedCodeCacheSize=512M",
            "-XX:InitialCodeCacheSize=128M",
            "-XX:MaxInlineLevel=20",
            "-XX:InlineSmallCode=3000",
            "-XX:FreqInlineSize=500",
            "-XX:CICompilerCount=4",
            "-XX:CompileThreshold=800"
        ),
        buildUltraFpsEnv(),
        70,    // 70% res — maximum GPU headroom
        1.0f,
        true,
        true
    ),

    // ─── Balanced ─────────────────────────────────────────────────────────────
    BALANCED(
        "Balanced",
        "Smooth gameplay with sensible battery and thermal use. Good for most devices.",
        Arrays.asList(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=8",
            "-XX:G1NewSizePercent=25",
            "-XX:G1MaxNewSizePercent=60",
            "-XX:G1HeapRegionSize=8M",
            "-XX:InitiatingHeapOccupancyPercent=15",
            "-XX:G1MixedGCLiveThresholdPercent=90",
            "-XX:G1RSetUpdatingPauseTimePercent=5",
            "-XX:MaxTenuringThreshold=1",
            "-XX:+DisableExplicitGC",
            "-XX:+ParallelRefProcEnabled",
            "-XX:ReservedCodeCacheSize=256M",
            "-XX:MaxInlineLevel=15",
            "-XX:CICompilerCount=3"
        ),
        buildBalancedEnv(),
        100,   // native resolution
        1.0f,
        false, // no sustained mode (saves battery)
        false
    ),

    // ─── Quality ──────────────────────────────────────────────────────────────
    QUALITY(
        "Quality",
        "Prioritises visual fidelity — native resolution, full mipmap chain, no shortcuts.",
        Arrays.asList(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=10",
            "-XX:G1NewSizePercent=25",
            "-XX:G1MaxNewSizePercent=55",
            "-XX:G1HeapRegionSize=8M",
            "-XX:InitiatingHeapOccupancyPercent=20",
            "-XX:MaxTenuringThreshold=1",
            "-XX:+DisableExplicitGC",
            "-XX:ReservedCodeCacheSize=256M",
            "-XX:MaxInlineLevel=12"
        ),
        buildQualityEnv(),
        100,
        1.2f,  // extra RAM for texture streaming
        false,
        false
    ),

    // ─── Low-End ──────────────────────────────────────────────────────────────
    LOW_END(
        "Low-End",
        "Tuned for devices with 2–4 GB RAM and 4-core CPUs. Reduced resolution.",
        Arrays.asList(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=15",
            "-XX:G1NewSizePercent=30",
            "-XX:G1MaxNewSizePercent=70",
            "-XX:G1HeapRegionSize=4M",
            "-XX:InitiatingHeapOccupancyPercent=20",
            "-XX:MaxTenuringThreshold=1",
            "-XX:+DisableExplicitGC",
            "-XX:ParallelGCThreads=2",
            "-XX:ConcGCThreads=1",
            "-XX:CICompilerCount=2",
            "-XX:ReservedCodeCacheSize=128M",
            "-XX:MaxInlineLevel=10",
            "-XX:-AlwaysPreTouch"
        ),
        buildLowEndEnv(),
        60,    // 60% for GPU relief
        0.75f, // 75% of default RAM
        false,
        false
    ),

    // ─── Battery Saver ────────────────────────────────────────────────────────
    BATTERY_SAVER(
        "Battery Saver",
        "Minimise CPU/GPU work and heat. Targets 30 FPS equivalence.",
        Arrays.asList(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=25",
            "-XX:G1NewSizePercent=30",
            "-XX:G1MaxNewSizePercent=70",
            "-XX:G1HeapRegionSize=4M",
            "-XX:InitiatingHeapOccupancyPercent=25",
            "-XX:MaxTenuringThreshold=1",
            "-XX:+DisableExplicitGC",
            "-XX:ParallelGCThreads=2",
            "-XX:ConcGCThreads=1",
            "-XX:CICompilerCount=2",
            "-XX:TieredStopAtLevel=3",
            "-XX:-AlwaysPreTouch"
        ),
        buildBatterySaverEnv(),
        50,    // half resolution
        0.7f,
        false,
        false
    );

    // ─── Fields ───────────────────────────────────────────────────────────────

    @NonNull public final String displayName;
    @NonNull public final String description;
    @NonNull public final List<String> jvmArgOverlay;
    @NonNull public final Map<String, String> envOverlay;
    /** Resolution scale percent (25–200, 100 = native). */
    public final int resolutionScalePercent;
    /** Multiplier applied to MemoryAllocationUtils.getDefaultAllocatedMemoryMb(). */
    public final float ramFactor;
    /** Request Window.setSustainedPerformanceMode(true). */
    public final boolean sustainedPerformanceMode;
    /** Hint to SystemPerformanceManager to pin threads to big cores. */
    public final boolean preferBigCores;

    PerformancePreset(
            @NonNull String displayName,
            @NonNull String description,
            @NonNull List<String> jvmArgOverlay,
            @NonNull Map<String, String> envOverlay,
            int resolutionScalePercent,
            float ramFactor,
            boolean sustainedPerformanceMode,
            boolean preferBigCores
    ) {
        this.displayName = displayName;
        this.description = description;
        this.jvmArgOverlay = Collections.unmodifiableList(jvmArgOverlay);
        this.envOverlay = Collections.unmodifiableMap(envOverlay);
        this.resolutionScalePercent = resolutionScalePercent;
        this.ramFactor = ramFactor;
        this.sustainedPerformanceMode = sustainedPerformanceMode;
        this.preferBigCores = preferBigCores;
    }

    // ─── Env builders ─────────────────────────────────────────────────────────

    private static Map<String, String> buildMaxFpsEnv() {
        Map<String, String> e = new HashMap<>();
        e.put("LIBGL_USEVBO", "1");
        e.put("LIBGL_FASTMATH", "1");
        e.put("LIBGL_NOBATCH", "0");           // 0 = batching ON
        e.put("MESA_SHADER_CACHE_DISABLE", "false");
        e.put("MESA_GLSL_CACHE_DISABLE", "false");
        e.put("mesa_glthread", "true");
        e.put("vblank_mode", "0");
        e.put("LIBGL_NOERROR", "1");
        e.put("MESA_NO_ERROR", "1");
        return e;
    }

    private static Map<String, String> buildUltraFpsEnv() {
        Map<String, String> e = buildMaxFpsEnv();
        e.put("LIBGL_VAOEXT", "1");            // VAO extension
        e.put("LIBGL_MIPMAP", "3");            // full mipmap chain for perf (3 = best quality kept)
        e.put("LIBGL_NORMALIZE", "1");
        e.put("MESA_GL_VERSION_OVERRIDE", "4.6");
        e.put("MESA_GLSL_VERSION_OVERRIDE", "460");
        e.put("MESA_GLTHREAD_INIT_BUFFER_SIZE", "512");
        e.put("GALLIUM_HUD", "");              // disable HUD overhead
        e.put("ZINK_DESCRIPTORS", "lazy");     // lazy descriptor sets = lower CPU overhead
        e.put("allow_higher_compat_version", "true");
        e.put("allow_glsl_extension_directive_midshader", "true");
        return e;
    }

    private static Map<String, String> buildBalancedEnv() {
        Map<String, String> e = new HashMap<>();
        e.put("LIBGL_USEVBO", "1");
        e.put("LIBGL_FASTMATH", "1");
        e.put("LIBGL_NOBATCH", "0");
        e.put("MESA_SHADER_CACHE_DISABLE", "false");
        e.put("mesa_glthread", "true");
        return e;
    }

    private static Map<String, String> buildQualityEnv() {
        Map<String, String> e = new HashMap<>();
        e.put("LIBGL_USEVBO", "1");
        e.put("LIBGL_MIPMAP", "3");
        e.put("LIBGL_NORMALIZE", "1");
        e.put("MESA_SHADER_CACHE_DISABLE", "false");
        return e;
    }

    private static Map<String, String> buildLowEndEnv() {
        Map<String, String> e = new HashMap<>();
        e.put("LIBGL_USEVBO", "1");
        e.put("LIBGL_FASTMATH", "1");
        e.put("LIBGL_NOBATCH", "0");
        e.put("LIBGL_MIPMAP", "1");
        e.put("MESA_NO_ERROR", "1");
        e.put("LIBGL_NOERROR", "1");
        e.put("vblank_mode", "0");
        return e;
    }

    private static Map<String, String> buildBatterySaverEnv() {
        Map<String, String> e = new HashMap<>();
        e.put("LIBGL_USEVBO", "1");
        e.put("LIBGL_MIPMAP", "1");
        e.put("MESA_NO_ERROR", "1");
        e.put("LIBGL_NOERROR", "1");
        return e;
    }
}
