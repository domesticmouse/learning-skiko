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

    @Test
    fun testRenderAndCaptureScreenshot() {
        val simSize = 256
        val target = TargetImage.loadOrGenerate(null, simSize, simSize)
        val baseEvaluator = Evaluator(simSize, simSize, target.pixelBytes)
        val alpha = 50_000.0
        val regEvaluator = RegularizedEvaluator(baseEvaluator, parsimonyAlpha = alpha, maxStrokeBudget = 40)

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
        val warningPaint =
            Paint().apply {
                color = 0xFFF59E0B.toInt()
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

        canvas.drawString("Lesson 9: Parsimony Pressure & Structural Mutation", 32f, 40f, titleFont, textPaint)
        canvas.drawString(
            "Balancing visual fidelity against stroke budget via Pareto loss: L = SSD + α · N_strokes",
            32f,
            62f,
            smallFont,
            dimPaint,
        )

        val cardY = 85f
        val cardW = 250f
        val cardH = 340f
        val c1X = 32f
        val c2X = 306f
        val c3X = 580f

        val rng = Random(1234)

        // Card 1: Dense / Unregularized (120 strokes)
        canvas.drawRRect(RRect.makeXYWH(c1X, cardY, cardW, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c1X, cardY, cardW, cardH, 10f), borderPaint)
        canvas.drawString("UNREGULARIZED (120 STROKES)", c1X + 16f, cardY + 28f, headerFont, warningPaint)

        val densePainting = Painting.createRandom(120, simSize.toFloat(), simSize.toFloat(), rng)
        val denseBitmap = Bitmap().apply { allocN32Pixels(simSize, simSize) }
        val denseCanvas = Canvas(denseBitmap)
        denseCanvas.clear(Color.WHITE)
        densePainting.render(denseCanvas, simSize.toFloat(), simSize.toFloat(), Paint(), Path())
        val denseImg = Image.makeFromBitmap(denseBitmap)
        canvas.drawImageRect(denseImg, Rect.makeXYWH(c1X + 15f, cardY + 45f, 220f, 220f))
        canvas.drawString("Over-segmented visual clutter", c1X + 16f, cardY + 295f, smallFont, dimPaint)
        val denseErr = baseEvaluator.evaluate(densePainting)
        canvas.drawString("Raw SSD: %,d".format(denseErr), c1X + 16f, cardY + 315f, smallFont, textPaint)

        // Card 2: Parsimonious Regularized (28 strokes)
        canvas.drawRRect(RRect.makeXYWH(c2X, cardY, cardW, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c2X, cardY, cardW, cardH, 10f), borderPaint)
        canvas.drawString("PARSIMONIOUS (28 STROKES)", c2X + 16f, cardY + 28f, headerFont, greenPaint)

        val parsimoniousPainting = Painting.createRandom(28, simSize.toFloat(), simSize.toFloat(), rng)
        val parsimoniousBitmap = Bitmap().apply { allocN32Pixels(simSize, simSize) }
        val parsimoniousCanvas = Canvas(parsimoniousBitmap)
        parsimoniousCanvas.clear(Color.WHITE)
        parsimoniousPainting.render(parsimoniousCanvas, simSize.toFloat(), simSize.toFloat(), Paint(), Path())
        val parsimoniousImg = Image.makeFromBitmap(parsimoniousBitmap)
        canvas.drawImageRect(parsimoniousImg, Rect.makeXYWH(c2X + 15f, cardY + 45f, 220f, 220f))
        canvas.drawString("Crisp, minimal essential geometry", c2X + 16f, cardY + 295f, smallFont, dimPaint)
        val parsimoniousErr = baseEvaluator.evaluate(parsimoniousPainting)
        val parsimoniousTotal = regEvaluator.evaluate(parsimoniousPainting)
        canvas.drawString("SSD + Penalty: %,.0f".format(parsimoniousTotal), c2X + 16f, cardY + 315f, smallFont, greenPaint)

        // Card 3: Metrics & Cost Analysis
        val c3W = 268f
        canvas.drawRRect(RRect.makeXYWH(c3X, cardY, c3W, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c3X, cardY, c3W, cardH, 10f), borderPaint)
        canvas.drawString("REGULARIZATION METRICS", c3X + 16f, cardY + 28f, headerFont, textPaint)

        val rowY = cardY + 65f
        canvas.drawString("Complexity Weight (α): 50,000", c3X + 16f, rowY, smallFont, dimPaint)
        canvas.drawString("Stroke Hard Limit: 40 strokes", c3X + 16f, rowY + 35f, headerFont, accentPaint)
        canvas.drawString("Dense Penalty: +6,000,000", c3X + 16f, rowY + 70f, headerFont, warningPaint)
        canvas.drawString("Parsimonious: +1,400,000", c3X + 16f, rowY + 105f, headerFont, greenPaint)
        canvas.drawString("Occam's Razor Gain: 76.7% fewer", c3X + 16f, rowY + 140f, smallFont, greenPaint)
        canvas.drawString("Dynamic Mutation: Add & Prune", c3X + 16f, rowY + 175f, smallFont, textPaint)

        // Footer Card
        val hudY = 445f
        val hudW = w - 64f
        val hudH = 120f
        canvas.drawRRect(RRect.makeXYWH(32f, hudY, hudW, hudH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(32f, hudY, hudW, hudH, 10f), borderPaint)
        canvas.drawString("VARIABLE-LENGTH CHROMOSOMES & PRUNING OPERATORS", 50f, hudY + 30f, headerFont, accentPaint)
        canvas.drawString(
            "1. Chromosomes can dynamically insert a stroke (exploration) or delete a redundant stroke (pruning).",
            50f,
            hudY + 55f,
            smallFont,
            textPaint,
        )
        canvas.drawString(
            "2. Parsimony penalty alpha penalizes each added stroke, forcing the engine to prefer simplicity.",
            50f,
            hudY + 75f,
            smallFont,
            textPaint,
        )
        canvas.drawString(
            "3. Hard stroke budgets reject any mutant exceeding the threshold, guaranteeing vector file size constraints.",
            50f,
            hudY + 98f,
            smallFont,
            dimPaint,
        )

        val image = surface.makeImageSnapshot()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        assertNotNull(data, "PNG data should not be null")
        assertTrue(data.bytes.isNotEmpty(), "PNG bytes must not be empty")

        saveScreenshot(data.bytes, "step-0009-parsimony-pressure-style-constraints.png")
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
