# IMPLEMENTED: IntelliJ XDebugger integration

**Status: built (milestone 6).** The implementation lives in
`src/main/java/com/intellij/plugins/haxe/hashlink/` — deliberately its own
package, fully separated from the legacy Flash/hxcpp debugger code, which is
untouched. Two deviations from the plan below, both deliberate:

1. **Separate runners instead of a branch in HaxeDebugRunner.** The plan wanted
   an HL branch inside `HaxeDebugRunner.doExecute`; the user asked for strict
   separation of the experimental HashLink support from the existing debugger.
   `HashLinkRunner` (plain Run) and `HashLinkDebugRunner` (Debug) are registered
   with `order="first"` and their `canRun` claims ONLY plain Haxe application
   configurations whose compilation target is HL (no NME/OpenFL, no custom
   executable) — every other configuration falls through to the untouched
   generic runners, so there is no runner race.
2. **Real Variables view instead of the placeholder.** The plan predates the
   scopes/variables milestones; frames now list the Locals scope inline and
   further scopes (Statics) as lazy groups, with expandable objects/arrays/enums.

## Manual test checklist (runIde)

Setup: Haxe SDK configured; SDK "HashLink executable" set (or HASHLINK_BIN env,
or hl on PATH); a module with target HL compiled with `-debug`.

- Plain Run executes `hl <output.hl>`, console shows program output, exit code.
- Debug: breakpoint in a `.hx` line with code → verified icon after launch;
  program stops there; Frames panel shows the stack with correct source lines.
- Variables: locals with values; expand an object/array/enum; Statics group.
- Step over/into/out move as expected; Resume runs to the next breakpoint.
- Program runs to completion → console output, exit code line, session ends.
- Stop button mid-run kills debuggee + adapter (no orphan hl.exe).
- Negatives: no hl configured → error naming the SDK field; missing/stale .hl
  → "build the module first" error; breakpoint on a comment line → invalid icon.

---

The original (pre-implementation) plan follows for reference.

## Decisions already made (by the user)

- **hl executable location:** store on the **Haxe SDK additional data** — add an
  `hlBinPath` field mirroring `nekoBinPath` in
  `common/src/main/java/com/intellij/plugins/haxe/config/sdk/impl/HaxeSdkAdditionalDataBaseImpl.java`
  and its interface `HaxeSdkAdditionalDataBase.java`; it is persisted via
  `src/main/java/com/intellij/plugins/haxe/config/sdk/HaxeSdkData.java`. Read it at
  debug time via `((HaxeSdkData) sdk.getSdkAdditionalData()).getHlBinPath()`, the
  way `NekoRunningState` reads `getNekoBinPath()`. Add a field to the SDK
  configurable panel so the user can set it.
- **Run scope:** cover **both Debug and plain Run** for HL. Besides the debug
  runner branch, also fix `HaxeRunner` (the `DefaultRunExecutor` runner) so a plain
  Run of an HL target launches `hl <program.hl>` instead of throwing
  `haxe.run.wrong.target`.
- **Stepping:** the adapter now provides real step over/into/out, so the IntelliJ
  process wires those through to `next`/`stepIn`/`stepOut` DAP requests (not the
  no-op that was originally considered when stepping was unavailable).
- **Variables view:** out of scope until the adapter/protocol gains
  scopes/variables/evaluate (a later milestone). Show a "not available yet"
  placeholder node in the Variables panel; `XStackFrame.computeChildren` must add an
  empty/placeholder node (do NOT leave it unimplemented — that spins the UI).

## Architecture (from exploration of the plugin)

All plugin-side code lives in the ROOT module under
`src/main/java/com/intellij/plugins/haxe/...`. The Java DAP client + protocol are
already on the plugin classpath (`hashlink-debug-adapter` is a `pluginModule` in the
root `build.gradle.kts`), so `com.intellij.plugins.haxe.runner.debugger.dap.*` is
directly importable.

