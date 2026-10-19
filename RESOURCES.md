# Genetic Art & Skiko Resources

## Knowledge

- [Skiko GitHub Repository & Documentation](https://github.com/JetBrains/skiko)
  Official Kotlin Multiplatform Skia bindings by JetBrains. Use for: SkikoRenderDelegate, SkiaLayer configuration, native binaries setup, and canvas API.
- [Google Skia Documentation & Architecture](https://skia.org/docs/)
  Foundational C++ 2D graphics engine documentation. Use for: Paint, Path, Canvas operations, Color models, and hardware acceleration principles.
- [Paper: Roger Alsing's "Evolution of Mona Lisa" (2008)](https://rogeralsing.com/2008/12/07/genetic-programming-evolution-of-mona-lisa/)
  The seminal implementation of image approximation using genetic hill climbing with geometric primitives (polygons). Use for: understanding (1+1) evolutionary strategies, mutation rates, and convergence behavior.
- [In-Depth Guide: Fast Offscreen Image Comparison in Java/Kotlin (L1 vs L2 Error Metrics)](https://en.wikipedia.org/wiki/Mean_squared_error)
  Mathematical foundations for image fitness evaluation: Manhattan (L1) distance vs Mean Squared Error (L2). Use for: designing high-throughput evaluator loops without GC pressure.

## Wisdom (Communities)

- [Kotlinlang Slack (#skiko, #graphics, #feed)](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up)
  Official JetBrains Kotlin community with active channels for Skiko and graphics programming. Use for: resolving low-level native rendering issues or Skiko version quirks.
- [r/geneticprogramming & r/generative](https://www.reddit.com/r/generative/)
  High-signal subreddits for evolutionary algorithms and procedural generative art. Use for: creative critique, chromosome variations, and visual optimization techniques.

## Gaps
- Low-level tutorials detailing standalone Skiko (without Compose Desktop) remain sparse in the broader ecosystem; our reference guides will explicitly document standalone SkiaLayer patterns.
