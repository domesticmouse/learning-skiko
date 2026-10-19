import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * Manages the asynchronous evolutionary loop, mutating paintings and accepting improvements.
 */
class GeneticPainter(
    val target: TargetImage,
    initialStrokeCount: Int = 120,
) {
    val width = target.width
    val height = target.height

    private val running = AtomicBoolean(false)
    val paused = AtomicBoolean(false)

    var strokeCount: Int = initialStrokeCount
        private set

    private val evaluator = Evaluator(width, height, target.pixelBytes)

    val generations = AtomicLong(0)
    val acceptedMutations = AtomicLong(0)
    val currentEvalsPerSec = AtomicLong(0)

    private val bestPaintingRef = AtomicReference<Painting>()
    val initialError: Long

    init {
        val initial = Painting.createRandom(strokeCount, width.toFloat(), height.toFloat())
        val err = evaluator.evaluate(initial)
        initialError = err
        bestPaintingRef.set(initial.copy(error = err))
    }

    fun getBestPainting(): Painting = bestPaintingRef.get()

    fun start() {
        if (running.getAndSet(true)) return

        thread(name = "GeneticPainter-Evolution", isDaemon = true) {
            val rng = Random(System.currentTimeMillis())
            var currentBest = bestPaintingRef.get()

            var localEvals = 0L
            var lastMetricTime = System.currentTimeMillis()

            while (running.get()) {
                if (paused.get()) {
                    Thread.sleep(50)
                    continue
                }

                // Create a mutated candidate from the current champion
                val candidate = currentBest.mutate(width.toFloat(), height.toFloat(), rng)

                // Evaluate candidate
                val candidateError = evaluator.evaluate(candidate)
                generations.incrementAndGet()
                localEvals++

                // Selection: Accept if better or equal (neutral drift can help cross plateaus)
                if (candidateError <= currentBest.error) {
                    currentBest = candidate.copy(error = candidateError)
                    bestPaintingRef.set(currentBest)
                    acceptedMutations.incrementAndGet()
                }

                // Compute evals/sec every 500ms
                val now = System.currentTimeMillis()
                val elapsed = now - lastMetricTime
                if (elapsed >= 500) {
                    val rate = (localEvals * 1000L) / elapsed
                    currentEvalsPerSec.set(rate)
                    localEvals = 0L
                    lastMetricTime = now
                }
            }
        }
    }

    fun stop() {
        running.set(false)
    }

    fun togglePause() {
        paused.set(!paused.get())
    }

    fun reset() {
        val newInit = Painting.createRandom(strokeCount, width.toFloat(), height.toFloat())
        val err = evaluator.evaluate(newInit)
        bestPaintingRef.set(newInit.copy(error = err))
        generations.set(0)
        acceptedMutations.set(0)
    }

    fun adjustStrokes(delta: Int) {
        val newCount = (strokeCount + delta).coerceIn(10, 500)
        if (newCount == strokeCount) return
        strokeCount = newCount

        val currentStrokes = bestPaintingRef.get().strokes.toMutableList()
        val rng = Random(System.currentTimeMillis())
        while (currentStrokes.size < strokeCount) {
            currentStrokes.add(StrokeGene.random(width.toFloat(), height.toFloat(), rng))
        }
        while (currentStrokes.size > strokeCount) {
            currentStrokes.removeAt(currentStrokes.size - 1)
        }
        val candidate = Painting(strokes = currentStrokes)
        val err = evaluator.evaluate(candidate)
        bestPaintingRef.set(candidate.copy(error = err))
    }
}
