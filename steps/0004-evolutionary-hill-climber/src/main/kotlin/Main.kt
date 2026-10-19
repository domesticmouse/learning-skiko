import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

class HillClimberRenderDelegate(
    val painter: GeneticPainter,
    val onFrameRendered: (() -> Unit)? = null,
) : SkikoRenderDelegate {
    private val strokePaint = Paint()
    private val strokePath = Path()
    private val hudBgPaint = Paint().apply { color = 0xCC181825.toInt() }
    private val textPaint =
        Paint().apply {
            color = 0xFFCDD6F4.toInt()
            isAntiAlias = true
        }
    private val font = Font().apply { size = 13f }

    override fun onRender(
        canvas: Canvas,
        width: Int,
        height: Int,
        nanoTime: Long,
    ) {
        val pw = painter.width.toFloat()
        val ph = painter.height.toFloat()
        val best = painter.getBestPainting()

        // 1. Draw Target Image
        canvas.drawImageRect(painter.target.image, Rect.makeXYWH(20f, 20f, pw, ph))

        // 2. Draw Current Best Evolving Painting
        canvas.save()
        canvas.translate(pw + 40f, 20f)
        best.render(canvas, pw, ph, strokePaint, strokePath)
        canvas.restore()

        // 3. Draw Telemetry Overlay
        canvas.drawRect(Rect.makeXYWH(20f, ph + 30f, width - 40f, 70f), hudBgPaint)
        val gens = painter.generations.get()
        val accepted = painter.acceptedMutations.get()
        val evalsSec = painter.currentEvalsPerSec.get()
        val status = if (painter.paused.get()) "PAUSED" else "RUNNING"

        canvas.drawString("[$status] Generations: $gens | Accepted: $accepted | Evals/sec: $evalsSec", 35f, ph + 55f, font, textPaint)
        canvas.drawString(
            "Best Error: ${best.error} | Initial: ${painter.initialError} | Strokes: ${painter.strokeCount}",
            35f,
            ph + 75f,
            font,
            textPaint,
        )
        canvas.drawString("Press [SPACE] to Pause/Resume | [R] to Reset", 35f, ph + 92f, font, textPaint)

        onFrameRendered?.invoke()
    }
}

fun main(args: Array<String>) {
    val simSize = 256
    val target = TargetImage.loadOrGenerate(args.firstOrNull { !it.startsWith("--") }, simSize, simSize)
    val painter = GeneticPainter(target = target, initialStrokeCount = 80)

    if (args.contains("--headless")) {
        println("Running Step 4 headless evolutionary loop verification...")
        println("Initial Error: ${painter.initialError}")
        painter.start()

        // Let background evolutionary loop evolve for 1000ms
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 1000L) {
            Thread.sleep(100)
        }
        painter.stop()

        val gens = painter.generations.get()
        val accepted = painter.acceptedMutations.get()
        val best = painter.getBestPainting()

        println("Generations evaluated: $gens")
        println("Accepted improvements: $accepted")
        println("Final Champion Error: ${best.error}")

        val surface = Surface.makeRasterN32Premul(simSize * 2 + 60, simSize + 120)
        val delegate = HillClimberRenderDelegate(painter)
        delegate.onRender(surface.canvas, surface.width, surface.height, System.nanoTime())
        println("Headless rendering test complete.")
        return
    }

    painter.start()

    SwingUtilities.invokeLater {
        val window =
            JFrame("Lesson 4: (1+1) Evolutionary Hill Climber").apply {
                defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                preferredSize = Dimension(simSize * 2 + 60, simSize + 130)
            }

        val skiaLayer = SkiaLayer()
        val delegate =
            HillClimberRenderDelegate(
                painter = painter,
                onFrameRendered = { skiaLayer.needRedraw() },
            )
        skiaLayer.renderDelegate = delegate

        window.addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    when (e.keyCode) {
                        KeyEvent.VK_SPACE -> {
                            painter.togglePause()
                            skiaLayer.needRedraw()
                        }
                        KeyEvent.VK_R -> {
                            painter.reset()
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
