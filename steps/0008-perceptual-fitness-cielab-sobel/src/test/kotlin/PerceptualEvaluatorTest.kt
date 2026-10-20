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

    @Test
    fun testRenderAndCaptureScreenshot() {
        val simSize = 256
        val target = TargetImage.loadOrGenerate(null, simSize, simSize)
        val evaluator = PerceptualEvaluator(target = target, edgeEmphasis = 4.0f)

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

        canvas.drawString("Lesson 8: Perceptual Fitness (CIELAB & Sobel Edge Saliency)", 32f, 40f, titleFont, textPaint)
        canvas.drawString(
            "Human visual system color distance (ΔE) paired with 3x3 gradient edge emphasis convolution",
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

        // Card 1: Target Image
        canvas.drawRRect(RRect.makeXYWH(c1X, cardY, cardW, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c1X, cardY, cardW, cardH, 10f), borderPaint)
        canvas.drawString("TARGET IMAGE", c1X + 16f, cardY + 28f, headerFont, accentPaint)
        canvas.drawImageRect(target.image, Rect.makeXYWH(c1X + 15f, cardY + 45f, 220f, 220f))
        canvas.drawString("Ground Truth Reference", c1X + 16f, cardY + 295f, smallFont, dimPaint)
        canvas.drawString("Format: BGRA_8888 (256x256)", c1X + 16f, cardY + 315f, smallFont, textPaint)

        // Card 2: Sobel Edge Map
        canvas.drawRRect(RRect.makeXYWH(c2X, cardY, cardW, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c2X, cardY, cardW, cardH, 10f), borderPaint)
        canvas.drawString("SOBEL SALIENCY MASK", c2X + 16f, cardY + 28f, headerFont, greenPaint)

        // Generate Sobel grayscale bitmap
        val sobelBitmap = Bitmap().apply { allocN32Pixels(simSize, simSize) }
        val sobelBytes = ByteArray(simSize * simSize * 4)
        for (i in 0 until (simSize * simSize)) {
            val weight = evaluator.edgeWeights[i]
            val intensity = (((weight - 1.0f) / 4.0f).coerceIn(0f, 1f) * 255f).toInt()
            val base = i * 4
            sobelBytes[base] = intensity.toByte() // B
            sobelBytes[base + 1] = intensity.toByte() // G
            sobelBytes[base + 2] = intensity.toByte() // R
            sobelBytes[base + 3] = 255.toByte() // A
        }
        sobelBitmap.installPixels(sobelBytes)
        val sobelImg = Image.makeFromBitmap(sobelBitmap)
        canvas.drawImageRect(sobelImg, Rect.makeXYWH(c2X + 15f, cardY + 45f, 220f, 220f))
        canvas.drawString("3x3 Gradient Convolution", c2X + 16f, cardY + 295f, smallFont, dimPaint)
        canvas.drawString("Highlights Horizon & Silhouette", c2X + 16f, cardY + 315f, smallFont, textPaint)

        // Card 3: Perceptual Candidate Painting
        canvas.drawRRect(RRect.makeXYWH(c3X, cardY, cardW, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c3X, cardY, cardW, cardH, 10f), borderPaint)
        canvas.drawString("EVOLVED CANDIDATE", c3X + 16f, cardY + 28f, headerFont, accentPaint)

        val rng = Random(123)
        val painting = Painting.createRandom(45, simSize.toFloat(), simSize.toFloat(), rng)
        val candBitmap = Bitmap().apply { allocN32Pixels(simSize, simSize) }
        val candCanvas = Canvas(candBitmap)
        candCanvas.clear(Color.WHITE)
        painting.render(candCanvas, simSize.toFloat(), simSize.toFloat(), Paint(), Path())
        val candImg = Image.makeFromBitmap(candBitmap)
        canvas.drawImageRect(candImg, Rect.makeXYWH(c3X + 15f, cardY + 45f, 220f, 220f))

        val candBytes = candBitmap.readPixels(candBitmap.imageInfo) ?: ByteArray(0)
        val deltaE = evaluator.evaluatePerceptual(candBytes)
        canvas.drawString("Perceptual Error: %.1f".format(deltaE), c3X + 16f, cardY + 295f, smallFont, greenPaint)
        canvas.drawString("CIELAB Uniform Color Space", c3X + 16f, cardY + 315f, smallFont, textPaint)

        // Footer Card
        val hudY = 445f
        val hudW = w - 64f
        val hudH = 120f
        canvas.drawRRect(RRect.makeXYWH(32f, hudY, hudW, hudH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(32f, hudY, hudW, hudH, 10f), borderPaint)
        canvas.drawString("WHY PERCEPTUAL FITNESS MATTERS OVER L2 SSD", 50f, hudY + 30f, headerFont, accentPaint)
        canvas.drawString(
            "1. CIELAB L*a*b* balances luminance against color opponent channels, matching human eye sensitivity.",
            50f,
            hudY + 55f,
            smallFont,
            textPaint,
        )
        canvas.drawString(
            "2. Sobel edge weights boost the penalty for blurred silhouettes, keeping mountain ridges and sun crisp.",
            50f,
            hudY + 75f,
            smallFont,
            textPaint,
        )
        canvas.drawString(
            "3. Fast 32x32x32 LUT quantization eliminates expensive sRGB->XYZ->Lab math in the hot evaluation loop.",
            50f,
            hudY + 98f,
            smallFont,
            dimPaint,
        )

        val image = surface.makeImageSnapshot()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        assertNotNull(data, "PNG data should not be null")
        assertTrue(data.bytes.isNotEmpty(), "PNG bytes must not be empty")

        saveScreenshot(data.bytes, "step-0008-perceptual-fitness-cielab-sobel.png")
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
