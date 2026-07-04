# Zalith Remake - FPS Boost Edition

A modified version of [ZalithLauncher](https://github.com/ZalithLauncher/ZalithLauncher) optimized for maximum FPS performance when running Minecraft Java Edition on Android, with a complete 3D GUI overhaul.

## What's Changed

### FPS Boost v2 - Version-Specific Optimization
Automatically detects your Minecraft version and applies the optimal JVM profile:

| Profile | MC Versions | Focus |
|---------|-------------|-------|
| **Legacy Boost** | 1.8.x - 1.12.x | Fast JIT warmup, lighter GC for older versions |
| **Modern Boost** | 1.13.x - 1.16.x | Balanced GC + string dedup for post-flattening |
| **Heavy Boost** | 1.17.x - 1.19.x | Aggressive GC for Caves & Cliffs heavy terrain gen |
| **Ultra Boost** | 1.20.x - 1.21.x | Maximum throughput for latest versions |

### Core Optimizations
- **G1GC Tuning**: 5-10ms pause target with `UnlockExperimentalVMOptions`
- **JIT Compilation**: CompressedOops, StringDeduplication, OptimizeStringConcat
- **GL4ES Performance**: VBO, FastMath, Batch rendering, VAO cache
- **Mesa/Zink**: GL 4.6 override, GLSL cache, no-error mode, lazy descriptors
- **Memory**: AlwaysPreTouch, ParallelRefProc, DisableExplicitGC
- **Defaults**: 80% resolution, sustained performance ON, big core affinity ON

### 3D GUI Overhaul
Every UI element has been redesigned with 3D depth effects:

- **3D Play Button** - Gradient with glass shine overlay and drop shadow
- **3D Menu Items** - Raised cards with shadow + top highlight + teal ripple
- **3D Settings Cards** - Layered depth with glass morphism effect
- **3D Dialogs** - Popup shadow with border glow and glass highlight
- **3D Buttons** - Press state changes depth (sinks in when pressed)
- **3D Edit Boxes** - Recessed inset shadow (looks carved into surface)
- **3D Top Bar** - Gradient sweep with light reflection and bottom edge shadow
- **3D Version Selector** - Gradient card with subtle border
- **Ambient Glow Effects** - Teal radial glows behind key elements
- **Modern Color Scheme** - Teal-ocean (light) + Slate-teal (dark mode)

## Build

```bash
chmod +x gradlew
./gradlew jre_lwjgl3glfw:build --no-daemon
./gradlew ZalithLauncher:assembleDebug -Darch=arm64
```

## Credits

- Original: [ZalithLauncher](https://github.com/ZalithLauncher/ZalithLauncher)
- FPS Boost & GUI Remake by Sittirahmadia
## License

Licensed under GPL-3.0 - same as the original ZalithLauncher.
