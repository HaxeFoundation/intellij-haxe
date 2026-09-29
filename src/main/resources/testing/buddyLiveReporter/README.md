# buddy live reporter

Haxe sources shipped inside the plugin jar and compiled by the USER's haxe
into their test build — resources, not plugin code. Unlike the other
frameworks there is no macro patching: buddy selects its reporter from its
own `-D reporter=<fqcn>` override, so the planner adds
`-cp <extracted dir> -D reporter=intellij_buddy.TcReporter` to buddy test
compiles; `HaxeTestReporterFiles` extracts this directory into the IDE
system path together with the shared sources
(`../sharedLiveReporter/intellij_haxe_test/`).

buddy has no TeamCity output of its own, so the reporter is the result
channel, not an optional enhancement. Events are emitted as one BATCH from
`done`: buddy's per-spec `progress` callback carries no suite context, and
only the finished tree has the describe-nesting, per-spec durations and the
captured traces (attributed to their spec via `testStdOut`).

Spec descriptions are prose, not identifiers — names stay as written and
the IDE has no source navigation for them (specs are describe/it closures
with no method PSI to map to; the VSCode adapter shares that ceiling).
Single-spec runs are not supported either: buddy filters via `@include`
metadata in code, not a define.

The files stay at the Haxe 4.1 language level — the user's compiler builds
them.
