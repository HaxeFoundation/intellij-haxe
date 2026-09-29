# tink_unittest live reporter

Haxe sources shipped inside the plugin jar and compiled by the USER's haxe
into their test build — resources, not plugin code. The test planner adds
`-cp <extracted dir> --macro intellij_tink.Macro.init()` to tink test
compiles; `HaxeTestReporterFiles` extracts this directory into the IDE
system path together with the shared sources
(`../sharedLiveReporter/intellij_haxe_test/`).

`Macro.init()` registers a `@:build` macro on `tink.testrunner.Runner` that
makes the TeamCity reporter the DEFAULT of `Runner.run`'s optional reporter
argument — a TestMain calling `Runner.run(batch)` gets it without any code
change, and one passing its own reporter keeps it. tink has no TeamCity
reporter of its own, so this is the IDE's result channel.

Every failure mode degrades to a plain run instead of breaking the build:
global metadata on an absent type is inert (a build without tink_testrunner
compiles untouched), and the build macro verifies the `run` signature
before patching — an unrecognized runner gets a warning and unmodified
fields.

The files stay at the Haxe 4.1 language level — the user's compiler builds
them.
