package com.movtery.zalithlauncher.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ── Teal Ocean design tokens ───────────────────────────────────────────────
private val TealPrimary   = Color(0xFF00BFA5)
private val TealDeep      = Color(0xFF00897B)
private val TealBright    = Color(0xFF1DE9B6)
private val CyanAccent    = Color(0xFF00E5FF)
private val Slate900      = Color(0xFF0F172A)
private val Slate800      = Color(0xFF1E293B)
private val Slate700      = Color(0xFF334155)
private val GlassSurface  = Color(0x1A1E293B)
private val GlassBorder   = Color(0x2600E5FF)
private val WarningAmber  = Color(0xFFF59E0B)
private val SuccessGreen  = Color(0xFF10B981)
private val ErrorRed      = Color(0xFFEF4444)
private val TextPrimary   = Color(0xFFF0FDFC)
private val TextMuted     = Color(0xFF80CBC4)

// ── Preset model (mirroring the Java enum for Compose) ────────────────────
data class PresetUiModel(
    val id: String,
    val name: String,
    val tagline: String,
    val icon: ImageVector,
    val accentColor: Color,
    val resolutionHint: String,
    val gcHint: String,
    val isNew: Boolean = false
)

private val allPresets = listOf(
    PresetUiModel("ULTRA_FPS",    "Ultra FPS ⚡",   "VBO·VAO·GL thread·shader cache",  Icons.Rounded.Bolt,               TealBright,  "70% res",  "G1 ≤1ms",  isNew = true),
    PresetUiModel("MAXIMUM_FPS",  "Maximum FPS",    "Big cores · G1GC ≤2ms",           Icons.Rounded.Speed,               TealPrimary, "75% res",  "G1 ≤2ms"),
    PresetUiModel("BALANCED",     "Balanced",       "Smooth · thermal-safe",            Icons.Rounded.Tune,                CyanAccent,  "100%",     "G1 ≤8ms"),
    PresetUiModel("QUALITY",      "Quality",        "Native res · full mipmaps",        Icons.Rounded.Star,                Color(0xFFB7C7FF), "100%", "G1 ≤10ms"),
    PresetUiModel("LOW_END",      "Low-End",        "2–4 GB RAM · 4-core safe",         Icons.Rounded.Memory,              WarningAmber,"60% res",  "G1 ≤15ms"),
    PresetUiModel("BATTERY_SAVER","Battery Saver",  "Cool · quiet · 30 FPS target",    Icons.Rounded.BatteryChargingFull, Color(0xFF4CAF50), "50% res", "G1 ≤25ms")
)

// ── Entry point ────────────────────────────────────────────────────────────

/**
 * Smart Performance Center — full-screen Compose UI.
 *
 * @param deviceLabel      e.g. "Samsung Galaxy S23 Ultra (API 33)"
 * @param cpuInfo          e.g. "8 cores · arm64-v8a"
 * @param totalRamMb       Total device RAM in MB
 * @param availRamMb       Currently available RAM in MB
 * @param savedPresetId    The ID of the currently saved preset
 * @param savedBenchScore  Last benchmark score, or 0 if none
 * @param onPresetSelected Called when user picks a preset
 * @param onRunBenchmark   Called to start benchmark; returns score via callback
 * @param onAutoOptimize   Called for auto-optimise (runs benchmark + applies best preset)
 */
