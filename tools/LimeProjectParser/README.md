# LimeProjectParser

Evaluates a lime/openfl project file — `project.xml` or `project.hxp` — into
the build configuration the IDE needs: defines, haxedefs, haxelibs (with
versions), source classpaths, the `<app>` export layout and the `<config>`
values (flattened to lime's dot keys, `air.output-directory`), as JSON on
stdout. Replaces scraping
`haxelib run lime display` output, which flattens haxelibs into `-cp`
classpaths and loses their identity.

Written in Haxe (compiled by us — 4.3.7 level), built to a JVM jar with
`--jvm`, so the plugin can run it on the IDE's own JRE: no neko, no `haxelib
run`, no dependency on the project's lime installation being executable
(except for `.hxp`, see below).

```
gradlew :tools:LimeProjectParser:buildParser           # build/libs/LimeProjectParser.jar
gradlew :tools:LimeProjectParser:testParser            # evaluator unit tests (--interp)
gradlew :tools:LimeProjectParser:integrationTestParser # project.xml + project.hxp end to end
java -jar LimeProjectParser.jar project.xml --target hl -D debug
java -jar LimeProjectParser.jar project.hxp --target html5
```

All tasks self-skip when no `haxe` is on the PATH; the integration test also
self-skips when the lime or hxp haxelib is missing.

## Semantics

The evaluation mirrors lime's `ProjectXMLParser` (lime 8.3.2):

- `if` — OR over `||` segments, each segment an AND over space-separated
  tokens; a token passes when it is `true`, a known define, an environment
  variable or the current command; `false` and unknown names fail.
- `unless` — same evaluation, a match excludes the element.
- `${name}` — lime's `replaceVariable`: `${haxelib:x}` is the library's root
  folder (forward slashes), then a define, an environment variable,
  `${projectDirectory}`; else a comparison (`${haxe_ver >= 4.3}`, with `==`,
  `!=`, `<=`, `<`, `>=`, `>`) whose sides are compared as STRINGS, yielding
  `true`/`false`; an unknown name stays literal, so `if="${unknown}"` fails.
  `$${x}` is the escaped form, resolved once more. A nested reference
  resolves over two rounds: `${${haxe_ver} < 4}` first yields
  `${haxe_ver < 4}`.
- The environment is the process environment without the target-named
  variables (`windows`, `html5`, …), plus `haxe` and `haxe_ver` (the
  `--haxe` executable's `-version`) and `haxe<major>=1`.
- `<section>` recurses. `<include path|name>` loads a file — a directory
  stands for its `include.lime`, `include.nmml` or `include.xml`, in that
  order — relative to the INCLUDING file's folder; when the file sits in a
  folder, that folder also becomes a source path. `<include haxelib="x">`
  merges the library's include file without registering the library.
- `<set>`/`<unset>` touch defines + environment (`<set name="BUILD_DIR">`
  also moves the app path); `<define>`/`<undefine>` also touch haxedefs;
  `<haxedef>` only haxedefs, and `<haxedef remove="x">` drops one. A
  `<haxeflag>`/`<compilerflag>` of the form `-D name[=value]` is a haxedef;
  other raw flags are not reported.
- Each `<haxelib>` additionally defines its own name with its resolved
  version — that is why `if="openfl"` works below a
  `<haxelib name="openfl"/>` line. Its root (the folder holding
  `haxelib.json`, found upwards from the `haxelib path` classpath) is checked
  for an include file, which merges inline as lime does. `optional="true"`
  skips a library haxelib cannot find; `path="…"` reads a local checkout
  (version and classpath from its `haxelib.json`) without asking haxelib.
- The target: `--target` seeds the condition defines of lime's
  `HXProject.initializeDefines` (`html5=1`, `platformType=web`,
  `targetType=cpp`, `native=1`, …). `hl`, `neko`, `cppia`, `java`, `cs` and
  `nodejs` build for the HOST platform with a flag set. The target also adds
  the `-D` flags the build passes beyond the project's haxedefs: the platform
  (never for flash), its type (`desktop|mobile|web|console`), `macos` for
  mac, `firefoxos` for firefox, and `tools=<lime version>`. `-D name[=value]`
  on the command line adds further condition defines; `-D debug` also
  selects the debug build type.

### Output

`haxedefs` is every `-D` the build passes to haxe for the target — the IDE's
define set. `defines` are lime's condition defines (`<set>` names, target
seeds, haxelib names) and only matter for `if`/`unless` evaluation. Library
versions (`-D openfl=9.5.2`) belong to neither: the IDE learns them from its
library sync.

Not mirrored (each has a `TODO:` at its site): `include.hxp` library
scripts, dotted `${project.field}` access, `<haxelib repository>`, raw
`<haxeflag>`s other than `-D`, and lime's merge rule that an included file
cannot override a define the including file already holds.

## .hxp evaluation

A `.hxp` is Haxe code extending `lime.tools.HXProject`, so it must run — with
the user's haxe and the lime + hxp haxelibs installed. The tool mirrors lime's
`HXProject.fromFile` mechanics (temp copy as `<Name>.hx`, `@:compiler(` lines
as extra args) but replaces the serialize/unserialize round trip: the shipped
`HxpRunner.hx` (a jar resource extracted beside the script) executes inside
that lime context, seeds the `HXProject` statics like lime's own main does,
instantiates the script class and prints this tool's JSON directly. Type
locations that cost a debugging round: `Haxelib` lives in the `hxp` package
(lime imports `hxp.*`), `Platform` in `lime.tools`.

Pseudo-targets that are not platforms of their own (`hl`, `neko`, `cppia`,
`java`, `cs`, `nodejs`) map to the host platform with a target flag set — the same
mapping lime's CommandLineTools applies. The runner adds the same `-D` flags
to `haxedefs` that the xml path does (platform, platform type, `macos`,
`tools`), so both file kinds report the compiler's define set.

## Plugin integration

`HaxeLimeProjectInfoService` invokes the bundled jar as its PRIMARY path
(`--target/--haxe/--haxelib`) and reads its `haxedefs`, falling back to
`haxelib run lime|openfl display` when the jar is missing or fails; the root
build bundles the jar into the plugin distribution.

## Planned

- Validation lane in the compat matrix comparing this tool's output against
  real `lime display` across lime versions, so semantic drift turns into a red
  lane instead of a user bug report.
