# LibGDX Greenfoot Wrapper

A cross-platform wrapper that allows [Greenfoot](https://www.greenfoot.org/) scenarios and games to run on [libGDX](https://libgdx.com/). Export your Greenfoot projects to Desktop (Windows, macOS, Linux), Mobile (Android).

## Key Features

- **Full Greenfoot API Compatibility**: Implements `greenfoot.Actor`, `greenfoot.World`, `greenfoot.Greenfoot`, `greenfoot.GreenfootImage`, `greenfoot.GreenfootSound`, `greenfoot.Color`, `greenfoot.Font`, and `greenfoot.MouseInfo`.
- **AWT Replacement Library**: Re-implements `java.awt.*` (`BufferedImage`, `Shape`, `Rectangle`, `Ellipse`, `Line`, `Polygon`, `Point`) using LibGDX `Pixmap` for 100% cross-platform mobile & desktop execution without AWT dependencies.
- **On-Screen Touch Pad (Virtual Controller)**: Integrated touch controls for mobile (Android) and testable on desktop via `-Dgreenfoot.virtualController=true`. Supports 2 to 8 button layouts with custom key mappings.
- **Spatial Collision Partitioning**: `ColManager` provides LibGDX-accelerated 2D collision detection and spatial querying for high performance.
- **Automatic Asset Copy & Project Detection**: Gradle task `copyGreenfootAssets` automatically locates `project.greenfoot` in `user-project/` and synchronizes assets to the runtime asset directory.

---

## Quick Start Guide

### 1. Adding Your Greenfoot Project
Place your Greenfoot project folder under `core/src/main/java/user-project/`:

```text
core/src/main/java/user-project/
└── MyGame/
    ├── project.greenfoot
    ├── MyWorld.java
    ├── Player.java
    ├── images/
    └── sounds/
```

### 2. Running & Building

| Target Platform | Command | Notes |
| :--- | :--- | :--- |
| **Desktop (LWJGL3)** | `./gradlew lwjgl3:run` | Launches the game on desktop |
| **Desktop Executable JAR** | `./gradlew lwjgl3:jar` | Creates runnable JAR at `lwjgl3/build/libs/` |
| **Android APK** | `./gradlew android:assembleDebug` | Creates debug APK at `android/build/outputs/apk/debug/` |

---

## Unsupported / Limited Features

The wrapper focuses on single-player and arcade Greenfoot scenario execution across desktop, mobile, and web. The following native Greenfoot features have limitations or stubs:

- **Greenfoot UserInfo Server**: `UserInfo.getMyInfo()` and `UserInfo.getTop()` operate locally using LibGDX `Preferences` rather than connecting to the central Greenfoot website server.
- **Microphone Input (`Greenfoot.getMicLevel()`)**: Stubbed, returns `0` (LibGDX does not provide a standard cross-platform microphone API).
- **Video Recording (`Greenfoot.startRecording()`, `stopRecording()`)**: Unimplemented / stubbed.
- **Native AWT / Swing Dialogs**: Swing dialogs (such as `JOptionPane`) and native AWT frames are replaced by standard wrapper classes; direct `java.awt.*` calls in user code should be updated to `greenfoot.awt.*` or standard Greenfoot APIs.

---

## Project Structure

- `core`: Core wrapper logic (`greenfoot.*`, `greenfoot.awt.*`, `id.qiupi3.greenfoot.*`) and user project sources (`user-project/`).
- `lwjgl3`: Desktop LWJGL3 platform launcher.
- `android`: Android application launcher & activity.
- `html`: GWT / WebGL web platform launcher.

---

## License & Attribution

This wrapper is licensed under the **GNU General Public License v2 with Classpath Exception** (GPLv2+Classpath Exception), maintaining full license compatibility with the original Greenfoot software.

- **LibGDX Wrapper Author**: Qiupi3, DavidsonRafaelK
- **Original Greenfoot Authors**: Poul Henriksen, Michael Kolling, Davin McCall, Neil Brown, Fabio Heday, Amjad Altadmri.
