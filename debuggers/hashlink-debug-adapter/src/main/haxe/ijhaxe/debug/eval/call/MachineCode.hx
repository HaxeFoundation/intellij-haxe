package ijhaxe.debug.eval.call;

import ijhaxe.debug.Pointer;

import haxe.Int64;
import haxe.io.Bytes;

/**
	Recognises the byte patterns HashLink's JIT emits for native calls, so the
	disassembly hacks (ConstructorResolver, BoxResolver, NativeResolver,
	NativeThrowResolver) can read the constants baked into them. Reading such
	a constant out of the machine code is called "mining" throughout the
	adapter.

	On x86-64 the JIT (`jit.c`, `call_native`) emits every native call as
	`mov rax, imm64` (REX.W `48 B8`, then the 8-byte address) followed by
	`call rax` (`FF D0`). On win64 a `sub rsp, 0x20` for the shadow space can
	sit between the two, so the `call` is not at a fixed offset. On x86 the
	JIT emits the 32-bit forms: `mov eax, imm32; call eax`, and `push imm32`
	for a stack argument. The public entry points take `is64` and pick the
	form, so callers never branch on the architecture.
**/
class MachineCode {
	/**
		Upper bound on the machine code of one opcode that is scanned for these
		patterns. Real sites are a handful of instructions, so a larger span is
		not the pattern being mined.
	**/
	public static inline var MAX_SITE_BYTES = 256;

	// the leading opcode bytes of each instruction, read little-endian
	static inline var MOV_RAX = 0xB848; // 48 B8 : mov rax, imm64
	static inline var CALL_RAX = 0xD0FF; // FF D0 : call rax (call eax on x86)
	static inline var MOV_RCX = 0xB948; // 48 B9 : mov rcx, imm64 (win64 arg0)
	static inline var MOV_RDI = 0xBF48; // 48 BF : mov rdi, imm64 (SysV arg0)
	static inline var PUSH_IMM32 = 0x68; // 68 : push imm32 (x86 stack arg0)

	/**
		Mines a "set argument 0, then call a native" site, the shape the JIT
		emits for `call_native_consts` (the allocation of ONew, the boxing of
		OToDyn): a constant argument is loaded, then `mov (r/e)ax, fn; call`.
		Returns the argument constant and the called function's address, or
		null when `code[at]` does not start that shape.

		x86-64 passes the argument in a register: `mov rcx/rdi, arg` (10 bytes),
		`mov rax, fn`, `call rax`. x86 pushes it: `push arg` (5 bytes),
		`mov eax, fn`, `call eax`.
	**/
	public static function mineArgThenCall(code:Bytes, at:Int, len:Int, is64:Bool, winCall:Bool):Null<{arg:Pointer, fn:Pointer}> {
		if (is64) {
			var argMov = winCall ? MOV_RCX : MOV_RDI; // mov rcx/rdi, imm64 (10 bytes)
			if (at + 12 > len || code.getUInt16(at) != argMov) {
				return null;
			}
			var fn = movRaxImmThenCall(code, at + 10, len);
			return fn == null ? null : {arg: read64(code, at + 2), fn: fn};
		}
		if (at + 5 > len || code.get(at) != PUSH_IMM32) { // push imm32 (5 bytes)
			return null;
		}
		var fn = movEaxImmThenCall(code, at + 5, len);
		return fn == null ? null : {arg: Int64.make(0, code.getInt32(at + 1)), fn: fn};
	}

	/**
		Mines a native call site: `mov rax, imm64; call rax` on x86-64,
		`mov eax, imm32; call eax` on x86. Returns the called address, or null
		when `code[at]` does not start that shape.
	**/
	public static function mineMovImmThenCall(code:Bytes, at:Int, len:Int, is64:Bool):Null<Pointer> {
		return is64 ? movRaxImmThenCall(code, at, len) : movEaxImmThenCall(code, at, len);
	}

	/**
		If `code[at]` begins `mov rax, imm64` and a `call rax` follows within a
		few bytes (skipping an optional shadow-space `sub rsp`), returns the
		imm64 (the called address); otherwise null.
	**/
	static function movRaxImmThenCall(code:Bytes, at:Int, len:Int):Null<Pointer> {
		if (at + 12 > len || code.getUInt16(at) != MOV_RAX) {
			return null;
		}
		if (!hasCallRax(code, at + 10, len)) {
			return null;
		}
		return read64(code, at + 2);
	}

	/**
		The 32-bit form of the same JIT shape: `mov eax, imm32` (`B8`, no REX)
		followed by `call eax` (`FF D0`). Returns the imm32 zero-extended.
	**/
	static function movEaxImmThenCall(code:Bytes, at:Int, len:Int):Null<Pointer> {
		if (at + 7 > len || code.get(at) != 0xB8) {
			return null;
		}
		if (!hasCallRax(code, at + 5, len)) {
			return null;
		}
		return Int64.make(0, code.getInt32(at + 1));
	}

	// Whether `FF D0` follows within a few bytes of `from`, allowing for an optional `sub rsp,imm8`.
	static function hasCallRax(code:Bytes, from:Int, len:Int):Bool {
		var limit = from + 8 < len - 1 ? from + 8 : len - 1;
		var j = from;
		while (j < limit) {
			if (code.getUInt16(j) == CALL_RAX) {
				return true;
			}
			j++;
		}
		return false;
	}

	static function read64(code:Bytes, pos:Int):Pointer {
		return Int64.make(code.getInt32(pos + 4), code.getInt32(pos));
	}
}
