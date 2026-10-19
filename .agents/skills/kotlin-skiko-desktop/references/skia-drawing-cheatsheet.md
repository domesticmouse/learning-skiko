# Skia 2D Graphics Drawing Cheatsheet (Skiko)

## 1. Paint Basics

Paints control how geometries and text are filled, stroked, and colored.

```kotlin
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode

// Solid Fill
val fillPaint = Paint().apply {
    color = 0xFF89B4FA.toInt() // ARGB
    isAntiAlias = true
}

// Stroke (Outline)
val strokePaint = Paint().apply {
    color = 0xFFA6E3A1.toInt()
    mode = PaintMode.STROKE
    strokeWidth = 3f
    isAntiAlias = true
}
```

---

## 2. Shapes & Primitives

```kotlin
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RRect

// Rectangles
canvas.drawRect(Rect.makeXYWH(50f, 50f, 200f, 100f), fillPaint)

// Rounded Rectangles
canvas.drawRRect(RRect.makeXYWH(50f, 180f, 200f, 100f, 16f), fillPaint)

// Circles
canvas.drawCircle(centerX = 400f, centerY = 300f, radius = 50f, fillPaint)

// Lines
canvas.drawLine(x0 = 0f, y0 = 0f, x1 = 800f, y1 = 600f, strokePaint)
```

---

## 3. Vector Paths & Bezier Curves

```kotlin
import org.jetbrains.skia.Path

val wavePath = Path().apply {
    moveTo(50f, 300f)
    cubicTo(
        cp1x = 150f, cp1y = 200f,
        cp2x = 250f, cp2y = 400f,
        toX = 350f, toY = 300f
    )
    lineTo(350f, 400f)
    lineTo(50f, 400f)
    closePath()
}

canvas.drawPath(wavePath, fillPaint)
```

---

## 4. Linear & Radial Gradients (Shaders)

```kotlin
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Point

val gradientShader = Shader.makeLinearGradient(
    x0 = 0f, y0 = 0f,
    x1 = 400f, y1 = 400f,
    colors = intArrayOf(0xFFF38BA8.toInt(), 0xFF89B4FA.toInt())
)

val gradientPaint = Paint().apply {
    shader = gradientShader
    isAntiAlias = true
}

canvas.drawRoundRect(50f, 50f, 350f, 250f, 20f, 20f, gradientPaint)
```

---

## 5. Typography & Text Rendering

```kotlin
import org.jetbrains.skia.Font
import org.jetbrains.skia.Typeface
import org.jetbrains.skia.FontStyle

// Default system font with size
val font = Font().apply { size = 24f }

// Named font (e.g. Montserrat, Helvetica, Courier)
val customFont = Font(Typeface.makeFromName("Helvetica", FontStyle.BOLD), 18f)

canvas.drawString("Hello Skiko!", 50f, 100f, font, fillPaint)
```

---

## 6. High-DPI (Retina) Scaling

Skiko handles hardware scaling via `skiaLayer.contentScale`:
```kotlin
val scale = skiaLayer.contentScale // e.g. 2.0 on Retina
```
- `width` and `height` received in `onRender(canvas, width, height, nanoTime)` are physical device pixels.
- Mouse events in Swing `MouseEvent` report logical points. Multiply by `skiaLayer.contentScale` if you need exact physical pixel coordinates.
