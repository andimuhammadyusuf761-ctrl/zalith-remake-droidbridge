package com.movtery.zalithlauncher.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.feature.crash.ModCrashAnalyzer
import com.movtery.zalithlauncher.setting.AllSettings
import kotlinx.coroutines.delay

enum class CrashScreenState {
    ANALYZING,
    RESULTS,
    FIXING,
    COMPLETED
}

sealed class CrashFixAction {
    abstract val label: String
    abstract val icon: @Composable () -> Unit
    abstract val description: String
    abstract val risk: RiskLevel

    enum class RiskLevel { SAFE, MODERATE, RISKY }

    data class ReduceRam(
        override val label: String = "Reduce RAM",
        override val description: String = "Lower memory allocation to prevent OOM crashes",
        override val risk: RiskLevel = RiskLevel.SAFE,
        override val icon: @Composable () -> Unit = { Icon(Icons.Rounded.Memory, null) }
    ) : CrashFixAction()

    data class ChangeRenderer(
        val currentRenderer: String,
        override val label: String = "Change Renderer",
        override val description: String = "Switch to a more stable rendering backend",
        override val risk: RiskLevel = RiskLevel.MODERATE,
        override val icon: @Composable () -> Unit = { Icon(Icons.Rounded.Refresh, null) }
    ) : CrashFixAction()

    data class ClearCache(
        override val label: String = "Clear Cache",
        override val description: String = "Remove corrupted cache files",
        override val risk: RiskLevel = RiskLevel.SAFE,
        override val icon: @Composable () -> Unit = { Icon(Icons.Rounded.Delete, null) }
    ) : CrashFixAction()

    data class DisableProblematicMods(
        val suspects: List<ModCrashAnalyzer.SuspectedMod>,
        override val label: String = "Disable Problematic Mods",
        override val description: String = "Temporarily disable mods suspected to cause crashes",
        override val risk: RiskLevel = RiskLevel.MODERATE,
        override val icon: @Composable () -> Unit = { Icon(Icons.Rounded.Warning, null) }
    ) : CrashFixAction()

    data class SafeMode(
        override val label: String = "Enter Safe Mode",
        override val description: String = "Launch with all mods disabled and default settings",
        override val risk: RiskLevel = RiskLevel.RISKY,
        override val icon: @Composable () -> Unit = { Icon(Icons.Rounded.Shield, null) }
    ) : CrashFixAction()
}

