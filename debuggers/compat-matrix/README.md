# Debugger compatibility matrix

Provisions the haxe (and, where available, HashLink) toolchains into
`<repo>/debuggerResources`, runs every debugger's test suite against each of
them, and writes a self-contained HTML report: a summary card per debugger,
a green/red grid per lane, and the list of any failing tests. This is the
"full check on all our debugger work" button.

Implemented as a plain JVM tool (`src/main/java`, module
`:debuggers:compat-matrix`) so it runs anywhere the build runs — Windows and
linux — with no PowerShell or python requirement.

## Running

```
gradlew debuggerCompatibilityReport                       # all lanes
gradlew debuggerCompatibilityReport -PmatrixLanes=eval    # one lane
gradlew debuggerCompatibilityReport -PmatrixHaxe=haxe_4_1_5  # one haxe
                                     # version (handy for rerunning one cell)
gradlew debuggerCompatibilityReport -PmatrixHl=hashlink-1.15.0  # one HL
                                     # runtime (overrides the smart-reduced
                                     # grid; combine with -PmatrixHaxe for
                                     # targeted bughunting, e.g. the HL
                                     # timing-flake investigation:
                                     #   -PmatrixLanes=hashlink
                                     #   -PmatrixHaxe=haxe_4_1_5,haxe_4_2_5
                                     #   -PmatrixHl=hashlink-1.15.0)
gradlew debuggerCompatibilityReport -PmatrixFull=true     # exhaustive HL grid
gradlew debuggerCompatibilityReport -PmatrixParallel=true # one thread per
                                     # lane (eval/hashlink/hxcpp run
                                     # concurrently; see Duration)
gradlew debuggerCompatibilityReport -PmatrixHlForks=1     # HL test classes in
                                     # N parallel fork JVMs per cell (default 4;
                                     # pass 1 for a fully sequential run).
                                     # Fixtures always compile BEFORE the cell
                                     # runs, never concurrently with tests.
gradlew debuggerCompatibilityReport -PmatrixReportOnly=true  # re-render the
                                     # report from the previous run's results
gradlew debuggerCompatibilityReport -PmatrixResources=D:\elsewhere  # custom
                                     # toolchain directory (default:
                                     # <repo>/debuggerResources)
```

The report lands in `build/reports/debugger-matrix/matrix-<date>_<time>.html`
(one self-contained file per run, so successive runs never overwrite each
other; `index.html` always mirrors the newest). Per-cell gradle logs and the
copied junit XMLs sit next to it (`logs/`, `results/`). When a failed suite
passes on its once-only retry (reported as *flaky*), the first attempt's
failure XMLs are kept in `results/<cell>/first-attempt/` for diagnosis.
Progress streams to the console and `progress.log`.

## Toolchain provisioning

The versions to certify live in `VersionManifest.java` (compile-checked; one
line per version). On first run each is downloaded from the official GitHub
releases and extracted into:

```
debuggerResources/            (gitignored)
  haxe/
    haxe_4_1_5/               haxe(.exe) + std/  [downloaded, both OSes]
    ...
    haxe_5_preview_1/
  hashlink/
    hashlink-1.15.0/          hl(.exe)           [downloaded on Windows]
    hashlink-nightly/         hl(.exe)           [downloaded on Windows AND
    ...                        linux - master CI artifacts via nightly.link;
                               delete the directory to pick up a newer build]
```

A `.provisioned` marker makes re-runs free; delete a version directory to
force a re-download.

**HashLink on linux**: the HashLink project publishes no linux binaries for
the supported versions (1.13+), so build them from source (or install a
package) and drop each into `debuggerResources/hashlink/<name>/` — any
directory containing an `hl` binary is DISCOVERED and joins the matrix. The
same discovery works on Windows for nightlies or local builds. On linux the
runtime's directory is put on `LD_LIBRARY_PATH` for the tests, so keep
`libhl.so` and the `.hdll` files beside `hl`.

The support floor (haxe >= 4.1, HashLink >= 1.13) is expressed by the
manifest simply not listing older versions.

## Duration

- eval lane: ~1 minute per haxe version (live suite against the real VM).
- hashlink lane: fixture build per haxe version + a test run per runtime;
  the default "smart-reduced" grid runs known-degraded old haxe versions
  (4.1.5/4.2.5) against the reference runtimes only — latest release and
  nightly — since their behaviour was proven identical on every runtime;
  `-PmatrixFull=true` runs every combination.
- `-PmatrixParallel=true` runs each lane in its own thread. The lanes are
  disjoint (separate modules, fixtures, and debugger binaries), so this is
  safe; the stray-process sweep is deferred to the end because it kills
  hl/haxe by name machine-wide. Honest expectation: the total becomes the
  HASHLINK lane's duration — eval and hxcpp (~9 min combined) simply hide
  inside it — so a ~55 min full run drops to ~45–48 min, not half. Progress
  lines are prefixed with `<lane> |` so the interleaved log stays readable.
- hxcpp lane: the slowest — every haxe version compiles the C++ fixtures,
  and the machine needs a working hxcpp/haxelib setup per version (see the
  main debugger docs); this lane is not provisioned automatically.

First run adds the downloads (roughly 200 MB for the haxe versions).

## Why the odd flags (do not "simplify")

- `--no-daemon` on every child gradle call: the forked test JVM must inherit
  the lane's `PATH`/`HAXE_STD_PATH`; a warm daemon keeps the env it was born
  with and silently tests the wrong haxe.
- Every lane cell logs `children resolve haxe <version>` first — a WARNING
  there means the lane environment leaked (a stale daemon, an IDE-started
  build, a surviving `Path` case-variant) and the cell would compile with
  the wrong haxe against the lane's std, producing a confusing salad of
  std-typing errors ("Compiler.hx ... Too many arguments"). Run
  `gradlew --stop` and rerun from a normal shell.
- `cleanTest --no-build-cache` per cell: the lane's haxe/runtime is not a
  tracked test input, so the build cache happily replays a previous cell's
  result as an 8-second "run".
- Bounded calls + process-tree kill + stray `hl`/`haxe`/fixture cleanup:
  a stuck runtime error dialog must not wedge the whole matrix.
- haxelib dev registrations happen once up front; lanes exclude those tasks
  so nothing races the shared haxelib repository.
- The HL debug adapter bytecode is built once with the dev haxe and stays
  PINNED; only fixtures are rebuilt per lane haxe (the shipped adapter is a
  dev-haxe artifact — that is exactly what users run).
- After the run the HL fixtures are rebuilt with the dev haxe so the
  working tree is back in its normal state.