@Composable
fun PerformanceCenterScreen(
    deviceLabel: String = "Galaxy S24 Ultra (API 34)",
    cpuInfo: String = "8 cores · arm64-v8a",
    totalRamMb: Int = 8192,
    availRamMb: Int = 3200,
    savedPresetId: String = "BALANCED",
    savedBenchScore: Int = 0,
    onPresetSelected: (String) -> Unit = {},
    onRunBenchmark: ((Int) -> Unit) -> Unit = { cb -> cb(1650) },
    onAutoOptimize: () -> Unit = {}
) {
    var activePresetId by rememberSaveable { mutableStateOf(savedPresetId) }
    var benchScore     by rememberSaveable { mutableIntStateOf(savedBenchScore) }
    var benchRunning   by remember { mutableStateOf(false) }
    var benchDone      by remember { mutableStateOf(savedBenchScore > 0) }
    var autoRunning    by remember { mutableStateOf(false) }

    // Simulated real-time metrics (in production, read from game process IPC)
    var simFps   by remember { mutableIntStateOf(58) }
    var simCpu   by remember { mutableFloatStateOf(0.62f) }
    var simGpu   by remember { mutableFloatStateOf(0.74f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            simFps  = (45..144).random()
            simCpu  = (40..90).random() / 100f
            simGpu  = (50..95).random() / 100f
        }
    }

    ZalithTheme {
        PremiumBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Header ─────────────────────────────────────────────
                PerfHeader()

                // ── Live metrics strip ─────────────────────────────────
                LiveMetricsStrip(fps = simFps, cpu = simCpu, gpu = simGpu)

                // ── Device info card ───────────────────────────────────
                DeviceInfoCard(
                    label    = deviceLabel,
                    cpuInfo  = cpuInfo,
                    totalRam = totalRamMb,
                    availRam = availRamMb
                )

                // ── Preset selector ────────────────────────────────────
                PresetSelectorSection(
                    presets          = allPresets,
                    activeId         = activePresetId,
                    onPresetSelected = { id ->
                        activePresetId = id
                        onPresetSelected(id)
                    }
                )

                // ── Benchmark card ─────────────────────────────────────
                BenchmarkCard(
                    score      = benchScore,
                    running    = benchRunning,
                    done       = benchDone,
                    onRun      = {
                        if (!benchRunning) {
                            benchRunning = true
                            benchDone    = false
                            onRunBenchmark { score ->
                                benchScore   = score
                                benchRunning = false
                                benchDone    = true
                            }
                        }
                    }
                )

                // ── Auto-optimise button ───────────────────────────────
                AutoOptimizeCard(
                    running   = autoRunning,
                    onOptimize = {
                        if (!autoRunning) {
                            autoRunning = true
                            onAutoOptimize()
                            // UI-only: reset after 3s if no real callback
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Header ─────────────────────────────────────────────────────────────────

@Composable
private fun PerfHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Performance Center",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }
        Text(
            text = "Tune presets, monitor live metrics, and benchmark your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

// ── Live metrics strip ──────────────────────────────────────────────────────

@Composable
private fun LiveMetricsStrip(fps: Int, cpu: Float, gpu: Float) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveMetric(label = "FPS", valueStr = fps.toString(), accent = TealBright, isHighGood = true, ratio = (fps / 144f).coerceIn(0f, 1f))
            VerticalDivider()
            LiveMetric(label = "CPU", valueStr = "${(cpu * 100).toInt()}%", accent = CyanAccent, isHighGood = false, ratio = cpu)
            VerticalDivider()
            LiveMetric(label = "GPU", valueStr = "${(gpu * 100).toInt()}%", accent = TealPrimary, isHighGood = false, ratio = gpu)
        }
    }
}

@Composable
private fun LiveMetric(label: String, valueStr: String, accent: Color, isHighGood: Boolean, ratio: Float) {
    val animRatio by animateFloatAsState(ratio, tween(600, easing = FastOutSlowInEasing), label = "metric_$label")
    val barColor  by animateColorAsState(
        targetValue = when {
            isHighGood && ratio > 0.6f -> accent
            !isHighGood && ratio > 0.85f -> ErrorRed
            !isHighGood && ratio > 0.7f  -> WarningAmber
            else -> accent
        },
        label = "bar_$label"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Circular gauge
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animRatio },
                modifier = Modifier.size(60.dp),
                color = barColor,
                trackColor = Slate700,
                strokeWidth = 5.dp,
                strokeCap = StrokeCap.Round
            )
            Text(
                text = valueStr,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(56.dp)
            .background(GlassBorder)
    )
}

// ── Device info card ────────────────────────────────────────────────────────

@Composable
private fun DeviceInfoCard(label: String, cpuInfo: String, totalRam: Int, availRam: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.DevicesOther, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
                Text("Device", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(cpuInfo, style = MaterialTheme.typography.bodySmall, color = TextMuted)

            // RAM bar
            val ramUsedFraction = ((totalRam - availRam).toFloat() / totalRam.toFloat()).coerceIn(0f, 1f)
            val ramBarAnim by animateFloatAsState(ramUsedFraction, tween(800), label = "ramBar")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Memory, null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RAM", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${availRam} MB free / ${totalRam} MB", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    LinearProgressIndicator(
                        progress = { ramBarAnim },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (ramUsedFraction > 0.85f) ErrorRed else TealPrimary,
                        trackColor = Slate700,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// ── Preset selector ─────────────────────────────────────────────────────────

@Composable
private fun PresetSelectorSection(
    presets: List<PresetUiModel>,
    activeId: String,
    onPresetSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Tune, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
            Text("Performance Preset", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(presets, key = { it.id }) { preset ->
                PresetCard(
                    preset   = preset,
                    selected = preset.id == activeId,
                    onClick  = { onPresetSelected(preset.id) }
                )
            }
        }
    }
}

@Composable
private fun PresetCard(preset: PresetUiModel, selected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) preset.accentColor else GlassBorder,
        tween(250),
        label = "border_${preset.id}"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (selected) 0.18f else 0.08f,
        tween(250),
        label = "bg_${preset.id}"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.97f,
        tween(200, easing = FastOutSlowInEasing),
        label = "scale_${preset.id}"
    )

    Column(
        modifier = Modifier
            .scale(scaleAnim)
            .width(148.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(preset.accentColor.copy(alpha = bgAlpha))
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(preset.icon, null, tint = preset.accentColor, modifier = Modifier.size(22.dp))
            if (preset.isNew) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(preset.accentColor.copy(0.2f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text("NEW", style = MaterialTheme.typography.labelSmall, color = preset.accentColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(preset.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(preset.tagline, style = MaterialTheme.typography.bodySmall, color = TextMuted, lineHeight = 16.sp)

        // Spec pills
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            SpecPill(preset.resolutionHint, preset.accentColor)
            SpecPill(preset.gcHint, preset.accentColor)
        }

        // Selected indicator
        AnimatedVisibility(selected) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.CheckCircle, null, tint = preset.accentColor, modifier = Modifier.size(14.dp))
                Text("Active", style = MaterialTheme.typography.labelSmall, color = preset.accentColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpecPill(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// ── Benchmark card ──────────────────────────────────────────────────────────

@Composable
private fun BenchmarkCard(score: Int, running: Boolean, done: Boolean, onRun: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Analytics, null, tint = CyanAccent, modifier = Modifier.size(22.dp))
                Text("Device Benchmark", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Runs a quick CPU + memory workload (~500ms) to score your device and recommend the best preset.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            // Score display
            AnimatedContent(
                targetState = Pair(running, done),
                transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(200))) },
                label = "bench_state"
            ) { (isRunning, isDone) ->
                when {
                    isRunning -> BenchmarkRunningIndicator()
                    isDone    -> BenchmarkScoreDisplay(score)
                    else      -> Text("No benchmark run yet.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }

            Button(
                onClick = onRun,
                enabled = !running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor   = Slate900
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (running) "Running…" else if (done) "Re-run Benchmark" else "Run Benchmark", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BenchmarkRunningIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "bench_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1.0f, label = "pulse",
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse)
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(color = CyanAccent, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        Column {
            Text("Benchmarking…", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("CPU float · CPU int · Memory bandwidth", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.alpha(pulse))
        }
    }
}

@Composable
private fun BenchmarkScoreDisplay(score: Int) {
    val tier = when {
        score >= 2000 -> Triple("FLAGSHIP", TealBright, "Ultra FPS recommended")
        score >= 1400 -> Triple("HIGH",     TealPrimary, "Maximum FPS recommended")
        score >= 900  -> Triple("MID",      CyanAccent, "Balanced recommended")
        score >= 500  -> Triple("LOW",      WarningAmber, "Low-End recommended")
        else          -> Triple("MINIMAL",  ErrorRed, "Battery Saver recommended")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Big score number
        Box(
            Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(tier.second.copy(0.25f), Color.Transparent),
                        radius = 80f
                    )
                )
                .border(2.dp, tier.second, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = tier.second,
                fontFamily = FontFamily.Monospace
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(tier.second))
                Text(tier.first, style = MaterialTheme.typography.titleSmall, color = tier.second, fontWeight = FontWeight.Bold)
            }
            Text(tier.third, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text("Score: $score pts", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontFamily = FontFamily.Monospace)
        }
    }
}

// ── Auto-optimise card ──────────────────────────────────────────────────────

@Composable
private fun AutoOptimizeCard(running: Boolean, onOptimize: () -> Unit) {
    // Animated teal pulse glow
    val infiniteTransition = rememberInfiniteTransition(label = "auto_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f, label = "glow",
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(TealDeep.copy(0.5f), Slate800.copy(0.8f)),
                    start  = Offset.Zero,
                    end    = Offset(Float.POSITIVE_INFINITY, 0f)
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(TealBright.copy(glowAlpha), CyanAccent.copy(glowAlpha * 0.5f))),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.AutoFixHigh, null, tint = TealBright, modifier = Modifier.size(24.dp))
                Column {
                    Text("Auto-Optimize", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black)
                    Text("Benchmark → profile → apply the best preset for your device in one tap.",
                         style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }

            Button(
                onClick = onOptimize,
                enabled = !running,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealPrimary,
                    contentColor   = Slate900
                ),
                shape  = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (running) {
                    CircularProgressIndicator(
                        color    = Slate900,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Analyzing device…", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.AutoFixHigh, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-Optimize My Device", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text  = "ⓘ  Runs benchmark + applies the recommended preset automatically.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
