# Pedagogical Review, Codebase Alignment & L2 SSD Standardization

Following a comprehensive review of all 11 HTML lessons (`0001`–`0011`) and `reference/skiko-genetic-art-cheatsheet.html` against the Kotlin codebase in `src/main/kotlin/`, the curriculum and agent skills were updated with the following key improvements:

1. **Standardized on L2 Sum of Squared Differences (SSD)**:
   - Migrated Lesson 3, Lesson 4, the reference cheatsheet, and the `genetic-art-skiko` skill from L1 Manhattan distance to L2 Squared Error (SSD) accumulated in a 64-bit `Long`, matching `Evaluator.kt`.
   - Added explicit coverage of `allocN32Pixels` 4-byte `BGRA_8888` pixel stride (`i += 4`) and signed Kotlin `Byte` masking (`and 0xFF`).

2. **Codebase Fidelity & Phase Boundary Clarity**:
   - Aligned Phase 1 and Phase 2 Kotlin snippets with `StrokeGene`, `Painting`, `Evaluator`, and `TargetImage` signatures and mutation probabilities (`80%` parameter / `15%` z-order swap / `5%` random replacement).
   - Framed the transition between Lesson 5 (Phase 1 core engine deconstruction) and Lessons 6–11 (Phase 2 drop-in extension modules).
   - Added callouts explaining **Neutral Drift (`<=` vs. `<`)**, **Immutability as the foundation of `AtomicReference<Painting>` thread safety**, and **Thread-confinement of mutable Skia native wrappers (`Bitmap`, `Canvas`, `Paint`, `Path`)**.

3. **Algorithmic Completeness & Bug Fixes**:
   - **Lesson 6 (`IslandCoordinator.kt`)**: Contrasted Fine-Grained $(1+\lambda)$ vs. Coarse-Grained Island parallelism and replaced redundant CPU re-evaluation with lock-free `globalChampion.accumulateAndGet(...)`.
   - **Lesson 7 (`AnnealingSchedule.kt`)**: Distinguished Step-Size Annealing from Metropolis Simulated Annealing, combined exponential base cooling (`baseSigma`) with Rechenberg's 1/5th adaptive gain, and added `Painting.upscale(scale: Float)`.
   - **Lesson 8 (`PerceptualEvaluator.kt`)**: Completed the $3\times 3$ Sobel convolution in `computeSobelWeights()` and fixed the `* 3` stride multiplier in the 5-bit $32\times 32\times 32$ CIELAB lookup table (`lutBaseIndex`).
   - **Lesson 9 (`RegularizedEvaluator.kt`)**: Added structural **Add Stroke** and **Remove Stroke** mutation operators (`Painting.mutateVariableLength`) required for parsimony pressure ($\alpha \cdot N_{\text{strokes}}$) to prune redundant strokes.
   - **Lesson 10 (`Gene.kt`)**: Explained polar angle-sorted vertex parameterization $(\theta_i, r_i)$ to prevent self-intersecting polygons and refactored `sealed interface Gene` to reuse pre-allocated `Paint` and `Path` buffers.
   - **Lesson 11 (`PaintingExport.kt`)**: Aligned `Painting.toSvg()` with `StrokeGene` properties and added `.gene` checkpoint save/load serialization (`saveCheckpoint` / `loadCheckpoint`).
