# Course Curriculum & Lesson Plans

Comprehensive lesson plan for **Genetic Art with Kotlin and Skiko**. This curriculum guides the learner from building standalone hardware-accelerated desktop canvases from the command line through to high-performance evolutionary algorithms, multi-core island concurrency, perceptual color spaces, diverse vector primitives, and SVG vector exports.

---

## Phase 1: Core Engine Deconstruction (Completed)

| Lesson | Title | Core Topics | Key Source Files |
| :--- | :--- | :--- | :--- |
| **[0001](0001-skiko-desktop-foundations.html)** | **Skiko Desktop Foundations** | Gradle CLI, native runtime resolution, `SkiaLayer`, `SkikoRenderDelegate`, rendering lifecycle | `build.gradle.kts`, `src/main/kotlin/Main.kt` |
| **[0002](0002-vector-brush-strokes.html)** | **Vector Brush Strokes & Genes** | Genotype vs phenotype, quadratic Bézier curves, immutable `StrokeGene`, `Painting` chromosome, stochastic mutations | `src/main/kotlin/StrokeGene.kt`, `src/main/kotlin/Painting.kt` |
| **[0003](0003-offscreen-fitness-evaluation.html)** | **Headless Fitness Evaluation** | Headless Skia bitmaps, zero-allocation buffers, L1 vs L2 sum of squared differences (SSD), target image rasterization | `src/main/kotlin/Evaluator.kt`, `src/main/kotlin/TargetImage.kt` |
| **[0004](0004-evolutionary-hill-climber.html)** | **The Evolutionary Loop** | (1+1) Hill Climber, daemon worker thread, atomic thread synchronization (`AtomicReference`, `AtomicLong`), neutral drift | `src/main/kotlin/GeneticPainter.kt` |
| **[0005](0005-realtime-visualization-hud.html)** | **Real-Time HUD & Controls** | Dual-pane rendering, vector HUD metrics panel, interactive `KeyAdapter` keyboard shortcuts, PNG encoding | `src/main/kotlin/Main.kt` |

---

## Phase 2: Advanced Extensions & Scaling (Completed)

### Track 1: Algorithmic Scaling & Concurrency

| Lesson | Title | Core Topics |
| :--- | :--- | :--- |
| **[0006](0006-multicore-island-model.html)** | **Multi-Core Island Model & $(1+\lambda)$ Strategy** | Thread pool concurrency, $(1+\lambda)$ evaluation pressure, isolated demes, asynchronous ring champion migration |
| **[0007](0007-adaptive-annealing-pyramids.html)** | **Adaptive Annealing & Multi-Resolution Pyramids** | Simulated annealing cooling schedules, Rechenberg 1/5th success rule, coarse-to-fine Gaussian image pyramids |

### Track 2: Fitness Models & Perceptual Aesthetics

| Lesson | Title | Core Topics |
| :--- | :--- | :--- |
| **[0008](0008-perceptual-fitness-cielab-sobel.html)** | **Perceptual Color Distances & Edge-Aware Fitness** | Human color vision, CIE $L^*a^*b^*$ $\Delta E$ metrics, precomputed 3D LUTs, Sobel convolution edge saliency masks |
| **[0009](0009-parsimony-pressure-style-constraints.html)** | **Parsimony Pressure & Style Constraints** | Genetic bloat prevention, Pareto optimality frontiers, stroke count & area penalties, impressionist styling |

### Track 3: Chromosome Expressiveness & Rendering

| Lesson | Title | Core Topics |
| :--- | :--- | :--- |
| **[0010](0010-diverse-vector-primitives.html)** | **Diverse Vector Primitives & Texture Shaders** | Cubic Bézier inflection curves, pressure-tapered calligraphy paths, convex polygon meshes, Skia gradient shaders |
| **[0011](0011-svg-export-timelapse-capture.html)** | **SVG Vector Export & Time-Lapse Capture** | W3C SVG XML path translation, infinite vector scalability, checkpointing, CLI video rendering via FFmpeg |

---

## Pedagogical Workflow
All lessons are self-contained HTML files located in this directory (`./lessons/000N-*.html`). They are designed for a pure command-line workflow:
- Build and execute the application: `./gradlew run`
- Validate styling and linting: `./gradlew ktlintCheck`
- View shared styles and interactive widgets: `../assets/lesson-style.css`, `../assets/quiz-widget.js`
