import kotlin.test.Test
import kotlin.test.assertEquals
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
}
