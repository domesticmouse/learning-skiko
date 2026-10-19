import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Surface
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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

        val rng = Random(123)
        val mutated = gene.mutate(100f, 100f, rng) as TaperedStrokeGene
        assertTrue(mutated.wPeak in 4f..36f)
        assertNotEquals(gene.wPeak, mutated.wPeak)
    }

    @Test
    fun testPolygonGeneRenderAndMutate() {
        val points =
            listOf(
                Pair(50f, 10f),
                Pair(90f, 80f),
                Pair(10f, 80f),
            )
        val gene = PolygonGene(points, 0x88AABBCC.toInt())

        val surface = Surface.makeRaster(ImageInfo(100, 100, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
        val canvas = surface.canvas
        val paint = Paint()
        val path = Path()

        // Rendering should complete without throwing or segfaulting
        gene.render(canvas, paint, path)

        val rng = Random(999)
        val mutated = gene.mutate(100f, 100f, rng) as PolygonGene
        assertEquals(3, mutated.points.size)
        assertTrue(mutated.points.all { it.first in 0f..100f && it.second in 0f..100f })
    }

    @Test
    fun testPolymorphicGenomeRendering() {
        val genome: List<Gene> =
            listOf(
                CubicCurveGene(0f, 0f, 25f, 50f, 75f, 50f, 100f, 100f, 2f, 0xFF00FF00.toInt()),
                TaperedStrokeGene(10f, 20f, 50f, 80f, 90f, 20f, 1f, 8f, 2f, 0xFFFF0000.toInt()),
                PolygonGene(listOf(Pair(10f, 10f), Pair(40f, 10f), Pair(25f, 40f)), 0x880000FF.toInt()),
            )

        val surface = Surface.makeRaster(ImageInfo(100, 100, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
        val canvas = surface.canvas
        val paint = Paint()
        val path = Path()

        for (gene in genome) {
            gene.render(canvas, paint, path)
        }
        assertEquals(3, genome.size)
    }
}
