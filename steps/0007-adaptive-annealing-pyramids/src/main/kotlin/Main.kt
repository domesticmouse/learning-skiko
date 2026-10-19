import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
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

fun main(args: Array<String>) {
    val targetPath = args.firstOrNull()

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

    if (args.contains("--headless")) {
        println("Running Step 5 headless HUD & controls verification...")
        painter.start()
        val start = System.currentTimeMillis()
        while (painter.generations.get() < 500L && System.currentTimeMillis() - start < 1500L) {
            Thread.sleep(50)
        }
        painter.stop()
        println("Evaluated ${painter.generations.get()} generations. Error improved to ${painter.getBestPainting().error}.")

        // Test saving snapshot to PNG
        val saveBitmap = Bitmap().apply { allocN32Pixels(target.width, target.height) }
        val saveCanvas = Canvas(saveBitmap)
        painter.getBestPainting().render(saveCanvas, target.width.toFloat(), target.height.toFloat(), Paint(), Path())
        val data = Image.makeFromBitmap(saveBitmap).encodeToData(EncodedImageFormat.PNG)
        if (data != null) {
            val testFile = File("build/test-headless-export.png")
            testFile.parentFile?.mkdirs()
            testFile.writeBytes(data.bytes)
            println("PNG export verified: ${testFile.absolutePath} (${testFile.length()} bytes)")
        }
        return
    }

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
            object : SkikoRenderDelegate {
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

                val titleFont = Font().apply { size = 20f }
                val headerFont = Font().apply { size = 15f }
                val bodyFont = Font().apply { size = 13f }
                val smallFont = Font().apply { size = 11.5f }

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
                    val w = width.toFloat()
                    val h = height.toFloat()

                    // 1. Background
                    canvas.drawRect(Rect.makeWH(w, h), bgPaint)

                    // 2. Header
                    canvas.drawString("Genetic Algorithm: Brush Stroke Painter", 32f, 40f, titleFont, textMainPaint)
                    canvas.drawString(
                        "Target: ${target.sourceDescription} (${target.width}x${target.height})",
                        32f,
                        64f,
                        bodyFont,
                        textDimPaint,
                    )

                    // 3. Central Display Area: Target vs Evolving Painting
                    val availableWidth = w - 64f
                    val displaySize = (availableWidth / 2f - 24f).coerceAtMost(h - 220f).coerceAtLeast(180f)
                    val cardY = 88f

                    // Left Card: Target Image
                    val leftCardX = 32f
                    canvas.drawString("TARGET IMAGE", leftCardX, cardY + 16f, headerFont, accentPaint)
                    val targetRect = Rect.makeXYWH(leftCardX, cardY + 28f, displaySize, displaySize)
                    canvas.drawRRect(RRect.makeXYWH(leftCardX, cardY + 28f, displaySize, displaySize, 8f), cardPaint)
                    canvas.drawImageRect(target.image, targetRect)
                    canvas.drawRRect(RRect.makeXYWH(leftCardX, cardY + 28f, displaySize, displaySize, 8f), borderPaint)

                    // Right Card: Evolving Painting
                    val rightCardX = leftCardX + displaySize + 32f
                    canvas.drawString("EVOLVED PAINTING", rightCardX, cardY + 16f, headerFont, greenPaint)
                    val evolveRect = Rect.makeXYWH(rightCardX, cardY + 28f, displaySize, displaySize)
                    canvas.drawRRect(RRect.makeXYWH(rightCardX, cardY + 28f, displaySize, displaySize, 8f), cardPaint)

                    // Render best painting into displayBitmap
                    val currentChampion = painter.getBestPainting()
                    currentChampion.render(
                        canvas = displayCanvas,
                        width = target.width.toFloat(),
                        height = target.height.toFloat(),
                        paint = strokePaint,
                        path = strokePath,
                        backgroundColor = Color.WHITE,
                    )
                    val currentImage = Image.makeFromBitmap(displayBitmap)
                    canvas.drawImageRect(currentImage, evolveRect)
                    canvas.drawRRect(RRect.makeXYWH(rightCardX, cardY + 28f, displaySize, displaySize, 8f), borderPaint)

                    // 4. Bottom HUD / Metrics Panel
                    val hudY = cardY + displaySize + 48f
                    val hudRect = Rect.makeXYWH(32f, hudY, w - 64f, 82f)
                    canvas.drawRRect(RRect.makeXYWH(32f, hudY, w - 64f, 82f, 10f), cardPaint)
                    canvas.drawRRect(RRect.makeXYWH(32f, hudY, w - 64f, 82f, 10f), borderPaint)

                    val gens = painter.generations.get()
                    val accepted = painter.acceptedMutations.get()
                    val evalsSec = painter.currentEvalsPerSec.get()
                    val error = currentChampion.error
                    val initialErr = max(1L, painter.initialError)
                    val improvement = ((1.0 - error.toDouble() / initialErr.toDouble()) * 100.0).coerceAtLeast(0.0)
                    val isPaused = painter.paused.get()

                    // Metrics Row 1
                    val col1 = 50f
                    val col2 = 230f
                    val col3 = 410f
                    val col4 = 600f
                    val col5 = 760f

                    canvas.drawString("GENERATIONS", col1, hudY + 28f, smallFont, textDimPaint)
                    canvas.drawString("%,d".format(gens), col1, hudY + 54f, headerFont, textMainPaint)

                    canvas.drawString("ACCEPTED", col2, hudY + 28f, smallFont, textDimPaint)
                    canvas.drawString("%,d".format(accepted), col2, hudY + 54f, headerFont, greenPaint)

                    canvas.drawString("IMPROVEMENT", col3, hudY + 28f, smallFont, textDimPaint)
                    canvas.drawString("%.1f%%".format(improvement), col3, hudY + 54f, headerFont, accentPaint)

                    canvas.drawString("SPEED", col4, hudY + 28f, smallFont, textDimPaint)
                    canvas.drawString("%,d evals/s".format(evalsSec), col4, hudY + 54f, headerFont, textMainPaint)

                    canvas.drawString("STROKES / STATUS", col5, hudY + 28f, smallFont, textDimPaint)
                    val statusText = if (isPaused) "PAUSED" else "RUNNING"
                    val statusColor = if (isPaused) warningPaint else greenPaint
                    canvas.drawString("${painter.strokeCount} ($statusText)", col5, hudY + 54f, headerFont, statusColor)

                    // 5. Footer Shortcuts
                    val footerY = hudY + 104f
                    val shortcuts = "[SPACE] Pause/Resume   |   [R] Reset   |   [+] / [-] Stroke Count   |   [S] Save PNG"
                    canvas.drawString(shortcuts, 34f, footerY, smallFont, textDimPaint)

                    // Notification banner if active
                    val now = System.currentTimeMillis()
                    if (now < notificationExpiry && statusNotification.isNotBlank()) {
                        canvas.drawString(statusNotification, w - 240f, footerY, smallFont, greenPaint)
                    }

                    // Request next frame
                    skiaLayer.needRedraw()
                }
            }

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
