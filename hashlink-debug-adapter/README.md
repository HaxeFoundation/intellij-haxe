# hashlink-debug-adapter

A standalone debug adapter for HashLink programs, written in Haxe and compiled to
HashLink bytecode (`build/hl/hl-debug-adapter.hl`). IntelliJ (or any other DAP client)
starts the adapter, connects to it over TCP, and speaks the
[Debug Adapter Protocol](https://microsoft.github.io/debug-adapter-protocol/specification).

The module also contains the Java-side DAP client
(`com.intellij.plugins.haxe.runner.debugger.dap`) used by the plugin and its tests.

## Layout

| Path | Contents |
|---|---|
| `src/main/haxe/adapter/` | adapter entry point, thread wiring, request dispatcher |
| `src/main/haxe/dap/protocol/` | DAP message typedefs, one per file |
| `src/main/haxe/dap/transport/` | Content-Length framing, frame reader/writer |
| `src/test/haxe/` | Haxe-side tests, run with the Haxe interpreter (`haxe test.hxml`) |
| `src/main/java/.../dap/protocol/` | DAP message classes (Lombok), one per file |
| `src/main/java/.../dap/transport/` | framing + socket connection |
| `src/main/java/.../dap/client/` | `DapClient`: request/response matching, event queue |
| `src/test/java/` | Java unit tests + integration tests against the real adapter |

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
