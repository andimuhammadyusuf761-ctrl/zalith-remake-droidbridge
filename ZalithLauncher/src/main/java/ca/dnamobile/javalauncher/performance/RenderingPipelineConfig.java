/*
 * Copyright (c) 2026 DNA Mobile Applications. All rights reserved.
 * DroidBridge project code — Rendering pipeline configuration.
 */
package ca.dnamobile.javalauncher.performance;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised GL/Gallium environment variable configurations for the
 * DroidBridge rendering pipeline.
 *
 * <p>These env-var maps are intended to be merged <em>on top of</em>
 * the renderer plugin's own env map (see {@code RendererInterface.getRendererEnv()})
 * by the launch planner.  Values here override the renderer defaults for the
 * selected performance preset.
 *
 * <p>Flag reference:
 * <ul>
 *   <li>LIBGL_USEVBO — enable Vertex Buffer Objects (batch geometry on GPU)</li>
 *   <li>LIBGL_VAOEXT — enable Vertex Array Object extension</li>
 *   <li>LIBGL_FASTMATH — allow fast (approximate) math in the GL driver</li>
 *   <li>LIBGL_NOBATCH=0 — enable draw-call batching (0 = batch on)</li>
 *   <li>LIBGL_MIPMAP — mipmap quality level (1=fast, 3=best)</li>
 *   <li>LIBGL_NORMALIZE — auto-normalize normals (needed for Minecraft lighting)</li>
 *   <li>LIBGL_NOERROR — skip GL error checking (minor GPU savings)</li>
 *   <li>MESA_NO_ERROR — Mesa driver skips error checking</li>
 *   <li>MESA_SHADER_CACHE_DISABLE — must be "false" to keep cache enabled</li>
 *   <li>MESA_GLSL_CACHE_DISABLE — must be "false" to keep GLSL cache enabled</li>
 *   <li>mesa_glthread — Mesa GL threading (async command stream, reduces CPU stall)</li>
 *   <li>vblank_mode — 0 = disable VSync at driver level for max FPS</li>
 *   <li>ZINK_DESCRIPTORS — "lazy" = fewer Vulkan descriptor updates per frame</li>
 *   <li>MESA_GL_VERSION_OVERRIDE — advertise higher GL version to the game</li>
 *   <li>MESA_GLSL_VERSION_OVERRIDE — advertise higher GLSL version</li>
 * </ul>
 */
public final class RenderingPipelineConfig {

    private RenderingPipelineConfig() {}

    // ── Per-preset GL env maps ─────────────────────────────────────────────

    /** Full Ultra FPS rendering pipeline — VBO + VAO + GL threading + shader cache. */
    @NonNull
    public static Map<String, String> getUltraFpsGlEnv() {
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        // VBO / VAO / geometry batching
        env.put("LIBGL_USEVBO", "1");
        env.put("LIBGL_VAOEXT", "1");
        env.put("LIBGL_NOBATCH", "0");
        // Fast math
        env.put("LIBGL_FASTMATH", "1");
        // Mipmap — keep quality so texture streaming is stable
        env.put("LIBGL_MIPMAP", "3");
        env.put("LIBGL_NORMALIZE", "1");
        // Error check bypass
        env.put("LIBGL_NOERROR", "1");
        env.put("MESA_NO_ERROR", "1");
        // Shader / GLSL cache — critical for eliminating first-frame stutters
        env.put("MESA_SHADER_CACHE_DISABLE", "false");
        env.put("MESA_GLSL_CACHE_DISABLE", "false");
        // Async GL command stream
        env.put("mesa_glthread", "true");
        env.put("MESA_GLTHREAD_INIT_BUFFER_SIZE", "512");
        // Disable vsync at driver level
        env.put("vblank_mode", "0");
        // Zink-specific: lazy descriptor batching
        env.put("ZINK_DESCRIPTORS", "lazy");
        // GL/GLSL version advertisement
        env.put("MESA_GL_VERSION_OVERRIDE", "4.6");
        env.put("MESA_GLSL_VERSION_OVERRIDE", "460");
        // GLSL extension compatibility
        env.put("allow_higher_compat_version", "true");
        env.put("allow_glsl_extension_directive_midshader", "true");
        env.put("force_glsl_extensions_warn", "true");
        return Collections.unmodifiableMap(env);
    }

