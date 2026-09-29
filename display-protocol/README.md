# display-protocol

A client for the Haxe compiler's JSON-RPC display protocol. It talks to the
same `haxe --wait <port>` compilation server that the plugin already runs for
builds. The DTOs mirror the std `haxe.display.*` typedefs, and the transport
uses the compiler's null-terminated socket format. Every fact below was
verified live against haxe 4.3.7, and against haxe 5.0.0-preview.1 where
marked.

## Wire protocol (`--wait <port>` TCP mode)

Each request uses its own socket:

1. Connect, write every compiler argument followed by `\n`, then write a
   single `\0` byte.
2. Read until the server closes the connection.
3. Classify each response line by its first byte. `0x01` starts a log line,
   and newlines inside a log message arrive as further `0x01` bytes. `0x02`
   is the fatal-error marker. Every other line is payload; for a display
   request, the payload is the raw JSON-RPC response envelope.

A display request is the build's normal argument list plus
`--display <json-rpc-payload>`. The response nests its data twice:
`{"jsonrpc":"2.0","id":1,"result":{"result":<data>,"timestamp":...}}`.
The inner wrapper is the std `Response<T>`.

Facts that shape the client:

- **The server handles one connection at a time.** Concurrent sockets wait
  in the OS listen backlog and are answered in order, at about 15 ms per
  request on localhost with a warm cache. Nothing is dropped, so the client
  needs no queue of its own for correctness.
- **Never hold a connection open.** A half-open connection (arguments sent,
  but no `\0`) blocks every other client, builds included, until a
  server-side read timeout of about 5 s. The client therefore has no
  connection pool and strictly connects, sends one request and closes.
- **No batching.** The server rejects a JSON-RPC batch array (`-32600`),
  ignores a second `--display` argument, and stops at the first display
  request after `--next`. Each connection carries exactly one request.
  Batching at the method level covers the real need: `display/diagnostics`
  takes `fileContents` for many files, or empty params for the whole
  project.
- **Result positions are 0-based**, both line and character. The
  `Position.hx` docstrings claim 1-based values, but the compiler converts
  them for LSP. The `offset` in request params is a unicode character offset
  into the file.
- **Unsaved buffers** travel in the request, as `contents` on
  `PositionParams` and `DiagnosticsParams`. But once a module is cached, the
  server IGNORES `contents` alone: it sees an unchanged mtime and serves the
  cached result. Send `server/invalidate {file}` whenever the editor buffer
  diverges from disk, as vshaxe does on every document change. The next
  request then reparses the file from the supplied contents. Saved files need
  nothing, because the server re-checks their mtime on every request.
- **The `server/*` introspection methods need a compile first.** Only an
  actual compile through the server fills the module cache (for example
  `-main X -js out.js --no-output`), and it must use the same argument
  signature. Display requests alone leave `server/modules` empty. The flow
  is:
  1. compile;
  2. `server/contexts`: the `after_init_macros` context holds the modules;
  3. `server/module`: dependencies and dependents, for invalidation;
  4. `server/type`: the complete type after macros ran, every field with its
     `JsonType`, macro-generated members included.
- **Modules created by `Context.defineType` are invisible to
  `server/modules`.** They appear ONLY in the `dependencies` lists of the
  modules that use them. `server/type` on the defined dot path answers
  normally. On haxe 4, `server/module` rejects such a module with "Compiler
  error"; haxe 5 serves its `ModuleInfo` too.
  `LiveDisplayServerTest.macroDefinedTypeAppearsInModulesAndBlueprintsAfterACompile`
  pins this behaviour. The IDE's type catalog finds generated types by
  walking these dependency lists.
- Check the method list that `initialize` returns before using a feature;
  for example, `display/diagnostics` needs haxe 4.3 or later.

## Completion (`display/completion`, verified against 4.3.7)

The params are `{file, offset, wasAutoTriggered, ?contents}`. The compiler
picks the completion mode from the position (`mode.kind`: 0 Field,
2 Toplevel, 4 TypeHint, 8 Import, 11 Pattern, 12 Override, ...).

The compiler completes at the END of a partial identifier and after a dot.
It refuses a request placed on an identifier that already resolves (inside
or at the end of `trace` in `trace(...)`) with a JSON-RPC "Compiler error"
whose data reads "Unsupported method". So send the editor's buffer as
`contents` with the caret offset, never a copy with a dummy identifier. For a
file the server has cached, send `server/invalidate` first.

Toplevel mode returns every visible type (over a thousand with the std),
packages, the literals (`null`, `true`, `false`, `this`), the keywords valid
at the position, locals and fields.

A position between class members answers `result: null`. That is an empty
line or a partial identifier in a class body, where `public`, `function` or
`var` would go. Haxe 4.3.7 has no class-field completion mode: the parser
only recovers from the error past the position. The server therefore cannot
supply field keywords. The reference client (haxe-language-server,
`createFieldKeywordItems`) adds the modifier keywords itself on a null answer
and offers `function` and `var` as snippets.

In the result, `replaceRange` (0-based) covers the typed prefix,
`filterString` repeats that prefix, and `isIncomplete` flags a list the
compiler cut short.

A Toplevel position that sits in an argument, an initializer, an assignment
or a return carries the type it expects in `mode.args.expectedType`, with
typedefs followed in `expectedTypeFollowed`. The items never include a
snippet: at `[1, 2, 3].filter(<caret>)` the list is the plain toplevel one,
and the expected type is a `TFun` (`Int -> Bool`, argument names empty).
The lambda suggestion the reference client shows there is built by the
client itself from that expected type; the plugin builds its own the same
way.

