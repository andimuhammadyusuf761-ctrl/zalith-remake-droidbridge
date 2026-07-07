package com.movtery.zalithlauncher.launch

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.movtery.zalithlauncher.feature.log.Logging

/**
 * Zalith Remake — DroidBridge Edition FPS Boost Configuration (v6 "Aurora Hyperdrive").
 *
 * Provides:
 *  1. Version-aware JVM tuning profiles for MC 1.8.x - 1.21.x.
 *  2. Adaptive device-tier overlays (HIGH / MID / LOW) so the same
 *     base profile scales to the actual hardware.
 *  3. Common code-cache, metaspace, security and JIT flags shared by
 *     every profile.
 *  4. "Hyper+" dedicated path for MC 1.21+ with Generational ZGC,
 *     deeper JIT inlining, and faster tier-escalation.
 *
 * IMPORTANT: -XX:+UnlockExperimentalVMOptions MUST come before any
 * experimental G1GC flags (G1NewSizePercent, G1MaxNewSizePercent,
 * G1MixedGCLiveThresholdPercent, G1RSetUpdatingPauseTimePercent)
 */
object FPSBoostConfig {

    enum class DeviceTier { HIGH, MID, LOW }

    data class BoostProfile(
        val name: String,
        val jvmArgs: List<String>,
        val envVars: Map<String, String>,
        val description: String
    )

    /**
     * Detect MC version range and return the optimal boost profile.
     * Pure version-based (kept for backwards compatibility).
     */
    fun getBoostProfile(versionName: String): BoostProfile {
        val ver = parseVersion(versionName)
        Logging.i("FPSBoost", "Detected MC version: $versionName -> parsed as $ver")

        val base = when {
            ver.first <= 12 -> getLegacyProfile()
            ver.first in 13..16 -> getModernProfile()
            ver.first in 17..19 -> getHeavyProfile()
            ver.first >= 20 -> getUltraProfile()
            else -> getDefaultProfile()
        }
        return withCommonAuroraFlags(base)
    }

    /**
     * Adaptive variant that also folds the detected device tier into the
     * profile (GC threads, heap region, pre-touch, JIT thresholds).
     *
     * Aurora v4: MC 1.21+ takes a dedicated "Hyper Boost" path with
     * Generational ZGC on HIGH-tier hardware, aggressive JIT inlining,
     * and Java 21's vector / FFM modules pre-enabled.
     */
    fun getAdaptiveBoostProfile(context: Context, versionName: String): BoostProfile {
        val tier = detectDeviceTier(context)
        val ver = parseVersion(versionName)
        Logging.i("FPSBoost", "Detected MC $versionName -> $ver, tier=$tier")

        if (ver.first >= 21) {
            val hyper = getHyperProfile(tier)
            return withCommonAuroraFlags(hyper)
        }

        val base = getBoostProfile(versionName)
        Logging.i("FPSBoost", "Adaptive tier=$tier for ${base.name}")
        return overlayDeviceTier(base, tier)
    }

