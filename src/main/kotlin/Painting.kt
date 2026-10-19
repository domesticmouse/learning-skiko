import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.Rect
import java.util.Collections
import kotlin.random.Random

/**
 * Represents an individual candidate artwork (a sequence of brush strokes).
 */
data class Painting(
    val strokes: MutableList<StrokeGene>,
    var error: Long = Long.MAX_VALUE,
) {
    fun clone(): Painting {
        val copiedStrokes = ArrayList<StrokeGene>(strokes.size)
        for (stroke in strokes) {
            copiedStrokes.add(stroke.copy())
        }
        return Painting(copiedStrokes, error)
    }

    fun render(
        canvas: Canvas,
        width: Float,
        height: Float,
        paint: Paint,
        path: Path,
        backgroundColor: Int = Color.WHITE,
    ) {
        // Draw background base coat
        paint.color = backgroundColor
        paint.mode = PaintMode.FILL
        canvas.drawRect(Rect.makeWH(width, height), paint)

        // Draw strokes in sequence
        for (stroke in strokes) {
            stroke.render(canvas, paint, path)
        }
    }

    /**
     * Applies a mutation to produce a child candidate.
     */
    fun mutate(
        width: Float,
        height: Float,
        rng: Random,
    ) {
        if (strokes.isEmpty()) return

        val roll = rng.nextFloat()
        when {
            roll < 0.80f -> {
                // Mutate 1 to 2 random strokes
                val count = if (rng.nextBoolean()) 1 else 2
                repeat(count) {
                    val idx = rng.nextInt(strokes.size)
                    strokes[idx].mutate(width, height, rng)
                }
            }
            roll < 0.95f -> {
                // Swap stroke layering order
                val i = rng.nextInt(strokes.size)
                val j = rng.nextInt(strokes.size)
                Collections.swap(strokes, i, j)
            }
            else -> {
                // Replace a stroke entirely with a new random stroke
                val idx = rng.nextInt(strokes.size)
                strokes[idx] = StrokeGene.random(width, height, rng)
            }
        }
    }

    companion object {
        fun createRandom(
            strokeCount: Int,
            width: Float,
            height: Float,
            rng: Random = Random.Default,
        ): Painting {
            val list = mutableListOf<StrokeGene>()
            for (i in 0 until strokeCount) {
                list.add(StrokeGene.random(width, height, rng))
            }
            return Painting(list)
        }
    }
}
