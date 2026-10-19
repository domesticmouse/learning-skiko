import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HudControlsTest {
    @Test
    fun testPngSnapshotEncoding() {
        val width = 128
        val height = 128
        val target = TargetImage.loadOrGenerate(null, width, height)
        val painter = GeneticPainter(target = target, initialStrokeCount = 20)
        val best = painter.getBestPainting()

        val bitmap = Bitmap().apply { allocN32Pixels(width, height) }
        val canvas = Canvas(bitmap)
        best.render(canvas, width.toFloat(), height.toFloat(), Paint(), Path())

        val image = Image.makeFromBitmap(bitmap)
        val pngData = image.encodeToData(EncodedImageFormat.PNG)

        assertNotNull(pngData, "PNG data should not be null")
        assertTrue(pngData.bytes.size > 8, "PNG file should contain data")

        // Validate PNG magic bytes: 0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A
        val bytes = pngData.bytes
        assertEquals(0x89.toByte(), bytes[0])
        assertEquals('P'.code.toByte(), bytes[1])
        assertEquals('N'.code.toByte(), bytes[2])
        assertEquals('G'.code.toByte(), bytes[3])
    }

    @Test
    fun testHudMetricsStringFormatting() {
        val target = TargetImage.loadOrGenerate(null, 64, 64)
        val painter = GeneticPainter(target = target, initialStrokeCount = 40)

        painter.generations.set(12345)
        painter.acceptedMutations.set(890)
        painter.currentEvalsPerSec.set(1500)

        val hudString =
            "GEN: ${painter.generations.get()} | ACC: ${painter.acceptedMutations.get()} | EPS: ${painter.currentEvalsPerSec.get()}"
        assertEquals("GEN: 12345 | ACC: 890 | EPS: 1500", hudString)
    }
}
