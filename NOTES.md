# Teaching Notes & User Preferences

- **Workflow Preference**: Pure command-line workflow. Use terminal Gradle (`./gradlew run`, `./gradlew test`), Ktlint, and standard text editing without assuming an IDE.
- **Pedagogical Strategy**: Deconstruct the working brush-stroke painter app in `src/main/kotlin` into incremental stages, then expand with modular advanced optimization, aesthetic, and export extensions.
- **Lesson Structure**: Self-contained HTML lessons in `./lessons/` numbered `0001-...`, linking to shared components in `./assets/` and reference guides in `./reference/`.
- **Target Concept Stack**: Kotlin CLI -> Skiko/Skia 2D rendering -> Genotype representation (Bézier curves) -> Headless pixel fitness evaluation -> Asynchronous evolutionary loop -> Live interactive HUD & controls.

## Lesson Plans & Curriculum Roadmap

### Phase 1: Foundations (Deconstruction of Base Engine)
- **Lesson 0001: Skiko Desktop Foundations**: CLI setup, Gradle dependencies, OS-native Skiko runtimes, `SkiaLayer`, `SkikoRenderDelegate`, and window lifecycle.
- **Lesson 0002: Vector Brush Strokes & Genes**: Genotype vs phenotype, quadratic Bézier curves, `StrokeGene`, `Painting` chromosome, and immutable stochastic mutation operators.
- **Lesson 0003: Headless Fitness Evaluation**: Headless Skia bitmaps, zero-allocation buffers, L1 vs L2 sum of squared differences (SSD), and `TargetImage` loader.
- **Lesson 0004: The Evolutionary Loop**: (1+1) Hill Climber, daemon worker thread, lock-free thread synchronization (`AtomicReference`, `AtomicLong`), and neutral drift.
- **Lesson 0005: Real-Time HUD & Controls**: Dual-pane display architecture, vector HUD overlay, keyboard event loop (`KeyAdapter`), PNG image encoding, and CLI execution.

### Phase 2: Extensions & Advanced Capabilities (Completed)
#### Track 1: Algorithmic Scaling & Concurrency
- **Lesson 0006: Multi-Core Island Model & $(1+\lambda)$ Strategy**: Multi-threaded candidate evaluation pools, island-model population topologies, and asynchronous champion migration to break local plateaus. (Implemented in `lessons/0006-multicore-island-model.html`)
- **Lesson 0007: Adaptive Annealing & Multi-Resolution Pyramids**: Dynamic temperature cooling schedules (diminishing mutation variances) and coarse-to-fine multi-resolution image pyramids. (Implemented in `lessons/0007-adaptive-annealing-pyramids.html`)

#### Track 2: Fitness Models & Perceptual Aesthetics
- **Lesson 0008: Perceptual Color Distances & Edge-Aware Fitness**: CIELAB $\Delta E$ perceptual color spaces and Sobel edge-detection weighting kernels to preserve salient structural features. (Implemented in `lessons/0008-perceptual-fitness-cielab-sobel.html`)
- **Lesson 0009: Parsimony Pressure & Style Constraints**: Multi-objective Pareto fitness penalizing stroke count, area, or ink density to achieve impressionist, minimalist aesthetic styles. (Implemented in `lessons/0009-parsimony-pressure-style-constraints.html`)

#### Track 3: Chromosome Expressiveness & Rendering
- **Lesson 0010: Diverse Vector Primitives & Texture Shaders**: Pressure-tapered paths, cubic splines, polygon meshes, and Skia gradient shaders (`Shader.makeLinearGradient`). (Implemented in `lessons/0010-diverse-vector-primitives.html`)
- **Lesson 0011: SVG Vector Export & Time-Lapse Capture**: Serializing evolved vector chromosomes directly to scalable `.svg` files and automated frame-dump capture for compiling evolution time-lapses. (Implemented in `lessons/0011-svg-export-timelapse-capture.html`)

