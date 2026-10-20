# Formatter comparison fixtures

Three-way comparison against [haxe-formatter](https://github.com/HaxeCheckstyle/haxe-formatter)
(HaxeCheckstyle, the vshaxe formatter), driven by `HaxeFormatterComparisonTest`.

Each rule directory holds:

| file | content |
|---|---|
| `input.hx` | deliberately misformatted source, violating the rule under test |
| `hxformat.hx` | `input.hx` as formatted by haxe-formatter (default config) — the ground truth |
| `hxformat.json` | present ONLY for rules testing a NON-default option: the config `hxformat.hx` was generated with (the tool finds it beside the file); the test applies the equivalent settings tweak |

The test configures our code style to the haxe-formatter DEFAULTS (tabs,
end-of-line braces, spaced keywords/operators, ...) via
`HxformatDefaultProfile.apply` in its setUp. Every rule asserts our output
equals `hxformat.hx` byte-for-byte (modulo the trailing newline, which the
IDE manages at save time).

An `input.hx` must actually VIOLATE its rule: one the tool leaves unchanged
(`input.hx` equal to `hxformat.hx`) proves only idempotence, which
`HaxeSecondReformatTest` already sweeps over every fixture. Fixture code
uses neutral names and no third-party library identifiers.

Regenerating `hxformat.hx` after editing an `input.hx` (needs
`haxelib install formatter`; run PER FILE — running on the directory would
reformat the inputs too; a rule's own `hxformat.json` is picked up from the
directory):

```
copy <rule>\input.hx <rule>\hxformat.hx
haxelib run formatter -s <rule>\hxformat.hx
```

Then confirm the two files differ. Generated with formatter 1.18.0.

`default-hxformat.json` is the tool's complete built-in configuration, the
word `HxformatDefaults` is checked against (`HxformatDefaultsTest`).
The tool only writes it over an EXISTING file:

```
type nul > default-hxformat.json
haxelib run formatter --default-config <absolute path>\default-hxformat.json
```
