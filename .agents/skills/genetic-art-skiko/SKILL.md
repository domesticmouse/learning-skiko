---
name: genetic-art-skiko
description: >-
  Design, build, and optimize evolutionary and genetic algorithm art generators
  using Skiko (Skia for Kotlin). Use when creating procedural/evolutionary image
  generators, evolving brush strokes or geometric polygons to approximate images,
  implementing (1+λ) Hill Climber evolutionary strategies, multi-core island models,
  performing fast offscreen raster fitness evaluation, CIELAB/Sobel perceptual fitness,
  parsimony constraints, SVG vector export, or streaming live evolutionary progress in a Skiko desktop UI.
---

# Genetic Art with Skiko Guide

This skill guides you through building hardware-accelerated evolutionary art engines in Kotlin using **Skiko** (JetBrains' Kotlin bindings for Skia).

---

## 1. Architectural Architecture

Evolutionary painting follows a decoupled architecture:

```
┌──────────────────────────────────────┐        ┌──────────────────────────────────────┐
│       Background Worker Thread       │        │         Swing EDT / Skiko UI         │
│                                      │        │                                      │
│  1. Clone Champion Painting          │        │  1. Read Atomic Champion Reference   │
│  2. Mutate Stroke(s) / Polygons      │        │  2. Render Target & Champion Side-by-│
│  3. Offscreen Canvas Evaluation      │───────►│     side on SkiaLayer                │
│  4. Fast Pixel SSD Comparison        │        │  3. Display Real-time HUD Metrics    │
│  5. Accept if Error <= Champion      │        │  4. Handle Key Controls (Pause, Save)│
└──────────────────────────────────────┘        └──────────────────────────────────────┘
```

---

## 2. Chromosome Representation: Immutable Data Classes

Idiomatic Kotlin favors **immutable data classes** (`val`) for chromosomes. This ensures predictable structural equality (`==`), prevents thread-safety hazards (e.g. the Swing UI rendering the champion while the background thread mutates candidates), and leverages compiler-generated `.copy(...)` for non-destructive mutation.

### Curved Brush Stroke Gene (`Path.quadTo`)
Translucent quadratic Bézier strokes mimic oil/watercolor glaze:

```kotlin
data class StrokeGene(
    val x0: Float, val y0: Float,
    val xc: Float, val yc: Float,
    val x1: Float, val y1: Float,
    val strokeWidth: Float,
    val r: Int, val g: Int, val b: Int, val a: Int,
) {
    fun render(canvas: Canvas, paint: Paint, path: Path) {
        paint.color = Color.makeARGB(a, r, g, b)
        paint.strokeWidth = strokeWidth
        paint.mode = PaintMode.STROKE
        paint.strokeCap = PaintStrokeCap.ROUND
        paint.isAntiAlias = true

        path.reset()
        path.moveTo(x0, y0)
        path.quadTo(xc, yc, x1, y1)
        canvas.drawPath(path, paint)
    }

    fun mutate(width: Float, height: Float, rng: Random): StrokeGene {
        val delta = (rng.nextFloat() - 0.5f) * 24f
        return copy(xc = (xc + delta).coerceIn(0f, width))
    }
}
```

### Polymorphic Vector Primitives (`sealed interface Gene`)
To combine diverse geometries beyond simple parabolic arcs while preserving the zero-allocation hot loop, pass the evaluator's reusable `Paint` and `Path` into `Gene.render`:
* **Cubic Béziers (`Path.cubicTo`)**: Two control handles ($P_1, P_2$) allow curvature inflection points (S-curves) essential for facial silhouettes and organic waves.
* **Pressure-Tapered Calligraphy**: Modulate stroke width along the curve envelope ($w(t) = w_{\text{start}}(1-t) + w_{\text{peak}}\cdot 4t(1-t) + w_{\text{end}}t$) for ink and brush realism.
* **Translucent Convex Polygons**: 3- to 6-vertex closed paths for low-poly or cubist stained-glass aesthetics. For $K \ge 4$ vertices, parameterize vertices in **polar coordinates** $(\theta_i, r_i)$ with sorted angles $0 \le \theta_1 < \theta_2 < \dots < \theta_K < 2\pi$ around a centroid $(c_x, c_y)$ so mutations never create self-intersecting "bowtie" polygons. **Critical**: Always close polygonal contours with `path.closePath()`. Never call `path.close()` (which invokes `AutoCloseable.close()`, frees the native C++ pointer, and causes `SIGSEGV in SkPath::isFinite()`).
* **Skia Shaders (`Shader.makeLinearGradient`)**: Multi-stop gradient shaders applied to paints for soft atmospheric lighting and dual-tone transitions.

```kotlin
sealed interface Gene {
    // Always reuse the caller's Paint and Path; never allocate Skia wrappers inside render()
    fun render(canvas: Canvas, paint: Paint, path: Path)
    fun mutate(width: Float, height: Float, rng: Random): Gene
}
```

### Painting Candidate (`Painting`)
A painting holds an immutable `List<StrokeGene>` and its cached evaluation score (`error: Long`). Deep immutability (`val` properties and read-only lists) is what makes publishing `Painting` snapshots via `AtomicReference<Painting>` across worker and UI threads completely race-free:

```kotlin
data class Painting(
    val strokes: List<StrokeGene>,
    val error: Long = Long.MAX_VALUE,
) {
    fun mutate(width: Float, height: Float, rng: Random): Painting {
        if (strokes.isEmpty()) return this
        val newStrokes = strokes.toMutableList()
        val roll = rng.nextFloat()
        when {
            roll < 0.80f -> { // 80%: Mutate one stroke's parameters
                val idx = rng.nextInt(newStrokes.size)
                newStrokes[idx] = newStrokes[idx].mutate(width, height, rng)
            }
            roll < 0.95f && newStrokes.size > 1 -> { // 15%: Z-order swap
                val i = rng.nextInt(newStrokes.size)
                val j = rng.nextInt(newStrokes.size)
                val tmp = newStrokes[i]
                newStrokes[i] = newStrokes[j]
                newStrokes[j] = tmp
            }
            else -> { // 5%: Replace with a fresh random stroke
                val idx = rng.nextInt(newStrokes.size)
                newStrokes[idx] = StrokeGene.random(width, height, rng)
            }
        }
        return copy(strokes = newStrokes, error = Long.MAX_VALUE)
    }
}
```

---

## 3. High-Performance Offscreen Fitness Evaluation

To evaluate 2,000–5,000+ candidates per second on the CPU:
1. **Thread-Confine Mutable Skia Wrappers**: `Bitmap`, `Canvas`, `Paint`, and `Path` wrap mutable native C++ pointers (`_ptr`) and are **not thread-safe**. Allocate them once per `Evaluator` instance and give each worker thread its own private `Evaluator`.
2. **Flat Byte Array L2 Sum of Squared Differences (SSD)**: `allocN32Pixels(width, height)` allocates 4 bytes per pixel (`BGRA_8888`). Stride by `i += 4`, square channel diffs (`db * db + dg * dg + dr * dr`), and accumulate into a 64-bit `Long` to prevent 32-bit overflow.
   * **Receiver Scoping Trap**: Inside `Bitmap().apply { allocN32Pixels(width, height) }`, unqualified `width` and `height` resolve to `Bitmap.width` and `Bitmap.height` (`0, 0`). Always qualify explicitly: `allocN32Pixels(this@Evaluator.width, this@Evaluator.height)`.
3. **Signed Byte Masking**: Kotlin `Byte` is signed ($-128 \dots 127$); always mask with `and 0xFF` before subtracting.

```kotlin
class Evaluator(val width: Int, val height: Int, val targetBytes: ByteArray) {
    private val bitmap = Bitmap().apply { allocN32Pixels(this@Evaluator.width, this@Evaluator.height) }
    private val canvas = Canvas(bitmap)
    private val paint = Paint()
    private val path = Path()

    fun evaluate(painting: Painting): Long {
        painting.render(canvas, width.toFloat(), height.toFloat(), paint, path)
        val candidateBytes = bitmap.readPixels(bitmap.imageInfo) ?: return Long.MAX_VALUE

        var sumSquaredDiff = 0L
        val totalBytes = width * height * 4
        var i = 0
        while (i < totalBytes) {
            val db = (candidateBytes[i].toInt() and 0xFF) - (targetBytes[i].toInt() and 0xFF)
            val dg = (candidateBytes[i + 1].toInt() and 0xFF) - (targetBytes[i + 1].toInt() and 0xFF)
            val dr = (candidateBytes[i + 2].toInt() and 0xFF) - (targetBytes[i + 2].toInt() and 0xFF)
            sumSquaredDiff += (db * db + dg * dg + dr * dr)
            i += 4 // BGRA_8888 stride (skips alpha at i + 3)
        }
        return sumSquaredDiff
    }
}
```

### Advanced Fitness Objectives
* **Perceptual Color Distance (3D CIELAB LUT)**: Precompute a 5-bit quantized $32 \times 32 \times 32$ lookup table (`FloatArray(32 * 32 * 32 * 3)`). When indexing `[L*, a*, b*]`, multiply the 15-bit packed cube index by `3`:
  `val baseIdx = (((r ushr 3 and 0x1F) shl 10) or ((g ushr 3 and 0x1F) shl 5) or (b ushr 3 and 0x1F)) * 3`
* **Sobel Edge Weighting**: Precompute a 2D edge salience mask $w(x,y) = 1 + \gamma \sqrt{G_x^2 + G_y^2}$ from the target image's luminance channel to weight errors at facial contours and structural boundaries higher than flat backgrounds.
* **Parsimony Pressure ($\text{SSD} + \alpha \cdot N_{\text{strokes}}$)**: Penalizing `strokes.size` only exerts selection pressure if genome length can vary; with a fixed stroke count, $\alpha \cdot N_{\text{strokes}}$ is an invariant constant offset that cancels out. Always pair parsimony regularization with **structural Add Stroke and Remove Stroke mutation branches**.

---

## 4. Evolutionary Strategies: $(1 + 1)$, $(1 + \lambda)$ & The Island Model

In image approximation, classic crossover (mating two candidate paintings) creates visual chaos due to spatial layer conflicts. Use single-parent mutation with **neutral drift**:

1. Maintain one current champion (`Painting`).
2. Mutate non-destructively to produce a candidate (`val candidate = currentBest.mutate(...)`).
3. If `candidateError <= currentBest.error`, accept and adopt:
   `currentBest = candidate.copy(error = candidateError)`
   * Accepting neutral mutations (`<=` instead of strict `<`) lets the genome drift across flat fitness plateaus (e.g., shifting a fully occluded stroke to a new position where a subsequent mutation can make it beneficial).

### Step-Size Annealing & Rechenberg's 1/5th Rule
Distinguish **Step-Size Annealing** (cooling the mutation radius $\sigma(t)$ while keeping greedy `<=` selection) from **Metropolis Simulated Annealing** (accepting worse candidates with Boltzmann probability $\exp(-\Delta E / T)$, which degrades crisp vector contours in high-dimensional stroke spaces).
1. **Combined Exponential Cooling + Adaptive Gain**:
   Compute the scheduled base radius $\sigma_{\text{base}}(t) = \sigma_{\min} + (\sigma_0 - \sigma_{\min}) \exp(-t/\tau)$ and multiply it by an adaptive gain factor `adaptiveGain` adjusted every 500–1,000 evaluations via Rechenberg's 1/5th rule:
   - If $r_{\text{acc}} > 0.22$: `adaptiveGain = (adaptiveGain * 1.15f).coerceAtMost(2.5f)`
   - If $r_{\text{acc}} < 0.18$: `adaptiveGain = (adaptiveGain * 0.85f).coerceAtLeast(0.25f)`
   - Return `(baseSigma * adaptiveGain).coerceIn(minSigma, initialSigma)`.
2. **Multi-Resolution Image Pyramids**:
   Evolve coarse foundation strokes on $64\times 64$ downsampled targets (16x faster), scale all stroke coordinates and widths by $2\times$ (`Painting.upscale(2.0f)`), and refine micro-detail on $128\times 128$ and $256\times 256$ rasters.

### Multi-Core Parallelism: Fine-Grained $(1+\lambda)$ vs. Coarse-Grained Island Model
- **Fine-Grained $(1+\lambda)$**: All threads evaluate $\lambda$ children of a single shared parent each generation, synchronizing at a barrier. Fast step velocity, but prone to premature convergence on a single lineage.
- **Coarse-Grained Island Model**: Spawn $N$ independent worker threads, each running its own isolated $(1+1)$ loop with a private `Evaluator` and zero barrier synchronization.
- **Ring Migration & Lock-Free Global Champion**: Every $M$ generations, Island $i$ deposits its `localBest` into `migrationInboxes[(i + 1) % numIslands]`. When updating `globalChampion`, compare cached `Painting.error` values atomically via `accumulateAndGet` instead of re-evaluating on the CPU:
  ```kotlin
  globalChampion.accumulateAndGet(localBest) { prev, next ->
      if (prev == null || next.error < prev.error) next else prev
  }
  ```

---

## 5. UI, Interaction & Vector Export

1. **Split-Screen Layout**: Display the reference target image on the left and the evolving champion on the right using `canvas.drawImageRect`.
2. **Thread Safety**: Pass immutable `Painting` snapshots between background evolution threads and the Swing EDT using `AtomicReference<Painting>`.
3. **HiDPI Canvas Normalization & In-Canvas Skia HUD**: Skiko's `onRender(canvas, width, height, ...)` receives physical backing pixels. If drawn without canvas scaling on a $2\times$ Retina display ($1920 \times 1360$), display viewports balloon, telemetry typography renders microscopic ($6\text{pt}$), and column offsets cram to the far left. Always normalize the canvas to Swing logical points at the start of `onRender`:
   ```kotlin
   val scale = skiaLayer.contentScale
   canvas.save()
   canvas.scale(scale, scale)
   val w = width / scale
   val h = height / scale

   canvas.drawRRect(RRect.makeXYWH(32f, hudY, w - 64f, 82f, 10f), cardPaint)
   canvas.drawString("GENERATIONS", col1, hudY + 28f, labelFont, textDimPaint)
   canvas.drawString("%,d".format(gens), col1, hudY + 54f, valueFont, textMainPaint)

   canvas.restore()
   ```
4. **Decoupled Redraw**: Trigger `skiaLayer.needRedraw()` at the end of `onRender()` to keep the UI smooth at 60 FPS while the worker thread evolves at full speed.
5. **Key Controls via KeyAdapter**:
   * `[Space]`: Toggle pause/resume (`painter.togglePause()`).
   * `[R]`: Reset population.
   * `[+]` / `[-]`: Dynamically increase/decrease stroke count (`painter.adjustStrokes(10)`).
   * `[S]`: Export raster PNG via `image.encodeToData(EncodedImageFormat.PNG)`.
6. **Scalable Vector Export (SVG) & `.gene` Checkpoints**:
   Serialize `Painting.strokes` directly to W3C SVG XML (`<svg>`, `<path d="M... Q..." />`) using `StrokeGene`'s exact fields (`x0, y0, xc, yc, x1, y1, strokeWidth, r, g, b, a`), and persist `.gene` CSV checkpoints (`# GENETIC_ART_V1 error=...`) so multi-hour runs can be paused and resumed.
   ```kotlin
   fun Painting.toSvg(w: Int, h: Int): String = buildString {
       appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $w $h">""")
       appendLine("""  <rect width="$w" height="$h" fill="#ffffff"/>""")
       strokes.forEach { s ->
           val opacity = "%.3f".format(java.util.Locale.US, s.a / 255f)
           appendLine("""  <path d="M ${s.x0},${s.y0} Q ${s.xc},${s.yc} ${s.x1},${s.y1}" stroke="rgb(${s.r},${s.g},${s.b})" stroke-opacity="$opacity" stroke-width="${s.strokeWidth}" stroke-linecap="round" fill="none"/>""")
       }
       appendLine("</svg>")
   }
   ```
7. **Time-Lapse Capture & FFmpeg Assembly**:
   Dump PNG snapshots on a geometric cadence (e.g. every 10 accepted mutations in early generations, every 250 in late generations). Compile to 60 FPS 4K video via CLI:
   ```bash
   ffmpeg -framerate 60 -i frame_%05d.png -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p timelapse.mp4
   ```
8. **Headless Smoke Execution & Stepwise Testing**:
   In headless CI or agent workflows, Swing GUI windows block execution. Implement a `--headless` command-line argument that runs a fixed generation budget (e.g., 500 generations) and writes an artifact to verify end-to-end correctness without an active display server. When maintaining progressive learning projects, scaffold milestones in sequential self-contained directories (`steps/0001-...`), verifying that each step independently passes `./gradlew ktlintCheck test` and `./gradlew run --args="--headless"`.

See detailed theory, mathematical derivations, and fitness algorithms in:
[`references/evolutionary-strategies-and-fitness.md`](./references/evolutionary-strategies-and-fitness.md).


