import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Path
import kotlin.random.Random

/**
 * Represents a single curved brush stroke gene.
 * Rendered as a quadratic Bézier curve with rounded caps and translucent color.
 */
data class StrokeGene(
    val x0: Float,
    val y0: Float,
    val xc: Float,
    val yc: Float,
    val x1: Float,
    val y1: Float,
    val strokeWidth: Float,
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int,
) {
    fun render(
        canvas: Canvas,
        paint: Paint,
        path: Path,
    ) {
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
     * Mutates one or more properties of this stroke and returns a new StrokeGene.
     */
    fun mutate(
        width: Float,
        height: Float,
        rng: Random,
    ): StrokeGene {
        val mutationType = rng.nextInt(5)
        return when (mutationType) {
            0 -> {
                // Jitter control point
                val delta = (rng.nextFloat() - 0.5f) * 24f
                if (rng.nextBoolean()) {
                    copy(xc = (xc + delta).coerceIn(0f, width))
                } else {
                    copy(yc = (yc + delta).coerceIn(0f, height))
                }
            }
            1 -> {
                // Jitter endpoints
                val deltaX = (rng.nextFloat() - 0.5f) * 20f
                val deltaY = (rng.nextFloat() - 0.5f) * 20f
                if (rng.nextBoolean()) {
                    copy(
                        x0 = (x0 + deltaX).coerceIn(0f, width),
                        y0 = (y0 + deltaY).coerceIn(0f, height),
                    )
                } else {
                    copy(
                        x1 = (x1 + deltaX).coerceIn(0f, width),
                        y1 = (y1 + deltaY).coerceIn(0f, height),
                    )
                }
            }
            2 -> {
                // Mutate color (RGB)
                val delta = rng.nextInt(31) - 15
                when (rng.nextInt(3)) {
                    0 -> copy(r = (r + delta).coerceIn(0, 255))
                    1 -> copy(g = (g + delta).coerceIn(0, 255))
                    else -> copy(b = (b + delta).coerceIn(0, 255))
                }
            }
            3 -> {
                // Mutate opacity (Alpha)
                val delta = rng.nextInt(21) - 10
                copy(a = (a + delta).coerceIn(20, 230))
            }
            4 -> {
                // Mutate stroke thickness
                val delta = (rng.nextFloat() - 0.5f) * 4f
                copy(strokeWidth = (strokeWidth + delta).coerceIn(2f, 35f))
            }
            else -> this
        }
    }

    companion object {
        fun random(
            width: Float,
            height: Float,
            rng: Random = Random.Default,
        ): StrokeGene {
            val startX = rng.nextFloat() * width
            val startY = rng.nextFloat() * height
            val length = 20f + rng.nextFloat() * 60f
            val angle = rng.nextFloat() * (Math.PI * 2.0).toFloat()
            val endX = (startX + kotlin.math.cos(angle) * length).coerceIn(0f, width)
            val endY = (startY + kotlin.math.sin(angle) * length).coerceIn(0f, height)
            val ctrlX = ((startX + endX) / 2f + (rng.nextFloat() - 0.5f) * 40f).coerceIn(0f, width)
            val ctrlY = ((startY + endY) / 2f + (rng.nextFloat() - 0.5f) * 40f).coerceIn(0f, height)

            return StrokeGene(
                x0 = startX,
                y0 = startY,
                xc = ctrlX,
                yc = ctrlY,
                x1 = endX,
                y1 = endY,
                strokeWidth = 4f + rng.nextFloat() * 16f,
                r = rng.nextInt(256),
                g = rng.nextInt(256),
                b = rng.nextInt(256),
                a = 40 + rng.nextInt(120),
            )
        }
    }
}
