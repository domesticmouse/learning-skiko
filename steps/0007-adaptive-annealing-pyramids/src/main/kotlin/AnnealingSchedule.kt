import kotlin.math.exp

class AnnealingSchedule(
    val initialSigma: Float = 60.0f,
    val minSigma: Float = 1.5f,
    val tau: Double = 15_000.0,
) {
    private var rollingAccepted = 0
    private var rollingEvaluated = 0
    var adaptiveGain: Float = 1.0f
        private set

    var currentSigma: Float = initialSigma
        private set

    fun getSigma(generation: Long): Float {
        // 1. Base exponential temperature decay
        val baseSigma = minSigma + (initialSigma - minSigma) * exp(-generation / tau).toFloat()

        // 2. Dynamic gain multiplier based on Rechenberg's 1/5th rule
        if (rollingEvaluated >= 500) {
            val ratio = rollingAccepted.toFloat() / rollingEvaluated
            if (ratio > 0.22f) {
                adaptiveGain = (adaptiveGain * 1.15f).coerceAtMost(2.5f)
            } else if (ratio < 0.18f) {
                adaptiveGain = (adaptiveGain * 0.85f).coerceAtLeast(0.25f)
            }
            rollingAccepted = 0
            rollingEvaluated = 0
        }

        // Combine scheduled cooling with live acceptance feedback
        currentSigma = (baseSigma * adaptiveGain).coerceIn(minSigma, initialSigma)
        return currentSigma
    }

    fun recordOutcome(accepted: Boolean) {
        rollingEvaluated++
        if (accepted) rollingAccepted++
    }
}

/**
 * Scales all stroke coordinates and widths when advancing up the Gaussian pyramid
 * (e.g., scale = 2.0f when promoting a champion from 64x64 -> 128x128).
 */
fun Painting.upscale(scale: Float): Painting =
    copy(
        strokes =
            strokes.map { s ->
                s.copy(
                    x0 = s.x0 * scale,
                    y0 = s.y0 * scale,
                    xc = s.xc * scale,
                    yc = s.yc * scale,
                    x1 = s.x1 * scale,
                    y1 = s.y1 * scale,
                    strokeWidth = (s.strokeWidth * scale).coerceIn(2f, 40f),
                )
            },
        error = Long.MAX_VALUE, // Force re-evaluation at the new pyramid resolution
    )
