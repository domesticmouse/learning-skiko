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

### 2. Fitness Metrics

### L2 Sum of Squared Differences (SSD) — Primary Metric
The standard quadratic metric used across `Evaluator.kt` and the curriculum:
$$\text{SSD} = \sum_{x,y} \left( (R_t - R_c)^2 + (G_t - G_c)^2 + (B_t - B_c)^2 \right)$$

* **Pros**: Modern JVMs auto-vectorize simple integer squaring loops. Squaring heavily penalizes large color mismatches ($2\times$ error $\to 4\times$ penalty), ensuring major background blocks and contrast edges establish rapidly before micro-refinement.
* **64-Bit Accumulation**: Always accumulate into a `Long` (`var sumSquaredDiff = 0L`) because a $256 \times 256$ canvas has a maximum SSD of $65,536 \times 3 \times 255^2 \approx 1.27 \times 10^{10}$, which overflows a 32-bit signed `Int`.
* **Kotlin Signed Byte Masking & Stride**: `allocN32Pixels` uses 4-byte `BGRA_8888` layout (`i += 4`). Because Kotlin `Byte` is signed ($-128 \dots 127$), always mask with `and 0xFF` before subtracting:
  ```kotlin
  val db = (candidateBytes[i].toInt() and 0xFF) - (targetBytes[i].toInt() and 0xFF)
  val dg = (candidateBytes[i + 1].toInt() and 0xFF) - (targetBytes[i + 1].toInt() and 0xFF)
  val dr = (candidateBytes[i + 2].toInt() and 0xFF) - (targetBytes[i + 2].toInt() and 0xFF)
  sumSquaredDiff += (db * db + dg * dg + dr * dr)
  ```

### L1 Manhattan Distance (Alternative Linear Metric)
$$\text{L1} = \sum_{x,y} \left( |R_t - R_c| + |G_t - G_c| + |B_t - B_c| \right)$$

* **Trade-off**: Penalizes all deviations linearly; less aggressive on high-contrast outliers than L2 SSD.

### Subsampling for Ultra-High Speed
To accelerate early generations or large images ($512\times 512$+):
- Evaluate only every 2nd or 4th pixel (`step = 8` or `step = 16`).
- Once improvement plateaus, switch to full-resolution evaluation.

---

## 3. Advanced Mutation Operators

1. **Step-Size Annealing + Rechenberg's 1/5th Rule**:
   - Combine exponential cooling $\sigma_{\text{base}}(t) = \sigma_{\min} + (\sigma_0 - \sigma_{\min}) \exp(-t/\tau)$ with a rolling `adaptiveGain` multiplier updated every 500–1,000 evaluations (`currentSigma = (baseSigma * adaptiveGain).coerceIn(minSigma, initialSigma)`).
2. **Variable-Length Structural Mutations (Add / Remove Stroke)**:
   - Periodically insert or delete a stroke so parsimony regularization can prune redundant strokes.
3. **Layer Ordering (Z-Index Mutation)**:
   - Swapping stroke indices in the list changes which stroke is rendered on top.

---

## 4. Multi-Core Island Model & Ring Migration

When scaling evolution across multiple CPU cores, sharing a single mutable champion creates cache thrashing and lock contention. Instead, use an **Island Model**:

