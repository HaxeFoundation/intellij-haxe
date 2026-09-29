# munit live reporter

Haxe sources shipped inside the plugin jar and compiled by the USER's haxe
into their test build — resources, not plugin code. The test planner adds
`-cp <extracted dir> --macro intellij_munit.Macro.init()` to munit test
compiles; `HaxeTestReporterFiles` extracts this directory into the IDE
system path together with the shared sources
(`../sharedLiveReporter/intellij_haxe_test/`).

`Macro.init()` registers a `@:build` macro on `massive.munit.TestRunner`
that appends an `addResultClient(new LiveClient(<root suite name>))` call to
the constructor. munit has no TeamCity reporter of its own, so this client
IS the IDE's result channel, not an optional enhancement. munit clients
hear about a test only AFTER it ran, so each test emits an adjacent
started/finished pair carrying the measured duration.

Wire facts, verified against munit 2.3.5:

- the runner hangs on the eval interpreter (munit predates it) — no interp
  lane;
- the classic TestMain exits 0 even on failures (its delayed completion
  handler misses the process end), so verdicts come from the events alone;
- munit has no test-filter define — single-test runs patch the test
  collection in `massive.munit.TestClassHelper` instead, and a filter that
  cannot be proven sound FAILS the compile (an unsound filter registers no
  tests and the run would pass empty);
- munit's PrintClient hijacks `haxe.Log.trace` and prints immediately;
  LiveClient (attached last) re-hijacks so traces buffer and replay as the
  reported test's own output, appearing exactly once on the right node.

Every other failure mode degrades to a plain run instead of breaking the
build: global metadata on an absent type is inert, and a munit without
`addResultClient` gets a warning and unmodified fields.

The files stay at the Haxe 4.1 language level — the user's compiler builds
them.
