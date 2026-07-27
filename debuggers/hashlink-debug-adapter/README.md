# hashlink-debug-adapter

A standalone debug adapter for HashLink programs, written in Haxe and compiled to
HashLink bytecode (`build/hl/hl-debug-adapter.hl`). IntelliJ (or any other DAP client)
starts the adapter, connects to it over TCP, and speaks the
[Debug Adapter Protocol](https://microsoft.github.io/debug-adapter-protocol/specification).

The module also contains the Java-side DAP client
(`com.intellij.plugins.haxe.runner.debugger.dap`) used by the plugin and its tests.

## What it does

The adapter launches a HashLink debuggee, attaches to it, and drives a real debug
session over DAP:

- **launch** — spawns `hl --debug <port> --debug-wait <program.hl>` (the `.hl` must be
  compiled with `-debug`), reads the VM's handshake, and attaches via the OS debug API.
- **breakpoints** — resolves `file:line` against the `.hl` debug tables, patches INT3 at
  the machine address, and reports verified breakpoints. Breakpoints set before launch
  are answered provisionally and re-verified afterwards.
- **stop / continue** — reports `stopped` (breakpoint or exception) with the thread id;
  `continue` steps over the breakpoint (trap-flag single step, re-arm) and resumes.
- **step over / into / out** — `next`/`stepIn`/`stepOut` plant temporary breakpoints
  at control-flow targets computed from the bytecode and report `stopped`
  (`reason:"step"`) at the next line, the callee, or the caller. See
  [docs/README.md](docs/README.md) §5.
- **stackTrace** — walks the frame-pointer chain and maps return addresses back to
  `file:line` with `Class.method` names.
- **output** — forwards the debuggee's stdout/stderr as `output` events.
- **exit / disconnect** — emits `exited`/`terminated`; `disconnect` kills the debuggee.

All `debug_*` OS calls run on one dedicated session thread (required on Windows), which
communicates with the rest of the adapter only through queues.

## Layout

| Path | Contents |
|---|---|
| `src/main/haxe/ijhaxe/adapter/` | adapter entry point, thread wiring, request dispatcher |
| `src/main/haxe/ijhaxe/dap/protocol/` | DAP base messages + shared types (Source, Breakpoint, Variable, …); `requests/`, `responses/`, `events/` hold the per-command arguments/bodies |
| `src/main/haxe/ijhaxe/dap/transport/` | Content-Length framing, frame reader/writer |
| `src/main/haxe/ijhaxe/debug/` | shared primitives (`Pointer`, `DebugError`) |
| `src/main/haxe/ijhaxe/debug/target/` | the live debuggee: `debug_*` API, process spawn/pumps, memory reads, registers, stack walker |
| `src/main/haxe/ijhaxe/debug/module/` | static `.hl`/jit metadata: bytecode debug tables, handshake reader, control-flow graph, locals resolver |
| `src/main/haxe/ijhaxe/debug/layout/` | pure memory-layout arithmetic: frame/object/enum offsets, globals table, alignment rules |
| `src/main/haxe/ijhaxe/debug/values/` | runtime value decoding: readers, children, runtime types, the per-stop variable inspector |
| `src/main/haxe/ijhaxe/debug/session/` | orchestration: the session thread/state machine, commands/events, breakpoints, stepping |
| `test-fixtures/` | a tiny debuggee compiled with `-debug` (one class per file), used by the integration tests |
| `src/test/haxe/` | Haxe-side tests, run with the Haxe interpreter (`haxe test.hxml`) |
| `src/main/java/.../dap/protocol/` | DAP base messages + shared types (Lombok); `requests/`, `responses/`, `events/` mirror the Haxe side |
| `src/main/java/.../dap/transport/` | framing + socket connection |
| `src/main/java/.../dap/client/` | `DapClient`: request/response matching, event queue |
| `src/test/java/` | Java unit tests + integration tests against the real adapter |

The `.hl` bytecode debug tables are read with the `format` haxelib (installed by a
pinned Gradle task). Reading the debuggee's memory, INT3 patching, single-stepping and
register access go through the HashLink VM's own `debug_*` natives, so no additional
native code is needed.

> **Working on the session, launch, handshake, stepping, or threading code?**
> Read [docs/README.md](docs/README.md) first. It explains the HashLink-specific
> pitfalls — the GC deadlock and the `hl.Gc.blocking` workaround, why the
> handshake is drained before parsing, the buffer/timeout sizes, the trap-flag
> single-step — and where these problems will resurface when adding features.

## Running the adapter

```
hl build/hl/hl-debug-adapter.hl [--port <n>]
```

`--port 0` (the default) binds an OS-assigned port on 127.0.0.1. The adapter prints
`DAP-ADAPTER-LISTENING:<port>` on stdout once it accepts connections, serves exactly
one client session, and exits when the client disconnects.

## Building and testing

The Haxe compiler must be on `PATH` (no hardcoded paths anywhere; the build works on
Windows, macOS and Linux). If `haxe` is missing, the adapter build and Haxe tests are
skipped with a warning, and the plugin distribution will not contain the adapter.
Set `buildHashlinkAdapter=false` in `gradle.properties` to opt out explicitly.

```
gradlew :hashlink-debug-adapter:buildDebugAdapter   # compile adapter to .hl
gradlew :hashlink-debug-adapter:testHaxeAdapter     # Haxe-side tests (haxe --interp, no HashLink needed)
gradlew :hashlink-debug-adapter:test                # Java unit + integration tests
gradlew :hashlink-debug-adapter:check               # all of the above
```

### HashLink executable for integration tests

The integration tests start the compiled adapter with a real HashLink VM. They look
for the executable in this order and **skip** (not fail) when none is found:

1. `-PhashlinkBin=<path to hl executable>` on the Gradle command line
   (forwarded to the tests as the `hashlink.executable` system property)
2. environment variables `HASHLINK_BIN`, `HASHLINK`, `HASHLINKPATH`
   (each tried as the executable itself, then as a directory containing `hl`/`hl.exe`)
3. `hl` / `hl.exe` on `PATH`

HashLink is not bundled with the repository — install it from
<https://github.com/HaxeFoundation/hashlink/> or point option 1 or 2 at an existing copy.
CI currently has no HashLink runtime, so the integration tests skip there; the framing,
JSON and dispatcher tests still run everywhere.

### Linux support

HashLink publishes no linux release binaries (Windows only since 1.6), so the only
provisionable linux runtime is the **nightly** — and the debugger works on it:
**102 of 104 integration tests pass**, identically across haxe 4.1.5 through
5.0.0-preview.1. Getting there needed linux-only adaptations (all no-ops on
Windows), because HashLink's linux `debug_*` natives are ptrace-based and
behave differently from the Windows debug API:

- `hl_debug_wait` on linux IGNORES its timeout — it is a plain blocking
  `waitpid`. The Windows-style "drain the post-attach event burst until a wait
  times out" loop therefore deadlocks on the SECOND wait. Linux delivers
  exactly one attach stop, so the drain keeps it and returns
  (`DebugSession.drainAttachEvents`).
- linux ptrace memory writes require a ptrace-STOPPED tracee (Windows'
  `WriteProcessMemory` works on a running process). The attach SIGSTOP is
  therefore HELD through launch-time breakpoint installation and released in
  `configurationDone`, when the handshake-socket gate opens anyway.
- while Running the session thread is parked in that blocking wait, so a
  queued command (pause, breakpoint changes) would sit until some debug event
  arrived. Enqueueing now NUDGES the debuggee (`forceBreak` = SIGTRAP on the
  traced thread); the resulting stop matches nothing patched, is resumed
  silently, and the command interleave runs (`DebugSession.send`).
- a signal-delivered VM error (null access = SIGSEGV) reaches hl_throw with
  no walkable chain: the C error path has no frame pointers, hl's SIGSEGV
  handler dismantles the kernel signal frame before the error path runs, and
  RBP is repurposed by the C code. The walker instead rebuilds the top frame
  from the VM's own throw capture (`hl_thread_info.exc_stack_trace`) and
  recovers its frame base by scanning the stack for the return address into
  the captured caller (`StackWalker.recoverThroughSignalFrame`).
- a debuggee killed without a parseable exit status (see the threads limit
  below) used to spin the session forever — `waitpid` failure is now treated
  as process death.
- float-register WRITES: hl's linux `debug_write_register` cannot write XMM
  (its ptrace write path never handled the FP pseudo-offsets its read path
  defines — the write silently no-ops). The adapter instead loads the
  register by running a two-instruction injected stub (`movsd xmm0,[mem]` +
  INT3) through the same eval-call machinery that already runs code in the
  debuggee — the write becomes something the debuggee does to itself, so the
  broken native is never called.

Machine requirement: **attach mode** (attaching to a debuggee the adapter did
not spawn) needs `kernel.yama.ptrace_scope=0` (`sudo sysctl
kernel.yama.ptrace_scope=0`; Ubuntu defaults to 1, which only allows tracing
your own descendants — launch mode is unaffected). The 2 remaining failures
are a limit of HashLink's linux natives (`src/std/debug.c`) with NO
adapter-side workaround: a breakpoint executed by a SECONDARY thread kills
the debuggee. `PTRACE_ATTACH`/`waitpid` on linux are per-thread and hl
attaches only the main thread, so a worker hitting a breakpoint INT3 is
untraced and its SIGTRAP takes the default (fatal) action before any adapter
code can intervene. Unlike the float write, code injection cannot help — the
obstacle is not a missing operation but which thread is traced — so this
needs per-tid attach (`/proc/<pid>/task` + `PTRACE_O_TRACECLONE`) and
`waitpid(-1, __WALL)` upstream. See the docs backlog entry on
multi-threading.

## Distribution

The root build copies `hl-debug-adapter.hl` into the plugin sandbox/zip at
`<plugin dir>/adapter/hl-debug-adapter.hl` (see `PrepareSandboxTask` wiring in the
root `build.gradle.kts`). At runtime the plugin resolves it via
`PluginManagerCore.getPlugin(...).getPluginPath()`.