    /**
     * Aurora v4 "Hyper Boost" — Java 21 / MC 1.21+ dedicated path.
     *
     * - HIGH tier: Generational ZGC (sub-ms GC pauses → kills combat /
     *   chunk-load stutter), JIT inlining boosted, big 384M code cache.
     * - MID / LOW tier: tightly-tuned G1 (ZGC needs too much heap on
     *   2-3GB devices) plus the same JIT inlining.
     * - Tier-scaled GC / JIT thread counts.
     * - `--add-modules=jdk.incubator.vector` so Sodium-style mods can
     *   use Java 21 SIMD intrinsics on supported CPUs.
     */
    private fun getHyperProfile(tier: DeviceTier): BoostProfile {
        val cores = Runtime.getRuntime().availableProcessors()
        val isHigh = tier == DeviceTier.HIGH
        val isLow = tier == DeviceTier.LOW

        // Aurora v6 "Hyperdrive" GC — Generational ZGC for HIGH, tuned G1 for MID/LOW
        val gcArgs: List<String> = if (isHigh) {
            listOf(
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseZGC",
                "-XX:+ZGenerational",
                "-XX:-ZProactive",
                "-XX:ZUncommitDelay=120",          // reclaim idle heap faster
                "-XX:ZCollectionInterval=5",        // trigger ZGC every 5s max
                "-XX:+UseStringDeduplication",
                "-XX:+UseCompressedOops",
                "-XX:+ParallelRefProcEnabled",
                "-XX:+DisableExplicitGC",
                "-XX:+AlwaysPreTouch"
            )
        } else {
            listOf(
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=" + (if (isLow) "8" else "2"),
                "-XX:G1NewSizePercent=20",
                "-XX:G1MaxNewSizePercent=60",
                "-XX:G1HeapRegionSize=" + (if (isLow) "4M" else "8M"),
                "-XX:G1ReservePercent=20",
                "-XX:G1HeapWastePercent=5",
                "-XX:G1MixedGCCountTarget=4",
                "-XX:InitiatingHeapOccupancyPercent=10",
                "-XX:G1MixedGCLiveThresholdPercent=90",
                "-XX:G1RSetUpdatingPauseTimePercent=5",
                "-XX:SurvivorRatio=32",
                "-XX:MaxTenuringThreshold=1",
                "-XX:+UseAdaptiveGCBoundary",
                "-XX:+UseStringDeduplication",
                "-XX:+UseCompressedOops",
                "-XX:+OptimizeStringConcat",
                "-XX:+ParallelRefProcEnabled",
                "-XX:+DisableExplicitGC"
            )
        }

        // Aurora v6: deeper inlining for HIGH, still aggressive for MID/LOW
        val jitArgs = if (isHigh) listOf(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+UseInlineCaches",
            "-XX:+DoEscapeAnalysis",
            "-XX:+EliminateLocks",
            "-XX:+EliminateAllocations",
            "-XX:+UseTypeSpeculation",
            "-XX:MaxInlineLevel=20",
            "-XX:InlineSmallCode=3000",
            "-XX:FreqInlineSize=500",
            "-XX:LoopUnrollLimit=300",
            "-XX:LiveNodeCountInliningCutoff=40000",
            "-XX:ReservedCodeCacheSize=512M",
            "-XX:InitialCodeCacheSize=128M"
        ) else listOf(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+UseInlineCaches",
            "-XX:+DoEscapeAnalysis",
            "-XX:+EliminateLocks",
            "-XX:+EliminateAllocations",
            "-XX:+UseTypeSpeculation",
            "-XX:MaxInlineLevel=15",
            "-XX:InlineSmallCode=2000",
            "-XX:FreqInlineSize=325",
            "-XX:LoopUnrollLimit=200",
            "-XX:ReservedCodeCacheSize=" + (if (isLow) "256M" else "384M"),
            "-XX:InitialCodeCacheSize=128M"
        )

        val perfArgs = listOf(
            "-XX:+PerfDisableSharedMem"
        )

        val tierThreads = when (tier) {
            DeviceTier.HIGH -> listOf(
                "-XX:ParallelGCThreads=${(cores / 2).coerceIn(4, 8)}",
                "-XX:ConcGCThreads=${(cores / 4).coerceIn(2, 4)}",
                "-XX:CICompilerCount=${(cores / 2).coerceIn(4, 6)}",
                "-XX:+AlwaysPreTouch"
            )
            DeviceTier.MID -> listOf(
                "-XX:ParallelGCThreads=${(cores / 2).coerceIn(2, 4)}",
                "-XX:ConcGCThreads=${(cores / 4).coerceIn(1, 2)}",
                "-XX:CICompilerCount=3"
            )
            DeviceTier.LOW -> listOf(
                "-XX:ParallelGCThreads=2",
                "-XX:ConcGCThreads=1",
                "-XX:CICompilerCount=2",
                "-XX:TieredStopAtLevel=3",
                "-XX:-AlwaysPreTouch"
            )
        }

        val networkArgs = listOf(
            "-Djava.net.preferIPv4Stack=true",
            "-Dnetworkaddress.cache.ttl=60"
        )

        // NOTE: jdk.incubator.vector / --enable-native-access were dropped because
        // PojavLauncher's bundled JRE 21 is stripped and missing the incubator
        // vector module, which made the boot layer fail to initialize.
        // The renderer fast path still picks up the JIT inlining wins below.
        val moduleArgs = emptyList<String>()

        val merged = mergeJvmArgs(
            gcArgs + jitArgs + perfArgs + tierThreads + networkArgs,
            moduleArgs
        )

        val gcLabel = if (isHigh) "ZGen" else "G1+"
        return BoostProfile(
            name = "Hyper+ Boost (1.21+) · ${tier.name} · $gcLabel",
            description = "Aurora v6 Hyperdrive: Java-21-tuned, " +
                if (isHigh) "Gen-ZGC 5s interval, 512M code cache, lvl-20 inlining"
                else "tight G1 pause≤${if (isLow) "8" else "2"}ms, aggressive JIT",
            jvmArgs = merged,
            envVars = emptyMap()
        )
    }

