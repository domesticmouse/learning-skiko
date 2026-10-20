import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    @Test
    fun testRenderAndCaptureScreenshot() {
        val simSize = 256
        val target = TargetImage.loadOrGenerate(null, simSize, simSize)
        val evaluator = Evaluator(simSize, simSize, target.pixelBytes)
        val painting = Painting.createRandom(40, simSize.toFloat(), simSize.toFloat())
        val evaluatedPainting = painting.copy(error = evaluator.evaluate(painting))

        val windowW = simSize * 2 + 60
        val windowH = simSize + 120
        val surface = Surface.makeRasterN32Premul(windowW, windowH)
        val delegate = EvaluatorRenderDelegate(target, evaluatedPainting, evaluator)

        delegate.onRender(surface.canvas, windowW, windowH, 1_000_000_000L)

        val image = surface.makeImageSnapshot()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        assertNotNull(data, "PNG data should not be null")
        assertTrue(data.bytes.isNotEmpty(), "PNG bytes must not be empty")

        saveScreenshot(data.bytes, "step-0003-offscreen-fitness-evaluation.png")
    }

    private fun saveScreenshot(
        bytes: ByteArray,
        fileName: String,
    ) {
        val localDir = File("build/reports/screenshots").apply { mkdirs() }
        File(localDir, fileName).writeBytes(bytes)

        val topLevelDir = File("../../artifacts/screenshots")
        if (topLevelDir.exists() || topLevelDir.mkdirs()) {
            File(topLevelDir, fileName).writeBytes(bytes)
        }
    }
}
