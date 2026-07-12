package debug.values;

import debug.Pointer;
import debug.layout.Align;
import debug.layout.EnumLayout;
import debug.layout.FrameLayout;
import debug.layout.GlobalTable;
import debug.layout.ObjectLayout;
import debug.module.JitInfo;
import debug.module.LocalsResolver;
import debug.module.ModuleDebugInfo;
import debug.target.MemoryReader;
import debug.target.StackFrameLocation;

/**
 * The "what can I see and change while stopped" FACADE. It constructs and wires
 * the value-inspection collaborators and exposes the small surface DebugSession
 * drives — stop lifecycle, scopes/variables, evaluate/condition, setVariable —
 * delegating each to the owning class:
 *
 *  - StopState           per-stop frame caches + variablesReference registry
 *  - SymbolResolver      variable path → writable {address, type}
 *  - VariablesView       frames/references → DAP scopes & variable lists
 *  - DebuggeeCallService running code in the debuggee (calls / new / string / box)
 *  - ExpressionEvaluator the evaluate-expression interpreter (M21b/M23)
 *  - VariableMutator     the write path (setVariable / assignment)
 *
 * Wired once at launch from the module/jit metadata; DebugSession sets the
 * per-session callbacks (frameWalker, cpuRegistersFor, functionCaller, ...),
 * feeds a new stop via startStop, and invalidates on every resume — a reference
 * must never outlive its stop, since the GC can move objects.
 */
class VariableInspector {
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final memory:MemoryReader;
	final align:Align;
	final frameLayout:FrameLayout;
	final localsResolver:LocalsResolver;
	final objectLayout:ObjectLayout;
	final globalTable:GlobalTable;
	final valueReader:ValueReader;
	final valueChildren:ValueChildren;
	final runtimeTypes:RuntimeTypes;
	final dynObjects:DynObjReader;
	// Resolves variable paths → writable {address, type}; shared by reads and writes.
	final resolver:SymbolResolver;
	// Runs code in the debuggee (calls / new / string+box materialization).
	final calls:DebuggeeCallService;
	// Turns frames + references into the DAP scopes/variables lists.
	final view:VariablesView;
	// The evaluate-expression interpreter (operators, is/ternary, calls).
	final evaluator:ExpressionEvaluator;
	// The value-modification path (setVariable / assignment).
	final mutator:VariableMutator;

	/** Enables value modification (setVariable / assignment) via `out`. */
	public function enableWrites(out:debug.target.MemoryWriter):Void {
		mutator.writer = new ValueWriter(memory, out, align, runtimeTypes);
		calls.memWriter = out;
	}

	// The per-stop frame caches and variablesReference registry (cleared on every
	// resume). Owns `stoppedThreadId` — the thread writes/eval-call run in.
	final stops = new StopState();

	// Set by DebugSession: walks a thread's stack (StackWalker) on demand.
	public var frameWalker(never, set):Null<Int->Array<StackFrameLocation>>;

	inline function set_frameWalker(walker:Null<Int->Array<StackFrameLocation>>):Null<Int->Array<StackFrameLocation>> {
		stops.frameWalker = walker;
		return walker;
	}

	// Set by DebugSession: a thread's CPU registers (forwarded to the view).
	public var cpuRegistersFor(never, set):Null<Int->Array<VariableInfo>>;

	inline function set_cpuRegistersFor(provider:Null<Int->Array<VariableInfo>>):Null<Int->Array<VariableInfo>> {
		view.cpuRegistersFor = provider;
		return provider;
	}

	// Set by DebugSession: writes the low half of XMM0 for the arrival-register
	// fixup; and surfaces a non-fatal write warning. Both forwarded to the mutator.
	public var xmm0Writer(never, set):Null<Float->Void>;
	public var warnSink(never, set):Null<String->Void>;

	inline function set_xmm0Writer(w:Null<Float->Void>):Null<Float->Void> {
		mutator.xmm0Writer = w;
		return w;
	}

	inline function set_warnSink(sink:Null<String->Void>):Null<String->Void> {
		mutator.warnSink = sink;
		return sink;
	}

