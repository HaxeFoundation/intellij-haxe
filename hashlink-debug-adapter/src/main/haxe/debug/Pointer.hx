package debug;

/**
 * A memory address in the debuggee. HashLink addresses are pointer-sized
 * (64-bit on the platforms we target), so we model them as Int64 regardless
 * of the host, and read them as 4 or 8 bytes per the handshake `is64` flag.
 */
typedef Pointer = haxe.Int64;
