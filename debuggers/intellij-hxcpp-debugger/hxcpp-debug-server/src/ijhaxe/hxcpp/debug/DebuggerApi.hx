package ijhaxe.hxcpp.debug;

/**
	Everything the server needs from `cpp.vm.Debugger`, behind an interface so
	the unit tests run under the interpreter against a scriptable fake (the
	native implementation is cpp-only).

	The data types alias the std classes DIRECTLY on the cpp target — the
	native implementation hands `getThreadInfos()` results over untouched and
	upstream std evolution is tracked for free. Every other target (the
	interpreter running the unit tests) gets API-identical stubs below,
	because the compiler rejects the cpp package there ("You cannot access
	the cpp package while targeting eval"). Keep the stubs' surface exactly
	in sync with std `cpp/vm/Debugger.hx`; the cpp compile of any fixture
	cross-checks shared code against the real types.
**/
interface DebuggerApi {
	/** Marks the calling thread as never-breaking (the server thread). */
	function excludeCurrentThread():Void;

	/**
		Enables breaking on the calling thread — hxcpp does NOT debug a thread
		until this is called, so the main thread must opt in or no breakpoint
		ever fires. Called on the main thread during server startup.
	**/
	function enableCurrentThread():Void;

	/**
		Installs the runtime stop/lifecycle notification handler. CAUTION: the
		runtime invokes it on the STOPPING thread — implementations of the
		server must queue and return, never do protocol I/O inside it.
	**/
	function setEventHandler(handler:DebugEvent->Void):Void;

	/** All live threads with their current status and stacks. */
	function threads():Array<DebugThread>;

	/** Resumes `threadNumber` (-1 = all) `count` times. */
	function continueThreads(threadNumber:Int, count:Int):Void;

	/** Stops all debuggable threads now; `wait` blocks until they are stopped. */
	function breakNow(wait:Bool):Void;

	/** Steps `threadNumber` once (a StepType.* value); resumes on completion. */
	function stepThread(threadNumber:Int, stepType:Int):Void;

	/**
		The runtime's source file names as they appear in stack positions (e.g.
		"Main.hx") — the form `addFileLineBreakpoint` matches against.
	**/
	function files():Array<String>;

	/**
		The absolute path of each `files()` entry, index-aligned — used to
		suffix-match an IDE-supplied path onto a runtime file key (handles moved
		projects and CI-built executables where the prefixes differ).
	**/
	function filesFullPath():Array<String>;

	/** Installs a breakpoint on `file`:`line`, returning its runtime number. */
	function addFileLineBreakpoint(file:String, line:Int):Int;

	/**
		Installs a breakpoint at the ENTRY of `className.functionName` (dotted
		class path, bare function name — the names generated code carries in its
		stack frames), returning its runtime number. Fires when a frame for that
		function is at its first line — the smart-step-into landing.
	**/
	function addClassFunctionBreakpoint(className:String, functionName:String):Int;

	/** Removes a previously installed breakpoint by its runtime number. */
	function deleteBreakpoint(number:Int):Void;

	/** The names of the local variables (and `this`) visible in a frame. */
	function stackVariables(threadNumber:Int, frame:Int):Array<String>;

	/** The live value of one local in a frame (a real Dynamic, any frame). */
	function stackVariableValue(threadNumber:Int, frame:Int, name:String):Dynamic;

	/**
		Writes `value` to a local in a frame — ANY frame (the marquee fix over
		vshaxe's top-frame-only writes). Returns the value actually stored.
	**/
	function setStackVariableValue(threadNumber:Int, frame:Int, name:String, value:Dynamic):Dynamic;
}

/** A runtime notification, re-delivered on the server thread. */
enum DebugEvent {
	ThreadCreated(threadNumber:Int);
	ThreadTerminated(threadNumber:Int);
	ThreadStarted(threadNumber:Int);
	// Captured ON THE STOPPING THREAD (status, hit breakpoint number, and the
	// stack — already trimmed of the debugger's own frames), so the server
	// thread reports the stop and serves stackTrace without a cross-thread
	// getThreadInfo race. The stack is innermost-LAST. `description` is the
	// runtime's criticalErrorDescription: null except for exception/critical
	// stops ("Uncatchable Throw: <value>", "Null Object Reference", ...).
	ThreadStopped(threadNumber:Int, status:Int, breakpoint:Int, stack:Array<DebugStackFrame>, description:Null<String>);
}

/** The STEP_* constants mirror cpp.vm.Debugger. */
class StepType {
	public static inline var INTO = 1;
	public static inline var OVER = 2;
	public static inline var OUT = 3;
}

// The std debugger data types on cpp; API-identical stubs (one type per file,
// package ijhaxe.hxcpp.debug.stubs) everywhere else.
#if cpp
typedef DebugParameter = cpp.vm.Debugger.Parameter;
typedef DebugStackFrame = cpp.vm.Debugger.StackFrame;
typedef DebugThread = cpp.vm.Debugger.ThreadInfo;
#else
typedef DebugParameter = ijhaxe.hxcpp.debug.stubs.Parameter;
typedef DebugStackFrame = ijhaxe.hxcpp.debug.stubs.StackFrame;
typedef DebugThread = ijhaxe.hxcpp.debug.stubs.ThreadInfo;
#end
