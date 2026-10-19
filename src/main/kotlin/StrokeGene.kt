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
    var x0: Float,
    var y0: Float,
    var xc: Float,
    var yc: Float,
    var x1: Float,
    var y1: Float,
    var strokeWidth: Float,
    var r: Int,
    var g: Int,
    var b: Int,
    var a: Int,
) {
    fun copy(): StrokeGene =
        StrokeGene(
            x0 = x0,
            y0 = y0,
            xc = xc,
            yc = yc,
            x1 = x1,
            y1 = y1,
            strokeWidth = strokeWidth,
            r = r,
            g = g,
            b = b,
            a = a,
        )

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
     * Mutates one or more properties of this stroke.
     */
    fun mutate(
        width: Float,
        height: Float,
        rng: Random,
    ) {
        val mutationType = rng.nextInt(5)
        when (mutationType) {
            0 -> {
                // Jitter control point
                val delta = (rng.nextFloat() - 0.5f) * 24f
                if (rng.nextBoolean()) {
                    xc = (xc + delta).coerceIn(0f, width)
                } else {
                    yc = (yc + delta).coerceIn(0f, height)
                }
            }
            1 -> {
                // Jitter endpoints
                val deltaX = (rng.nextFloat() - 0.5f) * 20f
                val deltaY = (rng.nextFloat() - 0.5f) * 20f
                if (rng.nextBoolean()) {
                    x0 = (x0 + deltaX).coerceIn(0f, width)
                    y0 = (y0 + deltaY).coerceIn(0f, height)
                } else {
                    x1 = (x1 + deltaX).coerceIn(0f, width)
                    y1 = (y1 + deltaY).coerceIn(0f, height)
                }
            }
            2 -> {
                // Mutate color (RGB)
                val delta = rng.nextInt(31) - 15
                when (rng.nextInt(3)) {
                    0 -> r = (r + delta).coerceIn(0, 255)
                    1 -> g = (g + delta).coerceIn(0, 255)
                    else -> b = (b + delta).coerceIn(0, 255)
                }
            }
            3 -> {
                // Mutate opacity (Alpha)
                val delta = rng.nextInt(21) - 10
                a = (a + delta).coerceIn(20, 230)
            }
            4 -> {
                // Mutate stroke thickness
                val delta = (rng.nextFloat() - 0.5f) * 4f
                strokeWidth = (strokeWidth + delta).coerceIn(2f, 35f)
            }
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
