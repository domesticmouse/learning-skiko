import kotlin.math.pow
import kotlin.math.sqrt

class PerceptualEvaluator(
    val target: TargetImage,
    val edgeEmphasis: Float = 3.0f,
) {
    // Precomputed weight map from 3x3 Sobel convolution
    val edgeWeights = FloatArray(target.width * target.height)

    // Precomputed RGB -> Lab packed lookup table (32x32x32 quantized grid * 3 channels)
    val labLut = FloatArray(32 * 32 * 32 * 3)

    init {
        computeSobelWeights()
        precomputeLabLut()
    }

    private fun luminanceAt(
        x: Int,
        y: Int,
    ): Float {
        val idx = (y * target.width + x) * 4
        val b = target.pixelBytes[idx].toInt() and 0xFF
        val g = target.pixelBytes[idx + 1].toInt() and 0xFF
        val r = target.pixelBytes[idx + 2].toInt() and 0xFF
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
    }

    private fun computeSobelWeights() {
        val w = target.width
        val h = target.height
        edgeWeights.fill(1.0f)

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx =
                    (-1f * luminanceAt(x - 1, y - 1)) + (1f * luminanceAt(x + 1, y - 1)) +
                        (-2f * luminanceAt(x - 1, y)) + (2f * luminanceAt(x + 1, y)) +
                        (-1f * luminanceAt(x - 1, y + 1)) + (1f * luminanceAt(x + 1, y + 1))

                val gy =
                    (-1f * luminanceAt(x - 1, y - 1)) + (-2f * luminanceAt(x, y - 1)) + (-1f * luminanceAt(x + 1, y - 1)) +
                        (1f * luminanceAt(x - 1, y + 1)) + (2f * luminanceAt(x, y + 1)) + (1f * luminanceAt(x + 1, y + 1))

                val magnitude = sqrt(gx * gx + gy * gy)
                edgeWeights[y * w + x] = 1.0f + edgeEmphasis * magnitude
            }
        }
    }

    fun evaluatePerceptual(candidateBytes: ByteArray): Double {
        var totalError = 0.0
        val pixelCount = target.width * target.height
        val targetBytes = target.pixelBytes

        for (p in 0 until pixelCount) {
            val i = p * 4 // BGRA_8888 stride
            val idx1 =
                lutBaseIndex(
                    r = candidateBytes[i + 2].toInt() and 0xFF,
                    g = candidateBytes[i + 1].toInt() and 0xFF,
                    b = candidateBytes[i].toInt() and 0xFF,
                )
            val idx2 =
                lutBaseIndex(
                    r = targetBytes[i + 2].toInt() and 0xFF,
                    g = targetBytes[i + 1].toInt() and 0xFF,
                    b = targetBytes[i].toInt() and 0xFF,
                )

            val dL = labLut[idx1] - labLut[idx2]
            val da = labLut[idx1 + 1] - labLut[idx2 + 1]
            val db = labLut[idx1 + 2] - labLut[idx2 + 2]
            val deltaE = sqrt(dL * dL + da * da + db * db)

            totalError += deltaE * edgeWeights[p]
        }
        return totalError
    }

    fun lutBaseIndex(
        r: Int,
        g: Int,
        b: Int,
    ): Int {
        val r5 = (r ushr 3) and 0x1F
        val g5 = (g ushr 3) and 0x1F
        val b5 = (b ushr 3) and 0x1F
        return ((r5 shl 10) or (g5 shl 5) or b5) * 3
    }

    private fun precomputeLabLut() {
        fun pivot(n: Double): Double = if (n > 0.008856) n.pow(1.0 / 3.0) else (7.787 * n) + (16.0 / 116.0)

        for (r5 in 0..31) {
            val r = (r5 * 255.0 / 31.0) / 255.0
            val rLin = if (r <= 0.04045) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)

            for (g5 in 0..31) {
                val g = (g5 * 255.0 / 31.0) / 255.0
                val gLin = if (g <= 0.04045) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)

                for (b5 in 0..31) {
                    val b = (b5 * 255.0 / 31.0) / 255.0
                    val bLin = if (b <= 0.04045) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

                    // Convert linear RGB to XYZ (D65 standard observer)
                    val x = (rLin * 0.4124 + gLin * 0.3576 + bLin * 0.1805) / 0.95047
                    val y = (rLin * 0.2126 + gLin * 0.7152 + bLin * 0.0722) / 1.00000
                    val z = (rLin * 0.0193 + gLin * 0.1192 + bLin * 0.9505) / 1.08883

                    val fx = pivot(x)
                    val fy = pivot(y)
                    val fz = pivot(z)

                    val lStar = ((116.0 * fy) - 16.0).toFloat()
                    val aStar = (500.0 * (fx - fy)).toFloat()
                    val bStar = (200.0 * (fy - fz)).toFloat()

                    val base = ((r5 shl 10) or (g5 shl 5) or b5) * 3
                    labLut[base] = lStar
                    labLut[base + 1] = aStar
                    labLut[base + 2] = bStar
                }
            }
        }
    }
}
