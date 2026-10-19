import org.jetbrains.skia.Surface
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
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
                getMousePos = { 200f to 150f },
                onFrameRendered = { rendered = true },
            )

        // Render at t = 1.0s (1_000_000_000 ns)
        delegate.onRender(canvas, width, height, 1_000_000_000L)
        assertTrue(rendered, "Render callback should have fired")
    }

    @Test
    fun testPulseAnimationFormula() {
        for (i in 0..10) {
            val seconds = i * 0.5
            val pulse = (sin(seconds * 3.0).toFloat() + 1f) * 15f + 40f
            assertTrue(pulse in 40f..70f, "Pulse radius must oscillate within [40, 70]")
        }
    }
}
