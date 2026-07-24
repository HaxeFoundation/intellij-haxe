package debug;

/**
	The platform the adapter and its debuggee run on, resolved ONCE at runtime.

	Runtime, not `#if windows`: the adapter ships as prebuilt `.hl` bytecode
	built on one machine and run on another, so a compile-time check would bake
	in the BUILD platform. `Sys.systemName()` is the only thing the standard
	library offers, and it returns a string, so the comparison lives here
	instead of being repeated at every call site.

	This is about the HOST. Code that selects an ABI or a struct layout
	(`FrameLayout`, `ModuleDebugInfo`) takes `isWindows` as a parameter instead,
	so its tests can exercise both layouts on either machine — do not replace
	those parameters with this.
**/
class HostPlatform {
	public static final IS_WINDOWS = Sys.systemName() == "Windows";

	/** True on every ptrace-based host (linux), where hl's debug natives differ. */
	public static inline function isPtraceBased():Bool {
		return !IS_WINDOWS;
	}
}
