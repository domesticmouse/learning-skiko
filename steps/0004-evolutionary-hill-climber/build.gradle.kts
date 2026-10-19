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
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}

ktlint {
    version.set("1.5.0")
    android.set(false)
    outputToConsole.set(true)
}
