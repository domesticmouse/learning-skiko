import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IslandCoordinatorTest {
    private val width = 64
    private val height = 64

    @Test
    fun testIslandInitialization() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val coordinator = IslandCoordinator(numIslands = 4, target = target, strokeCount = 20)

        assertEquals(4, coordinator.numIslands)
        assertEquals(20, coordinator.strokeCount)
        assertEquals(0L, coordinator.totalGenerations.get())
    }

    @Test
    fun testConcurrentEvolutionAndGlobalChampion() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val coordinator =
            IslandCoordinator(
                numIslands = 3,
                target = target,
                strokeCount = 20,
                migrationInterval = 20L,
            )

        coordinator.start()
        val start = System.currentTimeMillis()
        while (coordinator.totalGenerations.get() < 150L && System.currentTimeMillis() - start < 3000L) {
            Thread.sleep(20)
        }
        coordinator.stop()

        val champion = coordinator.globalChampion.get()
        assertNotNull(champion, "Global champion must be present after concurrent execution")
        assertTrue(coordinator.totalGenerations.get() >= 100L, "Should have executed at least 100 total generations")
        assertTrue(coordinator.acceptedMutations.get() > 0L, "Should have accepted mutations")
        assertTrue(champion.error < Long.MAX_VALUE, "Champion error must be a valid evaluated value")
    }

    @Test
    fun testCleanStopLifecycle() {
        val target = TargetImage.loadOrGenerate(null, width, height)
        val coordinator = IslandCoordinator(numIslands = 2, target = target, strokeCount = 10)

        coordinator.start()
        assertTrue(coordinator.isRunning.get())
        Thread.sleep(50)
        coordinator.stop()
        assertEquals(false, coordinator.isRunning.get())
    }
}
