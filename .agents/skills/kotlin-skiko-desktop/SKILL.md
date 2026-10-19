---
name: kotlin-skiko-desktop
description: >-
  Build, scaffold, and configure standalone hardware-accelerated 2D Kotlin desktop
  applications using Skiko (Skia for Kotlin) from the command line without an IDE.
  Use when initializing a new Skiko desktop project, configuring Gradle build files
  with dynamic OS/CPU native runtime resolution, setting up the Skiko AWT render
  loop, troubleshooting JVM compatibility (e.g. OpenJDK 21 LTS vs newer JDKs), or
  adding Ktlint for CLI-based code formatting and style validation.
---

# Kotlin Skiko Desktop Application Guide

This skill guides you through scaffolding, developing, and building standalone 2D desktop applications using **Skiko** (JetBrains' Kotlin bindings for Google's Skia graphics engine) from the command line without IntelliJ.

---

## Workflow Overview

1. [Environment & Toolchain Setup](#1-environment--toolchain-setup)
2. [Project Scaffolding](#2-project-scaffolding)
3. [Gradle Configuration (Skiko + Ktlint)](#3-gradle-configuration)
4. [Skiko Application Architecture](#4-skiko-application-architecture)
5. [Verification & Running](#5-verification--running)

---

## 1. Environment & Toolchain Setup

Skiko and the Kotlin compiler require a compatible Java Development Kit.

### Critical JDK Version Requirement
> **Warning**: Kotlin compiler daemons (including Kotlin 2.1+) fail with `IllegalArgumentException` on early-access or unfinalized JDKs (such as OpenJDK 27) because internal IntelliJ version parsers do not recognize future major versions.

Always target an **LTS release (Java 21 or Java 17)**:

```bash
# macOS (Homebrew)
brew install openjdk@21 gradle

# Ensure OpenJDK 21 is prioritized in your shell environment:
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
```

Verify with:
```bash
java -version    # Should report OpenJDK 21
gradle -v
```

---

## 2. Project Scaffolding

From the desired parent directory:

```bash
mkdir -p my-skiko-app/src/main/kotlin
cd my-skiko-app
touch settings.gradle.kts build.gradle.kts src/main/kotlin/Main.kt
```

Alternatively, run the automated scaffolding script:
[`scripts/init-skiko-project.sh`](./scripts/init-skiko-project.sh)

---

## 3. Gradle Configuration

### `settings.gradle.kts`
Enable automatic toolchain provisioning via the Foojay resolver:
```kotlin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "my-skiko-app"
```

### `build.gradle.kts`
Skiko requires two dependencies:
1. `org.jetbrains.skiko:skiko-awt` (Kotlin API classes)
2. `org.jetbrains.skiko:skiko-awt-runtime-<os>-<arch>` (OS- and CPU-specific native C++ binary)

Use the reference implementation in [`examples/build.gradle.kts`](./examples/build.gradle.kts) or configure the following essential blocks:

```kotlin
plugins {
    kotlin("jvm") version "2.1.0"
    application
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val skikoVersion = "0.8.18"

// Dynamic OS Detection
val osName = System.getProperty("os.name")
val targetOs = when {
    osName == "Mac OS X" -> "macos"
    osName.startsWith("Win") -> "windows"
    osName.startsWith("Linux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

// Dynamic Architecture Detection
val osArch = System.getProperty("os.arch")
val targetArch = when (osArch) {
    "x86_64", "amd64" -> "x64"
    "aarch64" -> "arm64"
    else -> error("Unsupported architecture: $osArch")
}

val skikoTarget = "$targetOs-$targetArch"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jetbrains.skiko:skiko-awt:$skikoVersion")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-$skikoTarget:$skikoVersion")
}

application {
    mainClass.set("MainKt")
}

ktlint {
    version.set("1.5.0")
    android.set(false)
    outputToConsole.set(true)
}
```

See [`references/architecture-and-os-matrix.md`](./references/architecture-and-os-matrix.md) for fat-jar and cross-compilation options.

---

## 4. Skiko Application Architecture

Skiko renders directly to a `SkiaLayer`, embedded inside a standard Java Swing `JFrame`.

### Core Rules for Skiko Desktop
1. **Always schedule window creation on the Swing Event Dispatch Thread (EDT)** using `SwingUtilities.invokeLater`.
2. **Allocate `Paint` and `Font` objects once** in your render delegate, not inside `onRender()`, to prevent GC pauses during 60/120 FPS animation.
3. **Instantiate fonts via `Font().apply { size = 24f }`**; avoid `Typeface.makeDefault()` which is unavailable in recent Skiko builds.
4. **Trigger continuous animations** by invoking `skiaLayer.needRedraw()`.

See the complete working reference in [`examples/Main.kt`](./examples/Main.kt).

For drawing paths, linear gradients, shapes, and high-DPI handling, consult:
[`references/skia-drawing-cheatsheet.md`](./references/skia-drawing-cheatsheet.md).

---

## 5. Verification & Running

### 1. Code Formatting & Linting
Validate style and auto-format files before building:
```bash
# Auto-format all Kotlin sources and Gradle scripts in place:
./gradlew ktlintFormat

# Check style (for CI):
./gradlew ktlintCheck
```

### 2. Compile and Run
```bash
# Compile Kotlin sources:
./gradlew compileKotlin

# Run application:
./gradlew run

# Package distribution into build/install/my-skiko-app/
./gradlew installDist
```
