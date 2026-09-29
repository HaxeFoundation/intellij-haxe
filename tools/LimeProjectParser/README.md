# LimeProjectParser

Evaluates a lime/openfl project file — `project.xml` or `project.hxp` — into
the build configuration the IDE needs: defines, haxedefs, haxelibs (with
versions) and source classpaths, as JSON on stdout. Replaces scraping
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
java -jar LimeProjectParser.jar project.xml --target hl -D hl -D debug
java -jar LimeProjectParser.jar project.hxp --target html5
```

All tasks self-skip when no `haxe` is on the PATH; the integration test also
self-skips when the lime or hxp haxelib is missing.

## Semantics

The evaluation mirrors lime's `ProjectXMLParser.isValidElement` exactly:

- `if` — OR over `||` segments, each segment an AND over space-separated
  tokens; a token passes when it is `true`, a known define, an environment
  variable or the current command; `false` and unknown names fail.
- `unless` — same evaluation, a match excludes the element.
- `<section>` recurses; `<include path>` loads a file (a directory resolves to
  its `include.xml`) relative to the project file.
- `<set>`/`<unset>` touch defines + environment; `<define>`/`<undefine>` also
  touch haxedefs; `<haxedef>` only haxedefs.
- Each `<haxelib>` additionally defines its own name — that is why
  `if="openfl"` works below a `<haxelib name="openfl"/>` line.
- `${name}` substitutes from defines, then environment.

The caller supplies the seed defines (target, platform, tool versions) via
`-D`; the tool stays ignorant of how they are derived.

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
`java`, `cs`) map to the host platform with a target flag set — the same
mapping lime's CommandLineTools applies.

## Plugin integration

`HaxeLimeProjectInfoService` invokes the bundled jar as its PRIMARY path
(`--target/--haxe/--haxelib` plus seed defines), falling back to
`haxelib run lime|openfl display` when the jar is missing or fails; the root
build bundles the jar into the plugin distribution.

## Planned

- Validation lane in the compat matrix comparing this tool's output against
  real `lime display` across lime versions, so semantic drift turns into a red
  lane instead of a user bug report.
