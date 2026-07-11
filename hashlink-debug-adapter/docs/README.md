# Adapter internals & HashLink gotchas

This document records the non-obvious problems we hit building the HashLink debug
adapter and the reasoning behind the workarounds. The adapter runs *on* the
HashLink VM (it is itself an `.hl` program) while it debugs *another* HashLink VM
out of process. That "HL debugging HL" arrangement is where most of the surprises
come from. Read this before touching the session/launch/handshake/stepping code
or adding features that spawn threads, read the debuggee's memory, or talk to the
VM's debug socket.

The overriding invariant is: **the adapter must never block on the debuggee.**
Every workaround below exists to keep that true.

---

## 1. The GC deadlock (output pumps)

### Symptom
With the debuggee's stdout/stderr "pump" threads running, `launch` would hang
partway through parsing the VM handshake (traced to the middle of the
`JitInfoReader` function-table loop) with **no exception and no error response**.
Disabling the pumps made the entire debug lifecycle work.

### Cause
HashLink has a stop-the-world garbage collector. When any thread needs to collect,
**every** thread registered with the runtime must reach a GC safepoint first. A
thread parked inside a *blocking* native call only reaches a safepoint if that
native marks itself as "blocking" before it parks.

- HL's **socket** read natives do this internally. That is why the DAP
  reader/writer threads (parked in socket reads) never caused trouble — the worker
  thread could allocate freely while they were parked.
- HL's **process-pipe** read native (`process_stdout_read`, see
  `std/hl/_std/sys/io/Process.hx`) does **not**. A pump thread parked in
  `readBytes` on the child's stdout is *not* at a safepoint.

So the sequence was: pump thread parks in `process_stdout_read` → session thread
allocates (the handshake parse allocates hundreds of small buffers) → session
thread triggers GC → GC waits for the parked pump thread to reach a safepoint →
it never does → **deadlock**. The hang looked like it was in `JitInfoReader`
only because that is where the session thread happened to allocate.

### Workaround (`DebuggeeProcess.blockingRead`)
Wrap the blocking pipe read in an explicit blocking section:

```haxe
hl.Gc.blocking(true);
var read = input.readBytes(buffer, 0, buffer.length); // parks here, GC-safe now
hl.Gc.blocking(false);
```

`hl.Gc.blocking(true)` tells the collector "do not wait for me while I'm parked."
The catch, per the HL docs, is that **you must not allocate while inside a blocking
section.** That is safe here because:

- `readBytes` fills a **preallocated** buffer (`Bytes.alloc(4096)` created once,
  outside the loop), so the common parked-read path allocates nothing.
- The only allocation is the terminal `haxe.io.Eof` the read throws at end of
  stream — but that also ends the pump, so it is a one-shot edge case. We reset
  `blocking(false)` in a `catch` before rethrowing so we spend as little time as
  possible allocating inside the section.

The calls are `#if hl`-guarded so the module still compiles under the interpreter
and other targets (where `hl.Gc` does not exist and there is no such GC).

### Where this bites again
Any **new background thread that makes a blocking native call which is not
GC-safe** will reintroduce this. Likely candidates as we add features:

- more child pipes, or reading files/named pipes on a helper thread;
- any `@:hlNative` blocking call we add and run off the main/session thread;
- future variable-inspection / evaluation / stepping helpers that offload work to
  a thread.

Rules of thumb:
1. Prefer doing blocking native I/O on a thread that does nothing else, and wrap
   the blocking call in `hl.Gc.blocking(true/false)`.
2. Never allocate inside a blocking section — read into a preallocated buffer.
3. Socket reads are already GC-safe; process/file reads are not. When unsure,
   assume a native call is *not* GC-safe and wrap it.

---

## 2. The handshake read: don't parse straight off the socket

### Symptom
Reading the VM's `--debug` handshake directly from the socket either was far too
slow or **hung about ~10 KB into the ~15 KB message**, stalling mid-way through
the function table.

### Cause
Two independent problems, both from "HL reading a socket another HL is writing":

- **Byte-at-a-time is too slow.** The handshake is ~15 KB. Parsing it field by
  field with `readByte` is one `recv` syscall per byte — thousands of syscalls,
  taking seconds.
- **Exact-size / buffered reads over-request and deadlock.** HL's socket
  `readBytes` blocks until it has filled the *entire* requested length (it does
  not return a partial buffer). The VM sends the whole handshake and then blocks
  on a 1-byte `recv` waiting for us to release it. If our read ever asks for more
  bytes than remain before that point, it waits forever for bytes that will never
  arrive. Combined with the two processes' send/recv buffer interplay, the reader
  stalls partway through the message.