An item is `{kind, args, ?type, index}`. A Local carries its type in
`args.type` and a field in `args.field.type`, and the item-level `type` may
be absent. `DisplayJson.decodeCompletion` falls back through both.

Doc comments come inline: `args.field.doc` for a field, and `args.doc` for a
type, metadata or define. The value is the comment's raw body between the
delimiters, source indentation included. A markdown renderer must strip that
indentation first, or every paragraph becomes a code block. A saved file, an
unsaved `contents` buffer and a cached module all carry the doc; only a
declaration without a comment arrives with `doc: null`.
`display/completionItem/resolve {index}` names the item by its position in
the list, which its `index` key repeats. It answers `{item}` in the same
shape and with the same doc.

## Diagnostics (`display/diagnostics`, verified against 4.3.7)

The params are `{file, ?contents}`, or `fileContents` for several files, or
empty for the whole project. The answer is `[{file, diagnostics}]`.

There are eight diagnostic kinds. Every compiler warning without a kind of
its own arrives as a `CompilerError` with severity Warning. On 4.x the
`args` string is therefore the only thing that tells warning classes apart;
haxe 5 adds the `code`.

- `UnresolvedIdentifier` args list suggestions. `kind` 0 is an import
  candidate given as a qualified path, and `kind` 1 is a typo correction. A
  type hint naming an unimported type gets both; a misspelt call gets
  corrections.
- `MissingFields` args are `{moduleType, moduleFile, entries[{fields,
  cause}]}`. `cause.kind` is one of `AbstractParent`,
  `ImplementedInterface`, `PropertyAccessor`, `FieldAccess` and
  `FinalFields`. Each field is the full JSON class field (name, type, kind,
  scope, expr). `FieldAccess` fires for a call to an unknown function
  (`trce("x")`), so it also serves as a source for creating the missing
  member. `FinalFields` fires for a class whose final fields no constructor
  initializes.
- `RemovableCode` reports unused LOCAL variables only. Its args hold
  `description` ("Unused variable") and `range`, the span to delete: the
  declaration up to its initializer, which itself stays. The compiler never
  reports unused private fields or functions, nor expressions without effect.
- Several warnings fold into `CompilerError`: an unused `case` ("This case
  is unused"), the deprecated `@:enum abstract` spelling, `$type(x)` (WInfo,
  where the message IS the type name), variable shadowing and unsafe enum
  equality. "Local variable used without being initialized" (WVarInit)
  arrives as an Error.
- `relatedInformation` carries secondary locations, as
  `{location{file, range}, depth, message}`. The shadowing warning, for
  example, points at the previous declaration.
- `InactiveBlock` is a Hint over the inactive region, with the condition in
  `{expr}`.
- The `-w` option switches warning classes per request. In 4.3.7,
  `+WVarShadow` and `+WUnsafeEnumEquality` are the only two classes disabled
  by default. `-WInfo` drops the `$type` output. Several flags can combine in
  one value (`+WVarShadow+WUnsafeEnumEquality`), or the option can repeat.
  `+WAll` enables everything.

## References and definition (`display/references`, `display/definition`, verified against 4.3.7)

- Both take `{file, offset, ?contents}`; references add `kind`: `direct`,
  `withBaseAndDescendants` (the base field and every override) or
  `withDescendants`. Both answer a list of `{file, range}` with the file as
  an absolute path and a 0-based range.
- A field usage is reported as the last name-length characters of the field
  access expression (the compiler's `patch_string_pos`), which is the name
  itself in `obj.field`, `Main.field` and a bare `field`.
- A compound assignment breaks that rule: for `field += x` (any compound
  operator, any receiver) the typer gives the left-hand access the position
  of the whole assignment, so the range is
  `[assignmentEnd - name.length, assignmentEnd)`, which lands on the end of
  the right-hand side (`ht(2)` for `total += weight(2)`). Plain `=`, reads
  and `++` are positioned correctly. The IDE maps such a range back through
  the assignment expression that ends where the range ends.
- `display/references` never lists the declaration itself; the query may
  be placed at the declaration or at any usage.

## Haxe 5 differences (verified against 5.0.0-preview.1)

The JSON-RPC surface is unchanged: the same methods, framing and envelopes.
The answers differ in ways a client must branch on, though. The live suite
names each difference in a capability helper, such as
`sendsDiagnosticCodes()` and `blueprintTypesResolved()`.

- **Diagnostics carry LSP-style `code`s**: the specific `-w` warning
  identifier (`WDeprecatedEnumAbstract`, not just the `WDeprecated` class).
  4.x sends `code: null` throughout. On both generations, `-w -WDeprecated`
  in the request args suppresses the whole class.
- **`RemovableCode` is renamed `ReplaceableCode`**, and its args gain an
  optional `newCode` replacement string next to `description` and `range`.
- **`server/type` member types arrive as unresolved `TMono`.** The server
  serializes the module cache before it forces lazy typing, so field and
  return types that 4.3.7 reports concretely (`TInst String`) come back as
  monomorphs. Member names, kinds and shapes are reliable on both
  generations; only type resolution degrades, and the resolve service
  renders types it cannot express as `Dynamic`. A `display/hover` on the
  same field DOES resolve the type; only the blueprint answer degrades.
- **The legacy `--version` wire request gets an empty answer**: the server
  just closes the connection, where 4.x returns the version string. To probe
  whether the server is ready, wait for any completed exchange instead of
  expecting bytes back.
- **Calling `Context.defineType` from an init macro is an error** ("Cannot
  use this API from initialization macros"). Defer the call with
  `Context.onAfterInitMacros(() -> ...)`, which works on 4.2+ as well.
