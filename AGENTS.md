# AGENTS.md

## Repository status
This repository must be treated as a conservative Forge64 migration target.

Important starting-point rule:
- The authoritative starting point is the state immediately after the codebase was transplanted into a Forge64 workspace.
- Do NOT treat later manual experiments, temporary edits, emergency stubs, exploratory migration attempts, or accidental breakages as authoritative intent.
- If later code conflicts with the migration policy written here, prefer this file unless the code clearly proves a required compatibility fix.

## Target environment
- Platform: Forge 64
- Dependency line: net.minecraftforge:forge:26.1.2-64.0.0
- Java: 25

## Source tree assumption
- Code under src/main/java is old-version Forge code.
- Treat src as a transplanted legacy Forge codebase that still contains old API assumptions.
- Do not assume the source tree is already Forge64-native.
- compileJava may pass while runClient still fails due to runtime module/package conflicts. Treat those as migration work, not redesign work.

## Primary migration objective
First objective:
- Make compileJava pass on Forge64 / Java 25.

Second objective:
- Reach a stable runClient launch point.

Current runtime target:
- Pass mod loading and continue toward title-screen reachability.

Do not move into redesign or feature work before compatibility restoration.

## Most important behavioral constraint
This task is a version migration task only.

Allowed:
- Code changes required strictly for API compatibility
- Signature updates
- Import/path updates
- Registration updates
- Build-system corrections
- Minimal compatibility shims
- Temporary stubs only for optional non-core integrations when no safer option exists

Forbidden:
- Behavior changes unless strictly unavoidable for compile/runtime compatibility
- Gameplay redesign
- Balance changes
- New features
- Logic rewrites that alter intended TaCZ behavior
- Silent functional simplification of core systems
- Cleanup for its own sake
- “Improvement” edits unrelated to migration blockers

When choosing between two fixes:
- Prefer the one that preserves old behavior more closely.
- Prefer the narrower diff.
- Prefer fixing usage sites over introducing broad abstractions.

## Asset policy
Assets are off-limits.

Do not modify:
- textures
- models
- animations
- sounds
- asset metadata
- bundled art resources

No direct or indirect asset editing is allowed.

## Functional scope constraints
Preserve TaCZ core behavior as much as possible.

Allowed later modification areas in the overall project:
- ammo system
- ammo acceptance rules
- input handling
- UI

However, for this migration phase:
- do not implement those redesigns yet
- do not expand those systems
- only restore compatibility

Non-target areas unless strictly required for compatibility:
- core shooting behavior
- reload behavior
- animation systems
- models
- assets

Areas that are especially sensitive and must not be redesigned:
- NBT accessor layer
- gun core logic
- firing state flow
- reload state flow
- animation/model behavior
- runtime resource behavior beyond compatibility restoration

## Self-directed execution policy
You should proceed autonomously in small steps until one of the stop conditions in this file is reached.

Default working mode:
1. inspect the latest compile/run blocker
2. fix the current blocker with minimal edits
3. rerun the relevant command
4. continue to the next blocker
5. stop only when a listed stop condition is hit, or when the current objective is reached

You do NOT need confirmation for ordinary small compatibility edits.

You SHOULD continue on your own when:
- the next blocker is a normal compile error cluster
- the next blocker is a normal runtime compatibility error
- the fix is local and behavior-preserving
- the fix does not require large deletion, large rewrite, or risky normalization

## Mandatory stop conditions
STOP immediately and report instead of continuing if any of the following is true:
- a fix appears to require deletion of multiple files
- a fix appears to require deletion of an entire folder or subsystem
- a fix appears to require removing a public API
- a fix appears to require broad rewrites across many files
- a fix appears to require redesign of a non-target system
- a fix appears to require behavior change rather than compatibility repair
- a fix appears to require touching assets
- a file appears corrupted, mismatched, duplicated, emptied, or replaced with the wrong type definition
- encoding problems appear
- package/module namespace conflicts suggest more than one possible deletion target and the safe target is not obvious

When stopping, report:
- target files
- reason
- whether core behavior may be affected
- whether compileJava currently passes
- whether runClient currently reaches mod loading
- the narrowest known next action

## Removal / stubbing policy
You may remove, isolate, guard, or temporarily stub ONLY when all of the following are true:
1. the component is optional or compatibility-only
2. it blocks migration
3. removing it does not change TaCZ core behavior
4. it is not needed for later TaCZ:WAVE core integration

Examples of likely-removable categories:
- optional compat layers
- KubeJS bridge code
- AR / accelerated rendering compatibility
- renderer optimization hooks
- non-core external integrations
- convenience integrations

Do NOT remove or stub:
- core gun logic
- firing logic
- reload logic
- essential registration paths
- code that materially changes TaCZ’s intended gameplay behavior

If uncertain, preserve behavior and choose the narrower compatibility edit.

