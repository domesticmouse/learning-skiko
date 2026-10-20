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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IslandCoordinatorTest {
    private val width = 64
    private val height = 64

    @Test
    fun testIslandInitialization() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val coordinator = IslandCoordinator(numIslands = 4, target = target, strokeCount = 20)

        assertEquals(4, coordinator.numIslands)
        assertEquals(20, coordinator.strokeCount)
        assertEquals(0L, coordinator.totalGenerations.get())
    }

    @Test
    fun testConcurrentEvolutionAndGlobalChampion() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val coordinator =
            IslandCoordinator(
                numIslands = 3,
                target = target,
                strokeCount = 20,
                migrationInterval = 20L,
            )

        coordinator.start()
        val start = System.currentTimeMillis()
        while (coordinator.totalGenerations.get() < 150L && System.currentTimeMillis() - start < 3000L) {
            Thread.sleep(20)
        }
        coordinator.stop()

        val champion = coordinator.globalChampion.get()
        assertNotNull(champion, "Global champion must be present after concurrent execution")
        assertTrue(coordinator.totalGenerations.get() >= 100L, "Should have executed at least 100 total generations")
        assertTrue(coordinator.acceptedMutations.get() > 0L, "Should have accepted mutations")
        assertTrue(champion.error < Long.MAX_VALUE, "Champion error must be a valid evaluated value")
    }

    @Test
    fun testCleanStopLifecycle() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val coordinator = IslandCoordinator(numIslands = 2, target = target, strokeCount = 10)

        coordinator.start()
        assertTrue(coordinator.isRunning.get())
        Thread.sleep(50)
        coordinator.stop()
        assertEquals(false, coordinator.isRunning.get())
    }

    @Test
    fun testRenderAndCaptureScreenshot() {
        val target = TargetImage.loadOrGenerate(null, 256, 256)
        val coordinator =
            IslandCoordinator(
                numIslands = 4,
                target = target,
                strokeCount = 50,
                migrationInterval = 30L,
            )

        coordinator.start()
        val start = System.currentTimeMillis()
        while (coordinator.totalGenerations.get() < 300L && System.currentTimeMillis() - start < 2000L) {
            Thread.sleep(20)
        }
        coordinator.stop()

        val w = 880
        val h = 640
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

        // Header
        canvas.drawString("Lesson 6: Multi-Core Island Model (Coarse-Grained Demes)", 32f, 40f, titleFont, textPaint)
        canvas.drawString(
            "Ring migration topology with lock-free atomic buffers across 4 concurrent CPU cores",
            32f,
            62f,
            smallFont,
            dimPaint,
        )

        // Draw 4 island cards in 2x2 grid
        val islandW = 190f
        val islandH = 220f
        val startX = 32f
        val startY = 85f
        val gapX = 24f
        val gapY = 24f

        val strokePaint = Paint()
        val strokePath = Path()
        val islandBitmap = Bitmap().apply { allocN32Pixels(target.width, target.height) }
        val islandCanvas = Canvas(islandBitmap)

        for (i in 0 until 4) {
            val col = i % 2
            val row = i / 2
            val cx = startX + col * (islandW + gapX)
            val cy = startY + row * (islandH + gapY)

            canvas.drawRRect(RRect.makeXYWH(cx, cy, islandW, islandH, 10f), cardPaint)
            canvas.drawRRect(RRect.makeXYWH(cx, cy, islandW, islandH, 10f), borderPaint)

            val islandChamp = coordinator.getIslandChampion(i) ?: coordinator.globalChampion.get()
            canvas.drawString("ISLAND #$i (Core ${i + 1})", cx + 12f, cy + 24f, headerFont, accentPaint)

            if (islandChamp != null) {
                islandCanvas.clear(Color.WHITE)
                islandChamp.render(islandCanvas, target.width.toFloat(), target.height.toFloat(), strokePaint, strokePath)
                val img = Image.makeFromBitmap(islandBitmap)
                canvas.drawImageRect(img, Rect.makeXYWH(cx + 15f, cy + 34f, 160f, 150f))

                canvas.drawString("Error: %,d".format(islandChamp.error), cx + 12f, cy + 204f, smallFont, greenPaint)
            }
        }

        // Global champion card on the right
        val rightX = startX + 2 * (islandW + gapX) + 10f
        val rightW = 380f
        val rightH = islandH * 2 + gapY

        canvas.drawRRect(RRect.makeXYWH(rightX, startY, rightW, rightH, 10f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(rightX, startY, rightW, rightH, 10f), borderPaint)
        canvas.drawString("GLOBAL CHAMPION & TELEMETRY", rightX + 16f, startY + 28f, headerFont, greenPaint)

        val globalChamp = coordinator.globalChampion.get()
        if (globalChamp != null) {
            islandCanvas.clear(Color.WHITE)
            globalChamp.render(islandCanvas, target.width.toFloat(), target.height.toFloat(), strokePaint, strokePath)
            val globalImg = Image.makeFromBitmap(islandBitmap)
            canvas.drawImageRect(globalImg, Rect.makeXYWH(rightX + 65f, startY + 45f, 250f, 250f))

            val statsY = startY + 325f
            canvas.drawString(
                "Total Generations: %,d".format(coordinator.totalGenerations.get()),
                rightX + 24f,
                statsY,
                headerFont,
                textPaint,
            )
            canvas.drawString(
                "Accepted Mutations: %,d".format(coordinator.acceptedMutations.get()),
                rightX + 24f,
                statsY + 25f,
                headerFont,
                textPaint,
            )
            canvas.drawString("Fittest Error: %,d".format(globalChamp.error), rightX + 24f, statsY + 50f, headerFont, accentPaint)
            canvas.drawString("Topology: Ring Migration (Every 30 gens)", rightX + 24f, statsY + 75f, headerFont, dimPaint)
        }

        val image = surface.makeImageSnapshot()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        assertNotNull(data, "PNG data should not be null")
        assertTrue(data.bytes.isNotEmpty(), "PNG bytes must not be empty")

        saveScreenshot(data.bytes, "step-0006-multicore-island-model.png")
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
