import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Font
import org.jetbrains.skia.Typeface
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.math.sin

fun main() {
    SwingUtilities.invokeLater {
        val window = JFrame("Kotlin Desktop with Skiko").apply {
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            preferredSize = Dimension(800, 600)
        }

        var mouseX = 400f
        var mouseY = 300f

        val skiaLayer = SkiaLayer()
        skiaLayer.renderDelegate = object : SkikoRenderDelegate {
            val bgPaint = Paint().apply { color = 0xFF1E1E2E.toInt() }
            val circlePaint = Paint().apply {
                color = 0xFF89B4FA.toInt()
                isAntiAlias = true
            }
            val glowPaint = Paint().apply {
                color = 0x55F38BA8.toInt()
                isAntiAlias = true
            }
            val textPaint = Paint().apply {
                color = 0xFFCDD6F4.toInt()
                isAntiAlias = true
            }
            val font = Font(Typeface.makeDefault(), 24f)
            val subFont = Font(Typeface.makeDefault(), 14f)

            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                // Clear background
                canvas.drawRect(org.jetbrains.skia.Rect.makeWH(width.toFloat(), height.toFloat()), bgPaint)

                val seconds = nanoTime / 1_000_000_000.0
                val pulse = (sin(seconds * 3.0).toFloat() + 1f) * 15f + 40f

                // Draw pulsing follower circle at mouse position
                canvas.drawCircle(mouseX, mouseY, pulse + 20f, glowPaint)
                canvas.drawCircle(mouseX, mouseY, pulse, circlePaint)

                // Draw text info
                canvas.drawString("Hello from Skiko (Skia for Kotlin)!", 50f, 70f, font, textPaint)
                canvas.drawString("Move your mouse around to interact with the 2D canvas.", 50f, 105f, subFont, textPaint)
                canvas.drawString("Window Size: ${width}x${height} | Time: ${String.format("%.1f", seconds)}s", 50f, height - 30f, subFont, textPaint)

                // Request next frame for animation
                skiaLayer.needRedraw()
            }
        }

        // Add mouse motion tracking
        skiaLayer.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                mouseX = e.x.toFloat()
                mouseY = e.y.toFloat()
                skiaLayer.needRedraw()
            }
        })

        skiaLayer.attachTo(window.contentPane)
        window.pack()
        window.setLocationRelativeTo(null)
        window.isVisible = true
    }
}
