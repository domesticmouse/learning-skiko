# Evolutionary Strategies & Fitness for Skiko Art

## 1. Comparing Evolutionary Strategies

| Strategy | Population Size | Operators | Fitness Landscape Fit | Best For |
| :--- | :--- | :--- | :--- | :--- |
| **$(1 + 1)$ Hill Climber** | 1 parent $\to$ 1 child | Mutation only | Extremely effective on high-dimensional pixel spaces | Rapid real-time visual convergence; high FPS |
| **$(1 + \lambda)$ Strategy** | 1 parent $\to \lambda$ children | Mutation only | Explores $\lambda$ directions around current champion | Parallel multi-core evaluation |
| **$(\mu + \lambda)$ Population GA** | $\mu$ parents, $\lambda$ offspring | Crossover + Mutation | Multimodal search | Coarse geometric shapes; symbolic formula evolution |

### Why Crossover Fails for Layered Strokes
In image reconstruction, shape #15 might be a dark pupil in Parent A, but a highlight on the chin in Parent B. Recombining indices creates destructive interference. Single-parent mutation with neutral drift outperforms crossover by orders of magnitude for vector painting.

---

## 2. Fitness Metrics

### L1 Manhattan Distance (Absolute Difference)
The highest-throughput metric for real-time CPU evaluation:
$$\text{L1} = \sum_{x,y} \left( |R_t - R_c| + |G_t - G_c| + |B_t - B_c| \right)$$

* **Pros**: Uses simple subtraction and `Math.abs`, eliminating multiplication instructions and avoiding integer overflow risks on large canvases. Achieves maximum evaluations/second.
* **Sensitivity**: Linear penalty curve; converges smoothly across both broad color blocks and fine edge strokes.
* **Kotlin Signed Byte Masking**: In Kotlin/Java, `Byte` is signed ($-128 \dots 127$). Always convert with `and 0xFF` before subtracting:
  `Math.abs((candidateBytes[i].toInt() and 0xFF) - (targetBytes[i].toInt() and 0xFF))`

### Sum of Squared Differences (SSD / MSE)
The standard quadratic metric for image approximation:
$$\text{SSD} = \sum_{x,y} \left( (R_t - R_c)^2 + (G_t - G_c)^2 + (B_t - B_c)^2 \right)$$

* **Pros**: Inexpensive on modern CPUs; modern JVMs vectorize simple arithmetic loops.
* **Sensitivity**: Penalizes large color mismatches heavily, ensuring large background blocks establish before fine lines. Risk of integer overflow if accumulating large image dimensions without `Long`.

### Subsampling for Ultra-High Speed
To accelerate early generations or large images ($512\times 512$+):
- Evaluate only every 2nd or 4th pixel (`step = 8` or `step = 16`).
- Once improvement plateaus, switch to full-resolution evaluation.

---

## 3. Advanced Mutation Operators

1. **Simulated Annealing / Adaptive Mutation**:
   - Start with large stroke adjustments ($\pm 30$px, $\pm 50$ RGB) in early generations ($< 10,000$).
   - Decay mutation step size as generations advance to refine subtle brush strokes.
2. **Stroke Pruning**:
   - Periodically remove a stroke. If error does not increase significantly, keep the pruned genome. This creates cleaner, minimalist paintings.
3. **Layer Ordering (Z-Index Mutation)**:
   - Swapping stroke indices in the list changes which stroke is rendered on top.
