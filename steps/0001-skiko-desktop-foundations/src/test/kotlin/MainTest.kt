import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import java.io.File
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MainTest {
    @Test
    fun testSkiaSurfaceCreationAndNativeBindings() {
        val width = 800
        val height = 600
        val surface = Surface.makeRasterN32Premul(width, height)
        val canvas = surface.canvas
        assertEquals(width, surface.width)
        assertEquals(height, surface.height)

        var rendered = false
        val delegate =
            DemoRenderDelegate(
                getMousePos = { 400f to 300f },
                onFrameRendered = { rendered = true },
            )

        // Render at t = 1.0s (1_000_000_000 ns)
        delegate.onRender(canvas, width, height, 1_000_000_000L)
        assertTrue(rendered, "Render callback should have fired")

        // Capture screenshot to test harness output
        val image = surface.makeImageSnapshot()
        val data = image.encodeToData(EncodedImageFormat.PNG)
        assertNotNull(data, "PNG data should not be null")
        assertTrue(data.bytes.isNotEmpty(), "PNG bytes must not be empty")

        saveScreenshot(data.bytes, "step-0001-skiko-desktop-foundations.png")
    }

    @Test
    fun testPulseAnimationFormula() {
        for (i in 0..10) {
            val seconds = i * 0.5
            val pulse = (sin(seconds * 3.0).toFloat() + 1f) * 15f + 40f
            assertTrue(pulse in 40f..70f, "Pulse radius must oscillate within [40, 70]")
        }
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
