---
name: teach
description: >-
  Author, extend, or review interactive HTML lessons (./lessons/*.html), reference cheatsheets (./reference/*.html),
  and learning records (./learning-records/*.md) in a teaching workspace. Use when teaching a topic, deconstructing a
  codebase into lessons, expanding a curriculum, or reviewing/auditing existing lessons for pedagogical quality and codebase alignment.
argument-hint: "What would you like to learn about?"
---

The user has asked you to teach them something or work on the teaching curriculum in this workspace. This is a stateful request - they intend to learn the topic over multiple sessions.

## Teaching Workspace

Treat the current directory as a teaching workspace. Workspace paths (`./lessons/` and the rest) resolve from the directory `/teach` was run in; only the `*-FORMAT.md` links resolve from this skill's folder. The state of their learning is captured in this directory in several files:

- `MISSION.md`: A document capturing the _reason_ the user is interested in the topic. This should be used to ground all teaching. Use the format in [MISSION-FORMAT.md](./MISSION-FORMAT.md).
- `./reference/*.html`: A directory of reference materials. These are the compressed learnings from the lessons - cheat sheets, reference algorithms, syntax, yoga poses, glossaries. They are the raw units of learning. They should be beautiful documents which print out well, and are designed for quick reference.
- `RESOURCES.md`: A list of resources which can be explored to ground your teaching in contextual knowledge, or to acquire knowledge and wisdom. Use the format in [RESOURCES-FORMAT.md](./RESOURCES-FORMAT.md).
- `./learning-records/*.md`: A directory of learning records, which capture what the user has learned. These are loosely equivalent to architectural decision records in software development - they capture non-obvious lessons and key insights that may need to be revised later, or drive future sessions. These should be used to calculate the zone of proximal development. They are titled `0001-<dash-case-name>.md`, where the number increments each time. Use the format in [LEARNING-RECORD-FORMAT.md](./LEARNING-RECORD-FORMAT.md).
- `./lessons/*.html`: A directory of lessons. A **lesson** is a single, self-contained HTML output that teaches one tightly-scoped thing tied to the mission. This is the primary unit of teaching in this workspace.
- `./assets/*`: Reusable **components** shared across lessons. See [Assets](#assets).
- `NOTES.md`: A scratchpad for you to jot down user preferences, or working notes.

## Philosophy

To learn at a deep level, the user needs three things:

- **Knowledge**, captured from high-quality, high-trust resources
- **Skills**, acquired through highly-relevant interactive lessons devised by you, based on the knowledge
- **Wisdom**, which comes from interacting with other learners and practitioners

Before the `RESOURCES.md` is well-populated, your focus should be to find high-quality resources which will help the user acquire knowledge. Never trust your parametric knowledge.

Some topics may require more skills than knowledge. Learning more about theoretical physics might be more knowledge-based. For yoga, more skills-based.

### Fluency vs Storage Strength

You should be careful to split between two types of learning:

- **Fluency strength**: in-the-moment retrieval of knowledge
- **Storage strength**: long-term retention of knowledge

Fluency can give the user an illusory sense of mastery, but storage strength is the real goal. Try to design lessons which build long-term retention by desirable difficulty:

- Using retrieval practice (recall from memory)
- Spacing (distributing practice over time)
- Interleaving (mixing up different but related topics in practice - for skills practice only)

## Lessons

A lesson is the main thing you produce: the unit in which knowledge and skills reach the user. Each lesson is one self-contained HTML file, saved to `./lessons/` and titled `0001-<dash-case-name>.html` where the number increments each time.

A lesson should be **beautiful**, with clean, readable typography and layout, since the user will return to these later to review. Think Tufte.

The lesson should be short, and completable very quickly. Learners' working memory is very small, and we need to stay within it. But each lesson should give the user a single tangible win that they can build on. It should be directly tied to the mission, and should be in the user's zone of proximal development.

If possible, open the lesson file for the user by running a CLI command. In sandboxed environments where launching external GUI applications via CLI is restricted (e.g., `open` failing with error -54), provide direct clickable `file://` links in your response.

Each lesson should link via HTML anchors to other lessons and reference documents.

Each lesson should recommend a primary source for the user to read or watch. This should be the most high-quality, high-trust resource you found on the topic.

Each lesson should contain a reminder to ask followup questions to the agent. The agent is their teacher, and can assist with anything that's unclear.

## Deconstructing Existing Codebases

When the user asks to turn an existing codebase or working application into a multi-part lesson series:

1. **Map Architectural Layers**:
   Deconstruct the application into progressive, incremental milestones rather than reviewing files arbitrarily:
   - *Stage 1: Setup & Environment* (Build scripts, native dependencies, entrypoint, linting).
   - *Stage 2: Domain & Representation* (Data classes, primitives, parameters, immutable chromosomes).
   - *Stage 3: Computation & Objectives* (Fitness functions, algorithmic transformations, memory allocation optimization).
   - *Stage 4: Engine & Concurrency* (Background worker threads, atomic synchronization, generational loop).
   - *Stage 5: Presentation & Interaction* (Canvases, live telemetry HUDs, keyboard shortcuts, exports).

2. **Establish Shared Course Assets Early**:
   Before generating multiple lessons, create `./assets/lesson-style.css` and reusable widgets (e.g., `./assets/quiz-widget.js`). Every lesson must share the same typography and component design.

3. **Unified Curriculum Navigation**:
   Include a course-level sidebar navigation menu in every lesson displaying the overall curriculum hierarchy alongside intra-lesson anchors, giving the learner persistent situational awareness.

4. **Interactive Intuition Preceding Code**:
   Include an interactive in-browser simulator, visualizer, or sandbox in the lesson HTML so the user grasps the conceptual behavior before reading and modifying backend code.

5. **Mathematical Notation & Formula Typesetting**:
   When lessons or reference cheatsheets (`./reference/*.html`) include mathematics (Bézier curves, objective functions, distance metrics, asymptotic complexity), include MathJax 3 in `<head>` so formulas render natively:
   ```html
   <!-- MathJax for LaTeX mathematical markup -->
   <script>
     window.MathJax = {
       tex: {
         inlineMath: [['\\(', '\\)']],
         displayMath: [['\\[', '\\]'], ['$$', '$$']]
       }
     };
   </script>
   <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"></script>
   ```
   Wrap standalone equations in `<div class="math-card">` (styled in `./assets/lesson-style.css`) with horizontal scrolling (`overflow-x: auto`) for mobile readability.

6. **Curriculum Expansion & Multi-Track Roadmapping**:
   When extending a completed foundational series into advanced topics or multi-track horizons:
   - **Organize by Thematic Tracks**: Group extension lessons into focused architectural tracks (e.g., Concurrency/Scaling, Objective & Fitness Modeling, Rendering & Serialization).
   - **Maintain Master Syllabus**: Document the entire curriculum in `./lessons/README.md` and `./NOTES.md`, detailing learning objectives, core concepts, and target source symbols.
   - **Synchronize Intra-Course Navigation**: Update the persistent sidebar navigation (`#sidebar .series-nav`) across all existing lesson HTML files with phase headers (`.nav-header`) and planned statuses (`.nav-planned`) so the learner maintains situational awareness of both completed foundations and upcoming roadmap milestones.
   - **Record in Learning Records & Mission**: Document the established curriculum expansion in `./learning-records/` and update `./MISSION.md` success criteria to reflect the new technical scope.

7. **Implementing Extension Lessons**:
   When authoring the extension lessons:
   - **Promote Planned Statuses to Active**: Transition `.nav-planned` spans to live hyperlinks (`<a href="...">`) across all existing and new lesson HTML files, marking the current lesson with `class="active"`.
   - **Sequential Lesson Footer Chaining**: Chain the `.lesson-nav` footer buttons (`<a class="nav-btn">`) smoothly across phase boundaries (e.g., the last foundational lesson links forward to the first extension lesson, and the final course capstone links to restart or review).
   - **Dedicated Interactive Simulators**: Each extension lesson must include an interactive in-browser canvas simulator with live telemetry metrics and controls to build spatial/algorithmic intuition before code.
   - **Formula Markup & Typesetting**: Always include MathJax 3 in `<head>` and wrap standalone equations in `<div class="math-card">` for mobile-responsive horizontal scrolling.
   - **Strict Quiz Formatting**: Strictly enforce identical word counts and balanced lengths across all multiple-choice options, varying the correct answer index across questions.

8. **Lesson Quality & Pedagogical Review Checklist**:
   When authoring or reviewing lessons (`./lessons/*.html` and `./reference/*.html`), verify every item below:
   - **Codebase Fidelity (Single Source of Truth)**:
     - Foundational lessons must match `src/`'s exact class names, constructor signatures, property names, probability weights, and mathematical metric (e.g., L2 SSD vs. L1 Manhattan).
     - When an introductory snippet simplifies the production architecture (e.g., a single-file starter `Main.kt`), add an explicit **architectural bridge callout** explaining how the starter maps to the multi-file codebase.
     - When Phase 2+ extension lessons design drop-in modules not yet in `src/`, explicitly announce the shift from *deconstructing existing files* to *designing extension modules* at the phase boundary, and call the exact Phase 1 symbols (`target.pixelBytes`, `Painting.createRandom`, `StrokeGene` fields) rather than inventing incompatible method or property names.
   - **Complete Core Algorithms**:
     - Write out the core equations, kernels, and serialization routines that a lesson or section is named after in full (e.g., the 6-line $3\times 3$ Sobel convolution in a Sobel lesson, or `.gene` save/load functions in a checkpointing section) rather than eliding the centerpiece logic with `// ... omitted for brevity`.
   - **Simulator Authenticity (Zero Potemkin Controls)**:
     - Wire every `<select>`, `<input>`, mode button, and draggable handle in an in-browser simulator to a live event listener and genuine mathematical calculation.
     - Simulators must demonstrate the visual phenomenon taught in the prose (e.g., multi-island or hill-climber canvases converging toward a visual target scene, pyramid level selectors visibly changing raster resolution and step throughput, and residual heatmaps computing actual `|Target − Evolved|` pixel differences).
   - **HTML & Asset Hygiene**:
     - Keep shared typography and component styles in `./assets/lesson-style.css`; limit inline `<style>` blocks to lesson-specific simulator layout rules.
     - Verify every `<div class="callout ...">` tag is closed before the next `<section>` (an unclosed callout swallows subsequent sections).
   - **HiDPI & Retina Display Scaling**:
     - In desktop GUI code (Skiko/AWT/Swing), always implement and teach the canonical canvas scaling pattern (`canvas.scale(skiaLayer.contentScale, skiaLayer.contentScale)`), keeping fonts, shapes, stroke widths, and window layouts proportional and crisp across all display scaling ratios.
     - In interactive in-browser `<canvas>` simulators, always back the canvas buffer with `window.devicePixelRatio` and normalize mouse coordinates (`(clientX - rect.left) * (logicalW / rect.width)`) to eliminate blurry rasterization and cursor desynchronization on Retina displays.

9. **Stepwise Curriculum Verification & Runnable Codebases**:
   When converting a curriculum into runnable exercises or verifying that each lesson generates working software:
   - **Sequential Step Subdirectories**: Scaffold an isolated subdirectory under `./steps/` for each lesson (e.g., `./steps/0001-<lesson-slug>/`, `./steps/0002-<lesson-slug>/`). Each step directory must be completely self-contained with its own build files, wrapper scripts, sources, and tests.
   - **Cumulative Build Progression**: Start at Lesson 1 and confirm that it compiles, passes style linting, passes unit tests, and executes a non-blocking smoke test (e.g., `./gradlew run --args="--headless"`). Once verified, copy that directory forward to the next lesson's directory, apply that lesson's increments, and verify again. Repeat across all lessons in sequence.
   - **Incorporate Automated Tests Directly into Lessons**: Unit tests verifying the lesson's additions must not only reside in `src/test/`, but must be incorporated directly into the lesson HTML file:
     - Add a dedicated "Automated Testing" section before the Knowledge Check.
     - Add the section link to the lesson's sidebar navigation (`#sidebar .series-nav`).
     - Provide complete, copyable test code with exact assertions and the terminal command to run them (e.g., `./gradlew test --tests "..."`).
     - This gives learners self-efficacy, enabling them to test-drive their progress and confirm that their implementation works before moving forward.

## Assets

Lessons are built from reusable **components**, stored in `./assets/`: stylesheets (with shared styles like `.math-card`), quiz widgets, simulators, diagram helpers, and anything else a second lesson could reuse.

Reuse is the default, not the exception. Before authoring a lesson, read `./assets/` and build from the components already there. When a lesson needs something new and reusable, write it as a component in `./assets/` and link to it; never inline code a future lesson would duplicate.

A shared stylesheet is the first component every workspace earns: every lesson links it, so the lessons look like one consistent course rather than a pile of one-offs. As the workspace grows, so should the component library.

## The Mission

Every lesson should be tied into the mission - the reason that the user is interested in learning about the topic.

If the user is unclear about the mission, or the `MISSION.md` is not populated, your first job should be to question the user on why they want to learn this.

Failing to understand the mission will mean knowledge acquisition is not grounded in real-world goals. Lessons will feel too abstract. You will have no way of judging what the user should do next.

Missions may change as the user develops more skills and knowledge. This is normal - make sure to update the `MISSION.md` and add a learning record to capture the change. Confirm with the user before changing the mission.

## Zone Of Proximal Development

Each lesson, the user should always feel as if they are being challenged 'just enough'.

The user may specify an exact thing they want to learn. If they don't, figure out their zone of proximal development by:

- Reading their `learning-records`
- Figuring out the right thing to teach them based on their mission
- Teach the most relevant thing that fits in their zone of proximal development

## Knowledge

Lessons should be designed around a skill the user is going to learn. The knowledge in the lesson should be only what's required to acquire that skill. You teach the knowledge first, then get the user to practice the skills via an interactive feedback loop.

Knowledge should first be gathered from trusted resources. Use `RESOURCES.md` to keep track of them. Lessons should be littered with citations - links to external resources to back up any claim made. This increases the trustworthiness of the lesson.

For acquiring knowledge, difficulty is the enemy. It eats working memory you need for understanding.

## Skills

If knowledge is all about acquisition, skills are about durability and flexibility. Make the knowledge stick.

For skill acquisition, difficulty is the tool. Effortful retrieval is what builds storage strength. Skills should be taught through interactive lessons. There are several tools at your disposal:

- Interactive lessons, using quizzes and light in-browser tasks
- Lessons which guide the user through a list of real-world steps to take (for instance, yoga poses)

Each of these should be based on a **feedback loop**, where the user receives feedback on their performance. This feedback loop should be as tight as possible, giving feedback immediately - and ideally automatically.

For quizzes, each answer should be exactly the same number of words (and characters, if possible). Vary which position holds the correct answer across questions. Don't give the user any clues about the answer through formatting or order.

## Acquiring Wisdom

Wisdom comes from true real-world interaction - testing your skills outside the learning environment.

When the user asks a question that appears to require wisdom, your default posture should be to attempt to answer - but to ultimately delegate to a **community**.

A community is a place (online or offline) where the user can test their skills in the real world. This might be a forum, a subreddit, a real-world class (budget permitting) or a local interest group.

You should attempt to find high-reputation communities the user can join. If the user expresses a preference that they don't want to join a community, respect it.

## Reference Documents

While creating lessons, you should also create reference documents. Lessons can reference these documents - they are useful for tracking raw units of knowledge useful across lessons.

Lessons will rarely be revisited later - reference documents will be. They should be the compressed essence of the lesson, in a format designed for quick reference.

Some learning topics lend themselves to reference:

- Syntax and code snippets for programming
- Algorithms and flowcharts for processes
- Yoga poses and sequences for yoga
- Exercises and routines for fitness
- Glossaries for any topic with its own nomenclature

Glossaries, in particular, are an essential reference. Once one is created, it should be adhered to in every lesson.

## `NOTES.md`

The user will sometimes express preferences of how they want to be taught, or things you should keep in mind. This is the place to record those preferences, so you can refer back to them when designing lessons or working with the user.
