#!/usr/bin/env bash
set -euo pipefail

# Scaffolds a new standalone Kotlin Skiko desktop project
PROJECT_NAME="${1:-skiko-desktop-app}"

echo "==> Creating Skiko desktop project in './${PROJECT_NAME}'..."
mkdir -p "${PROJECT_NAME}/src/main/kotlin"
cd "${PROJECT_NAME}"

# settings.gradle.kts
cat << 'EOF' > settings.gradle.kts
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "skiko-desktop-app"
EOF

# build.gradle.kts
cat << 'EOF' > build.gradle.kts
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

val osName = System.getProperty("os.name")
val targetOs =
    when {
        osName == "Mac OS X" -> "macos"
        osName.startsWith("Win") -> "windows"
        osName.startsWith("Linux") -> "linux"
        else -> error("Unsupported OS: $osName")
    }

val osArch = System.getProperty("os.arch")
val targetArch =
    when (osArch) {
        "x86_64", "amd64" -> "x64"
        "aarch64" -> "arm64"
        else -> error("Unsupported arch: $osArch")
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
EOF

# src/main/kotlin/Main.kt
cat << 'EOF' > src/main/kotlin/Main.kt
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate

fun main() {
    SwingUtilities.invokeLater {
        val window = JFrame("Skiko Desktop").apply {
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            preferredSize = Dimension(800, 600)
        }

        val skiaLayer = SkiaLayer()
        skiaLayer.renderDelegate = object : SkikoRenderDelegate {
            val bgPaint = Paint().apply { color = 0xFF1E1E2E.toInt() }
            val circlePaint = Paint().apply { color = 0xFF89B4FA.toInt(); isAntiAlias = true }
            val textPaint = Paint().apply { color = 0xFFCDD6F4.toInt(); isAntiAlias = true }
            val font = Font().apply { size = 24f }

            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                canvas.drawRect(Rect.makeWH(width.toFloat(), height.toFloat()), bgPaint)
                canvas.drawCircle(width / 2f, height / 2f, 80f, circlePaint)
                canvas.drawString("Skiko Ready!", 50f, 70f, font, textPaint)
            }
        }

        skiaLayer.attachTo(window.contentPane)
        window.pack()
        window.setLocationRelativeTo(null)
        window.isVisible = true
    }
}
EOF

echo "==> Generating Gradle wrapper..."
gradle wrapper

echo "==> Done! Run your app with:"
echo "    cd ${PROJECT_NAME} && ./gradlew run"
