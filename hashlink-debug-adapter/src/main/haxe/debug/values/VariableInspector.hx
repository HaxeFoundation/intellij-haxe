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
	// Non-null once value modification is enabled (a MemoryWriter is available).
	var writer:Null<ValueWriter> = null;
	var memWriter:Null<debug.target.MemoryWriter> = null;
	// Recovers construction recipes by disassembling ONew sites (a hack; see
	// ConstructorResolver). Created lazily on the first `new` evaluation.
	var constructors:Null<debug.eval.ConstructorResolver> = null;
	// Resolves C native addresses by disassembling call sites (a hack; see
	// NativeResolver). Created lazily on the first string materialization.
	var natives:Null<debug.eval.NativeResolver> = null;

	/** Enables value modification (setVariable / assignment) via `out`. */
	public function enableWrites(out:debug.target.MemoryWriter):Void {
		writer = new ValueWriter(memory, out, align, runtimeTypes);
		memWriter = out;
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

	// Set by DebugSession: a thread's CPU registers (the architecture-neutral
	// SP/BP/IP/FLAGS subset), shown on that thread's top frame.
	public var cpuRegistersFor:Null<Int->Array<VariableInfo>> = null;

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

	inline function frameAt(frameId:Int):Null<CachedFrame> {
		return stops.frameAt(frameId);
	}

	/** The scopes of a cached frame: Locals, plus Statics when the owning class has static data. */
	public function scopesFor(frameId:Int):Array<ScopeInfo> {
		var frame = frameAt(frameId);
		if (frame == null) {
			return [];
		}
		var scopes:Array<ScopeInfo> = [];
		scopes.push({name: "Locals", reference: stops.allocReference(RefLocals(frameId))});
		var statics = staticsScope(frame.location.fidx);
		if (statics != null) {
			scopes.push(statics);
		}
		scopes.push({name: "Registers", reference: stops.allocReference(RefRegisters(frameId)), hint: "registers"});
		return scopes;
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
		switch (e) {
			case EAssign(lhs, rhs):
				return assignExpr(frameId, lhs, rhs);
			case ECall(callee, args):
				var calleePath = chainToPath(callee);
				if (calleePath == null) {
					throw new debug.DebugError("The callee must be a function name or a variable path");
				}
				var values = [for (a in args) evalExpr(frameId, a)];
				return evaluateCall(frameId, calleePath, values);
			case ENew(className, args):
				var values = [for (a in args) evalExpr(frameId, a)];
				return decodeReturn("new " + className + "()", construct(frameId, className, values), constructedType(className));
			case EIndex(_, _):
				// the interpreter decides map-get vs array element (incl. computed keys)
				return renderValue(expression, evalExpr(frameId, e));
			default:
		}
		// a pure variable path keeps the pre-M21b reference walk: it renders
		// exactly like the Variables view (map entries, enum params, ...)
		var path = chainToPath(e);
		if (path != null) {
			return evaluatePath(frameId, path);
		}
		// anything else is an operator expression: interpret it (M21b)
		return renderValue(expression, evalExpr(frameId, e));
	}

	/**
	 * Evaluates a breakpoint condition to a Bool in the given frame (M22). The
	 * expression must yield a Bool — a number/string/object condition is a user
	 * error, surfaced with a clear message so the caller can fail safe (stop).
	 */
	public function evaluateBool(frameId:Int, expression:String):Bool {
		var e = debug.eval.ExprParser.parse(StringTools.trim(expression));
		if (e.match(EAssign(_, _))) {
			throw new debug.DebugError("A breakpoint condition cannot be an assignment");
		}
		return switch (evalExpr(frameId, e)) {
			case VBool(b): b;
			case other: throw new debug.DebugError("A breakpoint condition must be true/false, got "
				+ debug.eval.Operators.describe(other));
		}
	}

	// The pre-M21b path walk: resolves the root, then follows accessors through
	// the same variablesReference listings the Variables view uses.
	function evaluatePath(frameId:Int, path:ValuePath):VariableInfo {
		var start = 0;
		var current = resolveRoot(frameId, path.root);
		if (current == null) {
			// `MyClass.member`: a leading prefix naming a class resolves to its
			// statics container (locals/this/frame statics were tried first)
			var cls = resolver.staticsPrefix(path);
			if (cls != null) {
				current = {
					name: cls.className,
					value: "class " + cls.className,
					type: SymbolResolver.staticsContainerName(cls.className),
					reference: stops.allocReference(RefStatics(cls.singleton, cls.proto)),
				};
				start = cls.consumed;
			}
		}
		if (current == null) {
			throw new debug.DebugError('Unknown variable "' + path.root + '"');
		}
		for (i in start...path.accessors.length) {
			var accessor = path.accessors[i];
			var childName = switch (accessor) {
				case Field(name): name;
				case Index(index): Std.string(index);
			}
			if (current.reference <= 0) {
				throw new debug.DebugError('"' + current.name + '" has no members');
			}
			var next = findByName(variablesFor(current.reference), childName);
			if (next == null) {
				var what = accessor.match(Index(_)) ? "index [" + childName + "]" : 'field "' + childName + '"';
				throw new debug.DebugError('"' + current.name + '" has no ' + what);
			}
			current = next;
		}
		return current;
	}

	/**
	 * Sets a named child of a variablesReference (DAP `setVariable`) to any
	 * evaluate expression (literal, another variable, arithmetic, a call), and
	 * returns the child's new decoded value. Throws DebugError on any failure.
	 */
	public function setVariable(reference:Int, name:String, valueExpr:String):VariableInfo {
		var target = resolver.targetInReference(reference, name);
		var v = evalExpr(resolver.writeFrame, debug.eval.ExprParser.parse(StringTools.trim(valueExpr)));
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
				var recvPath = chainToPath(recv);
				if (recvPath == null) {
					throw new debug.DebugError("The receiver of [...] must be a variable path");
				}
				var target = resolver.targetOfPath(frameId, recvPath);
				var display = pathDisplay(recvPath) + "[...]";
				if (mapTypeOfTarget(target) != null) {
					// map bracket: sugar for set(key, value), read back via get
					var keyVal = evalExpr(frameId, key);
					var rhsVal = evalExpr(frameId, rhs);
					callRaw(frameId, pathPlus(recvPath, "set"), [keyVal, rhsVal]); // set returns Void
					var read = callRaw(frameId, pathPlus(recvPath, "get"), [keyVal]);
					return decodeReturn(display, read.raw, read.type);
				}
				// array element (constant or computed index): a writable slot
				var element = resolver.childTarget(target, Std.string(intKey(frameId, key)));
				writeValue(element, evalExpr(frameId, rhs));
				var decoded = valueReader.read(element.address, element.type);
				return {name: element.name, value: decoded.value, type: decoded.type, reference: decoded.reference};
			default:
		}
		var path = chainToPath(lhs);
		if (path == null) {
			throw new debug.DebugError('The left side of "=" must be a variable path (e.g. name, obj.field, arr[0])');
		}
		var target = resolver.targetOfPath(frameId, path);
		writeValue(target, evalExpr(frameId, rhs));
		fixupAfterWrite(target);
		var decoded = valueReader.read(target.address, target.type);
		return {name: target.name, value: decoded.value, type: decoded.type, reference: decoded.reference};
	}

	// The map type of a resolved receiver if it is one of the map classes
	// (StringMap/IntMap/ObjectMap or a BalancedTree), else null — the signal to
	// route `[]` to get/set rather than treat it as an array index.
	function mapTypeOfTarget(target:WriteTarget):Null<HLType> {
		var t = target.type;
		switch (t) {
			case HObj(_):
				var base = memory.readPointer(target.address);
				if (!Int64.eq(base, Int64.ofInt(0))) {
					t = resolver.refineObjectType(base, t);
				}
			default:
		}
		return isMapType(t) ? t : null;
	}

	static function isMapType(t:HLType):Bool {
		return switch (t) {
			case HObj(p): p != null && (ValueReader.mapKeyKind(p.name) != null || TreeMapReader.isTreeMap(p.name));
			default: false;
		}
	}

	// --- the expression interpreter (M21b) ---
	//
	// Leaves resolve through the SAME machinery as paths/writes (typed reads at
	// targetOfPath addresses, calls via callRaw, `new` via construct); operators
	// fold ADAPTER-SIDE on EvalValue — no debuggee code runs for arithmetic.

	function evalExpr(frameId:Int, e:debug.eval.ExprAst.Expr):debug.eval.EvalValue {
		return switch (e) {
			case EInt(v): VInt(v);
			case EFloat(f): VFloat(f);
			case EBool(b): VBool(b);
			case ENull: VNull;
			case EString(s): VString(s, null);
			case EIdent(_), EField(_, _):
				var path = chainToPath(e);
				if (path == null) {
					throw new debug.DebugError("This value cannot be resolved as a variable path");
				}
				valueOfPath(frameId, path);
			case EIndex(recv, key):
				indexValue(frameId, recv, key);
			case ECall(callee, args):
				var path = chainToPath(callee);
				if (path == null) {
					throw new debug.DebugError("The callee must be a function name or a variable path");
				}
				var values = [for (a in args) evalExpr(frameId, a)];
				var ret = callRaw(frameId, path, values);
				if (ret.type.match(HVoid)) {
					throw new debug.DebugError('"' + pathDisplay(path) + '" returns Void and cannot be used inside an expression');
				}
				toEvalValue(ret.raw, ret.type);
			case ENew(className, args):
				var values = [for (a in args) evalExpr(frameId, a)];
				VObject(construct(frameId, className, values), constructedType(className));
			case EUnop(op, inner):
				debug.eval.Operators.unop(op, evalExpr(frameId, inner));
			case EBinop("&&", l, r):
				// Haxe && / || short-circuit natively, so the right side only runs when needed
				VBool(debug.eval.Operators.asBool(evalExpr(frameId, l), "&&")
					&& debug.eval.Operators.asBool(evalExpr(frameId, r), "&&"));
			case EBinop("||", l, r):
				VBool(debug.eval.Operators.asBool(evalExpr(frameId, l), "||")
					|| debug.eval.Operators.asBool(evalExpr(frameId, r), "||"));
			case EBinop(op, l, r):
				debug.eval.Operators.binop(op, evalExpr(frameId, l), evalExpr(frameId, r));
			case ETernary(cond, thenE, elseE):
				// only the taken branch runs (a branch may call a function)
				debug.eval.Operators.asBool(evalExpr(frameId, cond), "?:")
					? evalExpr(frameId, thenE) : evalExpr(frameId, elseE);
			case EIs(inner, typeName):
				VBool(valueIsOfType(evalExpr(frameId, inner), typeName));
			case EAssign(_, _):
				throw new debug.DebugError("Assignment is only allowed at the top level of an expression");
		}
	}

	// `value is Type` (Haxe Std.isOfType semantics, the subset we support):
	// null is never an instance; Int/Float/Bool/String/Dynamic match by kind
	// (an Int satisfies Float, as in Haxe); a class/enum/struct name matches an
	// object whose runtime class equals it or descends from it (tsuper chain,
	// by full or simple name). Interfaces are not resolved. A type name that
	// names nothing is a user error (so a typo isn't a silent false).
	function valueIsOfType(v:debug.eval.EvalValue, typeName:String):Bool {
		if (v.match(VNull)) {
			return false;
		}
		switch (typeName) {
			case "Dynamic": return true;
			case "Int": return v.match(VInt(_));
			case "Float": return v.match(VFloat(_)) || v.match(VInt(_));
			case "Bool": return v.match(VBool(_));
			case "String": return v.match(VString(_, _));
			default:
		}
		if (!module.typeNameExists(typeName)) {
			throw new debug.DebugError('Unknown type "' + typeName + '" in an `is` check');
		}
		return switch (v) {
			case VObject(ptr, type):
				var runtime = switch (type) {
					case HObj(_), HStruct(_): type;
					default: runtimeTypes.typeAt(memory.readPointer(ptr));
				}
				classChainMatches(runtime, typeName);
			default:
				false; // a primitive/string against a (real) class name
		}
	}

	// Walks an object's runtime class and its superclasses, matching each class
	// name against `target` by full name (`pkg.Cls`) or simple name (`Cls`).
	function classChainMatches(type:Null<HLType>, target:String):Bool {
		var proto = switch (type) {
			case HObj(p), HStruct(p): p;
			default: null;
		}
		var seen = 0;
		while (proto != null && seen++ < 64) {
			if (proto.name == target || simpleClassName(proto.name) == target) {
				return true;
			}
			proto = proto.tsuper == null ? null : switch (proto.tsuper) {
				case HObj(p), HStruct(p): p;
				default: null;
			}
		}
		return false;
	}

	static inline function simpleClassName(full:String):String {
		var dot = full.lastIndexOf(".");
		return dot < 0 ? full : full.substr(dot + 1);
	}

	// A chain of EIdent/EField/EIndex(constant int) is exactly a ValuePath.
	static function chainToPath(e:debug.eval.ExprAst.Expr):Null<ValuePath> {
		var accessors:Array<PathAccessor> = [];
		var cur = e;
		while (true) {
			switch (cur) {
				case EIdent(name):
					accessors.reverse();
					return new ValuePath(name, accessors);
				case EField(inner, name):
					accessors.push(Field(name));
					cur = inner;
				case EIndex(inner, EInt(k)):
					var i = Int64.toInt(k);
					if (i < 0) {
						return null;
					}
					accessors.push(Index(i));
					cur = inner;
				default:
					return null;
			}
		}
	}

	static function pathDisplay(p:ValuePath):String {
		var s = p.root;
		for (a in p.accessors) {
			s += switch (a) {
				case Field(n): "." + n;
				case Index(i): "[" + i + "]";
			}
		}
		return s;
	}

	static function pathPlus(p:ValuePath, field:String):ValuePath {
		return new ValuePath(p.root, p.accessors.concat([Field(field)]));
	}

	function intKey(frameId:Int, key:debug.eval.ExprAst.Expr):Int {
		return switch (evalExpr(frameId, key)) {
			case VInt(v):
				var i = Int64.toInt(v);
				if (i < 0) {
					throw new debug.DebugError("An index must be >= 0");
				}
				i;
			default:
				throw new debug.DebugError("An array index must be an Int");
		}
	}

	// `recv[key]`: a map routes to get(key); anything else is an indexed element
	// (constant or computed key).
	function indexValue(frameId:Int, recv:debug.eval.ExprAst.Expr, key:debug.eval.ExprAst.Expr):debug.eval.EvalValue {
		var recvPath = chainToPath(recv);
		if (recvPath == null) {
			throw new debug.DebugError("The receiver of [...] must be a variable path");
		}
		var target = resolver.targetOfPath(frameId, recvPath);
		if (mapTypeOfTarget(target) != null) {
			var ret = callRaw(frameId, pathPlus(recvPath, "get"), [evalExpr(frameId, key)]);
			return toEvalValue(ret.raw, ret.type);
		}
		var element = resolver.childTarget(target, Std.string(intKey(frameId, key)));
		return evalValueAt(element.address, element.type);
	}

	function valueOfPath(frameId:Int, path:ValuePath):debug.eval.EvalValue {
		var target = resolver.targetOfPath(frameId, path);
		return evalValueAt(target.address, target.type);
	}

	// Typed read of a slot into the interpreter's currency.
	function evalValueAt(address:Pointer, t:HLType):debug.eval.EvalValue {
		return switch (t) {
			case HUi8: VInt(Int64.ofInt(memory.readU8(address)));
			case HUi16: VInt(Int64.ofInt(memory.readU16(address)));
			case HI32: VInt(Int64.ofInt(memory.readI32(address)));
			case HI64: VInt(memory.readI64(address));
			case HF32: VFloat(memory.readF32(address));
			case HF64: VFloat(memory.readF64(address));
			case HBool: VBool(memory.readU8(address) != 0);
			case HVoid: VNull;
			case HStruct(_), HPacked(_): VObject(address, t); // inline: the slot IS the base
			default: pointerValue(memory.readPointer(address), t);
		}
	}

	function pointerValue(ptr:Pointer, t:HLType):debug.eval.EvalValue {
		if (Int64.eq(ptr, Int64.ofInt(0))) {
			return VNull;
		}
		return switch (t) {
			case HNull(inner): evalValueAt(offset(ptr, align.ptr), inner); // box payload
			case HDyn: dynamicValue(ptr);
			case HObj(p) if (p != null && p.name == "String"): VString(valueReader.stringContentAt(ptr), ptr);
			case HObj(_): VObject(ptr, resolver.refineObjectType(ptr, t));
			default: VObject(ptr, t);
		}
	}

	// A Dynamic value: primitives live in a vdynamic box (hl_type* @0, payload
	// one pointer past); pointer kinds ARE the value (their own header says so).
	function dynamicValue(ptr:Pointer):debug.eval.EvalValue {
		var runtime = runtimeTypes.typeAt(memory.readPointer(ptr));
		if (runtime == null) {
			return VObject(ptr, HDyn);
		}
		return switch (runtime) {
			case HUi8: VInt(Int64.ofInt(memory.readU8(offset(ptr, align.ptr))));
			case HUi16: VInt(Int64.ofInt(memory.readU16(offset(ptr, align.ptr))));
			case HI32: VInt(Int64.ofInt(memory.readI32(offset(ptr, align.ptr))));
			case HI64: VInt(memory.readI64(offset(ptr, align.ptr)));
			case HF32: VFloat(memory.readF32(offset(ptr, align.ptr)));
			case HF64: VFloat(memory.readF64(offset(ptr, align.ptr)));
			case HBool: VBool(memory.readU8(offset(ptr, align.ptr)) != 0);
			case HObj(p) if (p != null && p.name == "String"): VString(valueReader.stringContentAt(ptr), ptr);
			default: VObject(ptr, runtime);
		}
	}

	// A call's raw return (RAX bits) into the interpreter's currency.
	function toEvalValue(raw:Pointer, t:HLType):debug.eval.EvalValue {
		return switch (t) {
			case HVoid: VNull;
			case HUi8, HUi16, HI32: VInt(Int64.ofInt(Int64.getLow(raw)));
			case HI64: VInt(raw);
			case HBool: VBool(Int64.getLow(raw) != 0);
			case HF64: VFloat(haxe.io.FPHelper.i64ToDouble(Int64.getLow(raw), Int64.getHigh(raw)));
			case HF32: VFloat(haxe.io.FPHelper.i32ToFloat(Int64.getLow(raw)));
			default: pointerValue(raw, t);
		}
	}

	function renderValue(name:String, v:debug.eval.EvalValue):VariableInfo {
		return switch (v) {
			case VInt(i): {name: name, value: Int64.toStr(i), type: "Int", reference: 0};
			case VFloat(f): {name: name, value: Std.string(f), type: "Float", reference: 0};
			case VBool(b): {name: name, value: b ? "true" : "false", type: "Bool", reference: 0};
			case VNull: {name: name, value: "null", type: "Dynamic", reference: 0};
			case VString(s, _): {name: name, value: "\"" + s + "\"", type: "String", reference: 0};
			case VObject(raw, t): decodeReturn(name, raw, t);
		}
	}

	// Set by DebugSession: runs a function inside the debuggee. Null until the
	// eval-call machinery is enabled.
	public var functionCaller:Null<(Pointer, Array<debug.eval.CallEmitter.CallArg>, Bool)->Pointer> = null;


	/**
	 * Evaluates a function call `callee(args...)` by running the callee in the
	 * debuggee (M13). `callee` must resolve to a function value or method; args
	 * are ALREADY-EVALUATED expression values lowered to the callee's declared
	 * parameter types. Returns the decoded result.
	 */
	function evaluateCall(frameId:Int, callee:ValuePath, args:Array<debug.eval.EvalValue>):VariableInfo {
		var call = callRaw(frameId, callee, args);
		return decodeReturn(pathDisplay(callee) + "()", call.raw, call.type);
	}

	/**
	 * Constructs `new className(args)` in the debuggee (M15) and returns the new
	 * instance pointer. Allocates via the recovered `hl_alloc_obj` + class type
	 * pointer (see ConstructorResolver — a disassembly hack), then runs the
	 * constructor `(this, args...)`. Construction is EXPERIMENTAL: if the
	 * allocator/ONew pattern can't be mined (non-x86-64, an unrecognised JIT, or
	 * the class is never constructed in the program so its `new` was stripped by
	 * DCE) it fails with a clear message rather than guessing.
	 */
	function construct(frameId:Int, className:String, args:Array<debug.eval.EvalValue>):Pointer {
		if (functionCaller == null) {
			throw new debug.DebugError("Constructing objects is not available in this session");
		}
		if (constructors == null) {
			constructors = new debug.eval.ConstructorResolver(module, jit, memory);
		}
		var site = constructors.resolve(className);
		if (site == null) {
			throw new debug.DebugError('Cannot construct "' + className
				+ '": no reachable constructor. Object construction is experimental — it only'
				+ ' works for classes the program itself instantiates (and on x86-64).');
		}
		// the constructor's declared type: arg0 is `this`, the rest are the params
		var ctorFun = switch (module.functionType(site.ctorFindex)) {
			case HFun(f): f;
			default: throw new debug.DebugError('The constructor of "' + className + '" is not a function');
		};
		var paramTypes = ctorFun.args.slice(1); // drop the leading `this`
		if (args.length != paramTypes.length) {
			throw new debug.DebugError('new ' + className + " takes " + paramTypes.length
				+ " argument(s), got " + args.length);
		}
		// allocate: hl_alloc_obj(classType) -> fresh zeroed instance
		var instance = functionCaller(site.allocFunction, [{isFloat: false, bits: site.typePointer}], false);
		if (Int64.eq(instance, Int64.ofInt(0))) {
			throw new debug.DebugError("Allocation returned null while constructing " + className);
		}
		// run the constructor: new(this, args...) -> void, initialising `instance`
		var ctorArgs:Array<debug.eval.CallEmitter.CallArg> = [{isFloat: false, bits: instance}];
		for (i in 0...args.length) {
			ctorArgs.push(lowerValue(args[i], paramTypes[i]));
		}
		functionCaller(jit.functionEntry(site.ctorFindex), ctorArgs, false);
		return instance;
	}

	// The module HLType of a construction result (the class named).
	function constructedType(className:String):format.hl.Data.HLType {
		var t = module.typeByName(className);
		return t == null ? HDyn : t;
	}

	// Runs `callee(args)` in the debuggee and returns the raw result (RAX, or
	// XMM0-as-RAX for a float return) plus the return type. Shared by evaluate
	// (for display), the interpreter, and assignment.
	function callRaw(frameId:Int, path:ValuePath, args:Array<debug.eval.EvalValue>):{raw:Pointer, type:format.hl.Data.HLType} {
		if (functionCaller == null) {
			throw new debug.DebugError("Calling functions is not available in this session");
		}
		var callee = pathDisplay(path);
		// `recv.method(args)` — the last segment is an instance method on the
		// receiver (proto method), not a closure-valued field. Try that first;
		// fall through to the closure-field call when it isn't a method.
		var method = tryMethodCall(frameId, path, args);
		if (method != null) {
			return method;
		}
		var target = resolver.targetOfPath(frameId, path);
		var fn = switch (target.type) {
			case HFun(f): f;
			default: throw new debug.DebugError('"' + callee + '" is not a function');
		};
		// The slot holds a vclosure {t @0, fun @+ptr, hasValue @+ptr*2, value @+ptr*3}.
		// When hasValue != 0 the closure is BOUND (an instance-method closure whose
		// value is the receiver, or a lambda whose value is its capture env): the
		// jit's OCallClosure emits `fun(value, args...)` — thread the captured value
		// through as the leading argument (M20). The closure's visible HFun type
		// already excludes that implicit parameter, so declared args map 1:1.
		var closurePtr = memory.readPointer(target.address);
		if (Int64.eq(closurePtr, Int64.ofInt(0))) {
			throw new debug.DebugError('"' + callee + '" is null');
		}
		var bound = memory.readI32(offset(closurePtr, align.ptr * 2)) != 0;
		var funcAddr = memory.readPointer(offset(closurePtr, align.ptr));
		if (args.length != fn.args.length) {
			throw new debug.DebugError('"' + callee + '" takes ' + fn.args.length + " argument(s), got " + args.length);
		}
		var callArgs:Array<debug.eval.CallEmitter.CallArg> = [];
		if (bound) {
			callArgs.push({isFloat: false, bits: memory.readPointer(offset(closurePtr, align.ptr * 3))});
		}
		for (i in 0...args.length) {
			callArgs.push(lowerValue(args[i], fn.args[i]));
		}
		var floatReturn = fn.ret.match(HF64) || fn.ret.match(HF32);
		return {raw: functionCaller(funcAddr, callArgs, floatReturn), type: fn.ret};
	}

	/**
	 * If `path` is `receiver.method` and `method` is an instance method on the
	 * receiver's runtime class, calls it with the receiver threaded as `this`
	 * (M16). Returns null when it isn't a method call (the caller then treats
	 * the path as a closure-valued field). Enables `map.set(k,v)`, `arr.push(x)`,
	 * getters, and any other mutation/query the program's own methods provide.
	 */
	function tryMethodCall(frameId:Int, path:ValuePath, args:Array<debug.eval.EvalValue>):Null<{raw:Pointer, type:format.hl.Data.HLType}> {
		if (path.accessors.length == 0) {
			return null; // a bare name: not `recv.method`
		}
		var last = path.accessors[path.accessors.length - 1];
		var methodName = switch (last) {
			case Field(name): name;
			default: return null; // `recv[i](...)` is not a method call
		};
		// resolve the receiver = the path without its last segment
		var receiver = resolver.targetOfPath(frameId, new ValuePath(path.root, path.accessors.slice(0, path.accessors.length - 1)));
		var base:Pointer;
		var runtimeType:HLType;
		switch (receiver.type) {
			case HStruct(_):
				base = receiver.address;
				runtimeType = receiver.type;
			case HObj(_):
				base = memory.readPointer(receiver.address);
				if (Int64.eq(base, Int64.ofInt(0))) {
					throw new debug.DebugError('"' + receiver.name + '" is null');
				}
				runtimeType = resolver.refineObjectType(base, receiver.type);
			default:
				return null; // methods only resolve on objects/structs
		}
		var findex = methodFindex(runtimeType, methodName);
		if (findex < 0) {
			return null; // no such proto method: fall back to closure-field handling
		}
		var arrayIndex = module.functionArrayIndex(findex);
		if (arrayIndex < 0) {
			throw new debug.DebugError('"' + methodName + '" has no callable body (native or removed)');
		}
		var fn = switch (module.functionType(arrayIndex)) {
			case HFun(f): f;
			default: throw new debug.DebugError('"' + methodName + '" is not a function');
		};
		var paramTypes = fn.args.slice(1); // drop the implicit `this`
		if (args.length != paramTypes.length) {
			throw new debug.DebugError('"' + methodName + '" takes ' + paramTypes.length
				+ " argument(s), got " + args.length);
		}
		var callArgs:Array<debug.eval.CallEmitter.CallArg> = [{isFloat: false, bits: base}];
		for (i in 0...args.length) {
			callArgs.push(lowerValue(args[i], paramTypes[i]));
		}
		var floatReturn = fn.ret.match(HF64) || fn.ret.match(HF32);
		return {raw: functionCaller(jit.functionEntry(arrayIndex), callArgs, floatReturn), type: fn.ret};
	}

	// The (raw) findex of instance method `name` on an HObj/HStruct type, walking
	// the superclass chain; -1 if not found. Static-dispatch by name on the
	// runtime class, so an override on a subclass is used.
	function methodFindex(t:HLType, name:String):Int {
		var proto = switch (t) {
			case HObj(p), HStruct(p): p;
			default: return -1;
		}
		var seen = 0;
		while (proto != null && seen++ < 64) { // bounded, in case a chain is cyclic
			var methods = proto.proto;
			if (methods != null) {
				for (m in methods) {
					if (m.name == name) {
						return m.findex;
					}
				}
			}
			proto = proto.tsuper == null ? null : switch (proto.tsuper) {
				case HObj(p), HStruct(p): p;
				default: null;
			}
		}
		return -1;
	}

	// Calls a bytecode function resolved by qualified name (a runtime helper),
	// via jit.addressOf. Returns the raw result.
	function callByName(name:String, args:Array<debug.eval.CallEmitter.CallArg>, floatReturn:Bool):Pointer {
		if (functionCaller == null) {
			throw new debug.DebugError("Calling functions is not available in this session");
		}
		var fidx = module.functionIndexByName(name);
		if (fidx < 0) {
			throw new debug.DebugError('Runtime helper "' + name + '" is unavailable in this program'
				+ " (it may have been removed as unused code)");
		}
		// call the true entry (prologue), not addressOf(fidx,0) which is past it
		return functionCaller(jit.functionEntry(fidx), args, floatReturn);
	}

	/**
	 * Materializes a String literal as a live heap String in the debuggee and
	 * returns its pointer (M13c). Allocates a byte buffer on the HEAP via the
	 * program's own `haxe.io.Bytes.alloc`, writes the UTF-8 bytes into it, then
	 * calls `String.fromUTF8` — both through the eval-call machinery. Heap
	 * (not stack) because on Windows there is no red zone: a buffer below Esp
	 * plus the callee's own stack use faults on the guard page. GC-safe: no
	 * allocation happens between reading the buffer pointer and the fromUTF8
	 * call that consumes it, and the buffer is fromUTF8's argument (kept live by
	 * the conservative stack scan) during its internal allocation.
	 */
	function makeString(text:String):Pointer {
		if (memWriter == null || functionCaller == null) {
			throw new debug.DebugError("Unable to create a string: value modification is not available in this session");
		}
		if (natives == null) {
			natives = new debug.eval.NativeResolver(module, jit, memory);
		}
		// Allocate the char buffer with the LOW-LEVEL `alloc_bytes` native (present
		// in any program that touches strings), reached by disassembling one of
		// its call sites — unlike `haxe.io.Bytes.alloc`, which the compiler
		// dead-code-eliminates when the program never uses `haxe.io.Bytes`.
		var allocBytes = natives.resolve("alloc_bytes");
		if (allocBytes == null) {
			throw new debug.DebugError("Unable to create a string: the debuggee's byte allocator (alloc_bytes)"
				+ " could not be located. String creation is x86-64 only and needs the program to allocate"
				+ " bytes somewhere (nearly all do).");
		}
		var utf8 = haxe.io.Bytes.ofString(text, haxe.io.Encoding.UTF8);
		// +1 for a guaranteed null terminator (alloc_bytes does not zero the tail)
		var bufferPtr = functionCaller(allocBytes, [{isFloat: false, bits: Int64.ofInt(utf8.length + 1)}], false);
		if (Int64.eq(bufferPtr, Int64.ofInt(0))) {
			throw new debug.DebugError("Unable to create a string: alloc_bytes returned null");
		}
		var buffer = haxe.io.Bytes.alloc(utf8.length + 1); // terminator byte defaults to 0
		buffer.blit(0, utf8, 0, utf8.length);
		memWriter.write(bufferPtr, buffer);
		var str = callByName("String.fromUTF8", [{isFloat: false, bits: bufferPtr}], false);
		if (Int64.eq(str, Int64.ofInt(0))) {
			throw new debug.DebugError("Unable to create a string: String.fromUTF8 returned null");
		}
		return str;
	}

	var boxer:Null<debug.eval.BoxResolver> = null;

	// Boxes a primitive literal into a fresh vdynamic so it can be passed to a
	// `Dynamic` parameter (M17). `alloc_dynamic(typePtr)` gives a GC-tracked
	// vdynamic tagged with the primitive's runtime type; `writePayload` writes
	// the value into its payload slot (HDYN_VALUE = one pointer past the type).
	function boxPrimitive(kind:Int, writePayload:Pointer->Void):debug.eval.CallEmitter.CallArg {
		if (memWriter == null || functionCaller == null) {
			throw new debug.DebugError("Unable to box a value: value modification is not available in this session");
		}
		if (boxer == null) {
			boxer = new debug.eval.BoxResolver(module, jit, memory);
		}
		var recipe = boxer.resolve(kind);
		if (recipe == null) {
			throw new debug.DebugError("Unable to box this primitive into a Dynamic: the debuggee never boxes a value"
				+ " of this type, so the boxing helper could not be located (boxing is x86-64 only and DCE-limited).");
		}
		var box = functionCaller(recipe.allocDynamic, [{isFloat: false, bits: recipe.typePointer}], false);
		if (Int64.eq(box, Int64.ofInt(0))) {
			throw new debug.DebugError("Unable to box a value: alloc_dynamic returned null");
		}
		writePayload(offset(box, align.ptr)); // HDYN_VALUE = one pointer past the hl_type*
		return {isFloat: false, bits: box};
	}

	// Lowers an ALREADY-EVALUATED expression value to the raw 64-bit value its
	// register needs, coercing to the callee's declared parameter type.
	function lowerValue(v:debug.eval.EvalValue, paramType:format.hl.Data.HLType):debug.eval.CallEmitter.CallArg {
		// A primitive going into a `Dynamic` parameter must be BOXED into a
		// vdynamic: passing the raw bits would be read as a pointer and stored
		// as garbage. Pointers (strings, objects) are dynamic-compatible and
		// pass as-is; primitives are boxed here (M17).
		if (paramType.match(HDyn)) {
			switch (v) {
				case VInt(i):
					return boxPrimitive(Type.enumIndex(HI32), box -> memWriter.writeI32(box, Int64.toInt(i)));
				case VFloat(f):
					return boxPrimitive(Type.enumIndex(HF64), box -> memWriter.writeF64(box, f));
				case VBool(b):
					return boxPrimitive(Type.enumIndex(HBool), box -> memWriter.writeU8(box, b ? 1 : 0));
				default:
			}
		}
		return switch (v) {
			case VInt(i):
				isFloatSlot(paramType)
					? {isFloat: true, bits: haxe.io.FPHelper.doubleToI64(Int64.toInt(i))}
					: {isFloat: false, bits: i};
			case VFloat(f):
				if (!isFloatSlot(paramType)) {
					throw new debug.DebugError("A float argument does not fit an integer parameter");
				}
				{isFloat: true, bits: haxe.io.FPHelper.doubleToI64(f)};
			case VBool(b):
				{isFloat: false, bits: Int64.ofInt(b ? 1 : 0)};
			case VNull:
				{isFloat: false, bits: Int64.ofInt(0)};
			case VString(text, ptr):
				if (isFloatSlot(paramType)) {
					throw new debug.DebugError("A string argument does not fit a float parameter");
				}
				{isFloat: false, bits: ptr != null ? (ptr : Pointer) : makeString(text)};
			case VObject(raw, t):
				if (t.match(HStruct(_)) || t.match(HPacked(_))) {
					throw new debug.DebugError("Passing a struct by value is not supported");
				}
				if (isFloatSlot(paramType)) {
					throw new debug.DebugError("An object argument does not fit a float parameter");
				}
				{isFloat: false, bits: raw};
		}
	}

	// Decodes a call's raw return value (RAX, or XMM0-as-RAX for a float return).
	function decodeReturn(name:String, raw:Pointer, retType:format.hl.Data.HLType):VariableInfo {
		return switch (retType) {
			case HVoid: {name: name, value: "void", type: "Void", reference: 0};
			case HUi8, HUi16, HI32: {name: name, value: Std.string(Int64.getLow(raw)), type: "Int", reference: 0};
			case HI64: {name: name, value: Int64.toStr(raw), type: "Int64", reference: 0};
			case HBool: {name: name, value: Int64.getLow(raw) != 0 ? "true" : "false", type: "Bool", reference: 0};
			case HF64: {name: name, value: Std.string(haxe.io.FPHelper.i64ToDouble(Int64.getLow(raw), Int64.getHigh(raw))), type: "Float", reference: 0};
			case HF32: {name: name, value: Std.string(haxe.io.FPHelper.i32ToFloat(Int64.getLow(raw))), type: "Float", reference: 0};
			default:
				// a pointer return: the raw value IS the object/string pointer
				if (Int64.eq(raw, Int64.ofInt(0))) {
					{name: name, value: "null", type: ValueReader.typeName(retType), reference: 0};
				} else {
					var decoded = valueReader.decodeReturnedPointer(raw, retType);
					{name: name, value: decoded.value, type: decoded.type, reference: decoded.reference};
				}
		}
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
				writer.assignRaw(target, ptr != null ? (ptr : Pointer) : makeString(text), stringType());
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

	function resolveRoot(frameId:Int, name:String):Null<VariableInfo> {
		var locals = readLocals(frameId);
		var local = findByName(locals, name);
		if (local != null) {
			return local;
		}
		// implicit this.field
		var self = findByName(locals, "this");
		if (self != null && self.reference > 0) {
			var member = findByName(variablesFor(self.reference), name);
			if (member != null) {
				return member;
			}
		}
		// static of the class owning the frame
		var frame = frameAt(frameId);
		if (frame != null) {
			var statics = staticsScope(frame.location.fidx);
			if (statics != null) {
				return findByName(variablesFor(statics.reference), name);
			}
		}
		return null;
	}

	static function findByName(variables:Array<VariableInfo>, name:String):Null<VariableInfo> {
		for (v in variables) {
			if (v.name == name) {
				return v;
			}
		}
		return null;
	}

	/** The children of a variablesReference ([] for an unknown/stale reference). */
	public function variablesFor(reference:Int):Array<VariableInfo> {
		var target = stops.referenceTarget(reference);
		if (target == null) {
			return [];
		}
		return switch (target) {
			case RefLocals(frameId):
				readLocals(frameId);
			case RefObject(pointer, type):
				valueChildren.of(pointer, type);
			case RefStatics(pointer, proto):
				readStaticFields(pointer, proto);
			case RefRegisters(frameId):
				readRegisters(frameId);
		}
	}

	/**
	 * The frame's HL bytecode registers r0..rN (every typed `ebp+offset` slot,
	 * including args and unnamed temporaries), each annotated with the local
	 * name currently bound to it. The stopped thread's CPU registers lead the
	 * list on the top frame (they are thread state, not frame state).
	 */
	function readRegisters(frameId:Int):Array<VariableInfo> {
		var handle = frameAt(frameId);
		if (handle == null) {
			return [];
		}
		var frame = handle.location;
		var variables:Array<VariableInfo> = [];
		// CPU registers are thread state: shown on each thread's TOP frame.
		if (handle.index == 0 && cpuRegistersFor != null) {
			for (register in cpuRegistersFor(handle.threadId)) {
				variables.push(register);
			}
		}
		var boundNames = new Map<Int, String>();
		for (local in localsResolver.localsAt(frame.fidx, frame.op)) {
			boundNames.set(local.register, local.name);
		}
		var offsets = frameLayout.registerOffsets(module.registers(frame.fidx), module.argCount(frame.fidx));
		for (i in 0...offsets.length) {
			var slot = offsets[i];
			var address = Int64.add(frame.ebp, Int64.ofInt(slot.offset));
			var bound = boundNames.get(i);
			var name = bound == null ? "r" + i : "r" + i + " (" + bound + ")";
			// Only slots bound to an in-scope local hold live values. Unbound
			// slots are leftovers from earlier calls: decoding one as a
			// pointer type would chase arbitrary garbage (a bogus String
			// length alone can demand a fatal multi-GB read), so they render
			// as their raw bits. Primitives are a fixed-size read of the
			// frame's own stack and always safe.
			var decoded = (bound != null || !chasesPointers(slot.t))
				? (try valueReader.read(address, slot.t) catch (e:Dynamic) rawSlot(address, slot.t))
				: rawSlot(address, slot.t);
			variables.push({name: name, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	function rawSlot(address:Pointer, t:format.hl.Data.HLType):DecodedValue {
		return {
			value: ValueReader.hex(memory.readPointer(address)),
			type: ValueReader.typeName(t),
			reference: 0,
		};
	}

	static function chasesPointers(t:format.hl.Data.HLType):Bool {
		return switch (t) {
			case HVoid, HUi8, HUi16, HI32, HI64, HF32, HF64, HBool: false;
			default: true;
		}
	}

	function readLocals(frameId:Int):Array<VariableInfo> {
		var handle = frameAt(frameId);
		if (handle == null) {
			return [];
		}
		var frame = handle.location;
		var offsets = frameLayout.registerOffsets(module.registers(frame.fidx), module.argCount(frame.fidx));
		var locals = localsResolver.localsAt(frame.fidx, frame.op);
		var variables:Array<VariableInfo> = [];
		for (local in locals) {
			if (local.register < 0 || local.register >= offsets.length) {
				continue;
			}
			var slot = offsets[local.register];
			var address = Int64.add(frame.ebp, Int64.ofInt(slot.offset));
			var decoded = valueReader.read(address, slot.t);
			variables.push({name: local.name, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	// A "Statics" scope for the class owning `fidx`, or null when that class has no
	// statics container, no allocated global, or its singleton isn't live yet.
	function staticsScope(fidx:Int):Null<ScopeInfo> {
		var proto = module.staticsProtoForFunction(fidx);
		if (proto == null || !hasStaticData(proto)) {
			return null;
		}
		var globalIndex = module.staticsGlobalIndex(proto);
		if (globalIndex < 0) {
			return null;
		}
		var slot = Int64.add(jit.globalsPtr, Int64.ofInt(globalTable.offsetOf(globalIndex)));
		var address = memory.readPointer(slot);
		if (Int64.eq(address, Int64.ofInt(0))) {
			return null;
		}
		var display = module.functionName(fidx);
		var dot = display.indexOf(".");
		var className = dot > 0 ? display.substr(0, dot) : display;
		return {name: "Statics (" + className + ")", reference: stops.allocReference(RefStatics(address, proto))};
	}

	// A statics container also holds its static methods (function-typed fields)
	// and compiler bookkeeping like __name__/__constructs__/__meta__; only count
	// the user's actual static variables.
	function hasStaticData(proto:ObjPrototype):Bool {
		for (field in proto.fields) {
			if (isDisplayableStatic(field.name, field.t)) {
				return true;
			}
		}
		return false;
	}

	// Like object expansion but for a statics singleton: static methods and the
	// compiler's __xx__ bookkeeping fields are hidden.
	function readStaticFields(pointer:Pointer, proto:ObjPrototype):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		for (field in objectLayout.fields(proto)) {
			if (!isDisplayableStatic(field.name, field.type)) {
				continue;
			}
			var address = Int64.add(pointer, Int64.ofInt(field.offset));
			var decoded = valueReader.read(address, field.type);
			variables.push({name: field.name, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	static function isDisplayableStatic(name:String, t:format.hl.Data.HLType):Bool {
		switch (t) {
			case HFun(_):
				return false; // a static method sharing the container
			default:
		}
		// compiler-generated metadata (__name__, __constructs__, __meta__, ...)
		if (name != null && name.length > 4
			&& StringTools.startsWith(name, "__") && StringTools.endsWith(name, "__")) {
			return false;
		}
		return true;
	}
}
