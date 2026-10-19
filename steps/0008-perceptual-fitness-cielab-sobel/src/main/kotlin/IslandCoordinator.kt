import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.random.Random

class IslandCoordinator(
    val numIslands: Int = Runtime.getRuntime().availableProcessors().coerceIn(2, 8),
    val target: TargetImage,
    val strokeCount: Int = 120,
    val migrationInterval: Long = 100L,
) {
    private val pool =
        Executors.newFixedThreadPool(numIslands) { runnable ->
            Thread(runnable).apply { isDaemon = true }
        }

    private val migrationInboxes = AtomicReferenceArray<Painting>(numIslands)
    val globalChampion = AtomicReference<Painting>()
    val totalGenerations = AtomicLong(0)
    val acceptedMutations = AtomicLong(0)
    val isRunning = AtomicBoolean(false)

    fun start() {
        if (isRunning.getAndSet(true)) return
        for (islandId in 0 until numIslands) {
            pool.submit {
                runIslandLoop(islandId)
            }
        }
    }

    fun stop() {
        isRunning.set(false)
        pool.shutdownNow()
    }

    private fun runIslandLoop(id: Int) {
        val evaluator = Evaluator(target.width, target.height, target.pixelBytes)
        val rng = Random(System.nanoTime() + id * 10_000L)
        val w = target.width.toFloat()
        val h = target.height.toFloat()

        val initial = Painting.createRandom(strokeCount, w, h, rng)
        var localBest = initial.copy(error = evaluator.evaluate(initial))
        globalChampion.accumulateAndGet(localBest) { prev, next ->
            if (prev == null || next.error < prev.error) next else prev
        }

        var generation = 0L

        while (isRunning.get() && !Thread.currentThread().isInterrupted) {
            generation++
            totalGenerations.incrementAndGet()

            // 1. Check if ring neighbor deposited a migrant champion
            val migrant = migrationInboxes.getAndSet(id, null)
            if (migrant != null && migrant.error < localBest.error) {
                localBest = migrant
            }

            // 2. Standard (1+1) mutation step within this island
            val mutated = localBest.mutate(w, h, rng)
            val candidateError = evaluator.evaluate(mutated)

            if (candidateError <= localBest.error) {
                localBest = mutated.copy(error = candidateError)
                acceptedMutations.incrementAndGet()

                globalChampion.accumulateAndGet(localBest) { prev, next ->
                    if (prev == null || next.error < prev.error) next else prev
                }
            }

            // 3. Periodic migration
            if (generation % migrationInterval == 0L) {
                val nextIsland = (id + 1) % numIslands
                migrationInboxes.set(nextIsland, localBest)
            }
        }
    }
}