1. **Topology**: Spawn $K$ worker threads. Each thread owns an independent $(1+1)$ evolutionary loop and a private `Evaluator` instance (since Skia's `Bitmap`, `Canvas`, `Paint`, and `Path` wrap mutable native pointers and are not thread-safe).
2. **Ring Migration**: Every $M$ generations (e.g., $M = 2,000$), each island deposits its `localBest` into `migrationInboxes[(id + 1) % numIslands]`.
3. **Immigration Acceptance & Global Champion**: Because `Painting` caches its `error: Long`, compare `migrant.error < localBest.error` directly without re-rasterizing, and update `globalChampion` lock-free via `accumulateAndGet`.

---

## 5. Perceptual Color Spaces & Edge-Weighted Kernels

### CIELAB $\Delta E$ Perceptual Distance
Euclidean distance in sRGB does not match human perception (the human eye is significantly more sensitive to green variations than blue, and non-linearly sensitive to luminance).
$$\Delta E_{76} = \sqrt{(L^*_1 - L^*_2)^2 + (a^*_1 - a^*_2)^2 + (b^*_1 - b^*_2)^2}$$
* **3D LUT Indexing**: Precompute a 5-bit quantized $32 \times 32 \times 32$ lookup table (`FloatArray(32 * 32 * 32 * 3)`). Multiply the packed 15-bit index by `3` to stride over `[L*, a*, b*]`:
  `val baseIdx = (((r ushr 3 and 0x1F) shl 10) or ((g ushr 3 and 0x1F) shl 5) or (b ushr 3 and 0x1F)) * 3`

### Sobel Edge-Weighted Evaluation
To prevent the algorithm from over-allocating strokes to flat backgrounds while blurring facial features or contours, compute a 2D edge salience mask using a $3\times 3$ Sobel operator ($G = \sqrt{G_x^2 + G_y^2}$):
$$\text{Fitness} = \sum_{x,y} \left( 1.0 + \gamma \cdot G(x, y) \right) \cdot \Delta E(x, y)$$

---

## 6. Parsimony Pressure & Style Constraints

Standard error metrics incentivize the algorithm to use every available stroke to shave off tiny pixel errors, resulting in high-density micro-clutter.

To encourage minimalist or painterly aesthetics, use **Parsimony Regularization**:
$$\text{TotalFitness} = \text{PixelSSD} + \alpha \cdot N_{\text{strokes}} + \beta \cdot \text{TotalArea}$$
* **Variable-Length Requirement**: $\alpha \cdot N_{\text{strokes}}$ only exerts selection pressure when `Painting.mutate()` includes structural **Add Stroke** and **Remove Stroke** branches; with a fixed stroke count, the penalty is a constant offset that cancels out between parent and child.

---

## 7. Vector Primitives & W3C SVG XML Serialization

### Primitives Mapping
| Primitive | Geometric Degrees of Freedom | Skia Path Command | W3C SVG Equivalent |
| :--- | :--- | :--- | :--- |
| **Quadratic Bézier** | 11: $(x_0, y_0), (x_c, y_c), (x_1, y_1), w, r, g, b, a$ | `path.quadTo(xc, yc, x1, y1)` | `<path d="M x0,y0 Q xc,yc x1,y1" stroke="rgb(r,g,b)" stroke-opacity="a" stroke-width="w"/>` |
| **Cubic Bézier** | 10: $(x_0, y_0), (x_{c1}, y_{c1}), (x_{c2}, y_{c2}), (x_3, y_3), w, \text{ARGB}$ | `path.cubicTo(cx1, cy1, cx2, cy2, x3, y3)` | `<path d="M x0,y0 C cx1,cy1 cx2,cy2 x3,y3" .../>` |
| **Convex Polygon** | Polar $(\theta_i, r_i)$ sorted by $\theta_i$ around $(c_x, c_y)$ + $\text{ARGB}$ | `path.lineTo(...)` + `path.closePath()` | `<polygon points="x0,y0 x1,y1 ..." fill="..."/>` |

### Deterministic Checkpointing & CLI Time-Lapse Assembly
1. **Chromosome Checkpoint Format (`.gene`)**: Serialize `Painting` to a line-oriented `.gene` file (`# GENETIC_ART_V1 error=<Long> strokes=<Int>` followed by `x0,y0,xc,yc,x1,y1,strokeWidth,r,g,b,a` per line) to allow pause/resume and headless rendering at $4\text{K}$ resolution.
2. **FFmpeg CLI Video Encoding**:
   ```bash
   ffmpeg -framerate 60 -i frame_%05d.png \
          -c:v libx264 -preset slow -crf 18 -pix_fmt yuv420p \
          timelapse.mp4
   ```


