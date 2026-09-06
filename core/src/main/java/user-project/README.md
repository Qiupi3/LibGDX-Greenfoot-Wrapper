# User Project Directory

Place your Greenfoot project folder inside this directory: `core/src/main/java/user-project/`

## Recommended Structure

```text
core/src/main/java/user-project/
└── MyGame/
    ├── project.greenfoot
    ├── MyWorld.java
    ├── Player.java
    ├── images/
    │   └── player.png
    └── sounds/
        └── jump.wav
```

## How It Works

1. **Auto-Detection**: The build system automatically scans `user-project/` for subfolders containing `project.greenfoot`.
2. **Automatic Asset Synchronization**: Running `./gradlew lwjgl3:run` or `./gradlew android:assembleDebug` triggers `copyGreenfootAssets`, which automatically copies the project's images, sounds, and configuration into the runtime `assets/` directory.
3. **Compilation**: Java files in `user-project/` are automatically compiled as part of the core module source set.