### Runner placement
Add an HL branch **inside `HaxeDebugRunner.doExecute`** rather than a second
`programRunner` (IntelliJ picks exactly one runner per executor+profile; a second
matching runner would race). `HaxeDebugRunner` already dispatches Flash vs hxcpp by
build system/target; HL currently falls through to the terminal
`else { throw ExecutionException("haxe.proper.debug.targets") }` — that is the clean
insertion point. Detect HL with
`HaxeModuleSettings.getInstance(module).getCompilationTarget() == HaxeTarget.HL`
(use `getCompilationTarget()`, which honours HXML `-hl` args), placed ahead of the
flash/hxcpp block. Delegate to a new `runHashLink(...)` that starts the session with
the same `XDebuggerManager.getInstance(project).startSession(env, starter)` pattern
as `runHxcpp`.

### New files (`src/main/java/com/intellij/plugins/haxe/runner/debugger/hl/`)
- `HashLinkDebugProcess extends XDebugProcess` — owns the adapter `Process`, the
  `DapClient`, the event-pump thread, breakpoint bookkeeping, and the XDebugger
  callbacks. Inner: `HashLinkSuspendContext extends XSuspendContext`,
  `HashLinkExecutionStack extends XExecutionStack`, `HashLinkStackFrame extends
  XStackFrame`, and the `XBreakpointHandler` bound to `HaxeBreakpointType.class`.
- `HashLinkAdapterLauncher` — resolve bundled adapter `.hl`, resolve hl, spawn
  `hl adapter.hl --port 0` (`redirectErrorStream(true)`), read stdout until
  `DAP-ADAPTER-LISTENING:<port>`, return `{Process, port}`. Mirrors
  `DebugLifecycleIntegrationTest.awaitListeningPort()`/`startAdapter()`.
- `HlExecutableResolver` (plugin copy) — the module's resolver is test-only and not
  shipped. Order: SDK `hlBinPath` (the chosen storage) → env
  `HASHLINK_BIN`/`HASHLINK`/`HASHLINKPATH` → `PATH`. Make env lookup injectable for
  unit testing.

### Reuse (no change)
- `HaxeBreakpointType` (gated on `HaxeFileType`, already covers `.hl` sources).
- `HaxeDebuggerEditorsProvider` / `HaxeDebuggerSupportUtils`.
- Bundled adapter at `<pluginDir>/adapter/hl-debug-adapter.hl` via
  `PluginManagerCore.getPlugin(PluginId.getId("com.intellij.plugins.haxe")).getPluginPath()`.
- Compiled `.hl` output via `HaxeCompilerUtil.calculateCompilerOutput(module)` (or
  the run-config custom-file override).

### `HashLinkDebugProcess` method design
- Constructor stores fields, builds the breakpoint handler, wraps the adapter
  process in a `ProcessHandler`. Does not block.
- `start()` (off-EDT — every DAP request blocks): initialize (adapterID
  `intellij-haxe`) → drain `initialized` via `pollEvent` → launch (program = `.hl`,
  hlPath = resolved hl, cwd) → per-file `setBreakpoints` for already-registered
  breakpoints → `configurationDone` → start the event-pump thread. On
  `!launch.isSuccess()` print `getMessage()` and stop.
- **Event pump** (pooled thread, the only `pollEvent` caller): loop `pollEvent`;
  `stopped` → `sendRequest(ThreadsRequest)` + `sendRequest(StackTraceRequest)` inline
  (single thread, correlated by `request_seq`, no deadlock) → build suspend context
  → `getSession().positionReached(ctx)` (background-safe); `output` →
  `AdapterProcessHandler.notifyTextAvailable(text, STDOUT/STDERR)`; `exited` →
  remember code; `terminated` → `getSession().stop()` + terminate + close;
  `breakpoint` → verify/update presentation.
- `resume` → `ContinueRequest(currentThreadId)` via a single-thread request executor
  (never block EDT). `startStepOver`/`startStepInto`/`startStepOut` →
  `NextRequest`/`StepInRequest`/`StepOutRequest`. `stop` → best-effort
  `DisconnectRequest` + `client.close()` + `process.destroy()/destroyForcibly()`.
