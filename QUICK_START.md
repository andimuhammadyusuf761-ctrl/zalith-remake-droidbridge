# Quick Start Guide - Zalith Launcher Pro

## Build Instructions

```bash
# 1. Build native libraries
./gradlew jre_lwjgl3glfw:build --no-daemon

# 2. Build debug APK (arm64)
./gradlew ZalithLauncher:assembleDebug -Darch=arm64

# 3. Build release APK
./gradlew ZalithLauncher:assembleRelease -Darch=arm64
```

## Key Features

### 1. Crash Handling & Auto Fix
- Automatic crash log analysis
- Smart fix suggestions (RAM, renderer, cache, mods)
- Risk-level indicators (Safe/Moderate/Risky)
- One-click apply fixes

### 2. Input Sounds & Haptics
- Ultra-low-latency SoundPool
- Procedural audio (no external assets)
- Mechanical keyboard sounds, mouse clicks
- Synchronized haptic feedback

### 3. Premium UI (Jetpack Compose)
- Glass morphism design system
- 3D depth effects, smooth animations
- Teal/ocean color scheme
- Material 3 components

### 4. Settings Overhaul
- 13 organized categories
- Global search, quick toggles
- Import/export functionality
- Real-time previews

### 5. Mod Management
- Advanced mod list with metadata
- Conflict detection (Duplicate IDs, circular deps)
- One-click enable/disable/delete
- Backup/restore system
- Modpack export/import (.zlmodpack)

### 6. FPS Boost System
- Version-specific optimization profiles
- G1GC tuning, JIT optimizations
- GL4ES/Mesa/Zink tweaks
- Device tier detection (HIGH/MID/LOW)

## File Structure

```
ZalithLauncher/src/main/java/com/movtery/zalithlauncher/
├── feature/
│   ├── crash/ModCrashAnalyzer.kt
│   ├── mod/ModManager.kt
│   └── sound/SoundManager.kt
├── launch/
│   ├── FPSBoostConfig.kt
│   └── RendererAutoOptimizer.kt
├── renderer/
│   └── RendererPluginManager.kt
├── setting/
│   ├── AllSettings.kt
│   └── Settings.kt
└── ui/compose/
    ├── CrashHandlerScreen.kt
    ├── PremiumDesignSystem.kt
    ├── PremiumMainScreens.kt
    └── SettingsExperience.kt
```

## License
GPL-3.0 - Same as original ZalithLauncher

## Credits
- Original: ZalithLauncher
- FPS Boost & GUI Remake: Sittirahmadia
- This Major Upgrade: Enhanced Edition
