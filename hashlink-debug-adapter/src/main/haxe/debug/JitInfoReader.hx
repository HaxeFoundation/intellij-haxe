package debug;

import haxe.Int64;
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
 *   structSizes[1..8]                8 × int32
 *   nfunctions                       int32
 *   per function:                    nops(int32) start(int32) large(byte)
 *                                    offsets[nops+1] × (large ? int32 : uint16)
 *
 * Only protocol version 1 is supported (what HashLink 1.15 emits); a different
 * version char raises DebugError rather than risk a silent misparse.
 */
class JitInfoReader {
	static inline var SUPPORTED_VERSION = 1;

	public static function read(input:Input):JitInfo {
		input.bigEndian = false;

		var magic = String.fromCharCode(input.readByte()) + String.fromCharCode(input.readByte()) + String.fromCharCode(input.readByte());
		if (magic != "HLD") {
			throw new DebugError('Bad debug handshake magic: "$magic" (expected "HLD")');
		}
		var version = input.readByte() - "0".code;
		if (version != SUPPORTED_VERSION) {
			throw new DebugError('Unsupported debug protocol version $version (this adapter supports version $SUPPORTED_VERSION)');
		}

		var flags = input.readInt32();
		var is64 = (flags & 1) != 0;
		var boolSize4 = (flags & 2) != 0;
		var threads = (flags & 4) != 0;
		var winCall = (flags & 8) != 0;

		var hlVersionRaw = input.readInt32();
		var major = (hlVersionRaw >> 16) & 0xFF;
		var minor = (hlVersionRaw >> 8) & 0xFF;
		var patch = hlVersionRaw & 0xFF;

		// pid present since protocol carried it (hlVersion >= 1.07)
		var hasPid = major > 1 || (major == 1 && minor >= 7);
		var pid = hasPid ? input.readInt32() : 0;

		var threadsPtr = readPointer(input, is64);
		var globalsPtr = readPointer(input, is64);
		var jitCodeBase = readPointer(input, is64);
		var codeSize = input.readInt32();
		var typesPtr = readPointer(input, is64);

		var structSizes = [0]; // index 0 unused, to match the 1..8 wire indices
		for (_ in 0...8) {
			structSizes.push(input.readInt32());
		}

		var nfunctions = input.readInt32();
		var functions:Array<JitFunction> = [];
		for (_ in 0...nfunctions) {
			var nops = input.readInt32();
			var start = input.readInt32();
			var large = input.readByte() != 0;
			var offsets = new Array<Int>();
			for (_ in 0...(nops + 1)) {
				offsets.push(large ? input.readInt32() : input.readUInt16());
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

	static function readPointer(input:Input, is64:Bool):Pointer {
		if (is64) {
			var low = input.readInt32();
			var high = input.readInt32();
			return Int64.make(high, low);
		}
		return Int64.make(0, input.readInt32());
	}
}
