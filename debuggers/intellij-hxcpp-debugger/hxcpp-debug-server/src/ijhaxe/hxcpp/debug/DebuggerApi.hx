package ijhaxe.hxcpp.debug;

/**
	Everything the server needs from `cpp.vm.Debugger`. It is an interface so
	that the unit tests can run under the interpreter against a scriptable
	fake; the native implementation is cpp-only.

	On the cpp target the data types are aliases of the std classes, so the
	native implementation passes `getThreadInfos()` results through untouched
	and follows changes to the std API automatically. Every other target,
	including the interpreter that runs the unit tests, gets stubs with the
	same API, because the compiler rejects the cpp package there ("You cannot
	access the cpp package while targeting eval"). Keep the stubs exactly in
	sync with std `cpp/vm/Debugger.hx`; compiling any native fixture checks
	the shared code against the real types.
**/
interface DebuggerApi {
	/** Marks the calling thread (the server thread) as one that never breaks. */
	function excludeCurrentThread():Void;

	/**
		Enables breaking on the calling thread. hxcpp does NOT debug a thread
		until this is called, so without it no breakpoint on the main thread
		ever fires. Called on the main thread during server startup.
	**/
	function enableCurrentThread():Void;

	/**
		Installs the handler for runtime stop and thread lifecycle events. The
		runtime calls it on the thread the event concerns, for example the
		thread that stopped. The handler must therefore only queue the event
		and return; protocol I/O belongs on the server thread.
	**/
	function setEventHandler(handler:DebugEvent->Void):Void;

	/** All live threads with their current status and stacks. */
	function threads():Array<DebugThread>;

	/**
		Resumes every stopped thread. `threadNumber` continues `count` times,
		so it stops again only at its `count`-th breakpoint hit (1 = the next);
		every other thread continues once.
	**/
	function continueThreads(threadNumber:Int, count:Int):Void;

	/** Stops all debuggable threads now; with `wait`, blocks until they have stopped. */
	function breakNow(wait:Bool):Void;

	/** Steps `threadNumber` once (a StepType value), which also resumes it. */
	function stepThread(threadNumber:Int, stepType:Int):Void;

	/**
		The runtime's source file names as they appear in stack positions
		(such as "Main.hx"), the form `addFileLineBreakpoint` matches against.
	**/
	function files():Array<String>;

	/**
		The absolute path of each `files()` entry, at the same index. It lets
		FileMatcher match an IDE path onto a runtime file key by suffix, which
		also works for moved projects and executables built elsewhere.
	**/
	function filesFullPath():Array<String>;

	/** Installs a breakpoint on `file`:`line` and returns its runtime number. */
	function addFileLineBreakpoint(file:String, line:Int):Int;

	/**
		Installs a breakpoint at the ENTRY of `className.functionName` and
		returns its runtime number, or -1 when the runtime rejects the class.
		The names are the ones generated code carries in its stack frames: a
		dotted class path and a bare function name. The breakpoint fires when
		a frame of that function is at its first line; smart step into relies
		on this.
	**/
	function addClassFunctionBreakpoint(className:String, functionName:String):Int;

	/** Removes a previously installed breakpoint by its runtime number. */
	function deleteBreakpoint(number:Int):Void;

	/** The names of the local variables (and `this`) visible in a frame. */
	function stackVariables(threadNumber:Int, frame:Int):Array<String>;

	/** The live value of one local in any frame, as a real Dynamic. */
	function stackVariableValue(threadNumber:Int, frame:Int, name:String):Dynamic;

	/**
		Writes `value` to a local in ANY frame, not only the top one. Returns
		the value actually stored.
	**/
	function setStackVariableValue(threadNumber:Int, frame:Int, name:String, value:Dynamic):Dynamic;
}

/** A runtime notification, handed over to the server thread. */
enum DebugEvent {
	ThreadCreated(threadNumber:Int);
	ThreadTerminated(threadNumber:Int);
	// the runtime's "started": the thread is running again after a stop
	ThreadStarted(threadNumber:Int);

	// Everything here is captured ON THE STOPPING THREAD: the status, the hit
	// breakpoint number and the stack, already trimmed of the debugger's own
	// frames. The server thread can then report the stop and answer
	// stackTrace without reading another thread's state, which would race.
	// The stack is innermost LAST. `description` is the runtime's
	// criticalErrorDescription, null except for exception and critical-error
	// stops ("Uncatchable Throw: <value>", "Null Object Reference", ...).
	ThreadStopped(threadNumber:Int, status:Int, breakpoint:Int, stack:Array<DebugStackFrame>, description:Null<String>);
}

/** The step types; the values mirror the STEP_* constants of cpp.vm.Debugger. */
class StepType {
	public static inline var INTO = 1;
	public static inline var OVER = 2;
	public static inline var OUT = 3;
}

// The std debugger data types on cpp; stubs with the same API (one type per
// file, package ijhaxe.hxcpp.debug.stubs) everywhere else.
#if cpp
typedef DebugParameter = cpp.vm.Debugger.Parameter;
typedef DebugStackFrame = cpp.vm.Debugger.StackFrame;
typedef DebugThread = cpp.vm.Debugger.ThreadInfo;
#else
typedef DebugParameter = ijhaxe.hxcpp.debug.stubs.Parameter;
typedef DebugStackFrame = ijhaxe.hxcpp.debug.stubs.StackFrame;
typedef DebugThread = ijhaxe.hxcpp.debug.stubs.ThreadInfo;
#end
