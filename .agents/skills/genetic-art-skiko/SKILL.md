---
name: genetic-art-skiko
description: >-
  Design, build, and optimize evolutionary and genetic algorithm art generators
  using Skiko (Skia for Kotlin). Use when creating procedural/evolutionary image
  generators, evolving brush strokes or geometric polygons to approximate images,
  implementing (1+λ) Hill Climber evolutionary strategies, performing fast offscreen
  raster fitness evaluation, or streaming live evolutionary progress in a Skiko desktop UI.
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

## 2. Chromosome Representation: Brush Strokes vs Polygons

### Curved Brush Stroke Gene (`Path.quadTo`)
Translucent quadratic Bézier strokes mimic oil/watercolor glaze:

```kotlin
data class StrokeGene(
    var x0: Float, var y0: Float,
    var xc: Float, var yc: Float,
    var x1: Float, var y1: Float,
    var strokeWidth: Float,
    var r: Int, var g: Int, var b: Int, var a: Int
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
}
```

---

## 3. High-Performance Offscreen Fitness Evaluation

To evaluate 1,000+ candidates per second:
1. **Reuse all objects**: Allocate `Bitmap`, `Canvas`, `Paint`, and `Path` once per evaluator thread.
2. **Flat Byte Array Sum of Squared Differences (SSD)**: Avoid `Color` object allocations in hot loops.

```kotlin
class Evaluator(val width: Int, val height: Int, val targetBytes: ByteArray) {
    private val bitmap = Bitmap().apply { allocN32Pixels(width, height) }
    private val canvas = Canvas(bitmap)
    private val paint = Paint()
    private val path = Path()

    fun evaluate(painting: Painting): Long {
        painting.render(canvas, width.toFloat(), height.toFloat(), paint, path)
        val candidateBytes = bitmap.readPixels(bitmap.imageInfo) ?: return Long.MAX_VALUE

        var error = 0L
        val len = candidateBytes.size
        var i = 0
        while (i < len) {
            val db = (candidateBytes[i].toInt() and 0xFF) - (targetBytes[i].toInt() and 0xFF)
            val dg = (candidateBytes[i + 1].toInt() and 0xFF) - (targetBytes[i + 1].toInt() and 0xFF)
            val dr = (candidateBytes[i + 2].toInt() and 0xFF) - (targetBytes[i + 2].toInt() and 0xFF)
            error += (dr * dr) + (dg * dg) + (db * db)
            i += 4 // BGRA / RGBA step
        }
        painting.error = error
        return error
    }
}
```

---

## 4. The $(1 + \lambda)$ Evolutionary Strategy

In image approximation, classic crossover (mating two candidate paintings) often creates visual chaos due to spatial layer conflicts. Use **$(1 + 1)$ or $(1 + \lambda)$ Hill Climbing**:

1. Maintain one current champion (`Painting`).
2. Clone and mutate with weighted operators:
   - **80%**: Nudge geometry ($\pm 10$px), color ($\pm 15$ RGB), opacity ($\pm 10$ alpha), or thickness.
   - **15%**: Swap layer order of two strokes (foreground $\leftrightarrow$ background).
   - **5%**: Re-seed a completely new random stroke (breaks local plateaus).
3. If `candidateError <= championError`, replace the champion (`champion = candidate`).
   * Allowing neutral mutations (`<=` instead of `<`) allows the genome to drift along neutral fitness plateaus toward better configurations.

---

## 5. UI & Interaction Best Practices

1. **Split-Screen Layout**: Display the reference target image on the left and the evolving champion on the right using `canvas.drawImageRect`.
2. **Thread Safety**: Pass champion snapshots between the background evolution thread and the Swing EDT using `AtomicReference<Painting>`.
3. **Live Metrics**: Display generation counter, accepted mutation rate, % improvement, and speed (`evals/sec`).
4. **Key Controls**:
   * `[Space]`: Toggle pause/resume (`AtomicBoolean`).
   * `[R]`: Reset population.
   * `[+]` / `[-]`: Dynamically increase/decrease stroke count.
   * `[S]`: Export snapshot to disk via `image.encodeToData(EncodedImageFormat.PNG)`.

See detailed theory and fitness algorithms in:
[`references/evolutionary-strategies-and-fitness.md`](./references/evolutionary-strategies-and-fitness.md).