    /** Maximum FPS — same as Ultra minus VAO (compatible with more GL4ES builds). */
    @NonNull
    public static Map<String, String> getMaximumFpsGlEnv() {
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("LIBGL_USEVBO", "1");
        env.put("LIBGL_NOBATCH", "0");
        env.put("LIBGL_FASTMATH", "1");
        env.put("LIBGL_MIPMAP", "3");
        env.put("LIBGL_NORMALIZE", "1");
        env.put("LIBGL_NOERROR", "1");
        env.put("MESA_NO_ERROR", "1");
        env.put("MESA_SHADER_CACHE_DISABLE", "false");
        env.put("MESA_GLSL_CACHE_DISABLE", "false");
        env.put("mesa_glthread", "true");
        env.put("vblank_mode", "0");
        return Collections.unmodifiableMap(env);
    }

    /** Balanced — VBO + batching + shader cache, no error bypass. */
    @NonNull
    public static Map<String, String> getBalancedGlEnv() {
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("LIBGL_USEVBO", "1");
        env.put("LIBGL_NOBATCH", "0");
        env.put("LIBGL_FASTMATH", "1");
        env.put("MESA_SHADER_CACHE_DISABLE", "false");
        env.put("mesa_glthread", "true");
        return Collections.unmodifiableMap(env);
    }

    /** Quality — VBO + full mipmap chain + shader cache, no error bypass. */
    @NonNull
    public static Map<String, String> getQualityGlEnv() {
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("LIBGL_USEVBO", "1");
        env.put("LIBGL_MIPMAP", "3");
        env.put("LIBGL_NORMALIZE", "1");
        env.put("MESA_SHADER_CACHE_DISABLE", "false");
        env.put("MESA_GLSL_CACHE_DISABLE", "false");
        return Collections.unmodifiableMap(env);
    }

    /** Low-end — VBO + batching + lower mipmap + error bypass. */
    @NonNull
    public static Map<String, String> getLowEndGlEnv() {
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("LIBGL_USEVBO", "1");
        env.put("LIBGL_NOBATCH", "0");
        env.put("LIBGL_FASTMATH", "1");
        env.put("LIBGL_MIPMAP", "1");
        env.put("LIBGL_NOERROR", "1");
        env.put("MESA_NO_ERROR", "1");
        env.put("vblank_mode", "0");
        return Collections.unmodifiableMap(env);
    }

    /** Battery saver — minimal GL flags, low mipmap, error bypass. */
    @NonNull
    public static Map<String, String> getBatterySaverGlEnv() {
        LinkedHashMap<String, String> env = new LinkedHashMap<>();
        env.put("LIBGL_USEVBO", "1");
        env.put("LIBGL_MIPMAP", "1");
        env.put("LIBGL_NOERROR", "1");
        env.put("MESA_NO_ERROR", "1");
        return Collections.unmodifiableMap(env);
    }

    // ── Utility: get GL env for a preset ──────────────────────────────────

    @NonNull
    public static Map<String, String> getGlEnvForPreset(@NonNull PerformancePreset preset) {
        switch (preset) {
            case ULTRA_FPS:    return getUltraFpsGlEnv();
            case MAXIMUM_FPS:  return getMaximumFpsGlEnv();
            case BALANCED:     return getBalancedGlEnv();
            case QUALITY:      return getQualityGlEnv();
            case LOW_END:      return getLowEndGlEnv();
            case BATTERY_SAVER:return getBatterySaverGlEnv();
            default:           return getBalancedGlEnv();
        }
    }

    /**
     * Merges {@code overlay} on top of {@code base}, with overlay winning on
     * duplicate keys.  Returns a new immutable map.
     */
    @NonNull
    public static Map<String, String> mergeEnv(
            @NonNull Map<String, String> base,
            @NonNull Map<String, String> overlay
    ) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>(base);
        result.putAll(overlay);
        return Collections.unmodifiableMap(result);
    }
}
