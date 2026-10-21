package ijhaxe.debug.target;

/**
	CPU register selectors for DebugApi.readRegister/writeRegister.

	The indexes match HashLink's `src/std/debug.c` (Windows x64): 0 = stack
	pointer, 1 = frame pointer, 2 = instruction pointer, 3 = flags, 4..9 = the
	debug registers Dr0-Dr3, Dr6 and Dr7, 11 = the low half of XMM0, the only
	float register the native exposes. Higher indexes silently read Rax on
	Windows; never use them.
**/
enum abstract Register(Int) from Int to Int {
	var Esp = 0;
	var Ebp = 1;
	var Eip = 2;
	var EFlags = 3;

	var Dr0 = 4;
	var Dr1 = 5;
	var Dr2 = 6;
	var Dr3 = 7;
	var Dr6 = 8;
	var Dr7 = 9;

	// RAX, the integer return register (index 10 in debug.c). It is written
	// only to save and restore it around an injected call (see
	// EvalCallInjector).
	var Eax = 10;
	var Xmm0 = 11;
}
