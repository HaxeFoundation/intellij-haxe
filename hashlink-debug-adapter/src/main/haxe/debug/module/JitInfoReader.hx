package debug.module;

import debug.DebugError;
import debug.Pointer;

import haxe.Int64;
import haxe.io.BytesInput;
import haxe.io.Input;

/**
 * Parses the "HLD1" handshake the debuggee VM sends over the --debug socket.
 *
 * Layout empirically verified against HashLink 1.15 (little-endian, 64-bit),
 * cross-checked with hashlink `src/debugger.c` and vshaxe/hashlink-debugger
 * `hld/JitInfo.hx`:
 *
 *   "HLD" + version char             3 + 1 bytes
 *   flags                            int32   bit0 is64, bit1 boolSize4, bit2 threads, bit3 winCall
 *   hlVersion                        int32   (major<<16)|(minor<<8)|patch
 *   pid                              int32   (present when hlVersion >= 1.07)
 *   threads / globals / jitCode      pointer each (8 bytes when is64)
 *   codeSize                         int32
 *   types                            pointer
 *   structSizes[1..8]                8 x int32
 *   nfunctions                       int32
 *   per function:                    nops(int32) start(int32) large(byte)
 *                                    offsets[nops+1] x (large ? int32 : uint16)
 *
 * The input is read in exact-size chunks (input.read(n)): the debuggee sends the
 * whole handshake and then blocks, so a single over-read would hang forever.
 * Byte-at-a-time reads over a socket are also far too slow, so each field group
 * is pulled in one read.
 *
 * Only protocol version 1 is supported (what HashLink 1.15 emits); a different
 * version char raises DebugError rather than risk a silent misparse.
 */
class JitInfoReader {
	static inline var SUPPORTED_VERSION = 1;

	public static function read(input:Input):JitInfo {
		var head = chunk(input, 4);
		var magic = String.fromCharCode(head.readByte()) + String.fromCharCode(head.readByte()) + String.fromCharCode(head.readByte());
		if (magic != "HLD") {
			throw new DebugError('Bad debug handshake magic: "$magic" (expected "HLD")');
		}
		var version = head.readByte() - "0".code;
		if (version != SUPPORTED_VERSION) {
			// Be precise about WHICH version this is: the handshake protocol digit
			// (the "N" in the runtime's HLDN greeting) — not the HashLink runtime
			// version and not the .hl bytecode format version.
			throw new DebugError('Unsupported debug handshake protocol version HLD$version from the HashLink runtime; '
				+ 'this adapter supports HLD$SUPPORTED_VERSION (emitted by HashLink 1.x). '
				+ 'A newer HashLink has likely changed the debug wire format - the adapter needs updating.');
		}

		var flags = chunk(input, 4).readInt32();
		var is64 = (flags & 1) != 0;
		var boolSize4 = (flags & 2) != 0;
		var threads = (flags & 4) != 0;
		var winCall = (flags & 8) != 0;
		var pointerSize = is64 ? 8 : 4;

		var hlVersionRaw = chunk(input, 4).readInt32();
		var major = (hlVersionRaw >> 16) & 0xFF;
		var minor = (hlVersionRaw >> 8) & 0xFF;
		var patch = hlVersionRaw & 0xFF;

		var hasPid = major > 1 || (major == 1 && minor >= 7);
		var pid = hasPid ? chunk(input, 4).readInt32() : 0;

		// threads + globals + jitCode + codeSize + types, in one read
		var block = chunk(input, pointerSize * 3 + 4 + pointerSize);
		var threadsPtr = readPointer(block, is64);
		var globalsPtr = readPointer(block, is64);
		var jitCodeBase = readPointer(block, is64);
		var codeSize = block.readInt32();
		var typesPtr = readPointer(block, is64);

		var sizes = chunk(input, 8 * 4);
		var structSizes = [0]; // index 0 unused, to match the 1..8 wire indices
		for (_ in 0...8) {
			structSizes.push(sizes.readInt32());
		}

		var nfunctions = chunk(input, 4).readInt32();
		var functions:Array<JitFunction> = [];
		for (_ in 0...nfunctions) {
			var fixed = chunk(input, 9);
			var nops = fixed.readInt32();
			var start = fixed.readInt32();
			var large = fixed.readByte() != 0;
			var elementSize = large ? 4 : 2;
			var offsetBytes = chunk(input, (nops + 1) * elementSize);
			var offsets = new Array<Int>();
			for (_ in 0...(nops + 1)) {
				offsets.push(large ? offsetBytes.readInt32() : offsetBytes.readUInt16());
			}
			functions.push({nops: nops, start: start, large: large, offsets: offsets});
		}

		return new JitInfo({
			is64: is64, boolSize4: boolSize4, threads: threads, winCall: winCall,
			hlVersionMajor: major, hlVersionMinor: minor, hlVersionPatch: patch,
			pid: pid, threadsPtr: threadsPtr, globalsPtr: globalsPtr, jitCodeBase: jitCodeBase,
			codeSize: codeSize, typesPtr: typesPtr, structSizes: structSizes, functions: functions
		});
	}

	// Reads exactly `size` bytes and returns them as a little-endian input.
	static function chunk(input:Input, size:Int):BytesInput {
		var bytes = new BytesInput(input.read(size));
		bytes.bigEndian = false;
		return bytes;
	}

	static function readPointer(input:BytesInput, is64:Bool):Pointer {
		if (is64) {
			var low = input.readInt32();
			var high = input.readInt32();
			return Int64.make(high, low);
		}
		return Int64.make(0, input.readInt32());
	}
}
