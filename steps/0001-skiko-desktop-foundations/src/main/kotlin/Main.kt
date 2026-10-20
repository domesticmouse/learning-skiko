import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
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
    val getScale: () -> Float = { 1.0f },
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
    val defaultTypeface =
        FontMgr.default.matchFamilyStyle("Helvetica", FontStyle.NORMAL)
            ?: FontMgr.default.matchFamilyStyle("Arial", FontStyle.NORMAL)
            ?: FontMgr.default.matchFamilyStyle(null, FontStyle.NORMAL)
    val titleFont = Font(defaultTypeface, 24f)
    val subFont = Font(defaultTypeface, 14f)

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

        // Clear background
        canvas.drawRect(Rect.makeWH(logicalW, logicalH), bgPaint)

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
            "Resolution: ${width}x${height}px (Scale: ${scale}x) | Time: ${String.format(Locale.US, "%.1f", seconds)}s",
            50f,
            logicalH - 30f,
            subFont,
            textPaint,
        )

        canvas.restore()
        onFrameRendered?.invoke()
    }
}

fun main(args: Array<String>) {
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
                getMousePos = { mouseX to mouseY },
                onFrameRendered = { skiaLayer.needRedraw() },
                getScale = { skiaLayer.contentScale },
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
