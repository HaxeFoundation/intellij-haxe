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
import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;
import haxe.Int64;

/**
 * Everything "what can I see while stopped": owns the per-stop frame cache and
 * the variablesReference registry, and turns frames into scopes and references
 * into variable lists — locals via the reconstructed frame layout, object/
 * array/enum children via ValueChildren, statics via the globals table.
 *
 * Wired once at launch from the module/jit metadata. DebugSession feeds it the
 * walked frames on every stop (setFrames) and invalidates it on every resume:
 * a reference must never outlive its stop, since the GC can move objects.
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
	// Non-null once value modification is enabled (a MemoryWriter is available).
	var writer:Null<ValueWriter> = null;

	/** Enables value modification (setVariable / assignment) via `out`. */
	public function enableWrites(out:debug.target.MemoryWriter):Void {
		writer = new ValueWriter(memory, out, align, runtimeTypes);
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

	// Set by DebugSession: writes the low half of XMM0. Register-passed float
	// arguments ARRIVE in XMM registers; the jitted code may consume the still
	// live arrival register instead of the (also updated) stack slot, so a
	// write to the first float argument of the top frame patches XMM0 too —
	// the only float register hl_debug_write_register exposes.
	public var xmm0Writer:Null<Float->Void> = null;

	// Set by DebugSession: surfaces a non-fatal warning to the client (as a
	// console output event) when a write cannot be made fully effective.
	public var warnSink:Null<String->Void> = null;

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
			case EAssign(lhs, rhs): assignExpr(frameId, lhs, rhs);
			default: evaluator.evaluateExpr(frameId, e, expression);
		}
	}

	/** Evaluates a breakpoint condition to a Bool in the given frame (M22). */
	public inline function evaluateBool(frameId:Int, expression:String):Bool {
		return evaluator.evaluateBool(frameId, expression);
	}

	/**
	 * Sets a named child of a variablesReference (DAP `setVariable`) to any
	 * evaluate expression (literal, another variable, arithmetic, a call), and
	 * returns the child's new decoded value. Throws DebugError on any failure.
	 */
	public function setVariable(reference:Int, name:String, valueExpr:String):VariableInfo {
		var target = resolver.targetInReference(reference, name);
		var v = evaluator.evalExpr(resolver.writeFrame, debug.eval.ExprParser.parse(StringTools.trim(valueExpr)));
		writeValue(target, v);
		fixupAfterWrite(target);
		var decoded = valueReader.read(target.address, target.type);
		return {name: name, value: decoded.value, type: decoded.type, reference: decoded.reference};
	}

	/**
	 * `target = expr` from evaluate: the target is a variable path, an array
	 * element (any Int key expression), or a map bracket (sugar for set).
	 */
	function assignExpr(frameId:Int, lhs:debug.eval.ExprAst.Expr, rhs:debug.eval.ExprAst.Expr):VariableInfo {
		switch (lhs) {
			case EIndex(recv, key):
				var recvPath = ExpressionEvaluator.chainToPath(recv);
				if (recvPath == null) {
					throw new debug.DebugError("The receiver of [...] must be a variable path");
				}
				var target = resolver.targetOfPath(frameId, recvPath);
				var display = recvPath.display() + "[...]";
				if (evaluator.mapTypeOfTarget(target) != null) {
					// map bracket: sugar for set(key, value), read back via get
					var keyVal = evaluator.evalExpr(frameId, key);
					var rhsVal = evaluator.evalExpr(frameId, rhs);
					calls.callRaw(frameId, recvPath.plus("set"), [keyVal, rhsVal]); // set returns Void
					var read = calls.callRaw(frameId, recvPath.plus("get"), [keyVal]);
					return evaluator.decodeReturn(display, read.raw, read.type);
				}
				// array element (constant or computed index): a writable slot
				var element = resolver.childTarget(target, Std.string(evaluator.intKey(frameId, key)));
				writeValue(element, evaluator.evalExpr(frameId, rhs));
				var decoded = valueReader.read(element.address, element.type);
				return {name: element.name, value: decoded.value, type: decoded.type, reference: decoded.reference};
			default:
		}
		var path = ExpressionEvaluator.chainToPath(lhs);
		if (path == null) {
			throw new debug.DebugError('The left side of "=" must be a variable path (e.g. name, obj.field, arr[0])');
		}
		var target = resolver.targetOfPath(frameId, path);
		writeValue(target, evaluator.evalExpr(frameId, rhs));
		fixupAfterWrite(target);
		var decoded = valueReader.read(target.address, target.type);
		return {name: target.name, value: decoded.value, type: decoded.type, reference: decoded.reference};
	}

	// Set by DebugSession: runs a function inside the debuggee. Forwarded to the
	// call service; null until the eval-call machinery is enabled.
	public var functionCaller(never, set):Null<(Pointer, Array<debug.eval.CallEmitter.CallArg>, Bool)->Pointer>;

	inline function set_functionCaller(caller:Null<(Pointer, Array<debug.eval.CallEmitter.CallArg>, Bool)->Pointer>):Null<(Pointer,
		Array<debug.eval.CallEmitter.CallArg>, Bool)->Pointer> {
		calls.functionCaller = caller;
		return caller;
	}

	// An argument's early uses may be compiled against the CPU register it
	// ARRIVED in rather than the (also updated) stack slot — verified live:
	// writing only the slot left a traced Float parameter unchanged. The first
	// float argument arrives in XMM0 on both conventions (win64 XMM indexes
	// are positional, so there it must also be argument 0) and XMM0 is the one
	// arrival register hl_debug_write_register exposes: patch it. Every other
	// register-passed argument cannot be fixed up — surface a console warning
	// so a "didn't take" write on the current line is explainable.
	function fixupAfterWrite(target:WriteTarget):Void {
		// the arrival-register fixup only applies to the stopped thread's top frame
		var stoppedFrames = stops.framesFor(stops.stoppedThreadId);
		if (stoppedFrames == null || stoppedFrames.length == 0) {
			return;
		}
		var frame = stoppedFrames[0].location;
		var argCount = module.argCount(frame.fidx);
		var offsets = frameLayout.registerOffsets(module.registers(frame.fidx), argCount);
		var argIndex = -1;
		for (i in 0...argCount) {
			if (Int64.eq(target.address, Int64.add(frame.ebp, Int64.ofInt(offsets[i].offset)))) {
				argIndex = i;
				break;
			}
		}
		if (argIndex < 0) {
			return; // not an argument of the top frame
		}
		var firstFloat = -1;
		for (i in 0...argCount) {
			if (isFloatSlot(offsets[i].t)) {
				firstFloat = i;
				break;
			}
		}
		if (argIndex == firstFloat && target.type.match(HF64)
			&& (!jit.winCall || firstFloat == 0) && xmm0Writer != null) {
			xmm0Writer(memory.readF64(target.address));
			return;
		}
		if (registerPassed(argIndex, offsets, argCount) && warnSink != null) {
			warnSink("[debugger] note: \"" + target.name + "\" is a register-passed argument; code on the "
				+ "current line may still use the value it arrived with. The new value applies to later uses; "
				+ "to steer this line, set the value in the caller before the call." + String.fromCharCode(10));
		}
	}

	static function isFloatSlot(t:format.hl.Data.HLType):Bool {
		return t.match(HF32) || t.match(HF64);
	}

	static inline function offset(p:Pointer, n:Int):Pointer {
		return Int64.add(p, Int64.ofInt(n));
	}

	// Whether argument `argIndex` arrives in a CPU register: win64 passes the
	// first 4 positionally; SysV the first 6 integer-class / 8 float-class.
	function registerPassed(argIndex:Int, offsets:Array<debug.layout.RegisterSlot>, argCount:Int):Bool {
		if (jit.winCall) {
			return argIndex < 4;
		}
		var ints = 0;
		var floats = 0;
		for (i in 0...argCount) {
			var float = isFloatSlot(offsets[i].t);
			if (i == argIndex) {
				return float ? floats < 8 : ints < 6;
			}
			if (float) {
				floats++;
			} else {
				ints++;
			}
		}
		return false;
	}

	// Writes an ALREADY-EVALUATED expression value into a target slot, mapping
	// each value kind onto the appropriate ValueWriter primitive.
	function writeValue(target:WriteTarget, v:debug.eval.EvalValue):Void {
		if (writer == null) {
			throw new debug.DebugError("Value modification is not available in this session");
		}
		switch (v) {
			case VInt(i):
				writer.write(target, LInt(i));
			case VFloat(f):
				writer.write(target, LFloat(f));
			case VBool(b):
				writer.write(target, LBool(b));
			case VNull:
				writer.write(target, LNull);
			case VString(text, ptr):
				writer.assignRaw(target, ptr != null ? (ptr : Pointer) : calls.makeString(text), stringType());
			case VObject(raw, t):
				if (t.match(HStruct(_)) || t.match(HPacked(_))) {
					throw new debug.DebugError("Assigning a whole struct is not supported");
				}
				writer.assignRaw(target, raw, t);
		}
	}

	function stringType():format.hl.Data.HLType {
		var t = module.typeByName("String");
		return t == null ? HDyn : t;
	}

	/** The children of a variablesReference ([] for an unknown/stale reference). */
	public inline function variablesFor(reference:Int):Array<VariableInfo> {
		return view.variablesFor(reference);
	}
}
