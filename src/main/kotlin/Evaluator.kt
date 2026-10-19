import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path

/**
 * Fast offscreen evaluator that measures how closely a candidate painting matches the target image.
 * Reuses internal buffers to minimize memory allocations.
 */
class Evaluator(
    val width: Int,
    val height: Int,
    val targetBytes: ByteArray,
    val backgroundColor: Int = Color.WHITE,
) {
    private val bitmap = Bitmap().apply { allocN32Pixels(width, height) }
    private val canvas = Canvas(bitmap)
    private val paint = Paint()
    private val path = Path()

    fun evaluate(painting: Painting): Long {
        // 1. Render painting onto offscreen canvas
        painting.render(
            canvas = canvas,
            width = width.toFloat(),
            height = height.toFloat(),
            paint = paint,
            path = path,
            backgroundColor = backgroundColor,
        )

        // 2. Read pixel buffer
        val candidateBytes = bitmap.readPixels(bitmap.imageInfo) ?: return Long.MAX_VALUE

        // 3. Compute Sum of Squared Differences
        var totalError = 0L
        val len = candidateBytes.size
        var i = 0
        while (i < len) {
            val d0 = (candidateBytes[i].toInt() and 0xFF) - (targetBytes[i].toInt() and 0xFF)
            val d1 = (candidateBytes[i + 1].toInt() and 0xFF) - (targetBytes[i + 1].toInt() and 0xFF)
            val d2 = (candidateBytes[i + 2].toInt() and 0xFF) - (targetBytes[i + 2].toInt() and 0xFF)
            totalError += (d0 * d0) + (d1 * d1) + (d2 * d2)
            i += 4
        }

        return totalError
    }
}
