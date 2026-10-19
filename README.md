# Learning Skiko

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.txt)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg)](https://kotlinlang.org)
[![Skiko](https://img.shields.io/badge/Skiko-0.8.18-orange.svg)](https://github.com/JetBrains/skiko)

A comprehensive, hands-on, command-line-first curriculum and reference implementation for mastering hardware-accelerated 2D graphics in Kotlin using [Skiko](https://github.com/JetBrains/skiko) (JetBrains' Skia bindings for Kotlin Multiplatform) and building high-performance genetic art engines.

This repository is published at [https://github.com/domesticmouse/learning-skiko](https://github.com/domesticmouse/learning-skiko).

---

## Overview

**Learning Skiko** guides you from initializing a standalone Skiko desktop window through to building an evolutionary hill climber that approximates target images with procedural vector brush strokes. The curriculum emphasizes a pure terminal workflow (Gradle CLI, Ktlint, text editor) without requiring heavy IDEs or UI frameworks like Compose Multiplatform or JavaFX.

### Core Highlights

- **Pure Skiko Desktop**: Configure native OS runtimes, `SkiaLayer`, and `SkikoRenderDelegate` directly via Gradle.
- **Vector Chromosomes**: Model artwork using immutable Bézier stroke genes and stochastic mutation operators.
- **Zero-Allocation Headless Fitness**: Evaluate image fitness at hundreds of iterations per second using offscreen Skia bitmaps and direct byte buffers.
- **Multi-Core Island Concurrency**: Run concurrent $(1+\lambda)$ evolutionary demes with asynchronous ring migration across CPU cores.
- **Perceptual Aesthetics**: Enhance approximations using CIELAB $\Delta E$ perceptual color spaces, Sobel edge saliency kernels, and parsimony pressure.
- **Vector Export & Video**: Export evolved chromosomes to scalable W3C SVG files and capture frame sequences for time-lapses.

---

## Repository Structure

```
learning-skiko/
├── assets/             # Shared CSS styles, quiz widgets, and target imagery
├── learning-records/   # Pedagogical notes, milestones, and alignment logs
├── lessons/            # Interactive self-contained HTML lessons (0001–0011)
├── reference/          # Skiko & genetic art reference sheets and cheat codes
├── steps/              # Incremental, runnable Gradle projects for each lesson
│   ├── 0001-skiko-desktop-foundations/
│   ├── 0002-vector-brush-strokes/
│   ├── ...
│   └── 0011-svg-export-timelapse-capture/
├── LICENSE.txt         # Apache 2.0 license
├── MISSION.md          # Project mission, goals, and constraints
└── README.md           # Project documentation
```

---

## Curriculum Roadmap

The curriculum is structured into two main phases with 11 incremental lessons:

### Phase 1: Core Engine Deconstruction

| Lesson | Title | Focus Topics | Step Project |
| :--- | :--- | :--- | :--- |
| **0001** | [Skiko Desktop Foundations](lessons/0001-skiko-desktop-foundations.html) | Native runtime resolution, `SkiaLayer`, render lifecycle | [`steps/0001-skiko-desktop-foundations`](steps/0001-skiko-desktop-foundations) |
| **0002** | [Vector Brush Strokes & Genes](lessons/0002-vector-brush-strokes.html) | Genotype vs. phenotype, quadratic Béziers, mutation | [`steps/0002-vector-brush-strokes`](steps/0002-vector-brush-strokes) |
| **0003** | [Headless Fitness Evaluation](lessons/0003-offscreen-fitness-evaluation.html) | Offscreen bitmaps, zero-allocation buffers, L1 vs. L2 SSD | [`steps/0003-offscreen-fitness-evaluation`](steps/0003-offscreen-fitness-evaluation) |
| **0004** | [The Evolutionary Loop](lessons/0004-evolutionary-hill-climber.html) | (1+1) Hill Climber, worker thread, atomic synchronization | [`steps/0004-evolutionary-hill-climber`](steps/0004-evolutionary-hill-climber) |
| **0005** | [Real-Time HUD & Controls](lessons/0005-realtime-visualization-hud.html) | Dual-pane rendering, vector HUD metrics, keyboard controls | [`steps/0005-realtime-visualization-hud`](steps/0005-realtime-visualization-hud) |

### Phase 2: Advanced Extensions & Scaling

| Lesson | Title | Focus Topics | Step Project |
| :--- | :--- | :--- | :--- |
| **0006** | [Multi-Core Island Model & $(1+\lambda)$](lessons/0006-multicore-island-model.html) | Thread pool concurrency, island demes, ring migration | [`steps/0006-multicore-island-model`](steps/0006-multicore-island-model) |
| **0007** | [Adaptive Annealing & Pyramids](lessons/0007-adaptive-annealing-pyramids.html) | Simulated annealing schedules, coarse-to-fine pyramids | [`steps/0007-adaptive-annealing-pyramids`](steps/0007-adaptive-annealing-pyramids) |
| **0008** | [Perceptual Color & Edge-Aware Fitness](lessons/0008-perceptual-fitness-cielab-sobel.html) | CIELAB $\Delta E$, 3D LUT caching, Sobel convolution masks | [`steps/0008-perceptual-fitness-cielab-sobel`](steps/0008-perceptual-fitness-cielab-sobel) |
| **0009** | [Parsimony Pressure & Style](lessons/0009-parsimony-pressure-style-constraints.html) | Bloat control, Pareto frontiers, stroke & area penalties | [`steps/0009-parsimony-pressure-style-constraints`](steps/0009-parsimony-pressure-style-constraints) |
| **0010** | [Diverse Vector Primitives](lessons/0010-diverse-vector-primitives.html) | Cubic splines, tapered calligraphy, polygon meshes, shaders | [`steps/0010-diverse-vector-primitives`](steps/0010-diverse-vector-primitives) |
| **0011** | [SVG Vector Export & Time-Lapses](lessons/0011-svg-export-timelapse-capture.html) | W3C SVG serialization, checkpointing, video capture | [`steps/0011-svg-export-timelapse-capture`](steps/0011-svg-export-timelapse-capture) |

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK)**: OpenJDK 21 LTS (recommended)
- **Operating System**: macOS (Apple Silicon / Intel), Linux (x86_64 / arm64), or Windows (x86_64)

### Running a Step

Each folder under `steps/` is an independent, runnable Gradle project. Navigate into any step directory and use the Gradle wrapper:

```bash
# Run step 1: basic Skiko window
cd steps/0001-skiko-desktop-foundations
./gradlew run

# Run step 5: interactive real-time genetic painter
cd ../0005-realtime-visualization-hud
./gradlew run
```

### Viewing Lessons

The lessons are interactive HTML pages equipped with visual aids, architectural diagrams, and self-check quizzes. Open any lesson directly in your browser:

```bash
# macOS
open lessons/0001-skiko-desktop-foundations.html

# Linux
xdg-open lessons/0001-skiko-desktop-foundations.html
```

You can also view the quick reference cheatsheet at [`reference/skiko-genetic-art-cheatsheet.html`](reference/skiko-genetic-art-cheatsheet.html).

---

## License & Disclaimer

This project is licensed under the Apache 2.0 License. See [LICENSE.txt](LICENSE.txt) for details.

This is not an official Google product.