- **Breakpoints** (`setBreakpoints` is whole-file): keep
  `Map<String path, LinkedHashSet<XLineBreakpoint>>` + `Map<XLineBreakpoint, Integer
  dapId>` under a lock. register/unregister mutate the map and schedule a per-file
  flush off-EDT; line sent = `getLine()+1`. On `SetBreakpointsResponse` iterate
  `body.breakpoints` in request order → `setBreakpointVerified`/invalid icon; store
  `id`. Before `configurationDone`, register/unregister only mutate; `start()` flushes
  once.
- **Suspend/stack** from `StackTraceResponse.body.stackFrames`:
  `HashLinkExecutionStack.computeStackFrames(first, container)` →
  `container.addStackFrames(frames.subList(first, frames.size()), true)` (use
  `frames.size()`, NOT hxcpp's `size()-1`). `HashLinkStackFrame.getSourcePosition()`
  → resolve `VirtualFile` from `source.path` (absolute) →
  `XSourcePositionImpl.create(file, line-1)`; fall back to hxcpp's
  `FilenameIndex`/`HaxelibClasspathUtils.findFileOnClasspath` cascade (consider
  extracting that into a shared static helper). `computeChildren` → placeholder node.
  `getEvaluator()` → null (no evaluate).
- **Console/ProcessHandler:** debuggee stdout/stderr arrive as DAP `OutputEvent`s
  (the adapter owns the child), not from a child ProcessHandler. Wrap the ADAPTER
  process in an `AdapterProcessHandler` for the console + Stop; forward
  `OutputEvent`s into it via `notifyTextAvailable`. `doGetProcessHandler()` returns
  it; `createConsole()` can use the default.

### Threading
- EDT calls (resume/step/stop/register/unregister) must not block; each submits to a
  single-thread `requestExecutor`. The event-pump thread is the sole `pollEvent`
  caller and issues its own `sendRequest`s (threads/stackTrace). `DapClient` uses an
  atomic seq + concurrent pending-response map, so concurrent `sendRequest` from the
  pump and the executor is safe; guard only OUR maps.

### Config / build
- New bean fields on `HaxeApplicationConfiguration` if needed (auto-serialized via
  `XmlSerializer`); the hl path itself lives on the SDK, not the run config.
- program `.hl`: custom-file override or `HaxeCompilerUtil.calculateCompilerOutput`.
- Verify in `runIde` that Debug/Run compiles first so the `.hl` exists (Flash/hxcpp
  rely on the default Build before-run task); `saveAllDocuments()` before launch.

### plugin.xml
No new `programRunner` and no new breakpoint type — reuse the registered
`HaxeDebugRunner` and `HaxeBreakpointType`. Only add new `HaxeBundle.properties`
error keys (`haxe.hl.executable.not.found`, `haxe.hl.adapter.missing`,
`haxe.hl.output.missing`).

### Tests
- Plugin-side (root module, JUnit4 IntelliJ platform framework): hl resolver
  (env precedence, injectable), and compiled-output-path glue. The DAP protocol
  itself is already covered by the adapter module's `DebugLifecycleIntegrationTest`.
- UI is manual `runIde`: set a breakpoint in a `.hl`-compiled `.hx`, Debug, verify
  suspend at the right line, Frames panel, Variables placeholder, Resume, step
  over/into/out, console output, clean exit, Stop kills the adapter. Negatives:
  missing hl, missing `.hl`, invalid breakpoint line.

### Primary template
The inner `DebugProcess` class in
`src/main/java/com/intellij/plugins/haxe/runner/debugger/HaxeDebugRunner.java`
(the hxcpp XDebugProcess) is the closest existing pattern to mirror — but note the
IDE is the DAP **client** (connects out to the adapter), whereas hxcpp opens a
`ServerSocket` and the debuggee connects in. That socket direction differs.
