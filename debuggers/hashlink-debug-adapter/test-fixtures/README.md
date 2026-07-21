# HashLink debugger test fixtures

Each `*.hxml` here compiles a small Haxe program (`src/`) to HashLink bytecode
under `../build/hl/`. These are the **debuggees** the integration tests attach
to and drive through the debug adapter (see `../src/test/java/.../integration`).

## Why every fixture sets `-D hl-legacy32`

All fixtures are built with `-D hl-legacy32`, which emits the legacy
(pre-native-i64) `Int64` representation.

The 32-bit HashLink VM **refuses to load** a module that uses native 64-bit
ints — it fails at load with:

```
The module you are loading is using 64 bit ints that are not supported by the
HL32. Please run using HL64 or compile with -D hl-legacy32
```

Haxe 5 defaults to native 64-bit ints, so **without this flag a haxe-5-built
fixture cannot run on 32-bit HashLink at all** (earlier Haxe versions were not
affected). The legacy form loads and runs on **both** HL32 and HL64, so a
single set of fixtures serves both bitnesses and lets the integration matrix
cover 32-bit and 64-bit runtimes uniformly.

The debug adapter itself is built the same way and for the same reason — see
the comment atop `../build.hxml`.

## Known limitations below haxe 4.3

A few behaviours of pre-4.3 compilers are **not supported by the current
debugger adapter**, and the tests covering them self-skip (via
`assumeFixtureHaxe43Plus()` in `DapIntegrationTestBase`) when the fixtures are
built with an older haxe:

- exception-stop **descriptions** don't name the thrown value (pre-4.3 wraps
  every throw through `Exception.thrown` with a message layout the adapter
  does not decode),
- **native stack entries** of `haxe.Exception` stay unresolved raw addresses,
- on haxe **4.1** only, stepping over a caught `throw` cannot identify the
  catch-block landing (4.1 emits bogus "line 1" debug info for catch
  handlers).

Sessions stay healthy in all three cases — the output is just degraded.

## Adding a fixture

Copy an existing `*.hxml`, point `-main`/`-hl` at your new program and output,
keep the `-debug` and `-D hl-legacy32` flags, then register the build + the
`.hl` path in the module `build.gradle.kts` (the `*Fixture` tasks and the
`dap.fixture.*` system properties) so the tests can find it.
