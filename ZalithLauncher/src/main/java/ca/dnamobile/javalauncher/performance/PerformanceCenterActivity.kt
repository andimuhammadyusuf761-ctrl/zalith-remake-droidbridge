/*
 * Copyright (c) 2026 DNA Mobile Applications. All rights reserved.
 * DroidBridge project code — Performance Center Activity.
 */
package ca.dnamobile.javalauncher.performance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ca.dnamobile.javalauncher.feature.log.Logging
import ca.dnamobile.javalauncher.settings.MemoryAllocationUtils
import ca.dnamobile.javalauncher.utils.FullscreenUtils
import com.movtery.zalithlauncher.ui.compose.PerformanceCenterScreen

/**
 * Hosts the Smart Performance Center Compose screen.
 *
 * Open with [PerformanceCenterActivity.intent].
 */
class PerformanceCenterActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PerfCenterActivity"

        @JvmStatic
        fun intent(ctx: Context): Intent = Intent(ctx, PerformanceCenterActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FullscreenUtils.enableImmersive(this)

        val deviceLabel = DeviceBenchmark.getDeviceLabel()
        val cpuInfo     = DeviceBenchmark.getCpuInfo()
        val totalRam    = DeviceBenchmark.getTotalRamMb(this)
        val availRam    = DeviceBenchmark.getAvailableRamMb(this)
        val presetId    = PerformanceCenterPreferences.getActivePreset(this).name
        val benchScore  = PerformanceCenterPreferences.getBenchmarkScore(this)

        Logging.i(TAG, "Opening Performance Center — preset=$presetId, benchScore=$benchScore")

        setContent {
            PerformanceCenterScreen(
                deviceLabel    = deviceLabel,
                cpuInfo        = cpuInfo,
                totalRamMb     = totalRam,
                availRamMb     = availRam,
                savedPresetId  = presetId,
                savedBenchScore = benchScore,
                onPresetSelected = { selectedId ->
                    try {
                        val preset = PerformancePreset.valueOf(selectedId)
                        PerformanceCenterPreferences.applyPreset(this, preset)
                        PerformanceCenterPreferences.setUltraFpsGlFlagsEnabled(
                            this, preset == PerformancePreset.ULTRA_FPS
                        )
                        Logging.i(TAG, "Preset applied: ${preset.displayName}")
                    } catch (e: IllegalArgumentException) {
                        Logging.w(TAG, "Unknown preset id: $selectedId")
                    }
                },
                onRunBenchmark = { callback ->
                    DeviceBenchmark.runAndAutoOptimise(this, false) { result ->
                        PerformanceCenterPreferences.saveBenchmarkResult(this, result.score)
                        callback(result.score)
                    }
                },
                onAutoOptimize = {
                    DeviceBenchmark.runAndAutoOptimise(this, true) { result ->
                        Logging.i(TAG, "Auto-optimize applied: ${result.recommendedPreset.displayName}")
                    }
                }
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) FullscreenUtils.enableImmersive(this)
    }
}
