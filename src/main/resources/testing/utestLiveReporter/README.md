# utest live reporter

Haxe sources shipped inside the plugin jar and compiled by the USER's haxe
into their test build — resources, not plugin code. The test planner adds
`-cp <extracted dir> --macro intellij_utest.Macro.init()` to test compiles
when the Build Tools | Haxe "Live test reporting" setting is on (the
default); `HaxeTestReporterFiles` extracts this directory into the IDE
system path once per content change, together with the sources every
framework's reporter shares (`../sharedLiveReporter/intellij_haxe_test/`:
TeamCity escaping, line printing, the hosted-run sentinel and the flash
shims) — one classpath root serves both.

`Macro.init()` registers a `@:build` macro on `utest.Runner` that appends a
`LiveReporter.attach(this, <root suite name>)` call to the constructor. The
reporter rides the runner's public `onTestStart`/`onTestComplete`
dispatchers and streams one TeamCity service message per test as it runs —
live test-tree population, real durations, and stdout printed during a test
attaches to it naturally.

utest's own batch reporter stays active (`-D teamcity` is always passed) and
replays the whole tree at the end of the run; the IDE's events converter
recognizes tests that already completed and swallows the replay.

Failure modes all degrade to the batch behavior instead of breaking the
build:

- build without utest: global metadata on an absent type is inert;
- utest without the runner dispatchers (predates them): the build macro
  checks the fields before patching and warns instead;
- runner constructor with an unexpected shape: warn, fields unchanged;
- extraction failure: the planner adds neither `-cp` nor `--macro`;
- setting disabled: same — batch reporting only.

The files stay at the Haxe 4.1 language level — the user's compiler builds
them (verified against utest 1.13.2).