    /**
     * Heuristic device tier classification.
     *  HIGH: 8+ cores, >= 6GB RAM, 64-bit, API 29+
     *  LOW : <= 4 cores OR < 3GB RAM
     *  MID : everything else
     */
    fun detectDeviceTier(context: Context): DeviceTier {
        val cores = Runtime.getRuntime().availableProcessors()
        val totalMb = totalDeviceMemoryMb(context)
        val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        val sdk = Build.VERSION.SDK_INT

        return when {
            cores >= 8 && totalMb >= 6 * 1024 && is64Bit && sdk >= Build.VERSION_CODES.Q -> DeviceTier.HIGH
            cores <= 4 || totalMb < 3 * 1024 -> DeviceTier.LOW
            else -> DeviceTier.MID
        }
    }

    private fun totalDeviceMemoryMb(context: Context): Long {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            info.totalMem / (1024L * 1024L)
        } catch (e: Exception) {
            Logging.w("FPSBoost", "Failed to read total device memory", e)
            2048L
        }
    }

    private fun parseVersion(versionName: String): Pair<Int, Int> {
        try {
            val cleaned = versionName.replace(Regex("[^0-9.]"), " ").trim().split(" ")[0]
            val parts = cleaned.split(".")
            if (parts.size >= 2) {
                val minor = parts[1].toIntOrNull() ?: 0
                val patch = if (parts.size >= 3) parts[2].toIntOrNull() ?: 0 else 0
                return Pair(minor, patch)
            }
        } catch (e: Exception) {
            Logging.w("FPSBoost", "Failed to parse version: $versionName", e)
        }
        return Pair(0, 0)
    }

    /**
     * Legacy profile for MC 1.8.x - 1.12.x
     */
    private fun getLegacyProfile(): BoostProfile {
        return BoostProfile(
            name = "Legacy Boost (1.8-1.12)",
            description = "Optimized for older MC versions with lighter resource usage",
            jvmArgs = listOf(
                // MUST be first to unlock experimental G1 options
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=5",
                "-XX:G1NewSizePercent=30",
                "-XX:G1MaxNewSizePercent=70",
                "-XX:G1HeapRegionSize=4M",
                "-XX:G1ReservePercent=15",
                "-XX:G1HeapWastePercent=5",
                "-XX:G1MixedGCCountTarget=4",
                "-XX:InitiatingHeapOccupancyPercent=20",
                "-XX:G1MixedGCLiveThresholdPercent=90",
                "-XX:G1RSetUpdatingPauseTimePercent=5",
                "-XX:SurvivorRatio=32",
                "-XX:+PerfDisableSharedMem",
                "-XX:MaxTenuringThreshold=1",
                "-XX:+UseCompressedOops",
                "-XX:+OptimizeStringConcat",
                // Note: UseBiasedLocking removed in Java 19+ (don't add it back)
                "-XX:+AlwaysPreTouch",
                "-XX:+ParallelRefProcEnabled",
                "-XX:+DisableExplicitGC",
                "-XX:+TieredCompilation",
                "-Dfml.ignoreInvalidMinecraftCertificates=true",
                "-Dfml.ignorePatchDiscrepancies=true"
            ),
            envVars = emptyMap()
        )
    }

    /**
     * Modern profile for MC 1.13.x - 1.16.x
     */
    private fun getModernProfile(): BoostProfile {
        return BoostProfile(
            name = "Modern Boost (1.13-1.16)",
            description = "Balanced for post-flattening MC with moderate resources",
            jvmArgs = listOf(
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=8",
                "-XX:G1NewSizePercent=25",
                "-XX:G1MaxNewSizePercent=65",
                "-XX:G1HeapRegionSize=8M",
                "-XX:G1ReservePercent=20",
                "-XX:G1HeapWastePercent=5",
                "-XX:G1MixedGCCountTarget=4",
                "-XX:InitiatingHeapOccupancyPercent=15",
                "-XX:G1MixedGCLiveThresholdPercent=90",
                "-XX:G1RSetUpdatingPauseTimePercent=5",
                "-XX:SurvivorRatio=32",
                "-XX:+PerfDisableSharedMem",
                "-XX:MaxTenuringThreshold=1",
                "-XX:+UseCompressedOops",
                "-XX:+OptimizeStringConcat",
                "-XX:+UseStringDeduplication",
                // Note: UseBiasedLocking removed in Java 19+ (don't add it back)
                "-XX:+AlwaysPreTouch",
                "-XX:+ParallelRefProcEnabled",
                "-XX:+DisableExplicitGC",
                "-XX:+TieredCompilation",
                "-Dfml.ignoreInvalidMinecraftCertificates=true",
                "-Dfml.ignorePatchDiscrepancies=true"
            ),
            envVars = emptyMap()
        )
    }

    /**
     * Heavy profile for MC 1.17.x - 1.19.x
     */
    private fun getHeavyProfile(): BoostProfile {
        return BoostProfile(
            name = "Heavy Boost (1.17-1.19)",
            description = "Aggressive optimization for Caves & Cliffs era",
            jvmArgs = listOf(
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=8",
                "-XX:G1NewSizePercent=20",
                "-XX:G1MaxNewSizePercent=60",
                "-XX:G1HeapRegionSize=8M",
                "-XX:G1ReservePercent=20",
                "-XX:G1HeapWastePercent=5",
                "-XX:G1MixedGCCountTarget=4",
                "-XX:InitiatingHeapOccupancyPercent=15",
                "-XX:G1MixedGCLiveThresholdPercent=90",
                "-XX:G1RSetUpdatingPauseTimePercent=5",
                "-XX:SurvivorRatio=32",
                "-XX:+PerfDisableSharedMem",
                "-XX:MaxTenuringThreshold=1",
                "-XX:+UseCompressedOops",
                "-XX:+OptimizeStringConcat",
                "-XX:+UseStringDeduplication",
                // Note: UseBiasedLocking removed in Java 19+ (don't add it back)
                "-XX:+AlwaysPreTouch",
                "-XX:+ParallelRefProcEnabled",
                "-XX:+DisableExplicitGC",
                "-XX:ParallelGCThreads=4",
                "-XX:ConcGCThreads=2"
            ),
            envVars = emptyMap()
        )
    }

    /**
     * Ultra profile for MC 1.20.x - 1.21.x (Java 17 path; Java 21 goes to Hyper+)
     */
    private fun getUltraProfile(): BoostProfile {
        val cores = Runtime.getRuntime().availableProcessors()
        return BoostProfile(
            name = "Ultra Boost (1.20-1.21)",
            description = "Maximum G1GC performance for 1.20-1.21, pause ≤ 2 ms",
            jvmArgs = listOf(
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+UseG1GC",
                "-XX:MaxGCPauseMillis=2",
                "-XX:G1NewSizePercent=20",
                "-XX:G1MaxNewSizePercent=60",
                "-XX:G1HeapRegionSize=16M",
                "-XX:G1ReservePercent=20",
                "-XX:G1HeapWastePercent=5",
                "-XX:G1MixedGCCountTarget=4",
                "-XX:InitiatingHeapOccupancyPercent=10",
                "-XX:G1MixedGCLiveThresholdPercent=90",
                "-XX:G1RSetUpdatingPauseTimePercent=5",
                "-XX:SurvivorRatio=32",
                "-XX:+PerfDisableSharedMem",
                "-XX:MaxTenuringThreshold=1",
                "-XX:+UseAdaptiveGCBoundary",
                "-XX:+UseCompressedOops",
                "-XX:+OptimizeStringConcat",
                "-XX:+UseStringDeduplication",
                "-XX:+AlwaysPreTouch",
                "-XX:+ParallelRefProcEnabled",
                "-XX:+DisableExplicitGC",
                "-XX:ParallelGCThreads=${(cores / 2).coerceIn(4, 8)}",
                "-XX:ConcGCThreads=${(cores / 4).coerceIn(2, 4)}",
                "-XX:CICompilerCount=${(cores / 2).coerceIn(3, 6)}",
                "-XX:ReservedCodeCacheSize=384M",
                "-XX:InitialCodeCacheSize=128M",
                "-XX:MaxInlineLevel=18",
                "-XX:InlineSmallCode=2500",
                "-XX:FreqInlineSize=400",
                "-Djava.net.preferIPv4Stack=true",
                "-Dnetworkaddress.cache.ttl=60"
            ),
            // NOTE: LIBGL_* and MESA_* env vars are NOT set here because they
            // conflict with renderer plugins (MobileGlues, gl4es, Zink).
            // The renderer plugin system handles GL/driver env vars correctly.
            // Setting them here would override plugin settings and cause crashes.
            envVars = emptyMap()
        )
    }

    private fun getDefaultProfile(): BoostProfile {
        return getUltraProfile().copy(name = "Default Boost", description = "Generic FPS optimization")
    }

    /**
     * Ultra FPS Mode preset — used by the Performance Center's ULTRA_FPS preset.
     *
     * Combines Hyper+ JVM tuning (for Java 21 / MC 1.21+) with the adaptive
     * device tier overlay so the JVM args are correctly scaled to the hardware.
     *
     * For Java 17 / MC < 1.21, falls back to the Ultra (1.20-1.21) profile
     * with the HIGH-tier overlay applied.
     */
    fun getUltraFpsModeProfile(context: Context, versionName: String): BoostProfile {
        val tier = detectDeviceTier(context)
        val ver  = parseVersion(versionName)
        return if (ver.first >= 21) {
            withCommonAuroraFlags(getHyperProfile(tier))
        } else {
            val base = getUltraProfile()
            overlayDeviceTier(withCommonAuroraFlags(base), tier)
        }
    }

    /**
     * Common performance JVM flags (fallback when no version detected)
     */
    fun getCommonJvmFlags(): List<String> {
        val base = listOf(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=10",
            "-XX:G1NewSizePercent=20",
            "-XX:G1MaxNewSizePercent=60",
            "-XX:G1HeapRegionSize=8M",
            "-XX:G1ReservePercent=20",
            "-XX:G1HeapWastePercent=5",
            "-XX:G1MixedGCCountTarget=4",
            "-XX:InitiatingHeapOccupancyPercent=15",
            "-XX:G1MixedGCLiveThresholdPercent=90",
            "-XX:G1RSetUpdatingPauseTimePercent=5",
            "-XX:SurvivorRatio=32",
            "-XX:+PerfDisableSharedMem",
            "-XX:MaxTenuringThreshold=1",
            "-XX:+UseCompressedOops",
            "-XX:+OptimizeStringConcat",
            "-XX:+UseStringDeduplication",
            // Note: UseBiasedLocking removed in Java 19+ (don't add it back)
            "-XX:+AlwaysPreTouch",
            "-XX:+ParallelRefProcEnabled",
            "-XX:+DisableExplicitGC"
        )
        return mergeJvmArgs(base, auroraSharedFlags())
    }

    /**
     * Aurora v3 + v5: shared JIT/code-cache/metaspace/security flags applied
     * to every profile via [withCommonAuroraFlags].
     *
     * Aurora v5 ("Turbo Pass") additions:
     * - {@code +UseFastUnorderedTimeStamps}: drops a syscall per
     *   {@code System.nanoTime()} which the renderer hits in the inner loop.
     * - {@code GuaranteedSafepointInterval=0}: disables the timer-based
     *   safepoint poll, removes a periodic ~5ms hitch cycle.
     * - {@code +DoEscapeAnalysis} / {@code +EliminateLocks}: extra inlining
     *   wins for the hot Sodium / Indium paths.
     * - {@code -Dosmesa.YInvert=1} / {@code -Dorg.lwjgl.util.NoChecks=true}:
     *   skips per-call argument validation in LWJGL once we're stable.
     */
    private fun auroraSharedFlags(): List<String> {
        return listOf(
            // Safety net: silently skip any flag the bundled JRE doesn't know about.
            // Must be first so it covers everything that follows.
            "-XX:+IgnoreUnrecognizedVMOptions",
            // Code cache / JIT — unlocks MUST come after IgnoreUnrecognizedVMOptions
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:ReservedCodeCacheSize=200M",
            "-XX:InitialCodeCacheSize=64M",
            "-XX:+SegmentedCodeCache",
            // Aurora v6: slightly lower compile threshold for faster JIT warm-up.
            // 1000 is safe; 500 caused excessive recompilation / OOM on mid-range devices.
            "-XX:CompileThreshold=1000",
            // Tier escalation — conservative values that won't overwhelm low-core devices
            "-XX:Tier3InvocationThreshold=500",
            "-XX:Tier4InvocationThreshold=5000",
            "-XX:Tier4MinInvocationThreshold=2500",
            // JIT analysis (all are standard public Hotspot flags)
            "-XX:+DoEscapeAnalysis",
            "-XX:+EliminateLocks",
            "-XX:+EliminateAllocations",
            "-XX:+UseTypeSpeculation",
            "-XX:TypeProfileLevel=222",
            "-XX:OnStackReplacePercentage=140",
            // Loop optimizations — public C2 flags, supported on Hotspot 11+
            "-XX:+UseCountedLoopSafepoints",
            "-XX:LoopStripMiningIter=1000",
            "-XX:LoopStripMiningIterShortLoop=100",
            // JIT inlining
            "-XX:+UseInlineCaches",
            "-XX:+InlineSynchronizedMethods",
            // String / allocation
            "-XX:+OptimizeStringConcat",
            // TLAB — reduce per-thread allocation contention
            "-XX:+UseTLAB",
            "-XX:+ResizeTLAB",
            // Metaspace
            "-XX:MetaspaceSize=128M",
            "-XX:MaxMetaspaceSize=384M",
            // Security / DNS / IO
            "-Djava.security.egd=file:/dev/./urandom",
            "-Dsun.io.useCanonCaches=false",
            "-Dsun.java2d.opengl=false",
            // Networking sane defaults
            "-Dsun.net.client.defaultConnectTimeout=15000",
            "-Dsun.net.client.defaultReadTimeout=30000",
            // Misc Minecraft mod-launcher friendliness
            "-Dlog4j2.formatMsgNoLookups=true",
            "-Dfml.earlyprogresswindow=false",
            // LWJGL fast path
            "-Dorg.lwjgl.util.NoChecks=true"
            // NOTE: -XX:+AggressiveOpts removed — deleted in JDK 11, causes JVM abort on 17/21.
            // NOTE: -XX:+UseFastUnorderedTimeStamps removed — diagnostic flag, absent on many
            //       Android Hotspot builds; causes JVM abort when -XX:+IgnoreUnrecognizedVMOptions
            //       is NOT present (bootstrapping race before safety net takes effect).
            // NOTE: -XX:GuaranteedSafepointInterval=0 removed — disables ALL safepoints; causes
            //       JVM hangs / ANRs on concurrent GC paths (ZGC/G1) on Android.
            // NOTE: -XX:+OptimizeFill removed — C2-only intrinsic, absent on most stripped JREs.
            // NOTE: -XX:LiveNodeCountInliningCutoff removed from shared flags (still in Hyper+).
            // NOTE: -XX:+UseLargePages / UseNUMA removed — require root on Android.
            // NOTE: -XX:ThreadPriorityPolicy=42 removed — non-standard, causes ANRs.
        )
    }

    /**
     * Append the common Aurora flags to a profile while de-duplicating any
     * keys that are already present (e.g. -Xx:Foo=bar).
     */
    private fun withCommonAuroraFlags(profile: BoostProfile): BoostProfile {
        val merged = mergeJvmArgs(profile.jvmArgs, auroraSharedFlags())
        return profile.copy(jvmArgs = merged)
    }

    /**
     * Apply tier-specific overlay on top of a base profile.
     */
    private fun overlayDeviceTier(profile: BoostProfile, tier: DeviceTier): BoostProfile {
        val cores = Runtime.getRuntime().availableProcessors()
        val tierFlags = when (tier) {
            DeviceTier.HIGH -> listOf(
                "-XX:ParallelGCThreads=${(cores / 2).coerceIn(4, 8)}",
                "-XX:ConcGCThreads=${(cores / 4).coerceIn(2, 4)}",
                "-XX:G1HeapRegionSize=16M",
                "-XX:+AlwaysPreTouch",
                "-XX:CICompilerCount=4"
            )
            DeviceTier.MID -> listOf(
                "-XX:ParallelGCThreads=${(cores / 2).coerceIn(2, 4)}",
                "-XX:ConcGCThreads=${(cores / 4).coerceIn(1, 2)}",
                "-XX:G1HeapRegionSize=8M",
                "-XX:CICompilerCount=2"
            )
            DeviceTier.LOW -> listOf(
                "-XX:ParallelGCThreads=2",
                "-XX:ConcGCThreads=1",
                "-XX:G1HeapRegionSize=4M",
                "-XX:-AlwaysPreTouch",
                "-XX:CICompilerCount=2"
                // TieredStopAtLevel=1 removed: interpreter-only mode kills in-game FPS.
                // Full tiered compilation is essential even on low-end devices.
            )
        }
        return profile.copy(
            name = profile.name + " · " + tier.name,
            description = profile.description + " (device tier: ${tier.name})",
            jvmArgs = mergeJvmArgs(profile.jvmArgs, tierFlags)
        )
    }

    /**
     * Merge two JVM arg lists; later list wins on duplicate "-XX:Flag=" /
     * "-D...=" keys so that overlays correctly override defaults.
     */
    private fun mergeJvmArgs(base: List<String>, overlay: List<String>): List<String> {
        val keyed = LinkedHashMap<String, String>()
        for (arg in base + overlay) {
            keyed[argKey(arg)] = arg
        }
        return keyed.values.toList()
    }

    private fun argKey(arg: String): String {
        val eq = arg.indexOf('=')
        return if (eq >= 0) arg.substring(0, eq) else arg
    }
}
