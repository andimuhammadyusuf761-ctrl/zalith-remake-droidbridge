package com.movtery.zalithlauncher.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class PremiumDestination(val label: String) { Home("Home"), Versions("Versions"), Play("Play"), Settings("Settings") }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PremiumLauncherApp(onOpenPerformanceCenter: () -> Unit = {}) {
    var destination by rememberSaveable { mutableStateOf(PremiumDestination.Home) }
    ZalithTheme {
        SharedTransitionLayout {
            Column(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = destination,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.98f)).togetherWith(fadeOut() + scaleOut(targetScale = 1.02f)) },
                    modifier = Modifier.weight(1f),
                    label = "Premium screen transition"
                ) { target ->
                    when (target) {
                        PremiumDestination.Home -> HomeScreen(onPlay = { destination = PremiumDestination.Play }, onSettings = { destination = PremiumDestination.Settings })
                        PremiumDestination.Versions -> VersionsScreen()
                        PremiumDestination.Play -> PlayScreen()
                        PremiumDestination.Settings -> SettingsScreen(onOpenPerformanceCenter = onOpenPerformanceCenter)
                    }
                }
                PremiumNavigationBar(destination) { destination = it }
            }
        }
    }
}

@Composable
private fun PremiumNavigationBar(current: PremiumDestination, onNavigate: (PremiumDestination) -> Unit) {
    NavigationBar(containerColor = ZalithCard) {
        PremiumDestination.entries.forEach { destination ->
            val icon = when (destination) {
                PremiumDestination.Home -> Icons.Rounded.Home
                PremiumDestination.Versions -> Icons.Rounded.Extension
                PremiumDestination.Play -> Icons.Rounded.PlayArrow
                PremiumDestination.Settings -> Icons.Rounded.Settings
            }
            NavigationBarItem(selected = current == destination, onClick = { onNavigate(destination) }, icon = { Icon(icon, null) }, label = { Text(destination.label) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(onPlay: () -> Unit = {}, onSettings: () -> Unit = {}) {
    PremiumBackground {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            HeroCard(title = "Zalith Launcher Pro", subtitle = "Premium Minecraft on Android — optimized, beautiful, and ready to play.", action = "Play now", onAction = onPlay)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard("FPS Boost", "Ready", "Renderer and memory tuned")
                StatCard("Mods", "Safe", "Conflict scan enabled")
                StatCard("Storage", "Healthy", "No cleanup needed")
                StatCard("Theme", "Dynamic Teal", "Dark mode perfected")
            }
            Button(onClick = onSettings) { Text("Open premium settings") }
        }
    }
}

@Composable
fun VersionsScreen(versions: List<String> = listOf("1.21.1 Fabric", "1.20.6 Forge", "1.19.4 Quilt", "1.18.2 OptiFine")) {
    PremiumBackground {
        LazyColumn(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Versions", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black) }
            items(versions) { version ->
                GlassCard {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Rounded.SportsEsports, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(version, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Shared-element ready artwork, mod badges, loader chips, and last-played metadata.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = {}) { Text("Details") }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayScreen() {
    PremiumBackground {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                val pulse by animateFloatAsState(targetValue = 0.92f, label = "Play pulse")
                GlassCard(modifier = Modifier.widthIn(max = 560.dp).alpha(pulse + 0.08f)) {
                    Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Ready to launch", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("Pre-flight checks, mod backups, account status, and renderer recommendations appear here before launch.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = {}) { Text("Launch Minecraft") }
                    }
                }
                AnimatedVisibility(true, enter = fadeIn(), exit = fadeOut()) { Text("Success feedback uses glow, haptics, and compact progress states.", color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun HeroCard(title: String, subtitle: String, action: String, onAction: () -> Unit) {
    GlassCard {
        Column(Modifier.fillMaxWidth().height(220.dp).padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column { Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, summary: String) {
    GlassCard(modifier = Modifier.widthIn(min = 180.dp, max = 260.dp), radius = 22.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(summary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
