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

> **Warning: `path.closePath()` vs `path.close()`**  
> In Skiko, `Path.close()` implements Java's `AutoCloseable.close()`, which deletes the underlying C++ native Skia pointer (`_ptr = 0`). Subsequent draws cause a fatal native crash (`SIGSEGV in SkPath::isFinite()`). To close a geometric contour, **always use `path.closePath()`**, never `path.close()`.

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

Skiko renders directly to a physical pixel backing buffer, exposing the hardware display multiplier via `skiaLayer.contentScale`:
```kotlin
val scale = skiaLayer.contentScale // e.g. 2.0 on macOS Retina, 1.0 on standard DPI
```
- `width` and `height` received in `onRender(canvas, width, height, nanoTime)` are **physical device pixels** (e.g. $1600 \times 1200$ for an $800 \times 600$ window at $2\times$ scale).
- Mouse events in Swing `MouseEvent` report in **logical points** ($[0..800]$ and $[0..600]$).

### Pattern A: Scale Mouse Events to Physical Canvas Coordinates
```kotlin
skiaLayer.addMouseMotionListener(object : MouseMotionAdapter() {
    override fun mouseMoved(e: MouseEvent) {
        val scale = skiaLayer.contentScale
        physicalMouseX = e.x.toFloat() * scale
        physicalMouseY = e.y.toFloat() * scale
        skiaLayer.needRedraw()
    }
})
```

### Pattern B: Pre-Scale Canvas to Work in Logical Points
```kotlin
override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
    val scale = skiaLayer.contentScale
    canvas.save()
    canvas.scale(scale, scale)
    // Draw all shapes, text, and mouse interactions in logical points (0..800, 0..600)
    canvas.drawCircle(logicalMouseX, logicalMouseY, radius, circlePaint)
    canvas.restore()
}
```

---

## 7. Offscreen Rendering & Direct Pixel Access

Render graphics to an offscreen buffer (e.g. for computer vision, AI/GA fitness evaluation, or thumbnail generation) without touching a window:

```kotlin
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color

// 1. Allocate an offscreen raster bitmap
val bitmap = Bitmap().apply { allocN32Pixels(256, 256) }
val offscreenCanvas = Canvas(bitmap)

// 2. Draw standard Skia elements
offscreenCanvas.clear(Color.WHITE)
offscreenCanvas.drawCircle(128f, 128f, 64f, fillPaint)

// 3. Extract raw pixel bytes (BGRA/RGBA, 4 bytes per pixel)
val pixelBytes: ByteArray = bitmap.readPixels(bitmap.imageInfo)!!
```

---

## 8. Image Loading, Scaling & Saving

### Loading Images (PNG, JPEG, WEBP)
```kotlin
import org.jetbrains.skia.Image
import java.io.File

val fileBytes = File("photo.png").readBytes()
val loadedImage = Image.makeFromEncoded(fileBytes)
```

### Scaling Images onto Canvas
```kotlin
import org.jetbrains.skia.Rect

// Draw and scale image into a specific bounding box
val dstRect = Rect.makeXYWH(x = 50f, y = 50f, w = 256f, h = 256f)
canvas.drawImageRect(loadedImage, dstRect)
```

### Saving Canvas/Bitmap to Disk (PNG / JPEG)
```kotlin
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

val snapshotImage = Image.makeFromBitmap(bitmap)
val encodedData = snapshotImage.encodeToData(EncodedImageFormat.PNG, quality = 100)
if (encodedData != null) {
    File("output.png").writeBytes(encodedData.bytes)
}
```