### Workaround (`DebugSession.readHandshake`)
Drain the **whole** handshake into memory first, then parse from an in-memory
`BytesInput`:

```haxe
handshakeSocket.setTimeout(HANDSHAKE_READ_TIMEOUT_S); // 0.5s
// loop readBytes(buffer /* 8192 */) into a BytesBuffer until a read times out
```

The VM sends everything and then goes quiet (parked on its 1-byte `recv`). We use
that quiet as the end-of-message signal: once a read **times out**, we have the
full handshake. Parsing then happens against a `BytesInput`, which cannot block or
deadlock. `JitInfoReader` reads exact-size chunks (`input.read(n)`) from that
in-memory buffer safely — the same code that deadlocks on a live socket is fine on
a `BytesInput`.

### Buffer/timeout sizes and why
- **Drain buffer = 8192 bytes.** The handshake is ~15 KB, so this drains it in two
  reads. Big enough to be fast, small and cheap enough to preallocate once.
  Partial reads are fine here — we accumulate whatever each read returns.
- **Drain timeout = 0.5 s.** Comfortably longer than loopback latency (so we never
  cut the message short) yet short enough that it adds no noticeable launch delay.
  It only ever fires once, at the true end of the message.
- **Pump buffer = 4096 bytes** (section 1). Output is streamed, so the size just
  bounds one copy; 4096 is a natural pipe-chunk size and partial reads are fine.

### Where this bites again
Any future exchange with the VM over a socket where the peer **sends a
variable-length message and then waits**. If we add more VM socket protocols:
- don't parse incrementally straight off the socket;
- either drain-then-parse (as here) or make the protocol length-prefixed so you
  know exactly how many bytes to read;
- never issue a socket read larger than the bytes you know are still coming.

---

## 3. Single-step / trap flag (stepping over a breakpoint)

### Symptom
A breakpoint inside a loop was only ever hit **once**; after the first `continue`
the program ran to completion.

### Cause (two parts)
1. **Compiler loop unrolling.** A constant range such as `for (i in 0...3)` is
   unrolled by the Haxe compiler, so the loop body is emitted three times and a
   single breakpoint address is genuinely hit only once. The test fixture now
   derives its loop bound from a runtime value (`Std.parseInt("3")`) specifically
   to keep the loop rolled — see the comment in `test-fixtures/src/Main.hx`.
2. **The trap flag was never cleared.** To continue past a breakpoint we restore
   the original instruction byte, set the CPU trap flag (`EFlags` bit `0x100`) to
   single-step over that one instruction, then re-arm the `0xCC`. We were setting
   the trap flag but never clearing it, so the debuggee kept single-stepping and
   the re-armed breakpoint never produced a clean hit. Fix: `clearTrapFlag` after
   the single step, before resuming (`DebugSession.stepOverAndResume`).

### Where this bites again
- **Stepping features** (step in/over/out) will reuse the trap flag. Always pair a
  set with a clear.
- **Conditional breakpoints / watchpoints** will touch registers (watchpoints use
  the debug registers Dr0–Dr3) the same way; keep register state changes balanced.
- When writing test fixtures, remember the compiler will unroll constant loops,
  inline small functions, and reorder — put breakpoint targets behind runtime
  values so the bytecode has a single, stable location.

---

## 4. Reading debuggee memory defensively

Reading another process's memory can fail: an address may be unmapped, protected,
or simply wrong if our address math is off. `debug_read` reports failure (or a
short read) rather than crashing us, but callers must treat that as expected:

- The stack walker validates each candidate return address with
  `JitInfo.isCodePtr` **before** trusting it, and stops at the first address that
  is not in the JIT code region, so a bad frame pointer ends the walk instead of
  wandering into garbage.
- Future **variable inspection / expression evaluation** will read arbitrary
  addresses (object fields, array elements, pointers we follow). Those reads must
  assume any dereference can fail: check the `debug_read` result, bound the number
  of bytes, and never loop on a pointer chain without a depth cap and a
  code/heap-range sanity check. A wrong pointer must degrade to "unavailable," not
  hang or crash the adapter.

---

## 5. Source-level stepping (step over / into / out)

Stepping is **temporary-breakpoint planting driven by the opcode control-flow
graph**, not machine single-stepping. Single-stepping instruction-by-instruction
would walk through the entire body of any function a line calls (and could run for
a very long time in library code); planting an INT3 where the step should land and
then just resuming is both faster and simpler.

