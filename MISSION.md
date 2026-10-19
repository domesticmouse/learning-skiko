# Mission: Genetic Art with Kotlin and Skiko

## Why
Master hardware-accelerated 2D graphics in Kotlin using Skiko and build high-performance evolutionary algorithms that approximate target imagery with procedural brush strokes, entirely driven via a command-line workflow.

## Success looks like
- Understand and configure the Skiko desktop rendering lifecycle (`SkiaLayer`, `SkikoRenderDelegate`, native Skia bindings) from the command line.
- Design expressive vector chromosome representations (Bézier curves, colors, alphas) and mutation operators.
- Implement high-speed offscreen pixel fitness evaluation without GC pressure.
- Coordinate multi-threaded evolutionary loops ((1+1) Hill Climber, multi-core island models) with real-time HUD metrics and interactive controls.
- Apply perceptual fitness metrics (CIELAB ΔE, edge-weighted kernels) and parsimony constraints to guide aesthetic outcomes.
- Export vector artifacts (.svg) and record reproducible evolution timelines.
- Confidently build, run, test, and format Kotlin desktop projects using Gradle CLI and Ktlint without requiring an IDE.

## Constraints
- Pure command-line workflow: Gradle CLI (`./gradlew run`), Ktlint, and terminal-first development. No mandatory IDE.
- Lessons must be modular, self-contained HTML files in `./lessons/` with shared assets in `./assets/`.

## Out of scope
- Heavy GUI frameworks (Compose Multiplatform, JavaFX) — focus stays on raw Skiko/Skia rendering.
- Complex neural approaches (GANs, diffusion) — focus is on classical genetic algorithms and stochastic hill climbers.
