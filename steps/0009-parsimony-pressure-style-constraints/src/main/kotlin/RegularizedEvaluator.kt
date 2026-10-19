import kotlin.random.Random

class RegularizedEvaluator(
    val baseEvaluator: Evaluator,
    val parsimonyAlpha: Double = 25_000.0,
    val maxStrokeBudget: Int = 100,
) {
    fun evaluate(painting: Painting): Double {
        val strokeCount = painting.strokes.size

        // Hard constraint: disqualify chromosomes exceeding stroke limit
        if (strokeCount > maxStrokeBudget) {
            return Double.MAX_VALUE
        }

        // Base L2 Sum of Squared Differences (SSD)
        val pixelSSD = baseEvaluator.evaluate(painting).toDouble()

        // Linear parsimony penalty per stroke: every stroke must "pay rent" in error reduction
        val penalty = strokeCount * parsimonyAlpha

        return pixelSSD + penalty
    }
}

/**
 * Extends Painting mutation with variable-length structural operators (Add / Remove stroke)
 * so parsimony pressure can actively prune strokes that do not reduce SSD by at least α.
 */
fun Painting.mutateVariableLength(
    width: Float,
    height: Float,
    maxStrokes: Int = 100,
    rng: Random = Random.Default,
): Painting {
    val roll = rng.nextFloat()
    return when {
        // 10% chance: Delete a stroke (if it was contributing less than α, child wins!)
        roll < 0.10f && strokes.size > 4 -> {
            val newStrokes = strokes.toMutableList()
            newStrokes.removeAt(rng.nextInt(newStrokes.size))
            copy(strokes = newStrokes, error = Long.MAX_VALUE)
        }
        // 10% chance: Insert a new random stroke (accepted only if it lowers SSD by > α)
        roll < 0.20f && strokes.size < maxStrokes -> {
            val newStrokes = strokes.toMutableList()
            val insertIdx = rng.nextInt(newStrokes.size + 1)
            newStrokes.add(insertIdx, StrokeGene.random(width, height, rng))
            copy(strokes = newStrokes, error = Long.MAX_VALUE)
        }
        // 80% chance: Standard parameter/z-order mutation from Phase 1
        else -> mutate(width, height, rng)
    }
}
