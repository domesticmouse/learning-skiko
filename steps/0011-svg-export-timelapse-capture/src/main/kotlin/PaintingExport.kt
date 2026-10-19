import java.io.File
import java.util.Locale

fun Painting.toSvg(
    width: Int,
    height: Int,
): String =
    buildString {
        appendLine("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $width $height">""")
        appendLine("""  <rect width="$width" height="$height" fill="#ffffff"/>""")
        for (stroke in strokes) {
            val opacity = String.format(Locale.US, "%.3f", stroke.a / 255f)
            appendLine(
                """  <path d="M ${stroke.x0},${stroke.y0} Q ${stroke.xc},${stroke.yc} ${stroke.x1},${stroke.y1}" """ +
                    """stroke="rgb(${stroke.r},${stroke.g},${stroke.b})" stroke-opacity="$opacity" """ +
                    """stroke-width="${stroke.strokeWidth}" stroke-linecap="round" fill="none"/>""",
            )
        }
        appendLine("</svg>")
    }

/**
 * Writes the current Painting chromosome to a human-readable .gene checkpoint file
 * so long evolutionary runs can be paused and resumed without losing progress.
 */
fun Painting.saveCheckpoint(file: File) {
    file.bufferedWriter().use { out ->
        out.appendLine("# GENETIC_ART_V1 error=$error strokes=${strokes.size}")
        for (s in strokes) {
            out.appendLine("${s.x0},${s.y0},${s.xc},${s.yc},${s.x1},${s.y1},${s.strokeWidth},${s.r},${s.g},${s.b},${s.a}")
        }
    }
}

fun loadCheckpoint(file: File): Painting {
    val lines = file.readLines()
    val header = lines.firstOrNull { it.startsWith("# GENETIC_ART_V1") } ?: ""
    val savedError = header.substringAfter("error=", "").substringBefore(" ").toLongOrNull() ?: Long.MAX_VALUE

    val strokes =
        lines
            .asSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val p = line.split(",")
                StrokeGene(
                    x0 = p[0].toFloat(),
                    y0 = p[1].toFloat(),
                    xc = p[2].toFloat(),
                    yc = p[3].toFloat(),
                    x1 = p[4].toFloat(),
                    y1 = p[5].toFloat(),
                    strokeWidth = p[6].toFloat(),
                    r = p[7].toInt(),
                    g = p[8].toInt(),
                    b = p[9].toInt(),
                    a = p[10].toInt(),
                )
            }.toList()

    return Painting(strokes = strokes, error = savedError)
}
