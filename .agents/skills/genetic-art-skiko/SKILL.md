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

    /**
     * Non-destructively mutates one or more properties using compiler-generated copy().
     */
    fun mutate(width: Float, height: Float, rng: Random): StrokeGene {
        val delta = (rng.nextFloat() - 0.5f) * 24f
        return copy(xc = (xc + delta).coerceIn(0f, width))
    }
}
```

### Painting Candidate (`Painting`)
A painting holds a read-only list of strokes and an evaluation score:

```kotlin
data class Painting(
    val strokes: List<StrokeGene>,
    val error: Long = Long.MAX_VALUE,
) {
    fun mutate(width: Float, height: Float, rng: Random): Painting {
        if (strokes.isEmpty()) return this
        val newStrokes = strokes.toMutableList()
        val idx = rng.nextInt(newStrokes.size)
        newStrokes[idx] = newStrokes[idx].mutate(width, height, rng)
        return copy(strokes = newStrokes, error = Long.MAX_VALUE)
    }
}
```

---

## 3. High-Performance Offscreen Fitness Evaluation

To evaluate 2,000–5,000+ candidates per second on the CPU:
1. **Reuse all objects**: Allocate `Bitmap`, `Canvas`, `Paint`, and `Path` once per evaluator thread.
2. **Flat Byte Array L1 Manhattan Distance**: Direct byte subtraction with `Math.abs` avoids squaring overhead and prevents integer overflow.
3. **Signed Byte Masking**: Kotlin `Byte` is signed ($-128 \dots 127$); apply `and 0xFF` before subtracting.
4. **Pure Evaluation**: Avoid mutating candidate objects in `evaluate()`; return the error score directly.

```kotlin
class Evaluator(val width: Int, val height: Int, val targetBytes: ByteArray) {
    private val bitmap = Bitmap().apply { allocN32Pixels(width, height) }
    private val canvas = Canvas(bitmap)
    private val paint = Paint()
    private val path = Path()

    fun evaluate(painting: Painting): Long {
        painting.render(canvas, width.toFloat(), height.toFloat(), paint, path)
        val candidateBytes = bitmap.readPixels(bitmap.imageInfo) ?: return Long.MAX_VALUE

        var diff = 0L
        val totalBytes = width * height * 4
        var i = 0
        while (i < totalBytes) {
            val db = Math.abs((candidateBytes[i].toInt() and 0xFF) - (targetBytes[i].toInt() and 0xFF))
            val dg = Math.abs((candidateBytes[i + 1].toInt() and 0xFF) - (targetBytes[i + 1].toInt() and 0xFF))
            val dr = Math.abs((candidateBytes[i + 2].toInt() and 0xFF) - (targetBytes[i + 2].toInt() and 0xFF))
            diff += (db + dg + dr)
            i += 4 // BGRA / RGBA step (skips alpha)
        }
        return diff
    }
}
```

### Advanced Fitness Objectives
* **Perceptual Color Distance**: Use CIELAB $\Delta E$ lookup tables instead of raw RGB to match human color sensitivity.
* **Sobel Edge Weighting**: Precompute a 2D edge salience mask from the target image to weight errors at facial contours and fine lines higher than flat background areas.
* **Parsimony Pressure**: Penalize excessive stroke counts ($\text{Error} + \alpha \cdot N_{\text{strokes}}$) to prevent overfitting and encourage clean, painterly compositions.

---

## 4. Evolutionary Strategies: $(1 + \lambda)$ & The Island Model

In image approximation, classic crossover (mating two candidate paintings) creates visual chaos due to spatial layer conflicts. Use single-parent mutation with neutral drift:

1. Maintain one current champion (`Painting`).
2. Mutate non-destructively to produce a candidate (`val candidate = currentBest.mutate(...)`) with weighted operators:
   - **80%**: Nudge geometry ($\pm 10$px), color ($\pm 15$ RGB), opacity ($\pm 10$ alpha), or thickness.
   - **15%**: Swap layer order of two strokes (foreground $\leftrightarrow$ background).
   - **5%**: Re-seed a completely new random stroke (breaks local plateaus).
3. If `candidateError <= currentBest.error`, accept and adopt:
   `currentBest = candidate.copy(error = candidateError)`
   * Allowing neutral mutations (`<=` instead of `<`) allows the genome to drift along neutral fitness plateaus toward better configurations.

### Multi-Core Island Model
To harness multi-core CPUs without synchronization bottlenecks:
- Spawn independent worker threads, each running its own $(1+1)$ or $(1+\lambda)$ evolutionary loop with a private `Evaluator`.
- Establish a **Ring Migration Topology**: every $M$ generations (e.g. 1,000 evals), each island passes a snapshot of its champion to its adjacent neighbor.
- If the incoming immigrant beats the local champion, adopt it. This injects genetic diversity and knocks workers out of local stagnation.

---

## 5. UI, Interaction & Vector Export

1. **Split-Screen Layout**: Display the reference target image on the left and the evolving champion on the right using `canvas.drawImageRect`.
2. **Thread Safety**: Pass champion snapshots between the background evolution thread and the Swing EDT using `AtomicReference<Painting>`.
3. **In-Canvas Skia HUD**: Rather than mixing Swing buttons, render rounded dashboard cards directly on the canvas using `RRect` and Skia `Font`:
   ```kotlin
   canvas.drawRRect(RRect.makeXYWH(32f, hudY, w - 64f, 82f, 10f), cardPaint)
   canvas.drawString("GENERATIONS", col1, hudY + 28f, labelFont, textDimPaint)
   canvas.drawString("%,d".format(gens), col1, hudY + 54f, valueFont, textMainPaint)
   ```
4. **Decoupled Redraw**: Trigger `skiaLayer.needRedraw()` at the end of `onRender()` to keep the UI smooth at 60 FPS while the worker thread evolves at full speed.
5. **Key Controls via KeyAdapter**:
   * `[Space]`: Toggle pause/resume (`painter.togglePause()`).
   * `[R]`: Reset population.
   * `[+]` / `[-]`: Dynamically increase/decrease stroke count (`painter.adjustStrokes(10)`).
   * `[S]`: Export raster PNG via `image.encodeToData(EncodedImageFormat.PNG)`.
6. **Scalable Vector Export (SVG)**:
   Serialize the vector chromosome (`strokes`) directly to W3C SVG XML (`<svg>`, `<path d="M... Q..." stroke="..." />`). This produces infinite-resolution vector artwork that can be rendered at any scale without raster pixelation.

See detailed theory, mathematical derivations, and fitness algorithms in:
[`references/evolutionary-strategies-and-fitness.md`](./references/evolutionary-strategies-and-fitness.md).
