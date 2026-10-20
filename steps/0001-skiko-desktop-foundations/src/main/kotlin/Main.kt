import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.util.Locale
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.math.sin

class DemoRenderDelegate(
    val getMousePos: () -> Pair<Float, Float> = { 400f to 300f },
    val onFrameRendered: (() -> Unit)? = null,
) : SkikoRenderDelegate {
    val bgPaint = Paint().apply { color = 0xFF1E1E2E.toInt() }
    val circlePaint =
        Paint().apply {
            color = 0xFF89B4FA.toInt()
            isAntiAlias = true
        }
    val glowPaint =
        Paint().apply {
            color = 0x55F38BA8.toInt()
            isAntiAlias = true
        }
    val textPaint =
        Paint().apply {
            color = 0xFFCDD6F4.toInt()
            isAntiAlias = true
        }
    val titleFont = Font().apply { size = 24f }
    val subFont = Font().apply { size = 14f }

    override fun onRender(
        canvas: Canvas,
        width: Int,
        height: Int,
        nanoTime: Long,
    ) {
        // Clear background
        canvas.drawRect(Rect.makeWH(width.toFloat(), height.toFloat()), bgPaint)

        // Compute smooth elapsed time animation
        val seconds = nanoTime / 1_000_000_000.0
        val pulse = (sin(seconds * 3.0).toFloat() + 1f) * 15f + 40f

        val (mouseX, mouseY) = getMousePos()

        // Draw outer glow and main interactive follower circle
        canvas.drawCircle(mouseX, mouseY, pulse + 20f, glowPaint)
        canvas.drawCircle(mouseX, mouseY, pulse, circlePaint)

        // Draw high-DPI crisp vector text
        canvas.drawString("Hello from Skiko (Skia for Kotlin)!", 50f, 70f, titleFont, textPaint)
        canvas.drawString("Move your mouse around to interact with the hardware canvas.", 50f, 105f, subFont, textPaint)
        canvas.drawString(
            "Resolution: ${width}x${height}px | Time: ${String.format(Locale.US, "%.1f", seconds)}s",
            50f,
            height - 30f,
            subFont,
            textPaint,
        )

        onFrameRendered?.invoke()
    }
}

fun main(args: Array<String>) {
    if (args.contains("--headless")) {
        println("Running headless smoke test verification...")
        val surface = Surface.makeRasterN32Premul(800, 600)
        val delegate = DemoRenderDelegate()
        delegate.onRender(surface.canvas, 800, 600, System.nanoTime())
        println("Render verification successful: drawn 800x600 frame to Skia surface.")
        return
    }

    SwingUtilities.invokeLater {
        val window =
            JFrame("Kotlin Desktop with Skiko").apply {
                defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                preferredSize = Dimension(800, 600)
            }

        var mouseX = 400f
        var mouseY = 300f

        val skiaLayer = SkiaLayer()
        val delegate =
            DemoRenderDelegate(
                getMousePos = { (mouseX * skiaLayer.contentScale) to (mouseY * skiaLayer.contentScale) },
                onFrameRendered = { skiaLayer.needRedraw() },
            )
        skiaLayer.renderDelegate = delegate

        skiaLayer.addMouseMotionListener(
            object : MouseMotionAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    mouseX = e.x.toFloat()
                    mouseY = e.y.toFloat()
                    skiaLayer.needRedraw()
                }

                override fun mouseDragged(e: MouseEvent) {
                    mouseX = e.x.toFloat()
                    mouseY = e.y.toFloat()
                    skiaLayer.needRedraw()
                }
            },
        )

        skiaLayer.attachTo(window.contentPane)
        window.pack()
        window.setLocationRelativeTo(null)
        window.isVisible = true
    }
}
