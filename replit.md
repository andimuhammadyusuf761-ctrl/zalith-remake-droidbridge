# Zalith Remake — DroidBridge Edition

A fork of [ZalithLauncher](https://github.com/ZalithLauncher/ZalithLauncher) that integrates the complete [DroidBridge Launcher](https://github.com/DNA-Mobile-Applications/DroidBridgeLauncher) GUI system and feature set.

## Overview

This project merges two Android Minecraft: Java Edition launchers:

| Component | Source |
|-----------|--------|
| FPS Boost JVM profiles | Zalith Remake (original) |
| Aurora 3D GUI + animations | Zalith Remake (original) |
| Material3 Settings UI | DroidBridge Launcher |
| Renderer plugin system | DroidBridge Launcher |
| Touch controls engine | DroidBridge Launcher |
| Gamepad input system | DroidBridge Launcher |
| Instance management | DroidBridge Launcher |
| Microsoft account / skin | DroidBridge Launcher |
| Mod compatibility patches | DroidBridge Launcher |
| Storage / backup system | DroidBridge Launcher |
| Update checker | DroidBridge Launcher |

## Stack

- **Language:** Kotlin + Java
- **Build:** Gradle (Android)
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **UI Framework:** AppCompat + Material3 (for DroidBridge settings)

## Build

```bash
chmod +x gradlew
./gradlew ZalithLauncher:assembleDebug -Darch=arm64
```

## Key Source Packages

| Package | Description |
|---------|-------------|
| `com.movtery.zalithlauncher.*` | Zalith Launcher core (Aurora GUI, FPS boost) |
| `ca.dnamobile.javalauncher.*` | DroidBridge system (settings, renderers, controls, auth) |
| `net.kdt.pojavlaunch.*` | PojavLauncher shared core (JRE bridge, game surface) |

## DroidBridge GUI Integration

The DroidBridge-style UI is available via:
- **Settings:** `ca.dnamobile.javalauncher.LauncherSettingsActivity` — Material3 card-based settings
- **Instance list:** `ca.dnamobile.javalauncher.ui.instance.LauncherInstanceAdapter`
- **Controls:** `ca.dnamobile.javalauncher.controls.ControlsActivity`
- **Main fragment:** `fragment_launcher_droidbridge.xml` — DroidBridge-style home

## FPS Boost Profiles (from Zalith Remake)

| Profile | MC Versions | JVM Strategy |
|---------|-------------|--------------|
| Legacy | 1.8–1.12 | Fast JIT warmup, lighter GC |
| Modern | 1.13–1.16 | Balanced GC + string dedup |
| Heavy | 1.17–1.19 | Aggressive GC for Caves & Cliffs |
| Ultra | 1.20–1.21 | Maximum throughput |
| Hyper | 1.21+ | Generational ZGC (64-bit devices) |

## User Preferences

- Keep Zalith's FPS boost code intact when making changes
- GUI style should follow DroidBridge's Material3 card pattern
- Both Aurora (Zalith) and Material3 (DroidBridge) themes must coexist
