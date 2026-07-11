package debug;

/**
 * A breakpoint the client asked for: the source line and the id the adapter
 * assigned to it (ids are stable across re-verification).
 */
typedef RequestedBreakpoint = {
	var id:Int;
	var line:Int;
}
