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
- **stackTrace** — walks the frame-pointer chain and maps return addresses back to
  `file:line` with `Class.method` names.
- **output** — forwards the debuggee's stdout/stderr as `output` events.
- **exit / disconnect** — emits `exited`/`terminated`; `disconnect` kills the debuggee.

All `debug_*` OS calls run on one dedicated session thread (required on Windows), which
communicates with the rest of the adapter only through queues.

## Layout

| Path | Contents |
|---|---|
| `src/main/haxe/adapter/` | adapter entry point, thread wiring, request dispatcher |
| `src/main/haxe/dap/protocol/` | DAP message typedefs, one per file |
| `src/main/haxe/dap/transport/` | Content-Length framing, frame reader/writer |
| `src/main/haxe/debug/` | debug session, OS debug API, handshake reader, `.hl` debug info, breakpoints, stack walker |
| `test-fixtures/` | a tiny debuggee compiled with `-debug`, used by the integration tests |
| `src/test/haxe/` | Haxe-side tests, run with the Haxe interpreter (`haxe test.hxml`) |
| `src/main/java/.../dap/protocol/` | DAP message classes (Lombok), one per file |
| `src/main/java/.../dap/transport/` | framing + socket connection |
| `src/main/java/.../dap/client/` | `DapClient`: request/response matching, event queue |
| `src/test/java/` | Java unit tests + integration tests against the real adapter |

The `.hl` bytecode debug tables are read with the `format` haxelib (installed by a
pinned Gradle task). Reading the debuggee's memory, INT3 patching, single-stepping and
register access go through the HashLink VM's own `debug_*` natives, so no native code of
our own is needed.

> **Working on the session, launch, handshake, stepping, or threading code?**
> Read [docs/README.md](docs/README.md) first. It explains the HashLink-specific
> pitfalls we hit — the GC deadlock and the `hl.Gc.blocking` workaround, why the
> handshake is drained before parsing, the buffer/timeout sizes, the trap-flag
> single-step, and where these problems will resurface as we add features.

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

## Distribution

The root build copies `hl-debug-adapter.hl` into the plugin sandbox/zip at
`<plugin dir>/adapter/hl-debug-adapter.hl` (see `PrepareSandboxTask` wiring in the
root `build.gradle.kts`). At runtime the plugin resolves it via
`PluginManagerCore.getPlugin(...).getPluginPath()`.
