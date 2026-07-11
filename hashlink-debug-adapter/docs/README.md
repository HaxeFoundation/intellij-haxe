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