### How targets are computed
On a step, from the current `(function, opcode, line)`:
- `CodeGraph` walks the function's opcodes following successors. A jump's target is
  `opIndex + 1 + offset` (conditional jumps add both the fall-through and the
  target; `OSwitch` adds every case; `ORet`/`OThrow` are terminal). The walk is
  guarded by a visited set so loops (back-edges) terminate.
- **next (step over):** plant a temp INT3 at the first opcode of every reachable
  line other than the current one, and — if a return is reachable — at the caller's
  return address. Calls are *not* entered; the call runs and returns to the next
  opcode, which the line scan already covers.
- **stepIn:** the same, plus a temp at the entry (first opcode) of every statically
  resolvable callee (`OCall0..N`). Dynamic/virtual/closure calls
  (`OCallMethod`/`OCallThis`/`OCallClosure`) can't be resolved from the bytecode, so
  stepIn falls back to step-over behaviour for those — a documented limitation.
- **stepOut:** a temp only at the caller's return address (frame 1 from the stack
  walker).

Then the debuggee is resumed via the existing step-over-the-current-instruction
dance. The pieces are all reused: opcode→line (`ModuleDebugInfo`), opcode→address
(`JitInfo.addressOf`), the return address (`StackWalker`), INT3 patch/restore
(`Breakpoints`, now with a separate temp set that is cleared on any stop).

### The frame guard (recursion)
A temp INT3 lives at a code address, so in a **recursive** function the same temp
can trap at a *deeper* frame than the step started in — that is not where the step
should land. We record the stack pointer at step start; the stack grows down, so a
shallower-or-equal frame has `Esp >= startEsp`. For step over/out a temp hit only
counts as the landing when `Esp >= startEsp`; otherwise we single-step past that
temp, re-arm it, and keep running. stepIn needs no guard (its callee-entry target is
*supposed* to be a deeper frame). This is the same class of subtlety as the
trap-flag bug in §3 — the stepping integration test must cover a repeated/recursive
line, not just a straight-line step.

### Breakpoints always win
If a user breakpoint and a step target trap at the same time, the user breakpoint
wins (the stop is reported as `reason:"breakpoint"`, not `"step"`). All temporary
breakpoints are removed on every stop (`Breakpoints.clearTemps`), and on debuggee
exit/exception too, so a step never leaves stray INT3s behind.

### Where this bites again
- **Conditional breakpoints** and **run-to-cursor** will reuse the same temp-set +
  frame-guard machinery.
- If HL bytecode ops or their jump-offset convention change, `CodeGraph` is the one
  place to update (its successor arithmetic is unit-tested with synthetic opcodes).
- Watch the interaction between a step and the step-over-the-current-instruction
  logic (both toggle the trap flag) — keep set/clear balanced.

---

## 6. Reading variables (locals, object members, statics)

The `scopes`/`variables` DAP requests turn a stopped frame into values. This is the
most layout-sensitive code in the adapter: every offset is reconstructed from the
bytecode, so a single wrong constant yields plausible-looking garbage. The defence is
empirical — `VariablesIntegrationTest` stops where locals/members/statics have
**known** values and asserts the adapter reads exactly those.

### Frame layout (where a local lives)

HashLink 1.15 uses the **legacy register-location** scheme (the per-instruction
location tables only exist in HL ≥ 2, which no released HashLink ships). A local's
address is `ebp + offset`, where `offset` is a *static per-function* value computed by
`FrameLayout.registerOffsets` — a port of `hld/Module.getFunctionRegs`, itself a port
of the `jit.c` prologue:

- locals and register-passed args spilled to the stack get a **negative** offset
  (`size += typeSize; size += pad; offset = -size`);
- stack-passed args get a **positive** offset (`argsSize + ptr*2`, skipping the return
  address + saved rbp), then `argsSize += stackSize`.
- **Windows x64** passes every argument on the stack (simple). **SysV (64-bit
  non-Windows)** passes the first 6 of each of the int/float classes in registers,
  spilled into locals. 32-bit is all-stack. Only Windows is verified locally; the SysV
  branch is implemented from the ABI but unverified (same posture as the handshake).

