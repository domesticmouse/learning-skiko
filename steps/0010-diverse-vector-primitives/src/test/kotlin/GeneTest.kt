import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
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
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeneTest {
    @Test
    fun testCubicCurveGeneRenderAndMutate() {
        val gene =
            CubicCurveGene(
                x0 = 10f,
                y0 = 10f,
                cx1 = 30f,
                cy1 = 80f,
                cx2 = 70f,
                cy2 = 20f,
                x3 = 90f,
                y3 = 90f,
                strokeWidth = 4f,
                argb = 0xFF123456.toInt(),
            )

        val surface = Surface.makeRaster(ImageInfo(100, 100, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
        val canvas = surface.canvas
        val paint = Paint()
        val path = Path()

        gene.render(canvas, paint, path)

        val rng = Random(42)
        val mutated = gene.mutate(100f, 100f, rng) as CubicCurveGene
        assertTrue(mutated.cx1 in 0f..100f)
        assertTrue(mutated.cy1 in 0f..100f)
        assertTrue(mutated.cx2 in 0f..100f)
        assertTrue(mutated.cy2 in 0f..100f)
        assertNotEquals(gene.cx1, mutated.cx1)
    }

    @Test
    fun testTaperedStrokeGeneRenderAndMutate() {
        val gene =
            TaperedStrokeGene(
                x0 = 10f,
                y0 = 10f,
                xc = 50f,
                yc = 90f,
                x1 = 90f,
                y1 = 10f,
                wStart = 2f,
                wPeak = 12f,
                wEnd = 3f,
                argb = 0xFF654321.toInt(),
            )

        val surface = Surface.makeRaster(ImageInfo(100, 100, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
        val canvas = surface.canvas
        val paint = Paint()
        val path = Path()

        gene.render(canvas, paint, path)

        val rng = Random(42)
        val mutated = gene.mutate(100f, 100f, rng) as TaperedStrokeGene
        assertTrue(mutated.wPeak in 4f..36f)
        assertNotEquals(gene.wPeak, mutated.wPeak)
    }

    @Test
    fun testPolygonGeneRenderAndMutate() {
        val gene =
            PolygonGene(
                points = listOf(10f to 10f, 90f to 20f, 80f to 80f, 20f to 90f),
                argb = 0xFFABCDEF.toInt(),
            )

        val surface = Surface.makeRaster(ImageInfo(100, 100, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
        val canvas = surface.canvas
        val paint = Paint()
        val path = Path()

        gene.render(canvas, paint, path)

        val rng = Random(42)
        val mutated = gene.mutate(100f, 100f, rng) as PolygonGene
        assertEquals(4, mutated.points.size)
        assertNotEquals(gene.points, mutated.points)
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

        canvas.drawString("Lesson 10: Diverse Vector Primitives (Polymorphic Chromosomes)", 32f, 40f, titleFont, textPaint)
        canvas.drawString(
            "Sealed hierarchy Gene: Cubic Béziers, Tapered Calligraphy Strokes, and N-vertex Polygons",
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

        val strokePaint = Paint()
        val strokePath = Path()

        // Card 1: Cubic Béziers
        canvas.drawRRect(RRect.makeXYWH(c1X, cardY, cardW, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c1X, cardY, cardW, cardH, 10f), borderPaint)
        canvas.drawString("CUBIC BÉZIER SPLINES", c1X + 16f, cardY + 28f, headerFont, accentPaint)

        val b1 = Bitmap().apply { allocN32Pixels(220, 220) }
        val cv1 = Canvas(b1)
        cv1.clear(Color.WHITE)
        val cubics =
            listOf(
                CubicCurveGene(20f, 180f, 60f, 40f, 160f, 200f, 200f, 40f, 12f, 0xAA82AAFF.toInt()),
                CubicCurveGene(20f, 40f, 80f, 190f, 140f, 30f, 200f, 180f, 8f, 0xAAF7768E.toInt()),
                CubicCurveGene(30f, 110f, 110f, 20f, 110f, 200f, 190f, 110f, 6f, 0xAA9ECE6A.toInt()),
            )
        cubics.forEach { it.render(cv1, strokePaint, strokePath) }
        val img1 = Image.makeFromBitmap(b1)
        canvas.drawImageRect(img1, Rect.makeXYWH(c1X + 15f, cardY + 45f, 220f, 220f))
        canvas.drawString("Dual control points (P1, P2)", c1X + 16f, cardY + 295f, smallFont, dimPaint)
        canvas.drawString("Inflection points & S-curves", c1X + 16f, cardY + 315f, smallFont, textPaint)

        // Card 2: Tapered Calligraphy Strokes
        canvas.drawRRect(RRect.makeXYWH(c2X, cardY, cardW, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c2X, cardY, cardW, cardH, 10f), borderPaint)
        canvas.drawString("TAPERED CALLIGRAPHY", c2X + 16f, cardY + 28f, headerFont, greenPaint)

        val b2 = Bitmap().apply { allocN32Pixels(220, 220) }
        val cv2 = Canvas(b2)
        cv2.clear(Color.WHITE)
        val tapered =
            listOf(
                TaperedStrokeGene(20f, 40f, 180f, 60f, 190f, 190f, 3f, 26f, 4f, 0xCC7AA2F7.toInt()),
                TaperedStrokeGene(30f, 190f, 60f, 30f, 180f, 100f, 2f, 20f, 2f, 0xCCE0AF68.toInt()),
                TaperedStrokeGene(190f, 30f, 100f, 180f, 40f, 170f, 4f, 18f, 1f, 0xCCBB9AF7.toInt()),
            )
        tapered.forEach { it.render(cv2, strokePaint, strokePath) }
        val img2 = Image.makeFromBitmap(b2)
        canvas.drawImageRect(img2, Rect.makeXYWH(c2X + 15f, cardY + 45f, 220f, 220f))
        canvas.drawString("Variable stroke width envelope", c2X + 16f, cardY + 295f, smallFont, dimPaint)
        canvas.drawString("wStart -> wPeak -> wEnd", c2X + 16f, cardY + 315f, smallFont, textPaint)

        // Card 3: N-Vertex Polygons
        canvas.drawRRect(RRect.makeXYWH(c3X, cardY, cardW, cardH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(c3X, cardY, cardW, cardH, 10f), borderPaint)
        canvas.drawString("CONVEX POLYGONS", c3X + 16f, cardY + 28f, headerFont, accentPaint)

        val b3 = Bitmap().apply { allocN32Pixels(220, 220) }
        val cv3 = Canvas(b3)
        cv3.clear(Color.WHITE)
        val polygons =
            listOf(
                PolygonGene(listOf(20f to 50f, 110f to 20f, 190f to 80f, 150f to 160f, 40f to 140f), 0x99FF9E64.toInt()),
                PolygonGene(listOf(80f to 70f, 180f to 100f, 160f to 190f, 60f to 180f), 0x997DCFFF.toInt()),
                PolygonGene(listOf(30f to 90f, 100f to 60f, 130f to 130f), 0x999ECE6A.toInt()),
            )
        polygons.forEach { it.render(cv3, strokePaint, strokePath) }
        val img3 = Image.makeFromBitmap(b3)
        canvas.drawImageRect(img3, Rect.makeXYWH(c3X + 15f, cardY + 45f, 220f, 220f))
        canvas.drawString("Planar silhouette approximation", c3X + 16f, cardY + 295f, smallFont, dimPaint)
        canvas.drawString("Sorted polar vertices (theta_i)", c3X + 16f, cardY + 315f, smallFont, textPaint)

        // Footer Card
        val hudY = 445f
        val hudW = w - 64f
        val hudH = 120f
        canvas.drawRRect(RRect.makeXYWH(32f, hudY, hudW, hudH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(32f, hudY, hudW, hudH, 10f), borderPaint)
        canvas.drawString("POLYMORPHIC CHROMOSOME ARCHITECTURE", 50f, hudY + 30f, headerFont, accentPaint)
        canvas.drawString(
            "1. Sealed interface Gene encapsulates rendering and mutation without runtime reflection or allocations.",
            50f,
            hudY + 55f,
            smallFont,
            textPaint,
        )
        canvas.drawString(
            "2. Pre-allocated Skia Paint and Path native buffers are passed into Gene.render(), maintaining 0 B/op throughput.",
            50f,
            hudY + 75f,
            smallFont,
            textPaint,
        )
        canvas.drawString(
            "3. Diverse primitives allow the genetic algorithm to represent sharp corners, gradients, and calligraphy in one painting.",
            50f,
            hudY + 98f,
            smallFont,
            dimPaint,
        )

        val image = surface.makeImageSnapshot()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        assertNotNull(data, "PNG data should not be null")
        assertTrue(data.bytes.isNotEmpty(), "PNG bytes must not be empty")

        saveScreenshot(data.bytes, "step-0010-diverse-vector-primitives.png")
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
