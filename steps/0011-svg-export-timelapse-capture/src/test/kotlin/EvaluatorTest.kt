import org.jetbrains.skia.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvaluatorTest {
    private val width = 64
    private val height = 64

    @Test
    fun testTargetImageProceduralGeneration() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        assertEquals(width, target.width)
        assertEquals(height, target.height)
        assertEquals(width * height * 4, target.pixelBytes.size)
        assertTrue(target.sourceDescription.contains("Procedural"), "Fallback should have procedural description")
    }

    @Test
    fun testEvaluatorZeroErrorOnIdenticalWhite() {
        // Create an all-white target image
        val whiteBytes = ByteArray(width * height * 4) { -1 } // 0xFF in signed byte
        val evaluator = Evaluator(width, height, whiteBytes, backgroundColor = Color.WHITE)

        val emptyPainting = Painting(strokes = emptyList())
        val error = evaluator.evaluate(emptyPainting)
        assertEquals(0L, error, "An empty white painting against an all-white target must yield error 0")
    }

    @Test
    fun testEvaluatorMaxErrorOnInvertedBlackAndWhite() {
        // Create an all-black target image (BGRA: B=0, G=0, R=0, A=255)
        val blackBytes = ByteArray(width * height * 4)
        for (i in 0 until (width * height)) {
            blackBytes[i * 4 + 3] = -1 // Alpha = 255
        }

        // Background is white: R=255, G=255, B=255
        val evaluator = Evaluator(width, height, blackBytes, backgroundColor = Color.WHITE)
        val emptyPainting = Painting(strokes = emptyList())
        val error = evaluator.evaluate(emptyPainting)

        // Expected error per pixel: 255^2 + 255^2 + 255^2 = 3 * 65025 = 195,075
        val expected = (width * height).toLong() * 3L * (255L * 255L)
        assertEquals(expected, error, "Error against opposite color must match exact SSD formula")
    }

    @Test
    fun testEvaluatorNonNegativeError() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val evaluator = Evaluator(width, height, target.pixelBytes)
        val painting = Painting.createRandom(20, width.toFloat(), height.toFloat())
        val error = evaluator.evaluate(painting)
        assertTrue(error >= 0L, "Fitness error must be non-negative")
    }
}