`ebp` per frame comes from the stack walk (top frame = live `Ebp`; callers =
`savedEbp`). `LocalsResolver` decides *which* register a source name maps to at the
current op, from the debug `assigns` table (args have `position < 0`; locals have
`position >= 0` with the register = the op's `dst`, latest-wins per register).

### Value layout (how a slot is decoded)

`ValueReader` reads at 64-bit offsets (port of `hld/Eval.readVal`):
- primitives **at** the slot: `ui8=1, ui16=2, i32=4 (signed), i64=8 (hi@+4/lo@+0),
  f32=4, f64=8, bool=1`;
- a **String** derefs then reads `bytes` ptr @ +8 and `length` @ +16, `length*2`
  bytes of UTF-16;
- an **object** (`HObj`/`HStruct`) has a `hl_type*` header at +0, then fields laid out
  by `ObjectLayout`: start at one pointer (the header), **superclass fields first**
  (recurse `tsuper`), each field aligned to its own `typeSize`;
- a null pointer slot reads as `null`; anything still unknown (maps' native tables,
  `HDynObj`, `HAbstract`, bytes) falls back to `<TypeName> @ 0xADDR`, non-expandable.

### Rich values (arrays, Dynamic, enums, anon objects, closures)

Most of these need the **runtime type**, not the static one. `RuntimeTypes` reads an
`hl_type*`: kind i32 @ +0 (the format lib's HLType constructor order matches the C
`hl_type_kind` indices exactly), kind data pointer @ +8. Primitive kinds map
directly; HOBJ/HSTRUCT read the UCS-2 class name (`hl_type_obj` name @ data+16) and
resolve it against the module's types by name; unresolvable kinds return null and
the caller keeps the static type.

- **Haxe arrays** are std wrappers, special-cased by class name:
  `hl.types.ArrayBytes_<T>` (length @ +8, bytes ptr @ +16, elements packed at the
  element type's stride — element type from the name suffix: `Int`→i32,
  `Float`→f64, …) and `hl.types.ArrayObj` (length @ +8, native varray @ +16; only
  `length` of the varray's pointer slots are live; the element type comes from the
  varray's runtime `at` @ +8). A native **varray** itself: at @ +8, size @ +16,
  elements from +24. Listing is capped at 512 elements with a trailing "…" marker
  (no DAP paging is advertised).
- **Dynamic** (vdynamic): runtime type @ +0, payload @ +8. If the runtime type is a
  pointer kind, the vdynamic address *is* the value (no extra indirection) — decode
  in place; primitives read the payload. **`Null<T>` boxes** likewise hold the value
  at +8.
- **Objects** prefer their runtime class (header @ +0) over the static type, so a
  Base-typed slot holding a Sub expands with Sub's fields.
- **Enums** (venum): constructor index i32 @ +8; param offsets from `EnumLayout` —
  header is ptr + i32, then each param aligned with **`Align.padStruct`**, i.e. the
  C struct alignments from the handshake's `structSizes`, *not* `typeSize`. That
  distinction is real: an i32 first param lands at **+12**, inside the venum
  header's tail padding. Display is `Ctor(v0, v1)` with params as children.
- **Anonymous structures** (vvirtual): header `t`/`value`/`next` (3 pointers), then
  one *indirect field pointer* per field (in the HVirtual type's field order); each
  points at the field's slot. A null field pointer means the field lives on the
  wrapped dynobj — shown as `?` rather than chased.
- **Closures** (vclosure): function pointer @ +8, resolved to a name via the jit
  table (`JitInfo.resolveAddress` → `functionName`); lambdas without a proto
  binding render as `function fn@N`.

**Fixture gotcha**: the Haxe analyzer constant-folds aggressively even with
`-debug`. An array whose every read is statically known (`ints[2]`) never
materializes — the fixture indexes with runtime values (`ints[n]`) and routes anon
objects through `Std.string` so the locals actually exist at the breakpoint.

### Statics (the globals table)

A class's static fields live in a singleton object reachable through the **global data
block** (`globalsPtr` from the handshake). The tricky part is the indirection:

- the **instance** type (`Config`) carries the `globalValue`, but the fields
  (`version`, `title`) and static-method bindings live on the **`$Config` container**
  type — and the singleton is itself a global *of that container type*. So we find the
  global index by scanning `data.globals` for the container type name, not from
  `proto.globalValue`.
- the singleton address is `readPointer(globalsPtr + GlobalTable.offsetOf(index))`,
  where `GlobalTable` replicates `hl_module_init`'s `globals_indexes` (index order,
  each global aligned to its `typeSize`) — the same alignment discipline as
  `ObjectLayout`.
- the container also holds its static **methods** as function-typed fields; those are
  hidden from the scope (only data fields are shown, and the scope is omitted entirely
  when a class has no static data).

The owning class for the "Statics" scope is the one owning the stopped frame's
function: static methods map back to their `$Class` container via the container's
`bindings` (`binding.mid` = the method's findex).

### The per-stop reference registry

`variablesReference`s (and the frame cache) are handed out lazily from `REF_BASE`
(1000) and **cleared on every resume/step** (`refreshFrames`, `stepOverAndResume`). A
reference outlives its stop only as freed/moved memory — the GC can relocate objects —
so a stale expand must never read. Keep the clear in the same places the frame cache is
invalidated.

---

## 7. Disconnect teardown (intermittent detach hang)

### Symptom
Rarely — roughly every other *full* integration-suite run, never in a single test
class — a test timed out waiting for the `disconnect` response. Always the last
request of a test that had done several continue/step cycles; everything before it
succeeded. Unreproducible with tracing enabled (a classic timing heisenbug).

### Cause (best supported theory)
`handleDisconnect` called `DebugActiveProcessStop` (via `api.stop`) while the
debuggee was **suspended at an un-continued debug event** (parked at an INT3 whose
event we never passed to `ContinueDebugEvent` — that normally happens on resume).
Windows wants outstanding debug events continued before a detach; detaching a
suspended debuggee occasionally hung the calling (session) thread, so the
disconnect response was never emitted.

### Workaround (`DebugSession.handleDisconnect`)
Teardown order is now: **kill → continue the pending event → detach → close**.
Killing first works on a suspended process and guarantees the debuggee cannot run
into another breakpoint after we release it; the resume then just lets the
termination complete; the detach finally runs against a process with no pending
events.

### What the tracing later showed (and the instrumentation that stays)
A later recurrence was captured with full pipeline tracing and **exonerated the
adapter**: the failing session had received the disconnect request, torn down
cleanly, and written the response frame to the socket — the loss was client-side.
The one silent failure mode there was `DapClient`'s reader thread: any
framing/decode exception killed the demultiplexer without a word, after which
every request times out with no hint why. It now reports its own death (unless
the close was deliberate).

Diagnostics kept in place, all gated by `DAP_ADAPTER_TRACE=1` (the integration
tests set it):
- the adapter traces every received request, every sent frame (first 100 chars),
  every session command, non-timeout wait outcomes, and each disconnect stage;
- `debug.Trace` serializes writes (three threads trace; unsynchronized stderr
  writes interleave bytes into garbage);
- the tests' base class drains the adapter's merged stdout/stderr with a
  **background gobbler for the whole test** and prints it on teardown. The
  gobbler is load-bearing: the trace volume can exceed the OS pipe buffer, and
  an undrained pipe would block the adapter mid-write — a self-inflicted hang.

---

## Quick reference

| Concern | Rule |
|---|---|
| Blocking native call on a background thread | Wrap in `hl.Gc.blocking(true/false)`; read into a preallocated buffer; allocate nothing inside the section |
| Socket vs process/file reads | Socket reads are GC-safe; process/file reads are not |
| Reading a variable-length VM message off a socket | Drain-then-parse (with a read timeout) or length-prefix; never over-request |
| HL socket `readBytes` | Blocks until the full requested length is read — do not ask for more than is coming |
| Trap flag for single-step | Always clear what you set |
| Test fixtures for breakpoints | Use runtime values so the compiler can't unroll/inline the target away |
| Reading debuggee memory | Assume any read can fail; validate pointers; cap depth |
| Stepping | Plant temp INT3s at CFG-computed targets; clear them on every stop; user breakpoints win; frame-guard step over/out against recursion |
| Local address | `ebp + FrameLayout.offset(register)`; legacy scheme (HL 1.15); Windows all-stack args, SysV first-6-register |
| Value decode | Verify against known values in `VariablesIntegrationTest` — wrong offsets read as plausible garbage |
| Object fields | Header pointer first, superclass fields first, each aligned to its `typeSize` (`ObjectLayout`) |
| Statics | Singleton is a global *of the `$Class` container type*; find its index by scanning `data.globals`, not `proto.globalValue` |
| Runtime types | hl_type kind @ +0 == format HLType constructor index; resolve HOBJ by UCS-2 name, fall back to the static type |
| Enum params | Align with `padStruct` (handshake structSizes), not `typeSize` — an i32 param packs at +12 |
| Fixture locals | Index arrays with runtime values or the analyzer folds them away even with `-debug` |
| Disconnect teardown | kill → continue pending event → detach → close; never detach a suspended debuggee |
| variablesReference lifetime | Per-stop only; cleared on every resume/step or a stale expand reads freed/moved memory |
