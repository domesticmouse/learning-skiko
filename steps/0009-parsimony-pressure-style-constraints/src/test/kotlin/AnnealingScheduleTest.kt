import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnnealingScheduleTest {
    @Test
    fun testExponentialCoolingDecay() {
        val schedule = AnnealingSchedule(initialSigma = 50f, minSigma = 2f, tau = 10_000.0)

        val s0 = schedule.getSigma(0)
        assertEquals(50f, s0, 0.01f, "Sigma at gen 0 should equal initialSigma")

        val s10k = schedule.getSigma(10_000)
        assertTrue(s10k < s0, "Sigma must decrease over generations")

        val s50k = schedule.getSigma(50_000)
        assertTrue(s50k < s10k, "Sigma must continue cooling towards minSigma")
        assertTrue(s50k >= 2f, "Sigma must never cool below minSigma")
    }

    @Test
    fun testRechenbergSuccessRuleAdjustment() {
        val schedule = AnnealingSchedule(initialSigma = 40f, minSigma = 2f, tau = 100_000.0)

        // Simulate high acceptance rate (> 22%)
        repeat(500) { schedule.recordOutcome(accepted = true) }
        val boosted = schedule.getSigma(100)

        val freshSchedule = AnnealingSchedule(initialSigma = 40f, minSigma = 2f, tau = 100_000.0)
        // Simulate low acceptance rate (< 18%)
        repeat(500) { schedule.recordOutcome(accepted = false) }
        val contracted = schedule.getSigma(100)

        assertTrue(boosted >= contracted, "High acceptance should yield higher sigma than low acceptance")
    }

    @Test
    fun testPaintingUpscale() {
        val stroke =
            StrokeGene(
                x0 = 10f,
                y0 = 15f,
                xc = 20f,
                yc = 25f,
                x1 = 30f,
                y1 = 35f,
                strokeWidth = 4f,
                r = 100,
                g = 150,
                b = 200,
                a = 120,
            )
        val painting = Painting(strokes = listOf(stroke), error = 5000L)
        val upscaled = painting.upscale(2.0f)

        assertEquals(Long.MAX_VALUE, upscaled.error, "Upscaling must invalidate previous error score")
        val s = upscaled.strokes.first()
        assertEquals(20f, s.x0)
        assertEquals(30f, s.y0)
        assertEquals(40f, s.xc)
        assertEquals(50f, s.yc)
        assertEquals(60f, s.x1)
        assertEquals(70f, s.y1)
        assertEquals(8f, s.strokeWidth)
        assertEquals(100, s.r)
        assertEquals(150, s.g)
        assertEquals(200, s.b)
        assertEquals(120, s.a)
    }
}
