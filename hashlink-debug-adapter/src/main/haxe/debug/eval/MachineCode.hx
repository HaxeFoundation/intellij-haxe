package debug.eval;

import debug.Pointer;

import haxe.Int64;
import haxe.io.Bytes;

/**
 * Shared x86-64 byte patterns for the eval-call disassembly hacks
 * (ConstructorResolver, NativeResolver). HashLink's JIT (`jit.c`,
 * `call_native`) emits every native call as `mov rax, imm64` (REX.W `48 B8`,
 * then the 8-byte address) followed by `call rax` (`FF D0`). On win64 a
 * `sub rsp, 0x20` (shadow space) can sit between the two, so the `call` is not
 * at a fixed offset. These helpers recognise that shape. x86-64 only.
 */
class MachineCode {
	// little-endian first two bytes of each instruction
	public static inline var MOV_RAX = 0xB848; // 48 B8 : mov rax, imm64
	public static inline var CALL_RAX = 0xD0FF; // FF D0 : call rax
	public static inline var MOV_RCX = 0xB948; // 48 B9 : mov rcx, imm64 (win64 arg0)
	public static inline var MOV_RDI = 0xBF48; // 48 BF : mov rdi, imm64 (SysV arg0)

	/**
	 * If `code[at]` begins `mov rax, imm64` and a `call rax` follows within a
	 * few bytes (skipping an optional shadow-space `sub rsp`), returns the
	 * imm64 (the called address); otherwise null.
	 */
	public static function movRaxImmThenCall(code:Bytes, at:Int, len:Int):Null<Pointer> {
		if (at + 12 > len || code.getUInt16(at) != MOV_RAX) {
			return null;
		}
		if (!hasCallRax(code, at + 10, len)) {
			return null;
		}
		return read64(code, at + 2);
	}

	// `FF D0` within a short window from `from` (skips an optional `sub rsp,imm8`).
	public static function hasCallRax(code:Bytes, from:Int, len:Int):Bool {
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

	public static function read64(code:Bytes, pos:Int):Pointer {
		return Int64.make(code.getInt32(pos + 4), code.getInt32(pos));
	}
}
