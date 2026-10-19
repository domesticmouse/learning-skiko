import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Surface
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class VectorBrushTest {
    private val rng = Random(12345)
    private val width = 400f
    private val height = 300f

    @Test
    fun testStrokeGeneCreationAndBounds() {
        val stroke = StrokeGene.random(width, height, rng)
        assertTrue(stroke.x0 in 0f..width, "x0 must be within width")
        assertTrue(stroke.y0 in 0f..height, "y0 must be within height")
        assertTrue(stroke.xc in 0f..width, "xc must be within width")
        assertTrue(stroke.yc in 0f..height, "yc must be within height")
        assertTrue(stroke.x1 in 0f..width, "x1 must be within width")
        assertTrue(stroke.y1 in 0f..height, "y1 must be within height")
        assertTrue(stroke.strokeWidth in 3f..25f, "strokeWidth must be within expected random range")
        assertTrue(stroke.r in 0..255, "r in 0..255")
        assertTrue(stroke.g in 0..255, "g in 0..255")
        assertTrue(stroke.b in 0..255, "b in 0..255")
        assertTrue(stroke.a in 30..180, "alpha in 30..180")
    }

    @Test
    fun testStrokeGeneMutationStaysWithinBounds() {
        var stroke = StrokeGene.random(width, height, rng)
        for (i in 0 until 500) {
            stroke = stroke.mutate(width, height, rng)
            assertTrue(stroke.x0 in 0f..width)
            assertTrue(stroke.y0 in 0f..height)
            assertTrue(stroke.xc in 0f..width)
            assertTrue(stroke.yc in 0f..height)
            assertTrue(stroke.x1 in 0f..width)
            assertTrue(stroke.y1 in 0f..height)
            assertTrue(stroke.strokeWidth in 2f..35f)
            assertTrue(stroke.r in 0..255)
            assertTrue(stroke.g in 0..255)
            assertTrue(stroke.b in 0..255)
            assertTrue(stroke.a in 20..230)
        }
    }

    @Test
    fun testPaintingCreationAndMutation() {
        val strokeCount = 40
        val painting = Painting.createRandom(strokeCount, width, height, rng)
        assertEquals(strokeCount, painting.strokes.size)

        val mutated = painting.mutate(width, height, rng)
        assertEquals(strokeCount, mutated.strokes.size)
        assertEquals(Long.MAX_VALUE, mutated.error, "Mutated child should have error reset to MAX_VALUE")
        assertNotEquals(painting.strokes, mutated.strokes, "Mutated painting strokes should differ")
    }

    @Test
    fun testPaintingRenderOntoSurface() {
        val surface = Surface.makeRasterN32Premul(width.toInt(), height.toInt())
        val canvas = surface.canvas
        val paint = Paint()
        val path = Path()

        val painting = Painting.createRandom(25, width, height, rng)
        painting.render(canvas, width, height, paint, path)

        // Verifies the offscreen raster surface can be rendered without exception
        assertEquals(width.toInt(), surface.width)
        assertEquals(height.toInt(), surface.height)
    }
}