@Composable
fun CrashHandlerScreen(
    crashLogPath: String? = null,
    gameDirPath: String? = null,
    onDismiss: () -> Unit = {},
    onRestartGame: () -> Unit = {},
    onOpenLogFolder: () -> Unit = {}
) {
    var state by remember { mutableStateOf(CrashScreenState.ANALYZING) }
    var analysisResult by remember { mutableStateOf<ModCrashAnalyzer.AnalysisResult?>(null) }
    var selectedActions by remember { mutableStateOf<Set<CrashFixAction>>(emptySet()) }
    var fixProgress by remember { mutableIntStateOf(0) }
    var fixResults by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        delay(800)
        analysisResult = ModCrashAnalyzer.analyze(gameDirPath)
        state = CrashScreenState.RESULTS
    }

    PremiumBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CrashHeader(state)

            when (state) {
                CrashScreenState.ANALYZING -> AnalyzingContent()
                CrashScreenState.RESULTS -> ResultsContent(
                    result = analysisResult,
                    selectedActions = selectedActions,
                    onToggleAction = { action ->
                        selectedActions = if (selectedActions.contains(action)) {
                            selectedActions - action
                        } else {
                            selectedActions + action
                        }
                    },
                    onOpenLogFolder = onOpenLogFolder
                )
                CrashScreenState.FIXING -> FixingContent(
                    progress = fixProgress,
                    results = fixResults
                )
                CrashScreenState.COMPLETED -> CompletedContent(
                    results = fixResults,
                    onRestart = onRestartGame,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun CrashHeader(state: CrashScreenState) {
    val icon = when (state) {
        CrashScreenState.ANALYZING -> Icons.Rounded.BugReport
        CrashScreenState.RESULTS -> Icons.Rounded.ReportProblem
        CrashScreenState.FIXING -> Icons.Rounded.Refresh
        CrashScreenState.COMPLETED -> Icons.Rounded.Shield
    }

    val title = when (state) {
        CrashScreenState.ANALYZING -> "Analyzing Crash..."
        CrashScreenState.RESULTS -> "Crash Analysis"
        CrashScreenState.FIXING -> "Applying Fixes..."
        CrashScreenState.COMPLETED -> "Fixes Applied"
    }

    GlassCard(warning = state == CrashScreenState.RESULTS) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (state == CrashScreenState.RESULTS) ZalithWarn else MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (state == CrashScreenState.ANALYZING) {
                    Text(
                        text = "Scanning logs and checking mods...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyzingContent() {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )
            Text(
                text = "Analyzing crash log...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "This may take a few seconds",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultsContent(
    result: ModCrashAnalyzer.AnalysisResult?,
    selectedActions: Set<CrashFixAction>,
    onToggleAction: (CrashFixAction) -> Unit,
    onOpenLogFolder: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Summary
        GlassCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Analysis Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = result?.summary ?: "Unable to analyze crash log",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (result?.isLikelyModCrash == true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = ZalithWarn,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Mod-related crash detected",
                            color = ZalithWarn,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Suspected mods
        if (!result?.suspects.isNullOrEmpty()) {
            GlassCard(warning = true) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Error,
                            contentDescription = null,
                            tint = ZalithWarn
                        )
                        Text(
                            text = "Suspected Problematic Mods",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    result?.suspects?.forEach { suspect ->
                        SuspectedModCard(suspect)
                    }
                }
            }
        }

        // Auto-fix suggestions
        GlassCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Recommended Fixes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Generate fix suggestions based on analysis
                val suggestedFixes = generateFixSuggestions(result)

                suggestedFixes.forEach { fix ->
                    FixActionCard(
                        action = fix,
                        isSelected = selectedActions.contains(fix),
                        onToggle = { onToggleAction(fix) }
                    )
                }
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onOpenLogFolder,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Open Logs")
            }

            Button(
                onClick = { /* Apply selected fixes */ },
                modifier = Modifier.weight(1f),
                enabled = selectedActions.isNotEmpty()
            ) {
                Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Apply ${selectedActions.size} Fixes")
            }
        }
    }
}

@Composable
private fun SuspectedModCard(suspect: ModCrashAnalyzer.SuspectedMod) {
    GlassCard(elevated = false, radius = 12.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = suspect.file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                RiskBadge(suspect.score)
            }
            Text(
                text = suspect.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (suspect.modId != null) {
                Text(
                    text = "Mod ID: ${suspect.modId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RiskBadge(score: Int) {
    val (color, label) = when {
        score >= 80 -> Color(0xFFFF6B6B) to "Critical"
        score >= 50 -> ZalithWarn to "High"
        score >= 25 -> Color(0xFF4ECDC4) to "Medium"
        else -> Color(0xFF95E1D3) to "Low"
    }

    GlassCard(elevated = false, radius = 6.dp) {
        Text(
            text = "$label ($score)",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FixActionCard(
    action: CrashFixAction,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val riskColor = when (action.risk) {
        CrashFixAction.RiskLevel.SAFE -> MaterialTheme.colorScheme.primary
        CrashFixAction.RiskLevel.MODERATE -> ZalithWarn
        CrashFixAction.RiskLevel.RISKY -> Color(0xFFFF6B6B)
    }

    GlassCard(
        elevated = isSelected,
        warning = action.risk == CrashFixAction.RiskLevel.RISKY
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when (action) {
                    is CrashFixAction.ReduceRam -> Icons.Rounded.Memory
                    is CrashFixAction.ChangeRenderer -> Icons.Rounded.Refresh
                    is CrashFixAction.ClearCache -> Icons.Rounded.Delete
                    is CrashFixAction.DisableProblematicMods -> Icons.Rounded.Warning
                    is CrashFixAction.SafeMode -> Icons.Rounded.Shield
                    else -> Icons.Rounded.Info
                },
                contentDescription = null,
                tint = riskColor,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RiskLabel(action.risk)
            }

            androidx.compose.material3.Switch(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun RiskLabel(risk: CrashFixAction.RiskLevel) {
    val (color, text) = when (risk) {
        CrashFixAction.RiskLevel.SAFE -> MaterialTheme.colorScheme.primary to "Safe"
        CrashFixAction.RiskLevel.MODERATE -> ZalithWarn to "Moderate Risk"
        CrashFixAction.RiskLevel.RISKY -> Color(0xFFFF6B6B) to "High Risk"
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun FixingContent(progress: Int, results: List<String>) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )

            Text(
                text = "Applying fixes...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompletedContent(
    results: List<String>,
    onRestart: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Fixes Applied Successfully",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (results.isNotEmpty()) {
                Text(
                    text = "Actions taken:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                results.forEach { result ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = result,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }
                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Restart Game")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
private fun generateFixSuggestions(result: ModCrashAnalyzer.AnalysisResult?): List<CrashFixAction> {
    val actions = mutableListOf<CrashFixAction>()

    // Always suggest safe options
    actions.add(CrashFixAction.ReduceRam())
    actions.add(CrashFixAction.ClearCache())

    // Add renderer change if graphics-related
    if (result?.logFile?.readText()?.contains("OpenGL|GLFW|Renderer", ignoreCase = true) == true) {
        actions.add(CrashFixAction.ChangeRenderer("current"))
    }

    // Add mod-related fixes
    if (!result?.suspects.isNullOrEmpty()) {
        result?.suspects?.let {
            actions.add(CrashFixAction.DisableProblematicMods(it))
        }
    }

    // Always add safe mode as last resort
    actions.add(CrashFixAction.SafeMode())

    return actions
}