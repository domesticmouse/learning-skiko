import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Path
import kotlin.random.Random

sealed interface Gene {
    // Reuses pre-allocated Skia native wrappers to avoid JNI heap pressure in the hot loop
    fun render(
        canvas: Canvas,
        paint: Paint,
        path: Path,
    )

    fun mutate(
        canvasWidth: Float,
        canvasHeight: Float,
        rng: Random = Random.Default,
    ): Gene
}

data class CubicCurveGene(
    val x0: Float,
    val y0: Float,
    val cx1: Float,
    val cy1: Float,
    val cx2: Float,
    val cy2: Float,
    val x3: Float,
    val y3: Float,
    val strokeWidth: Float,
    val argb: Int,
) : Gene {
    override fun render(
        canvas: Canvas,
        paint: Paint,
        path: Path,
    ) {
        paint.color = argb
        paint.mode = PaintMode.STROKE
        paint.strokeWidth = strokeWidth
        paint.strokeCap = PaintStrokeCap.ROUND

        path.reset()
        path.moveTo(x0, y0)
        path.cubicTo(cx1, cy1, cx2, cy2, x3, y3)
        canvas.drawPath(path, paint)
    }

    override fun mutate(
        canvasWidth: Float,
        canvasHeight: Float,
        rng: Random,
    ): Gene =
        copy(
            cx1 = (cx1 + (rng.nextFloat() - 0.5f) * 20f).coerceIn(0f, canvasWidth),
            cy1 = (cy1 + (rng.nextFloat() - 0.5f) * 20f).coerceIn(0f, canvasHeight),
            cx2 = (cx2 + (rng.nextFloat() - 0.5f) * 20f).coerceIn(0f, canvasWidth),
            cy2 = (cy2 + (rng.nextFloat() - 0.5f) * 20f).coerceIn(0f, canvasHeight),
        )
}

data class TaperedStrokeGene(
    val x0: Float,
    val y0: Float,
    val xc: Float,
    val yc: Float,
    val x1: Float,
    val y1: Float,
    val wStart: Float,
    val wPeak: Float,
    val wEnd: Float,
    val argb: Int,
) : Gene {
    override fun render(
        canvas: Canvas,
        paint: Paint,
        path: Path,
    ) {
        paint.color = argb
        paint.mode = PaintMode.FILL
        val steps = 16
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val u = 1f - t
            val x = u * u * x0 + 2f * u * t * xc + t * t * x1
            val y = u * u * y0 + 2f * u * t * yc + t * t * y1
            val w = wStart * u + wPeak * (4f * t * u) + wEnd * t
            canvas.drawCircle(x, y, w * 0.5f, paint)
        }
    }

    override fun mutate(
        canvasWidth: Float,
        canvasHeight: Float,
        rng: Random,
    ): Gene = copy(wPeak = (wPeak + (rng.nextFloat() - 0.5f) * 6f).coerceIn(4f, 36f))
}

data class PolygonGene(
    val points: List<Pair<Float, Float>>,
    val argb: Int,
) : Gene {
    override fun render(
        canvas: Canvas,
        paint: Paint,
        path: Path,
    ) {
        if (points.isEmpty()) return
        paint.color = argb
        paint.mode = PaintMode.FILL

        path.reset()
        path.moveTo(points[0].first, points[0].second)
        for (i in 1 until points.size) {
            path.lineTo(points[i].first, points[i].second)
        }
        path.closePath()
        canvas.drawPath(path, paint)
    }

    override fun mutate(
        canvasWidth: Float,
        canvasHeight: Float,
        rng: Random,
    ): Gene {
        if (points.isEmpty()) return this
        val idx = rng.nextInt(points.size)
        val updated = points.toMutableList()
        val (px, py) = updated[idx]
        updated[idx] =
            Pair(
                (px + (rng.nextFloat() - 0.5f) * 20f).coerceIn(0f, canvasWidth),
                (py + (rng.nextFloat() - 0.5f) * 20f).coerceIn(0f, canvasHeight),
            )
        return copy(points = updated)
    }
}
