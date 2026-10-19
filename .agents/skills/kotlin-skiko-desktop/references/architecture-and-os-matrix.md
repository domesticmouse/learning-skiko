# Skiko Architecture and OS Target Matrix

## Supported Native Runtimes

Skiko distributes precompiled native Skia binaries as separate artifacts for each operating system and CPU architecture.

| OS | Architecture | Artifact Coordinate |
| :--- | :--- | :--- |
| **macOS** | Apple Silicon (`aarch64` / M1/M2/M3/M4) | `org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:<version>` |
| **macOS** | Intel (`x86_64`) | `org.jetbrains.skiko:skiko-awt-runtime-macos-x64:<version>` |
| **Windows** | x64 | `org.jetbrains.skiko:skiko-awt-runtime-windows-x64:<version>` |
| **Windows** | ARM64 | `org.jetbrains.skiko:skiko-awt-runtime-windows-arm64:<version>` |
| **Linux** | x64 | `org.jetbrains.skiko:skiko-awt-runtime-linux-x64:<version>` |
| **Linux** | ARM64 | `org.jetbrains.skiko:skiko-awt-runtime-linux-arm64:<version>` |

---

## Single Fat-Jar Approach (`skiko-awt-runtime-all`)

If you want a cross-platform binary that can run on any host without dynamic platform detection, you can depend on `skiko-awt-runtime-all`:

```kotlin
dependencies {
    val skikoVersion = "0.8.18"
    implementation("org.jetbrains.skiko:skiko-awt-runtime-all:$skikoVersion")
}
```

*Tradeoff:* `skiko-awt-runtime-all` is significantly larger (~80MB+) because it bundles native C++ shared libraries for all six platforms.

---

## Repositories

Skiko releases and release candidates are mirrored on:
- `mavenCentral()`
- JetBrains Space Compose repository: `https://maven.pkg.jetbrains.space/public/p/compose/dev`
