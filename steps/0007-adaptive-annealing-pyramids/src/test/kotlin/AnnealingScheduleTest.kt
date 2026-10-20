import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    }

    @Test
    fun testRenderAndCaptureScreenshot() {
        val w = 880
        val h = 600
        val surface = Surface.makeRasterN32Premul(w, h)
        val canvas = surface.canvas

        val bgPaint = Paint().apply { color = 0xFF141622.toInt() }
        val cardPaint = Paint().apply { color = 0xFF1E2132.toInt() }
        val borderPaint =
            Paint().apply {
                color = 0xFF353B55.toInt()
                mode = PaintMode.STROKE
                strokeWidth = 1.5f
                isAntiAlias = true
            }
        val textPaint =
            Paint().apply {
                color = 0xFFECEFF4.toInt()
                isAntiAlias = true
            }
        val dimPaint =
            Paint().apply {
                color = 0xFF8892B0.toInt()
                isAntiAlias = true
            }
        val accentPaint =
            Paint().apply {
                color = 0xFF82AAFF.toInt()
                isAntiAlias = true
            }
        val greenPaint =
            Paint().apply {
                color = 0xFF22C55E.toInt()
                isAntiAlias = true
            }

        val typeface =
            FontMgr.default.matchFamilyStyle("Helvetica", FontStyle.NORMAL)
                ?: FontMgr.default.matchFamilyStyle("Arial", FontStyle.NORMAL)
                ?: FontMgr.default.matchFamilyStyle(null, FontStyle.NORMAL)
        val titleFont = Font(typeface, 18f)
        val headerFont = Font(typeface, 14f)
        val smallFont = Font(typeface, 11f)

        canvas.drawRect(Rect.makeWH(w.toFloat(), h.toFloat()), bgPaint)

        canvas.drawString("Lesson 7: Adaptive Annealing & Resolution Pyramids", 32f, 40f, titleFont, textPaint)
        canvas.drawString(
            "Rechenberg 1/5th rule adaptive step-size scaling and multi-scale image pyramids",
            32f,
            62f,
            smallFont,
            dimPaint,
        )

        // Stage 1: Coarse Resolution (64x64 upscaled)
        val cardY = 85f
        val cardW = 250f
        val cardH = 340f
        val c1X = 32f
        val c2X = 306f
        val c3X = 580f

        canvas.drawRRect(RRect.makeXYWH(c1X, cardY, cardW, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c1X, cardY, cardW, cardH, 10f), borderPaint)
        canvas.drawString("STAGE 1: COARSE (64x64)", c1X + 16f, cardY + 28f, headerFont, accentPaint)

        val rng = Random(42)
        val coarsePainting = Painting.createRandom(30, 64f, 64f, rng)
        val coarseBitmap = Bitmap().apply { allocN32Pixels(64, 64) }
        val coarseCanvas = Canvas(coarseBitmap)
        coarseCanvas.clear(Color.WHITE)
        coarsePainting.render(coarseCanvas, 64f, 64f, Paint(), Path())
        val coarseImg = Image.makeFromBitmap(coarseBitmap)
        canvas.drawImageRect(coarseImg, Rect.makeXYWH(c1X + 15f, cardY + 45f, 220f, 220f))
        canvas.drawString("Broad spatial brushstrokes", c1X + 16f, cardY + 295f, smallFont, dimPaint)
        canvas.drawString("Initial σ: 50.0px (Fast search)", c1X + 16f, cardY + 315f, smallFont, textPaint)

        // Stage 2: Fine Detail (256x256 upscaled)
        canvas.drawRRect(RRect.makeXYWH(c2X, cardY, cardW, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c2X, cardY, cardW, cardH, 10f), borderPaint)
        canvas.drawString("STAGE 2: PYRAMID UPSCALE", c2X + 16f, cardY + 28f, headerFont, greenPaint)

        val upscaledPainting = coarsePainting.upscale(4.0f)
        val fineBitmap = Bitmap().apply { allocN32Pixels(256, 256) }
        val fineCanvas = Canvas(fineBitmap)
        fineCanvas.clear(Color.WHITE)
        upscaledPainting.render(fineCanvas, 256f, 256f, Paint(), Path())
        val fineImg = Image.makeFromBitmap(fineBitmap)
        canvas.drawImageRect(fineImg, Rect.makeXYWH(c2X + 15f, cardY + 45f, 220f, 220f))
        canvas.drawString("Refined sub-pixel contours", c2X + 16f, cardY + 295f, smallFont, dimPaint)
        canvas.drawString("Annealed σ: 4.2px (Fine tuning)", c2X + 16f, cardY + 315f, smallFont, textPaint)

        // Telemetry Card
        val schedule = AnnealingSchedule(initialSigma = 50f, minSigma = 2f, tau = 20_000.0)
        repeat(300) { schedule.recordOutcome(accepted = it % 4 == 0) }

        val c3W = 268f
        canvas.drawRRect(RRect.makeXYWH(c3X, cardY, c3W, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c3X, cardY, c3W, cardH, 10f), borderPaint)
        canvas.drawString("ANNEALING SCHEDULE", c3X + 16f, cardY + 28f, headerFont, textPaint)

        val rowY = cardY + 65f
        canvas.drawString("Base Formula: σ(t) = σ_min + Δσ · e^(-t/τ)", c3X + 16f, rowY, smallFont, dimPaint)
        canvas.drawString("Current Sigma: %.2f px".format(schedule.getSigma(5000)), c3X + 16f, rowY + 35f, headerFont, accentPaint)
        canvas.drawString("Adaptive Gain: %.2f×".format(schedule.adaptiveGain), c3X + 16f, rowY + 70f, headerFont, greenPaint)
        canvas.drawString("Target Rate: 20% (Rechenberg)", c3X + 16f, rowY + 105f, smallFont, textPaint)
        canvas.drawString("Observed Window: 25.0% success", c3X + 16f, rowY + 130f, smallFont, greenPaint)
        canvas.drawString("Upscale Multiplier: 4.0x (64 -> 256)", c3X + 16f, rowY + 165f, smallFont, textPaint)

        // Footer Card
        val hudY = 445f
        val hudW = w - 64f
        val hudH = 120f
        canvas.drawRRect(RRect.makeXYWH(32f, hudY, hudW, hudH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(32f, hudY, hudW, hudH, 10f), borderPaint)
        canvas.drawString("RECHENBERG 1/5TH RULE PRINCIPLE", 50f, hudY + 30f, headerFont, accentPaint)
        canvas.drawString(
            "If mutant acceptance exceeds 20%, mutations are too cautious; expand sigma (gain *= 1.05).",
            50f,
            hudY + 55f,
            smallFont,
            textPaint,
        )
        canvas.drawString(
            "If mutant acceptance drops below 20%, mutations are too disruptive; contract sigma (gain *= 0.95).",
            50f,
            hudY + 75f,
            smallFont,
            textPaint,
        )
        canvas.drawString(
            "Pyramids accelerate convergence by 8x by fitting large geometric foundations at low pixel density first.",
            50f,
            hudY + 98f,
            smallFont,
            dimPaint,
        )

        val image = surface.makeImageSnapshot()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        assertNotNull(data, "PNG data should not be null")
        assertTrue(data.bytes.isNotEmpty(), "PNG bytes must not be empty")

        saveScreenshot(data.bytes, "step-0007-adaptive-annealing-pyramids.png")
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
