import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PerceptualEvaluatorTest {
    private val width = 32
    private val height = 32

    @Test
    fun testSobelWeightComputationOnSyntheticEdge() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val evaluator = PerceptualEvaluator(target = target, edgeEmphasis = 4.0f)

        assertEquals(width * height, evaluator.edgeWeights.size)
        // Verify edge weights are always at least 1.0f
        for (w in evaluator.edgeWeights) {
            assertTrue(w >= 1.0f, "Edge weight must be >= 1.0")
        }

        // On the procedural target with mountains and sun, some edges should exceed 1.5f
        val maxWeight = evaluator.edgeWeights.maxOrNull() ?: 1.0f
        assertTrue(maxWeight > 1.5f, "Sobel detector should highlight image silhouettes")
    }

    @Test
    fun testLabLookupTableQuantizationBounds() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val evaluator = PerceptualEvaluator(target = target)

        assertEquals(32 * 32 * 32 * 3, evaluator.labLut.size)

        // Index for black (0, 0, 0)
        val blackIdx = evaluator.lutBaseIndex(0, 0, 0)
        val lBlack = evaluator.labLut[blackIdx]
        assertTrue(lBlack in 0f..5f, "Black L* should be near 0")

        // Index for white (255, 255, 255)
        val whiteIdx = evaluator.lutBaseIndex(255, 255, 255)
        val lWhite = evaluator.labLut[whiteIdx]
        assertTrue(lWhite in 95f..105f, "White L* should be near 100")
    }

    @Test
    fun testPerceptualEvaluationIdenticalYieldsZero() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val evaluator = PerceptualEvaluator(target = target)

        val zeroDiff = evaluator.evaluatePerceptual(target.pixelBytes)
        assertEquals(0.0, zeroDiff, 0.001, "Evaluating target against itself must yield deltaE = 0")
    }
}
