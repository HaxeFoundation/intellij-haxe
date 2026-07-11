package debug;

/**
 * CPU register selectors for DebugApi.readRegister/writeRegister.
 *
 * Index values verified against HashLink `src/std/debug.c` (Windows x64):
 * 0 = stack pointer, 1 = frame pointer, 2 = instruction pointer, 3 = flags,
 * 4..9 = debug registers Dr0-Dr3, Dr6, Dr7.
 */
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
}
