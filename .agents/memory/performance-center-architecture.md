---
name: Performance Center Architecture
description: Key decisions for the DroidBridge Smart Performance Center — preset system, GL env layering, Kotlin activity bridge, and system optimization hooks.
---

# Performance Center Architecture

## Preset system
`PerformancePreset` (Java enum) carries JVM overlay, GL env map, resolution scale, RAM factor, sustained-mode flag, and big-core flag. `PerformanceCenterPreferences` persists the active preset and all individual overrides atomically via `applyPreset()`.

**Why:** Presets must be one source of truth. Both the Compose UI and the launch planner read from `PerformanceCenterPreferences`; the Java launch path then merges preset JVM args on top of `FPSBoostConfig` base profiles.

**How to apply:** When adding a new preset, add it to `PerformancePreset` enum, add a GL env builder method in `RenderingPipelineConfig`, and add it to `DeviceBenchmark.recommend()`.

## GL env layering rule
Renderer plugin env vars (`RendererInterface.getRendererEnv()`) are the base layer. `RenderingPipelineConfig.getGlEnvForPreset()` returns the overlay. `mergeEnv(base, overlay)` is called by the launch planner with overlay winning on duplicate keys.

**Why:** Renderer plugins own their core config (e.g. GALLIUM_DRIVER=zink). Presets only add/override performance-tuning vars (VBO, batching, shader cache). Never set renderer-core vars in presets.

## VulkanZinkRenderer enhanced flags (commit 4d1a977)
Added: LIBGL_USEVBO=1, LIBGL_VAOEXT=1, LIBGL_NOBATCH=0 (batching on), LIBGL_FASTMATH=1, mesa_glthread=true, MESA_GLTHREAD_INIT_BUFFER_SIZE=512, ZINK_DESCRIPTORS=lazy, MESA_SHADER_CACHE_DISABLE=false, MESA_GLSL_CACHE_DISABLE=false, vblank_mode=0.

**Why:** Zink didn't have VBO/VAO, GL threading, or shader cache enabled by default — these are the biggest per-frame GPU/CPU gains available without touching game code.

## Activity bridge pattern
`PerformanceCenterActivity` is Kotlin (extends `ComponentActivity` + `setContent{}`). The Java `FullscreenUtils.enableImmersive()` and `Logging` are called directly from Kotlin. No SAM wrapper needed.

**Why:** Tried a Java activity with Compose `setContent` — the `ComponentActivity` Java API for `setContent` doesn't exist cleanly; Kotlin is the correct host language for Compose activities.

## FPSBoostConfig.getUltraFpsModeProfile()
Public method added to select the right JVM profile for the Ultra FPS preset: Hyper+ (ZGC/G1, Java 21 path) for MC 1.21+, Ultra G1 with HIGH-tier overlay for MC < 1.21.

## SystemPerformanceManager API gating
- `setProcessGroup(THREAD_GROUP_TOP_APP)`: gated API 28+
- `setSustainedPerformanceMode`: gated API 24+ with null Window check
- Both ops wrapped in `try/catch(Throwable)` so launch never aborts on unusual ROMs

## Benchmark score ranges (synthetic)
HIGH device ≥ 1400 pts → Maximum FPS; ≥ 2000 pts → Ultra FPS.
MID 900–1400 → Balanced. LOW 500–900 → Low-End. < 500 → Battery Saver.
Score = (cpuFloat + cpuInt)/2 + memBandwidth (capped 600) + ramBonus (0–200).
