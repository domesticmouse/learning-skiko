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

class EvaluatorRenderDelegate(
    val target: TargetImage,
    var painting: Painting,
    val evaluator: Evaluator,
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
    private val font = Font(typeface, 14f)

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

        val paneW = target.width.toFloat()
        val paneH = target.height.toFloat()

        // 1. Draw Target Image on the left
        canvas.drawImageRect(target.image, Rect.makeXYWH(20f, 20f, paneW, paneH))

        // 2. Draw Candidate Painting on the right
        canvas.save()
        canvas.translate(paneW + 40f, 20f)
        painting.render(canvas, paneW, paneH, strokePaint, strokePath)
        canvas.restore()

        // 3. Draw HUD
        canvas.drawRect(Rect.makeXYWH(20f, paneH + 30f, logicalW - 40f, 60f), hudBgPaint)
        canvas.drawString("Target: ${target.sourceDescription}", 35f, paneH + 55f, font, textPaint)
        canvas.drawString(
            "Strokes: ${painting.strokes.size} | L2 SSD Error: ${painting.error} | Press [SPACE] to Mutate & Evaluate",
            35f,
            paneH + 75f,
            font,
            textPaint,
        )

        canvas.restore()
        onFrameRendered?.invoke()
    }
}

fun main(args: Array<String>) {
    val simSize = 256
    val target = TargetImage.loadOrGenerate(args.firstOrNull { !it.startsWith("--") }, simSize, simSize)
    val evaluator = Evaluator(simSize, simSize, target.pixelBytes)
    val rng = Random(42)

    val initialPainting = Painting.createRandom(40, simSize.toFloat(), simSize.toFloat(), rng)
    val initialError = evaluator.evaluate(initialPainting)
    var currentPainting = initialPainting.copy(error = initialError)

    SwingUtilities.invokeLater {
        val window =
            JFrame("Lesson 3: Headless Fitness Evaluation").apply {
                defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                preferredSize = Dimension(simSize * 2 + 60, simSize + 120)
            }

        val skiaLayer = SkiaLayer()
        val delegate =
            EvaluatorRenderDelegate(
                target = target,
                painting = currentPainting,
                evaluator = evaluator,
                onFrameRendered = { skiaLayer.needRedraw() },
                getScale = { skiaLayer.contentScale },
            )
        skiaLayer.renderDelegate = delegate

        window.addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_SPACE) {
                        val candidate = currentPainting.mutate(simSize.toFloat(), simSize.toFloat(), Random.Default)
                        val err = evaluator.evaluate(candidate)
                        if (err <= currentPainting.error) {
                            currentPainting = candidate.copy(error = err)
                            delegate.painting = currentPainting
                            skiaLayer.needRedraw()
                        }
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
