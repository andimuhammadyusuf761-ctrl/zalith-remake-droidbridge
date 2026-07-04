package com.movtery.zalithlauncher.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

interface SettingsPreferenceStore {
    fun boolean(key: String, default: Boolean): Boolean
    fun float(key: String, default: Float): Float
    fun putBoolean(key: String, value: Boolean)
    fun putFloat(key: String, value: Float)
    fun exportAll(): String
    fun importAll(serialized: String)
    fun resetSection(sectionId: String)
}

data class SettingSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val risky: Boolean = false,
    val items: List<SettingSpec>
)

sealed class SettingSpec(open val key: String, open val title: String, open val summary: String) {
    data class Toggle(override val key: String, override val title: String, override val summary: String, val default: Boolean, val risky: Boolean = false) : SettingSpec(key, title, summary)
    data class Range(override val key: String, override val title: String, override val summary: String, val default: Float, val range: ClosedFloatingPointRange<Float>, val suffix: String = "") : SettingSpec(key, title, summary)
    data class Action(override val key: String, override val title: String, override val summary: String, val emphasis: Boolean = false) : SettingSpec(key, title, summary)
}

val PremiumSettingsSections = listOf(
    SettingSection("general", "General", "Language, startup behavior, and launcher flow.", Icons.Rounded.Tune, items = listOf(
        SettingSpec.Toggle("quick_resume", "Quick resume", "Open directly to the last played profile.", true),
        SettingSpec.Toggle("confirm_before_launch", "Confirm before launch", "Show a compact launch summary first.", false)
    )),
    SettingSection("appearance", "Appearance & Theme", "Dynamic color, accent, typography, and motion.", Icons.Rounded.ColorLens, items = listOf(
        SettingSpec.Toggle("dynamic_color", "Dynamic color", "Blend Material You colors with Zalith teal.", true),
        SettingSpec.Range("font_scale", "Font size", "Accessibility-friendly text scaling.", 1f, 0.85f..1.35f, "×"),
        SettingSpec.Toggle("reduced_motion", "Reduced motion", "Prefer fades over parallax and bounce.", false),
        SettingSpec.Action("accent_picker", "Custom accent picker", "Choose a signature color for cards and buttons.")
    )),
    SettingSection("input", "Input & Controls", "Touch controls, gestures, mouse, keyboard, and haptics.", Icons.Rounded.Gamepad, items = listOf(
        SettingSpec.Range("control_opacity", "Control opacity", "Preview control visibility over gameplay.", 0.72f, 0.25f..1f),
        SettingSpec.Range("gesture_sensitivity", "Gesture sensitivity", "Tune swipes, taps, and camera gestures.", 0.55f, 0f..1f),
        SettingSpec.Range("keyboard_mouse_volume", "Keyboard/mouse sound volume", "Feedback volume for physical input.", 0.35f, 0f..1f)
    )),
    SettingSection("audio", "Audio & Haptics", "Master sound, UI cues, enhancement, and vibration.", Icons.Rounded.Audiotrack, items = listOf(
        SettingSpec.Range("master_volume", "Master volume", "Global launcher and game volume preference.", 0.8f, 0f..1f),
        SettingSpec.Toggle("ui_sounds", "UI sounds", "Subtle premium clicks and success tones.", true),
        SettingSpec.Toggle("sound_enhancement", "In-game sound enhancement", "Improve clarity for small speakers.", false),
        SettingSpec.Range("haptic_intensity", "Haptic intensity", "Vibration strength for controls and actions.", 0.45f, 0f..1f)
    )),
    SettingSection("mods", "Mods & Resource Packs", "Updates, conflicts, backups, and launch safety.", Icons.Rounded.Science, items = listOf(
        SettingSpec.Toggle("auto_update_mods", "Auto-update mods", "Check compatible updates before launch.", false),
        SettingSpec.Toggle("mod_conflict_check", "Conflict check on launch", "Warn about duplicate or incompatible mods.", true),
        SettingSpec.Toggle("mod_backup", "Back up mods", "Snapshot mods before risky changes.", true)
    )),
    SettingSection("performance", "Performance", "Open Performance Center for renderer, memory, and FPS tuning.", Icons.Rounded.Bolt, items = listOf(
        SettingSpec.Action("open_performance_center", "Performance Center", "Recommended RAM, renderer, FPS boost, and diagnostics.", true)
    )),
    SettingSection("account", "Account & Security", "Accounts, tokens, privacy, and device trust.", Icons.Rounded.Security, items = listOf(
        SettingSpec.Toggle("require_biometric", "Biometric lock", "Protect accounts with device authentication.", false),
        SettingSpec.Toggle("crash_reporting", "Crash reporting", "Send crash diagnostics to improve stability.", true),
        SettingSpec.Toggle("analytics", "Analytics opt-in", "Share anonymous feature usage.", false),
        SettingSpec.Action("data_export", "Export privacy data", "Create a portable archive of launcher data.")
    )),
    SettingSection("downloads", "Downloads & Storage", "Versions, cache, cleanup, and usage breakdown.", Icons.Rounded.Storage, items = listOf(
        SettingSpec.Toggle("auto_cleanup", "Auto-cleanup old versions", "Suggest removing unused runtime files.", false),
        SettingSpec.Range("cache_limit", "Cache size limit", "Maximum cache before cleanup recommendations.", 4f, 1f..16f, " GB"),
        SettingSpec.Action("storage_breakdown", "Storage usage breakdown", "See versions, assets, mods, logs, and cache.")
    )),
    SettingSection("advanced", "Advanced", "Debugging, experiments, safe mode, and developer tools.", Icons.Rounded.Keyboard, risky = true, items = listOf(
        SettingSpec.Toggle("verbose_debugging", "Verbose debugging", "Creates larger logs and may reduce performance.", false, risky = true),
        SettingSpec.Toggle("experimental_features", "Experimental features", "May be unstable or change without notice.", false, risky = true),
        SettingSpec.Toggle("safe_mode", "Safe mode", "Disable mods and risky optimizations next launch.", false),
        SettingSpec.Action("log_level", "Log level", "Choose Info, Debug, Trace, or Error only.")
    )),
    SettingSection("about", "About", "Launcher version, licenses, credits, and support.", Icons.Rounded.Info, items = listOf(
        SettingSpec.Action("about_launcher", "About Zalith Launcher Pro", "Credits, changelog, licenses, and support links.")
    ))
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    sections: List<SettingSection> = PremiumSettingsSections,
    onOpenPerformanceCenter: () -> Unit = {},
    onImportSettings: () -> Unit = {},
    onExportSettings: () -> Unit = {},
    onApplyRecommended: () -> Unit = {}
) {
    var query by rememberSaveable { mutableStateOf("") }
    val expanded = remember { mutableStateMapOf<String, Boolean>().apply { sections.forEach { this[it.id] = true } } }
    val filtered = sections.mapNotNull { section ->
        val matching = section.items.filter { it.title.contains(query, true) || it.summary.contains(query, true) || section.title.contains(query, true) }
        if (query.isBlank() || matching.isNotEmpty()) section.copy(items = if (query.isBlank()) section.items else matching) else null
    }

    PremiumBackground {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text("Premium control center for visuals, gameplay, storage, privacy, and advanced launch safety.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                label = { Text("Search every setting") },
                singleLine = true
            )
            QuickSettingsRow(onImportSettings, onExportSettings, onApplyRecommended)
            filtered.forEach { section ->
                SettingsSectionCard(section, expanded[section.id] == true, onToggleExpanded = { expanded[section.id] = !(expanded[section.id] ?: false) }, onOpenPerformanceCenter = onOpenPerformanceCenter)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickSettingsRow(onImport: () -> Unit, onExport: () -> Unit, onRecommended: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AssistChip(onClick = onRecommended, label = { Text("Recommended Settings") }, leadingIcon = { Icon(Icons.Rounded.Bolt, null) })
        AssistChip(onClick = onImport, label = { Text("Import") }, leadingIcon = { Icon(Icons.Rounded.Download, null) })
        AssistChip(onClick = onExport, label = { Text("Export") }, leadingIcon = { Icon(Icons.Rounded.SaveAlt, null) })
    }
}

@Composable
fun SettingsSectionCard(section: SettingSection, expanded: Boolean, onToggleExpanded: () -> Unit, onOpenPerformanceCenter: () -> Unit) {
    GlassCard(warning = section.risky) {
        Column(Modifier.animateContentSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(section.icon, null, modifier = Modifier.size(32.dp), tint = if (section.risky) ZalithWarn else MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(section.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onToggleExpanded) { Icon(Icons.Rounded.Animation, contentDescription = if (expanded) "Collapse" else "Expand") }
                IconButton(onClick = { }) { Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset ${section.title} to default") }
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    section.items.forEachIndexed { index, item ->
                        SettingItem(item, onAction = { if (item.key == "open_performance_center") onOpenPerformanceCenter() })
                        if (index != section.items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    }
                    if (section.id == "appearance") ThemePreview()
                    if (section.id == "input") ControlPreview()
                    if (section.id == "downloads") StoragePreview()
                }
            }
        }
    }
}

@Composable
fun SettingItem(spec: SettingSpec, onAction: () -> Unit) {
    when (spec) {
        is SettingSpec.Toggle -> {
            var checked by rememberSaveable(spec.key) { mutableStateOf(spec.default) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(spec.title, fontWeight = FontWeight.SemiBold)
                    Text(spec.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    if (spec.risky) Text("Advanced option — review before enabling.", color = ZalithWarn, style = MaterialTheme.typography.labelMedium)
                }
                Switch(checked = checked, onCheckedChange = { checked = it })
            }
        }
        is SettingSpec.Range -> {
            var value by rememberSaveable(spec.key) { mutableFloatStateOf(spec.default) }
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(spec.title, fontWeight = FontWeight.SemiBold)
                    Text("${"%.2f".format(value)}${spec.suffix}", color = MaterialTheme.colorScheme.primary)
                }
                Text(spec.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Slider(value = value, onValueChange = { value = it }, valueRange = spec.range)
            }
        }
        is SettingSpec.Action -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(spec.title, fontWeight = FontWeight.SemiBold)
                Text(spec.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onAction) { Text(if (spec.emphasis) "Open" else "Manage") }
        }
    }
}

@Composable
private fun ThemePreview() {
    GlassCard(elevated = false, radius = 20.dp) { Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { FilterChip(true, {}, { Text("Teal") }); FilterChip(false, {}, { Text("Amethyst") }); FilterChip(false, {}, { Text("Copper") }) } }
}

@Composable
private fun ControlPreview() {
    GlassCard(elevated = false, radius = 20.dp) { Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { listOf("Jump", "Sneak", "Attack").forEach { Button(onClick = {}) { Text(it) } } } }
}

@Composable
private fun StoragePreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.PrivacyTip, null); Text("Storage preview: versions 42%, assets 31%, mods 18%, cache 9%") }
        LinearProgressIndicator(progress = { 0.42f }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(2.dp))
    }
}
