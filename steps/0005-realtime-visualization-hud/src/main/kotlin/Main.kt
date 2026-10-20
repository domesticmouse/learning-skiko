import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.io.File
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.math.max

class HudRenderDelegate(
    val target: TargetImage,
    val painter: GeneticPainter,
    val onFrameRendered: (() -> Unit)? = null,
    val getScale: () -> Float = { 1.0f },
    val getStatusNotification: () -> Pair<String, Long> = { "" to 0L },
) : SkikoRenderDelegate {
    val bgPaint = Paint().apply { color = 0xFF141622.toInt() }
    val cardPaint = Paint().apply { color = 0xFF1E2132.toInt() }
    val borderPaint =
        Paint().apply {
            color = 0xFF353B55.toInt()
            mode = PaintMode.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
    val textMainPaint =
        Paint().apply {
            color = 0xFFECEFF4.toInt()
            isAntiAlias = true
        }
    val textDimPaint =
        Paint().apply {
            color = 0xFF8892B0.toInt()
            isAntiAlias = true
        }
    val accentPaint =
        Paint().apply {
            color = 0xFF82AAFF.toInt()
            isAntiAlias = true
        }
    val greenPaint =
        Paint().apply {
            color = 0xFF22C55E.toInt()
            isAntiAlias = true
        }
    val warningPaint =
        Paint().apply {
            color = 0xFFF59E0B.toInt()
            isAntiAlias = true
        }

    val typeface =
        FontMgr.default.matchFamilyStyle("Helvetica", FontStyle.NORMAL)
            ?: FontMgr.default.matchFamilyStyle("Arial", FontStyle.NORMAL)
            ?: FontMgr.default.matchFamilyStyle(null, FontStyle.NORMAL)
    val titleFont = Font(typeface, 20f)
    val headerFont = Font(typeface, 15f)
    val bodyFont = Font(typeface, 13f)
    val smallFont = Font(typeface, 11.5f)

    // Offscreen render helpers for displaying the best painting candidate
    val displayBitmap = Bitmap().apply { allocN32Pixels(target.width, target.height) }
    val displayCanvas = Canvas(displayBitmap)
    val strokePaint = Paint()
    val strokePath = Path()

    override fun onRender(
        canvas: Canvas,
        width: Int,
        height: Int,
        nanoTime: Long,
    ) {
        val scale = getScale()
        canvas.save()
        canvas.scale(scale, scale)
        val w = width / scale
        val h = height / scale

        // 1. Background
        canvas.drawRect(Rect.makeWH(w, h), bgPaint)

        // 2. Header
        canvas.drawString("Genetic Algorithm: Brush Stroke Painter", 32f, 40f, titleFont, textMainPaint)
        canvas.drawString(
            "Hardware-accelerated evolutionary optimization via Skiko (Skia for Kotlin)",
            32f,
            62f,
            bodyFont,
            textDimPaint,
        )

        // 3. Side-by-side Viewports
        val cardY = 86f
        val cardW = 280f
        val cardH = 320f
        val leftCardX = 32f
        val rightCardX = 336f

        // Target Card
        canvas.drawRRect(RRect.makeXYWH(leftCardX, cardY, cardW, cardH, 12f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(leftCardX, cardY, cardW, cardH, 12f), borderPaint)
        canvas.drawString("TARGET IMAGE", leftCardX + 16f, cardY + 28f, headerFont, accentPaint)
        canvas.drawImageRect(
            target.image,
            Rect.makeXYWH(leftCardX + 12f, cardY + 44f, 256f, 256f),
        )

        // Evolved Candidate Card
        canvas.drawRRect(RRect.makeXYWH(rightCardX, cardY, cardW, cardH, 12f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(rightCardX, cardY, cardW, cardH, 12f), borderPaint)
        canvas.drawString("EVOLVED PAINTING", rightCardX + 16f, cardY + 28f, headerFont, greenPaint)

        // Render current champion to bitmap snapshot
        val best = painter.getBestPainting()
        displayCanvas.clear(Color.WHITE)
        best.render(
            canvas = displayCanvas,
            width = target.width.toFloat(),
            height = target.height.toFloat(),
            paint = strokePaint,
            path = strokePath,
            backgroundColor = Color.WHITE,
        )
        val bestImage = Image.makeFromBitmap(displayBitmap)
        canvas.drawImageRect(
            bestImage,
            Rect.makeXYWH(rightCardX + 12f, cardY + 44f, 256f, 256f),
        )

        // 4. Live Telemetry HUD Card
        val hudY = 422f
        val hudW = w - 64f
        val hudH = 100f
        canvas.drawRRect(RRect.makeXYWH(32f, hudY, hudW, hudH, 12f), cardPaint)
        canvas.drawRRect(RRect.makeXYWH(32f, hudY, hudW, hudH, 12f), borderPaint)

        val gens = painter.generations.get()
        val accepted = painter.acceptedMutations.get()
        val evalsSec = painter.currentEvalsPerSec.get()
        val initErr = painter.initialError
        val curErr = best.error
        val improvement =
            if (initErr > 0L) {
                max(0.0, ((initErr - curErr).toDouble() / initErr.toDouble()) * 100.0)
            } else {
                0.0
            }

        val col1 = 56f
        val col2 = 220f
        val col3 = 390f
        val col4 = 570f
        val col5 = 750f

        canvas.drawString("GENERATIONS", col1, hudY + 28f, smallFont, textDimPaint)
        canvas.drawString("%,d".format(gens), col1, hudY + 54f, headerFont, textMainPaint)

        canvas.drawString("ACCEPTED", col2, hudY + 28f, smallFont, textDimPaint)
        canvas.drawString("%,d".format(accepted), col2, hudY + 54f, headerFont, greenPaint)

        canvas.drawString("IMPROVEMENT", col3, hudY + 28f, smallFont, textDimPaint)
        canvas.drawString("%.1f%%".format(improvement), col3, hudY + 54f, headerFont, accentPaint)

        canvas.drawString("SPEED", col4, hudY + 28f, smallFont, textDimPaint)
        canvas.drawString("%,d evals/s".format(evalsSec), col4, hudY + 54f, headerFont, textMainPaint)

        canvas.drawString("STROKES / STATUS", col5, hudY + 28f, smallFont, textDimPaint)
        val statusText = if (painter.paused.get()) "PAUSED" else "ACTIVE"
        val statusColor = if (painter.paused.get()) warningPaint else greenPaint
        canvas.drawString("${painter.strokeCount} ($statusText)", col5, hudY + 54f, headerFont, statusColor)

        // 5. Hotkeys & Notification Footer
        val footerY = h - 24f
        val shortcuts = "[SPACE] Pause/Resume   [R] Reset   [+] / [-] Strokes   [S] Save Snapshot PNG"
        canvas.drawString(shortcuts, 34f, footerY, smallFont, textDimPaint)

        val (statusNotification, notificationExpiry) = getStatusNotification()
        if (statusNotification.isNotEmpty() && System.currentTimeMillis() < notificationExpiry) {
            canvas.drawString(statusNotification, w - 240f, footerY, smallFont, greenPaint)
        }

        canvas.restore()
        onFrameRendered?.invoke()
    }
}

fun main(args: Array<String>) {
    val targetPath = args.firstOrNull { !it.startsWith("--") }

    println("==================================================")
    println(" Genetic Algorithm: Brush Stroke Painter (Skiko) ")
    println("==================================================")
    if (targetPath != null) {
        println("Target image argument provided: $targetPath")
    } else {
        println("No image path provided in args. Using procedural fallback target.")
        println("Tip: Run with: ./gradlew run --args=\"path/to/image.png\"")
    }

    val target = TargetImage.loadOrGenerate(targetPath, simWidth = 256, simHeight = 256)
    val painter = GeneticPainter(target = target, initialStrokeCount = 120)

    painter.start()

    SwingUtilities.invokeLater {
        val window =
            JFrame("Genetic Algorithm Painter (Skiko)").apply {
                defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                preferredSize = Dimension(960, 680)
                minimumSize = Dimension(800, 600)
            }

        val skiaLayer = SkiaLayer()
        var statusNotification = ""
        var notificationExpiry = 0L

        skiaLayer.renderDelegate =
            HudRenderDelegate(
                target = target,
                painter = painter,
                onFrameRendered = { skiaLayer.needRedraw() },
                getScale = { skiaLayer.contentScale },
                getStatusNotification = { statusNotification to notificationExpiry },
            )

        // Keyboard Controls
        window.addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    when (e.keyCode) {
                        KeyEvent.VK_SPACE -> {
                            painter.togglePause()
                            statusNotification = if (painter.paused.get()) "Evolution Paused" else "Evolution Resumed"
                            notificationExpiry = System.currentTimeMillis() + 2000L
                        }
                        KeyEvent.VK_R -> {
                            painter.reset()
                            statusNotification = "Reset Population"
                            notificationExpiry = System.currentTimeMillis() + 2000L
                        }
                        KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS, KeyEvent.VK_ADD -> {
                            painter.adjustStrokes(10)
                            statusNotification = "Strokes: ${painter.strokeCount}"
                            notificationExpiry = System.currentTimeMillis() + 2000L
                        }
                        KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT -> {
                            painter.adjustStrokes(-10)
                            statusNotification = "Strokes: ${painter.strokeCount}"
                            notificationExpiry = System.currentTimeMillis() + 2000L
                        }
                        KeyEvent.VK_S -> {
                            try {
                                val saveBitmap = Bitmap().apply { allocN32Pixels(target.width, target.height) }
                                val saveCanvas = Canvas(saveBitmap)
                                val current = painter.getBestPainting()
                                current.render(
                                    canvas = saveCanvas,
                                    width = target.width.toFloat(),
                                    height = target.height.toFloat(),
                                    paint = Paint(),
                                    path = Path(),
                                    backgroundColor = Color.WHITE,
                                )
                                val saveImage = Image.makeFromBitmap(saveBitmap)
                                val data = saveImage.encodeToData(EncodedImageFormat.PNG)
                                if (data != null) {
                                    val outFile = File("evolved-painting.png")
                                    outFile.writeBytes(data.bytes)
                                    statusNotification = "Saved: ${outFile.name}"
                                    notificationExpiry = System.currentTimeMillis() + 3000L
                                    println("Successfully saved current painting to: ${outFile.absolutePath}")
                                }
                            } catch (ex: Exception) {
                                System.err.println("Failed to save image: ${ex.message}")
                            }
                        }
                    }
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
