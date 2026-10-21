package ijhaxe.debug;

/**
	The platform the adapter and its debuggee run on, detected once at runtime.

	A compile-time `#if windows` would not work: the adapter ships as prebuilt
	`.hl` bytecode, so it would report the platform the adapter was BUILT on.
	The standard library only offers the `Sys.systemName()` string, so the
	comparison lives here rather than at every call site.

	This describes the HOST. Code that selects a calling convention
	(`FrameLayout`) takes it as a parameter instead, so its tests can exercise
	both conventions on any machine. Do not replace that parameter with this.
**/
class HostPlatform {
	public static final IS_WINDOWS = Sys.systemName() == "Windows";

	/** True on hosts whose debug natives are built on ptrace (linux), which behave differently. */
	public static inline function isPtraceBased():Bool {
		return !IS_WINDOWS;
	}
}
