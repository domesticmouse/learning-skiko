import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.math.sin
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate

fun main() {
    // Schedule GUI creation on the Swing Event Dispatch Thread (EDT)
    SwingUtilities.invokeLater {
        val window =
            JFrame("Kotlin Desktop with Skiko").apply {
                defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                preferredSize = Dimension(800, 600)
            }

        var mouseX = 400f
        var mouseY = 300f

        val skiaLayer = SkiaLayer()
        skiaLayer.renderDelegate =
            object : SkikoRenderDelegate {
                // Allocate paints and fonts outside the loop for high FPS
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
                val font = Font().apply { size = 24f }
                val subFont = Font().apply { size = 14f }

                override fun onRender(
                    canvas: Canvas,
                    width: Int,
                    height: Int,
                    nanoTime: Long,
                ) {
                    // Canonical HiDPI scaling: normalize canvas to Swing logical coordinates
                    val scale = skiaLayer.contentScale
                    canvas.save()
                    canvas.scale(scale, scale)
                    val logicalW = width / scale
                    val logicalH = height / scale

                    // Clear background
                    canvas.drawRect(Rect.makeWH(logicalW, logicalH), bgPaint)

                    // Animation calculation using frame timestamp
                    val seconds = nanoTime / 1_000_000_000.0
                    val pulse = (sin(seconds * 3.0).toFloat() + 1f) * 15f + 40f

                    // Draw follower circle (mouseX/mouseY match 1:1 with logical canvas points)
                    canvas.drawCircle(mouseX, mouseY, pulse + 20f, glowPaint)
                    canvas.drawCircle(mouseX, mouseY, pulse, circlePaint)

                    // Draw crisp vector text at native display DPI
                    canvas.drawString("Hello from Skiko (Skia for Kotlin)!", 50f, 70f, font, textPaint)
                    canvas.drawString("Move your mouse around to interact with the 2D canvas.", 50f, 105f, subFont, textPaint)
                    canvas.drawString("Resolution: ${width}x${height}px (Scale: ${scale}x) | Time: ${String.format("%.1f", seconds)}s", 50f, logicalH - 30f, subFont, textPaint)

                    canvas.restore()
                    // Request continuous redraw for smooth 60/120 FPS
                    skiaLayer.needRedraw()
                }
            }

        // Add mouse motion tracking (e.x and e.y map directly to logical canvas points)
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
