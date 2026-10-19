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

---

## 4. Multi-Core Island Model & Ring Migration

When scaling evolution across multiple CPU cores, sharing a single mutable champion creates cache thrashing and lock contention. Instead, use an **Island Model**:

1. **Topology**: Spawn $K$ worker threads (typically $K = \text{availableProcessors} - 1$). Each thread owns an independent $(1+1)$ or $(1+\lambda)$ evolutionary engine and private `Evaluator` instance.
2. **Ring Migration**: Every $M$ generations (e.g., $M = 1,000$), each island sends its champion to its neighbor ($i \to (i+1) \pmod K$) via a thread-safe ring buffer or `ConcurrentLinkedQueue`.
3. **Immigration Acceptance**: An island evaluates the incoming immigrant. If the immigrant outperforms the island's current champion, it becomes the new local champion.
4. **Benefit**: Different islands explore distinct regions of the fitness landscape (different stroke configurations and color blendings). Migration allows one island to break through another's local plateau.

---

## 5. Perceptual Color Spaces & Edge-Weighted Kernels

### CIELAB $\Delta E$ Perceptual Distance
Euclidean distance in sRGB does not match human perception (the human eye is significantly more sensitive to green variations than blue, and non-linearly sensitive to luminance).
$$\Delta E_{76} = \sqrt{(L^*_1 - L^*_2)^2 + (a^*_1 - a^*_2)^2 + (b^*_1 - b^*_2)^2}$$
* **Implementation Tip**: Precompute an sRGB $\to$ Lab LUT (lookup table) or integer approximation to preserve zero-allocation high-speed evaluation in the inner loop.

### Sobel Edge-Weighted Evaluation
To prevent the algorithm from over-allocating strokes to flat backgrounds while blurring facial features or contours, compute a 2D edge salience mask using a Sobel operator ($G = \sqrt{G_x^2 + G_y^2}$):
$$\text{Fitness} = \sum_{x,y} \left( 1.0 + w_{\text{edge}} \cdot S(x, y) \right) \cdot \text{Error}(x, y)$$
where $S(x, y) \in [0, 1]$ is the normalized edge gradient magnitude, and $w_{\text{edge}}$ is a weighting factor (e.g. $1.5 \dots 3.0$).

---

## 6. Parsimony Pressure & Style Constraints

Standard error metrics incentivize the algorithm to add the maximum allowed number of strokes to shave off tiny pixel errors, resulting in high-density noise.

To encourage minimalist or painterly aesthetics, use **Parsimony Regularization**:
$$\text{TotalFitness} = \text{PixelError} + \alpha \cdot N_{\text{strokes}} + \beta \cdot \text{TotalArea}$$
* $\alpha$: Stroke count penalty coefficient. Allows dynamic insertion and deletion of strokes, finding the optimal balance between detail and simplicity.
* $\beta$: Area penalty to encourage broad watercolor glazes or delicate pointillist marks.

