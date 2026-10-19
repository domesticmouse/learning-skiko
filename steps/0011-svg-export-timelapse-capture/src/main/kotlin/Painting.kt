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
    val strokes: List<StrokeGene>,
    val error: Long = Long.MAX_VALUE,
) {
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
    ): Painting {
        if (strokes.isEmpty()) return this

        val roll = rng.nextFloat()
        val newStrokes = strokes.toMutableList()
        when {
            roll < 0.80f -> {
                // Mutate 1 to 2 random strokes
                val count = if (rng.nextBoolean()) 1 else 2
                repeat(count) {
                    val idx = rng.nextInt(newStrokes.size)
                    newStrokes[idx] = newStrokes[idx].mutate(width, height, rng)
                }
            }
            roll < 0.95f -> {
                // Swap stroke layering order
                val i = rng.nextInt(newStrokes.size)
                val j = rng.nextInt(newStrokes.size)
                Collections.swap(newStrokes, i, j)
            }
            else -> {
                // Replace a stroke entirely with a new random stroke
                val idx = rng.nextInt(newStrokes.size)
                newStrokes[idx] = StrokeGene.random(width, height, rng)
            }
        }
        return copy(strokes = newStrokes, error = Long.MAX_VALUE)
    }

    companion object {
        fun createRandom(
            strokeCount: Int,
            width: Float,
            height: Float,
            rng: Random = Random.Default,
        ): Painting {
            val list = ArrayList<StrokeGene>(strokeCount)
            for (i in 0 until strokeCount) {
                list.add(StrokeGene.random(width, height, rng))
            }
            return Painting(list)
        }
    }
}