## Large deletion safety rule
If a fix requires:
- deletion of multiple files
- deletion of entire folders
- removal of public APIs
- removal of systems that may affect runtime structure
- broad removal of “old remnants” or “unused code”

Then:
- STOP immediately
- Do NOT proceed with deletion
- Report the candidate deletion set and why it seems necessary
- Wait for explicit confirmation before continuing

Do not perform broad deletion just because code looks obsolete.

## Stub / namespace conflict rule
Be extremely careful with classes placed under:
- net.minecraft.*
- net.minecraftforge.*

Rules:
- Do NOT introduce new classes under those namespaces unless strictly required for compilation and no safer local compatibility layer is possible.
- If a local class under those namespaces conflicts with runtime-provided classes, prefer removing the local class and adapting usage sites.
- Resolve namespace/package export conflicts one file at a time.
- Do NOT mass-delete net.minecraft.* stubs or net.minecraftforge.* stubs in one step unless explicitly approved.

When a runtime conflict occurs:
1. identify the exact package named in the error
2. identify the exact local file exporting that package
3. remove or replace only the file(s) directly responsible
4. rerun runClient
5. continue iteratively

## NBT accessor safety rule
NBT accessor files are high-risk.
Do NOT redesign, replace, consolidate, or broadly rewrite the NBT accessor layer.

Forbidden in this layer:
- redesign
- mass renaming
- file merging/splitting without necessity
- broad normalization rewrites
- content replacement based on guesswork

If an accessor file appears mismatched or corrupted:
- STOP and report
- compare the affected file(s)
- restore file/type alignment with the smallest possible fix

## Encoding and file integrity rule
Encoding incidents have happened before. Treat them as critical.

Strictly forbidden:
- bulk auto-replacement across files
- encoding-wide rewrites
- UTF-8 normalization across existing files
- rewriting an entire file to fix a local issue
- resaving many non-ASCII files without necessity

If encoding or corruption is suspected:
- STOP
- identify the first affected file(s)
- report the likely cause
- use the smallest file-level or line-level repair possible

Non-ASCII comments in existing files are not a reason to rewrite the file.

## Editing policy
Prefer:
- small, reviewable commits
- minimal compatibility changes
- explicit API-porting edits
- preserving old behavior
- narrow diffs
- local fixes over framework-like abstractions

Avoid:
- broad refactors
- style-only rewrites
- renaming without necessity
- speculative cleanup
- “improvements” that alter behavior
- replacing a precise local fix with a generalized system

## Compile / runtime workflow
Preferred order:
1. restore compile compatibility
2. rerun compileJava
3. if compileJava passes, run runClient
4. fix the first runtime blocker
5. rerun runClient
6. continue until the next hard blocker or objective completion

When compileJava is broken:
- prioritize getting compileJava green again before chasing runtime issues

When runClient is broken:
- prioritize the first concrete runtime blocker shown in the log
- fix one blocker at a time
- rerun runClient after each meaningful fix

## Compat trimming strategy
Optional compat should be trimmed conservatively and only when it blocks migration.

Likely low-priority / removable if blocking:
- compat/kubejs
- compat/ar
- renderer optimization integrations
- non-core third-party bridges

But:
- do not mass-remove optional systems without checking stop conditions
- do not remove compat and unrelated code in the same step

## Build commands
Primary:
- .\gradlew.bat build --console=plain

If needed:
- .\gradlew.bat compileJava --stacktrace

Runtime:
- .\gradlew.bat runClient --stacktrace --console=plain

Optional:
- .\gradlew.bat clean

## Allowed command policy
Rules:
- Do NOT use unnecessary Gradle tasks.
- Do NOT run full test suites unless explicitly required.
- Do NOT use --refresh-dependencies unless dependency resolution is clearly broken.
- Avoid repeated clean unless needed to fix a build-state issue.
- Do not spam repeated builds without code changes.
- Keep build iterations fast and deterministic.

## Reporting policy
When reporting progress, include:
- whether compileJava passes
- whether runClient reaches mod loading
- the current first blocker
- files changed in the last step
- whether any temporary stub or shim was added
- whether any file was removed
- whether any stop condition was triggered

If you stopped because of a safety rule, say so explicitly.

## PR policy
Open a PR from the working branch to the requested base branch.

In the PR description, include:
- which error clusters were fixed
- which optional compat layers were removed/isolated
- whether any temporary stubs were introduced
- whether any namespace-conflict files were removed
- confirmation that asset files were not touched
- confirmation that the intent was behavior-preserving migration only
- remaining blockers, if any

## Final instruction
This is not a redesign task.
This is not a cleanup task.
This is not a feature task.

This is a conservative Forge64 / Java25 migration task with:
- behavior preservation
- minimal diffs
- zero asset edits
- iterative compile/run restoration
- controlled deletion
- controlled handling of namespace conflicts
- mandatory stop/report behavior for risky operations
