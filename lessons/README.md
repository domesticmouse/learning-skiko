# Course Curriculum & Lesson Plans

Comprehensive lesson plan for **Genetic Art with Kotlin and Skiko**. This curriculum guides the learner from building standalone hardware-accelerated desktop canvases from the command line through to high-performance evolutionary algorithms, multi-core island concurrency, perceptual color spaces, and vector exports.

---

## Phase 1: Core Engine Deconstruction (Completed)

| Lesson | Title | Core Topics | Key Source Files |
| :--- | :--- | :--- | :--- |
| **[0001](file:///Users/brett/tmp/kotlin-experiment/lessons/0001-skiko-desktop-foundations.html)** | **Skiko Desktop Foundations** | Gradle CLI, native runtime resolution, `SkiaLayer`, `SkikoRenderDelegate`, rendering lifecycle | `build.gradle.kts`, `src/main/kotlin/Main.kt` |
| **[0002](file:///Users/brett/tmp/kotlin-experiment/lessons/0002-vector-brush-strokes.html)** | **Vector Brush Strokes & Genes** | Genotype vs phenotype, quadratic Bézier curves, immutable `StrokeGene`, `Painting` chromosome, stochastic mutations | `src/main/kotlin/StrokeGene.kt`, `src/main/kotlin/Painting.kt` |
| **[0003](file:///Users/brett/tmp/kotlin-experiment/lessons/0003-offscreen-fitness-evaluation.html)** | **Headless Fitness Evaluation** | Headless Skia bitmaps, zero-allocation buffers, L1 vs L2 sum of squared differences (SSD), target image rasterization | `src/main/kotlin/Evaluator.kt`, `src/main/kotlin/TargetImage.kt` |
| **[0004](file:///Users/brett/tmp/kotlin-experiment/lessons/0004-evolutionary-hill-climber.html)** | **The Evolutionary Loop** | (1+1) Hill Climber, daemon worker thread, atomic thread synchronization (`AtomicReference`, `AtomicLong`), neutral drift | `src/main/kotlin/GeneticPainter.kt` |
| **[0005](file:///Users/brett/tmp/kotlin-experiment/lessons/0005-realtime-visualization-hud.html)** | **Real-Time HUD & Controls** | Dual-pane rendering, vector HUD metrics panel, interactive `KeyAdapter` keyboard shortcuts, PNG encoding | `src/main/kotlin/Main.kt` |

---

## Phase 2: Advanced Extensions & Scaling (Roadmap)

### Track 1: Algorithmic Scaling & Concurrency
* **Lesson 0006: Multi-Core Island Model & $(1+\lambda)$ Strategy**
  * **Objective**: Scale candidate generation across multi-core CPUs without synchronization bottlenecks.
  * **Concepts**: Fork-join and worker pool concurrency, $(1+\lambda)$ selection pressure, independent population islands, asynchronous champion migration topology to escape local plateaus.
  * **Hands-on Target**: Refactor `GeneticPainter.kt` to coordinate multiple evaluator threads and implement an island migration ring.

* **Lesson 0007: Adaptive Annealing & Multi-Resolution Pyramids**
  * **Objective**: Accelerate convergence speed and fine-detail resolution through dynamic scheduling.
  * **Concepts**: Simulated annealing temperature schedules (decaying mutation delta variances over time), multi-scale image pyramids (evolving broad strokes on $64\times64$, freezing lower layers, and refining at $256\times256$).
  * **Hands-on Target**: Implement multi-stage annealing in `GeneticPainter.kt` and pyramid downsampling in `TargetImage.kt`.

### Track 2: Fitness Models & Perceptual Aesthetics
* **Lesson 0008: Perceptual Color Distances & Edge-Aware Fitness**
  * **Objective**: Replace naive RGB Euclidean differences with human-vision-aligned perceptual metrics.
  * **Concepts**: CIELAB $\Delta E 76 / \Delta E 94$ color space conversion, luminance weighting, Sobel filter convolution kernels to boost fitness weight on structural contours and edges.
  * **Hands-on Target**: Extend `Evaluator.kt` with a Sobel edge-detection pre-pass and CIELAB lookup tables.

* **Lesson 0009: Parsimony Pressure & Style Constraints**
  * **Objective**: Steer evolutionary convergence toward artistic and minimalist aesthetics rather than photo-matching.
  * **Concepts**: Multi-objective fitness (Pareto dominance vs weighted penalties), parsimony pressure penalizing total stroke count and stroke surface area, brush directionality biasing.
  * **Hands-on Target**: Add configurable complexity regularization penalties to candidate fitness scores in `Evaluator.kt`.

### Track 3: Chromosome Expressiveness & Rendering
* **Lesson 0010: Diverse Vector Primitives & Texture Shaders**
  * **Objective**: Expand the visual vocabulary of the genetic painter beyond uniform curved lines.
  * **Concepts**: Cubic Bézier curves, tapered pressure dynamics (variable stroke width profiles), translucent convex polygon genes, Skia gradient shaders (`Shader.makeLinearGradient`).
  * **Hands-on Target**: Create polymorphic gene interfaces and implement `PolygonGene` / `TaperedStrokeGene` in `StrokeGene.kt`.

* **Lesson 0011: SVG Vector Export & Time-Lapse Capture**
  * **Objective**: Transform evolved artwork into infinite-resolution vector assets and video artifacts.
  * **Concepts**: Translating Skia paths and paint styles directly into valid W3C SVG XML documents, automated milestone frame rendering, assembling time-lapse video via CLI (`ffmpeg`).
  * **Hands-on Target**: Implement SVG serializer in `Painting.kt` and frame-dump export hooks in `Main.kt`.

---

## Pedagogical Workflow
All lessons are self-contained HTML files located in this directory (`./lessons/000N-*.html`). They are designed for a pure command-line workflow:
- Build and execute the application: `./gradlew run`
- Validate styling and linting: `./gradlew ktlintCheck`
- View shared styles and interactive widgets: `../assets/lesson-style.css`, `../assets/quiz-widget.js`
