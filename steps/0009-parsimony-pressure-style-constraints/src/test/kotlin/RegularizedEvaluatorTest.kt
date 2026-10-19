import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegularizedEvaluatorTest {
    private val width = 64
    private val height = 64

    @Test
    fun testHardStrokeBudgetEnforcement() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val baseEvaluator = Evaluator(width, height, target.pixelBytes)
        val regEvaluator = RegularizedEvaluator(baseEvaluator, maxStrokeBudget = 15)

        val withinBudget = Painting.createRandom(15, width.toFloat(), height.toFloat())
        assertTrue(regEvaluator.evaluate(withinBudget) < Double.MAX_VALUE, "Within budget should evaluate normally")

        val overBudget = Painting.createRandom(16, width.toFloat(), height.toFloat())
        assertEquals(Double.MAX_VALUE, regEvaluator.evaluate(overBudget), "Over budget must be disqualified")
    }

    @Test
    fun testParsimonyPenaltyCalculation() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val baseEvaluator = Evaluator(width, height, target.pixelBytes)
        val alpha = 10_000.0
        val regEvaluator = RegularizedEvaluator(baseEvaluator, parsimonyAlpha = alpha, maxStrokeBudget = 50)

        val painting = Painting.createRandom(20, width.toFloat(), height.toFloat())
        val baseScore = baseEvaluator.evaluate(painting).toDouble()
        val regScore = regEvaluator.evaluate(painting)

        val expected = baseScore + 20 * alpha
        assertEquals(expected, regScore, 0.001, "Regularized score must add alpha * N_strokes")
    }

    @Test
    fun testMutateVariableLengthCanAddAndRemoveStrokes() {
        val initial = Painting.createRandom(20, width.toFloat(), height.toFloat())
        val rng = Random(42)

        var strokeCountDecreased = false
        var strokeCountIncreased = false

        for (i in 0 until 200) {
            val mutated = initial.mutateVariableLength(width.toFloat(), height.toFloat(), maxStrokes = 40, rng = rng)
            if (mutated.strokes.size < initial.strokes.size) strokeCountDecreased = true
            if (mutated.strokes.size > initial.strokes.size) strokeCountIncreased = true
        }

        assertTrue(strokeCountDecreased, "Variable length mutation must support stroke deletion")
        assertTrue(strokeCountIncreased, "Variable length mutation must support stroke insertion")
    }
}
