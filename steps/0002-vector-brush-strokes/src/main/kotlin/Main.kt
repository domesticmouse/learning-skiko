import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Rect
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.random.Random

class VectorPaintingRenderDelegate(
    var painting: Painting,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val onFrameRendered: (() -> Unit)? = null,
    val getScale: () -> Float = { 1.0f },
) : SkikoRenderDelegate {
    private val strokePaint = Paint()
    private val strokePath = Path()
    private val hudBgPaint = Paint().apply { color = 0xCC181825.toInt() }
    private val textPaint =
        Paint().apply {
            color = 0xFFCDD6F4.toInt()
            isAntiAlias = true
        }
    private val typeface =
        FontMgr.default.matchFamilyStyle("Helvetica", FontStyle.NORMAL)
            ?: FontMgr.default.matchFamilyStyle("Arial", FontStyle.NORMAL)
            ?: FontMgr.default.matchFamilyStyle(null, FontStyle.NORMAL)
    private val font = Font(typeface, 15f)

    override fun onRender(
        canvas: Canvas,
        width: Int,
        height: Int,
        nanoTime: Long,
    ) {
        val scale = getScale()
        canvas.save()
        canvas.scale(scale, scale)
        val logicalW = width / scale
        val logicalH = height / scale

        painting.render(canvas, logicalW, logicalH, strokePaint, strokePath)

        // Draw HUD overlay
        canvas.drawRect(Rect.makeXYWH(20f, 20f, 360f, 50f), hudBgPaint)
        canvas.drawString(
            "Strokes: ${painting.strokes.size} | Press [SPACE] to Mutate",
            30f,
            50f,
            font,
            textPaint,
        )

        canvas.restore()
        onFrameRendered?.invoke()
    }
}

fun main(args: Array<String>) {
    val simWidth = 800f
    val simHeight = 600f
    val rng = Random(42)
    var painting = Painting.createRandom(50, simWidth, simHeight, rng)

    SwingUtilities.invokeLater {
        val window =
            JFrame("Lesson 2: Vector Brush Strokes & Genes").apply {
                defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                preferredSize = Dimension(simWidth.toInt(), simHeight.toInt())
            }

        val skiaLayer = SkiaLayer()
        val delegate =
            VectorPaintingRenderDelegate(
                painting = painting,
                canvasWidth = simWidth,
                canvasHeight = simHeight,
                onFrameRendered = { skiaLayer.needRedraw() },
                getScale = { skiaLayer.contentScale },
            )
        skiaLayer.renderDelegate = delegate

        window.addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_SPACE) {
                        painting = painting.mutate(simWidth, simHeight, Random.Default)
                        delegate.painting = painting
                        skiaLayer.needRedraw()
                    }
                }
            },
        )

        skiaLayer.attachTo(window.contentPane)
        window.pack()
        window.setLocationRelativeTo(null)
        window.isVisible = true
    }
}