	public function new(module:ModuleDebugInfo, jit:JitInfo, memory:MemoryReader) {
		this.module = module;
		this.jit = jit;
		this.memory = memory;

		align = new Align(jit.is64, jit.boolSize4);
		align.structSizes = jit.structSizes;
		frameLayout = new FrameLayout(align, jit.winCall);
		localsResolver = new LocalsResolver(module);
		objectLayout = new ObjectLayout(align);
		globalTable = new GlobalTable(align, module.globals());

		runtimeTypes = new RuntimeTypes(memory, name -> module.typeByName(name));
		var enumLayout = new EnumLayout(align);
		valueReader = new ValueReader(memory, align);
		valueReader.referenceAllocator = (pointer, type) -> stops.allocReference(RefObject(pointer, type));
		valueReader.runtimeTypes = runtimeTypes;
		valueReader.enumLayout = enumLayout;
		valueReader.functionNameResolver = funPtr -> {
			var location = jit.resolveAddress(funPtr);
			return location == null ? null : module.functionName(location.fidx);
		};
		dynObjects = new DynObjReader(memory, align, runtimeTypes, hash -> module.reverseHash(hash));
		var maps = new MapReader(memory, align,
			jit.hlVersionMajor > 1 || (jit.hlVersionMajor == 1 && jit.hlVersionMinor >= 13));
		var treeMaps = new TreeMapReader(memory, align, objectLayout, runtimeTypes);
		valueReader.dynObjects = dynObjects;
		valueReader.maps = maps;
		valueReader.treeMaps = treeMaps;
		valueChildren = new ValueChildren(memory, align, valueReader, objectLayout);
		valueChildren.runtimeTypes = runtimeTypes;
		valueChildren.enumLayout = enumLayout;
		valueChildren.dynObjects = dynObjects;
		valueChildren.maps = maps;
		valueChildren.treeMaps = treeMaps;
		resolver = new SymbolResolver(stops, memory, module, jit, frameLayout, localsResolver, globalTable,
			valueChildren, runtimeTypes);
		calls = new DebuggeeCallService(resolver, memory, module, jit, align);
		view = new VariablesView(stops, memory, module, jit, frameLayout, localsResolver, globalTable,
			objectLayout, valueReader, valueChildren);
		evaluator = new ExpressionEvaluator(resolver, calls, view, valueReader, memory, module, align,
			runtimeTypes, stops);
		mutator = new VariableMutator(resolver, evaluator, calls, valueReader, memory, module, jit,
			frameLayout, stops);
	}

	/** Begins a new stop landed in `threadId` (see StopState.startStop). */
	public inline function startStop(threadId:Int):Void {
		stops.startStop(threadId);
	}

	/** Clears every per-stop cache (on resume). */
	public inline function invalidate():Void {
		stops.invalidate();
	}

	/** True once a stop has produced at least one frame (any thread walked). */
	public inline function hasFrames():Bool {
		return stops.hasFrames();
	}

	/**
	 * The frames of `threadId` (walked+cached on first request; all threads are
	 * frozen at a stop). Each carries the globally-unique frame id the client
	 * uses for scopes/variables/evaluate.
	 */
	public inline function framesFor(threadId:Int):Array<CachedFrame> {
		return stops.framesFor(threadId);
	}

	/** The scopes of a cached frame: Locals, plus Statics when the owning class has static data. */
	public inline function scopesFor(frameId:Int):Array<ScopeInfo> {
		return view.scopesFor(frameId);
	}

	/**
	 * Evaluates a VARIABLE PATH (`name`, `obj.field`, `arr[3]`, ...) in a
	 * cached frame. Root resolution order: the frame's locals, then fields of
	 * `this`, then the owning class's statics, then a class named by a leading
	 * path prefix (`MyClass.member`, `pkg.MyClass.member` — resolves to that
	 * class's statics container). Throws debug.DebugError with a user-facing
	 * message when the path cannot be resolved.
	 */
	public function evaluate(frameId:Int, expression:String):VariableInfo {
		// a single-line expression may carry a trailing ';' (e.g. copied from
		// source); it is not part of the expression grammar, so drop it
		expression = StringTools.trim(expression);
		while (StringTools.endsWith(expression, ";")) {
			expression = StringTools.rtrim(expression.substr(0, expression.length - 1));
		}
		var e = debug.eval.ExprParser.parse(expression);
		// a top-level assignment is a WRITE; everything else the interpreter renders
		return switch (e) {
			case EAssign(lhs, rhs): mutator.assignExpr(frameId, lhs, rhs);
			default: evaluator.evaluateExpr(frameId, e, expression);
		}
	}

	/** Evaluates a breakpoint condition to a Bool in the given frame (M22). */
	public inline function evaluateBool(frameId:Int, expression:String):Bool {
		return evaluator.evaluateBool(frameId, expression);
	}

	/** Sets a variablesReference child to an evaluate expression (DAP `setVariable`). */
	public inline function setVariable(reference:Int, name:String, valueExpr:String):VariableInfo {
		return mutator.setVariable(reference, name, valueExpr);
	}

	// Set by DebugSession: runs a function inside the debuggee. Forwarded to the
	// call service; null until the eval-call machinery is enabled.
	public var functionCaller(never, set):Null<(Pointer, Array<debug.eval.call.CallEmitter.CallArg>, Bool)->Pointer>;

	inline function set_functionCaller(caller:Null<(Pointer, Array<debug.eval.call.CallEmitter.CallArg>, Bool)->Pointer>):Null<(Pointer,
		Array<debug.eval.call.CallEmitter.CallArg>, Bool)->Pointer> {
		calls.functionCaller = caller;
		return caller;
	}

	/** The children of a variablesReference ([] for an unknown/stale reference). */
	public inline function variablesFor(reference:Int):Array<VariableInfo> {
		return view.variablesFor(reference);
	}
}
