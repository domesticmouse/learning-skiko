import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.Rect
import java.io.File

class TargetImage(
    val bitmap: Bitmap,
    val image: Image,
    val pixelBytes: ByteArray,
    val width: Int,
    val height: Int,
    val sourceDescription: String,
) {
    companion object {
        fun loadOrGenerate(
            path: String?,
            simWidth: Int = 256,
            simHeight: Int = 256,
        ): TargetImage {
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists() && file.isFile) {
                    try {
                        val encodedBytes = file.readBytes()
                        val loadedImage = Image.makeFromEncoded(encodedBytes)
                        val scaledBitmap = Bitmap().apply { allocN32Pixels(simWidth, simHeight) }
                        val canvas = Canvas(scaledBitmap)
                        canvas.drawImageRect(
                            loadedImage,
                            Rect.makeWH(simWidth.toFloat(), simHeight.toFloat()),
                        )
                        val pixels = scaledBitmap.readPixels(scaledBitmap.imageInfo)!!
                        val finalImage = Image.makeFromBitmap(scaledBitmap)
                        return TargetImage(
                            bitmap = scaledBitmap,
                            image = finalImage,
                            pixelBytes = pixels,
                            width = simWidth,
                            height = simHeight,
                            sourceDescription = file.name,
                        )
                    } catch (e: Exception) {
                        System.err.println("Failed to load '$path' (${e.message}). Falling back to procedural target.")
                    }
                } else {
                    System.err.println("Target image file '$path' not found. Falling back to procedural target.")
                }
            }

            return generateFallback(simWidth, simHeight)
        }

        private fun generateFallback(
            width: Int,
            height: Int,
        ): TargetImage {
            val bitmap = Bitmap().apply { allocN32Pixels(width, height) }
            val canvas = Canvas(bitmap)
            val paint = Paint().apply { isAntiAlias = true }

            val w = width.toFloat()
            val h = height.toFloat()

            // 1. Sky gradient background (deep dusk purple to warm orange)
            val skySteps = 30
            for (i in 0 until skySteps) {
                val t = i.toFloat() / skySteps
                val r = (30 + (240 - 30) * t).toInt()
                val g = (20 + (110 - 20) * t).toInt()
                val b = (80 + (50 - 80) * t).toInt()
                paint.color = Color.makeRGB(r, g, b)
                paint.mode = PaintMode.FILL
                canvas.drawRect(
                    Rect.makeLTRB(0f, t * h * 0.7f, w, (t + 1f / skySteps) * h * 0.7f),
                    paint,
                )
            }

            // 2. Glowing Golden Sun
            paint.color = Color.makeARGB(200, 255, 220, 100)
            canvas.drawCircle(w * 0.5f, h * 0.42f, w * 0.18f, paint)
            paint.color = Color.makeARGB(255, 255, 245, 180)
            canvas.drawCircle(w * 0.5f, h * 0.42f, w * 0.12f, paint)

            // 3. Mountain silhouette in background (dark violet)
            paint.color = Color.makeRGB(45, 25, 65)
            val mountainBack =
                Path().apply {
                    moveTo(0f, h * 0.72f)
                    lineTo(w * 0.28f, h * 0.45f)
                    lineTo(w * 0.55f, h * 0.65f)
                    lineTo(w * 0.85f, h * 0.40f)
                    lineTo(w, h * 0.68f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
            canvas.drawPath(mountainBack, paint)

            // 4. Foreground hill silhouette (deep indigo/teal)
            paint.color = Color.makeRGB(15, 30, 45)
            val hillFore =
                Path().apply {
                    moveTo(0f, h * 0.78f)
                    quadTo(w * 0.45f, h * 0.62f, w, h * 0.82f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
            canvas.drawPath(hillFore, paint)

            val pixels = bitmap.readPixels(bitmap.imageInfo)!!
            val image = Image.makeFromBitmap(bitmap)

            return TargetImage(
                bitmap = bitmap,
                image = image,
                pixelBytes = pixels,
                width = width,
                height = height,
                sourceDescription = "Procedural Sunset Landscape",
            )
        }
    }
}
