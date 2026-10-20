import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeneticPainterTest {
    private val width = 64
    private val height = 64

    @Test
    fun testInitializationAndChampionPresence() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val painter = GeneticPainter(target = target, initialStrokeCount = 30)

        assertEquals(width, painter.width)
        assertEquals(height, painter.height)
        assertEquals(30, painter.strokeCount)
        assertTrue(painter.initialError > 0L, "Initial error must be positive")

        val best = painter.getBestPainting()
        assertEquals(30, best.strokes.size)
        assertEquals(painter.initialError, best.error)
    }

    @Test
    fun testEvolutionMonotonicFitnessImprovement() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val painter = GeneticPainter(target = target, initialStrokeCount = 25)

        painter.start()
        val start = System.currentTimeMillis()
        while (painter.generations.get() < 100L && System.currentTimeMillis() - start < 3000L) {
            Thread.sleep(20)
        }
        painter.stop()

        assertTrue(painter.generations.get() >= 50L, "Should have executed at least 50 generations")
        val finalBest = painter.getBestPainting()
        assertTrue(
            finalBest.error <= painter.initialError,
            "Best error (${finalBest.error}) must be <= initial error (${painter.initialError})",
        )
    }

    @Test
    fun testPauseResumeLifecycle() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val painter = GeneticPainter(target = target, initialStrokeCount = 20)

        assertEquals(false, painter.paused.get())
        painter.togglePause()
        assertEquals(true, painter.paused.get())
        painter.togglePause()
        assertEquals(false, painter.paused.get())
    }

    @Test
    fun testAdjustStrokesBoundaryClamping() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val painter = GeneticPainter(target = target, initialStrokeCount = 50)

        painter.adjustStrokes(20)
        assertEquals(70, painter.strokeCount)
        assertEquals(70, painter.getBestPainting().strokes.size)

        painter.adjustStrokes(-1000) // Lower bound clamp at 10
        assertEquals(10, painter.strokeCount)
        assertEquals(10, painter.getBestPainting().strokes.size)

        painter.adjustStrokes(1000) // Upper bound clamp at 500
        assertEquals(500, painter.strokeCount)
        assertEquals(500, painter.getBestPainting().strokes.size)
    }

    @Test
    fun testReset() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val painter = GeneticPainter(target = target, initialStrokeCount = 20)

        painter.generations.set(500)
        painter.acceptedMutations.set(120)
        painter.reset()

        assertEquals(0L, painter.generations.get())
        assertEquals(0L, painter.acceptedMutations.get())
    }

    @Test
    fun testRenderAndCaptureScreenshot() {
        val simSize = 256
        val target = TargetImage.loadOrGenerate(null, simSize, simSize)
        val painter = GeneticPainter(target = target, initialStrokeCount = 60)

        // Evolve for a short run to populate telemetry and candidate strokes
        painter.start()
        val start = System.currentTimeMillis()
        while (painter.generations.get() < 250L && System.currentTimeMillis() - start < 1500L) {
            Thread.sleep(20)
        }
        painter.stop()

        val windowW = simSize * 2 + 60
        val windowH = simSize + 120
        val surface = Surface.makeRasterN32Premul(windowW, windowH)
        val delegate = HillClimberRenderDelegate(painter)

        delegate.onRender(surface.canvas, windowW, windowH, 1_000_000_000L)

        val image = surface.makeImageSnapshot()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        assertNotNull(data, "PNG data should not be null")
        assertTrue(data.bytes.isNotEmpty(), "PNG bytes must not be empty")

        saveScreenshot(data.bytes, "step-0004-evolutionary-hill-climber.png")
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
