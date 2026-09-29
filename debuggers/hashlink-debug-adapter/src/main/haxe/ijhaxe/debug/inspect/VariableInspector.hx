package ijhaxe.debug.inspect;
import ijhaxe.debug.DebugError;
import haxe.io.Path;

import ijhaxe.debug.values.*;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.eval.ExprParser;
import ijhaxe.debug.eval.call.CallArg;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.layout.EnumLayout;
import ijhaxe.debug.layout.FrameLayout;
import ijhaxe.debug.layout.GlobalTable;
import ijhaxe.debug.layout.ObjectLayout;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.module.LocalsResolver;
import ijhaxe.debug.module.ModuleDebugInfo;
import ijhaxe.debug.target.MemoryReader;
import ijhaxe.debug.target.MemoryWriter;
import ijhaxe.debug.target.StackFrameLocation;

/**
	The FACADE for everything that can be seen and changed while stopped. It
	builds and wires the value-inspection collaborators and exposes the small
	surface DebugSession drives: the stop lifecycle, scopes and variables,
	evaluate and breakpoint conditions, and setVariable. Each call is
	delegated to the class that owns it:

	| collaborator          | role                                                        |
	|-----------------------|-------------------------------------------------------------|
	| `StopState`           | per-stop frame caches and the variablesReference registry   |
	| `PathResolver`        | variable path → writable {address, type}                    |
	| `VariablesView`       | frames and references → DAP scopes and variable lists       |
	| `DebuggeeCallService` | running code in the debuggee (calls, `new`, strings, boxes) |
	| `ExpressionEvaluator` | the evaluate-expression interpreter                         |
	| `VariableMutator`     | the write path (setVariable and assignment)                 |

	It is wired once at launch from the module and JIT metadata. DebugSession
	sets the per-session callbacks (frameWalker, cpuRegistersFor,
	functionCaller, ...), announces each stop through startStop, and calls
	invalidate on every resume: the GC can move objects, so a reference must
	never outlive its stop.
**/
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

	// the collaborators in the class doc's table
	final resolver:PathResolver;
	final calls:DebuggeeCallService;
	final view:VariablesView;
	final evaluator:ExpressionEvaluator;
	final mutator:VariableMutator;

	/**
		Enables value modification (setVariable and assignment) through `out`.
	**/
	public function enableWrites(out:MemoryWriter):Void {
		mutator.writer = new ValueWriter(memory, out, align, runtimeTypes);
		calls.memWriter = out;
	}

	// The per-stop frame caches and variablesReference registry, cleared on every
	// resume. It also records `stoppedThreadId`, the thread that writes and
	// eval-calls run in.
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

	// Set by DebugSession and forwarded to the mutator: `xmm0Writer` writes the
	// low half of XMM0 for the arrival-register fixup (see VariableMutator), and
	// `warnSink` reports a non-fatal write warning.
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

		runtimeTypes = new RuntimeTypes(memory, align, name -> module.typeByName(name));
		var enumLayout = new EnumLayout(align);
		valueReader = new ValueReader(memory, align);
		valueReader.referenceAllocator = (pointer, type) -> stops.allocReference(RefObject(pointer, type));
		valueReader.runtimeTypes = runtimeTypes;
		valueReader.enumLayout = enumLayout;
		valueReader.functionNameResolver = funPtr -> {
			var location = jit.resolveAddress(funPtr);
			return location == null ? null : module.functionName(location.fidx);
		};
		valueReader.symbolResolver = codePtr -> {
			var location = jit.resolveAddress(codePtr);
			if (location == null) return null;
			var name = module.functionName(location.fidx);
			var source = module.sourceLineAt(location.fidx, location.op);
			if (source == null || source.file == null) return name;
			return '$name (${Path.withoutDirectory(source.file)}:${source.line})';
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

		resolver = new PathResolver(stops, memory, module, jit, frameLayout, localsResolver, globalTable,
			valueChildren, runtimeTypes);
		calls = new DebuggeeCallService(resolver, memory, module, jit, align);
		view = new VariablesView(stops, memory, module, jit, frameLayout, localsResolver, globalTable,
			objectLayout, valueReader, valueChildren);
		evaluator = new ExpressionEvaluator(resolver, calls, view, valueReader, memory, module, align,
			runtimeTypes, stops);
		mutator = new VariableMutator(resolver, evaluator, calls, valueReader, memory, module, jit,
			frameLayout, stops);
	}

	/**
		Begins a new stop landed in `threadId` (see StopState.startStop).
	**/
	public inline function startStop(threadId:Int):Void {
		stops.startStop(threadId);
	}

	/**
		Clears every per-stop cache (on resume).
	**/
	public inline function invalidate():Void {
		stops.invalidate();
	}

	/**
		The frames of `threadId` (walked+cached on first request; all threads are
		frozen at a stop). Each carries the globally-unique frame id the client
		uses for scopes/variables/evaluate.
	**/
	public inline function framesFor(threadId:Int):Array<CachedFrame> {
		return stops.framesFor(threadId);
	}

	/**
		The cached frame a DAP frameId names, or null when unknown/stale.
	**/
	public inline function frameAt(frameId:Int):Null<CachedFrame> {
		return stops.frameAt(frameId);
	}

	/**
		The scopes of a cached frame: Locals, Statics when the owning class has static data, and Registers.
	**/
	public inline function scopesFor(frameId:Int):Array<ScopeInfo> {
		return view.scopesFor(frameId);
	}

	/**
		The value in register `reg`, rendered for an exception-stop description. A
		thrown haxe.Exception is unwrapped to the text it carries (see VariablesView).
	**/
	public inline function thrownRegisterPreview(frameId:Int, reg:Int):Null<VariableInfo> {
		return view.thrownRegisterPreview(frameId, reg);
	}

	/**
		True when register `reg` holds an object matching one of the type filters.
	**/
	public inline function registerValueMatchesType(frameId:Int, reg:Int, wanted:Array<String>):Bool {
		return view.registerValueMatchesType(frameId, reg, wanted);
	}

	/**
		Display text for the vdynamic at `ptr` (e.g. hl_throw's exc_value); null when undecodable.
	**/
	public inline function previewDynamicPointer(ptr:Pointer):Null<String> {
		return view.previewDynamicPointer(ptr);
	}

	/**
		Evaluates an expression in a cached frame and returns the displayed
		result. A top-level assignment writes (see VariableMutator); anything
		else goes to the interpreter. Names resolve in this order: the frame's
		locals, fields of `this`, the owning class's statics, then a class named
		by a leading path prefix (`MyClass.member`, `pkg.MyClass.member`), which
		resolves to that class's statics container. Throws DebugError with a
		user-facing message when the expression cannot be evaluated.
	**/
	public function evaluate(frameId:Int, expression:String):VariableInfo {
		// a trailing ';', as in a line copied from source, is not part of the
		// grammar and is dropped
		expression = StringTools.trim(expression);
		while (StringTools.endsWith(expression, ";")) {
			expression = StringTools.rtrim(expression.substr(0, expression.length - 1));
		}
		var e = ExprParser.parse(expression);
		// a top-level assignment is a WRITE; everything else the interpreter renders
		return switch (e) {
			case EAssign(lhs, rhs): mutator.assignExpr(frameId, lhs, rhs);
			default: evaluator.evaluateForDisplay(frameId, e, expression);
		}
	}

	/**
		Evaluates a breakpoint condition to a Bool in the given frame.
	**/
	public inline function evaluateBool(frameId:Int, expression:String):Bool {
		return evaluator.evaluateBool(frameId, expression);
	}

	/**
		Sets a variablesReference child to an evaluate expression (DAP `setVariable`).
	**/
	public inline function setVariable(reference:Int, name:String, valueExpr:String):VariableInfo {
		return mutator.setVariable(reference, name, valueExpr);
	}

	// The client's opt-in (custom/setToStringRendering) to label objects with
	// their toString. It is stored but not used yet: a fault such as a stack
	// overflow inside a plain injected call is unrecoverable, because the HL
	// debug API cannot continue past it.
	// TODO: render toString labels once the injected call is fault-proof (hl_dyn_call_safe).
	public var renderWithToString:Bool = false;

	// Set by DebugSession: runs a function inside the debuggee. Forwarded to the
	// call service; null until eval-calls are enabled.
	public var functionCaller(never, set):Null<(Pointer, Array<CallArg>, Int)->Pointer>;

	inline function set_functionCaller(caller:Null<(Pointer, Array<CallArg>, Int)->Pointer>):Null<(Pointer,
		Array<CallArg>, Int)->Pointer> {
		calls.functionCaller = caller;
		return caller;
	}

	/**
		The children of a variablesReference ([] for an unknown/stale reference).
	**/
	public inline function variablesFor(reference:Int):Array<VariableInfo> {
		return view.variablesFor(reference);
	}
}
