import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaintingExportTest {
    @Test
    fun testToSvgGeneratesValidXml() {
        val strokes =
            listOf(
                StrokeGene(10f, 20f, 30f, 40f, 50f, 60f, 4.5f, 255, 128, 0, 200),
                StrokeGene(5f, 15f, 25f, 35f, 45f, 55f, 2.0f, 0, 255, 128, 100),
            )
        val painting = Painting(strokes = strokes, error = 123456L)

        val svg = painting.toSvg(256, 256)

        assertTrue(svg.startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\""))
        assertTrue(svg.contains("viewBox=\"0 0 256 256\""))
        assertTrue(svg.contains("<rect width=\"256\" height=\"256\" fill=\"#ffffff\"/>"))
        assertTrue(svg.contains("d=\"M 10.0,20.0 Q 30.0,40.0 50.0,60.0\""))
        assertTrue(svg.contains("stroke=\"rgb(255,128,0)\""))
        assertTrue(svg.contains("stroke-width=\"4.5\""))
        assertTrue(svg.trimEnd().endsWith("</svg>"))
    }

    @Test
    fun testCheckpointSaveAndLoadRoundtrip() {
        val strokes =
            listOf(
                StrokeGene(12.5f, 34.2f, 56.1f, 78.9f, 90.0f, 12.3f, 6.5f, 250, 100, 50, 180),
                StrokeGene(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 1.5f, 10, 20, 30, 40),
            )
        val original = Painting(strokes = strokes, error = 987654321L)

        val tempFile = File.createTempFile("checkpoint_test", ".gene")
        try {
            original.saveCheckpoint(tempFile)
            assertTrue(tempFile.exists() && tempFile.length() > 0)

            val loaded = loadCheckpoint(tempFile)
            assertEquals(original.error, loaded.error)
            assertEquals(original.strokes.size, loaded.strokes.size)

            for (i in original.strokes.indices) {
                val s1 = original.strokes[i]
                val s2 = loaded.strokes[i]
                assertEquals(s1.x0, s2.x0)
                assertEquals(s1.y0, s2.y0)
                assertEquals(s1.xc, s2.xc)
                assertEquals(s1.yc, s2.yc)
                assertEquals(s1.x1, s2.x1)
                assertEquals(s1.y1, s2.y1)
                assertEquals(s1.strokeWidth, s2.strokeWidth)
                assertEquals(s1.r, s2.r)
                assertEquals(s1.g, s2.g)
                assertEquals(s1.b, s2.b)
                assertEquals(s1.a, s2.a)
            }
        } finally {
            tempFile.delete()
        }
    }
}
