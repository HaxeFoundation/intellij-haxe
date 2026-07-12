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
	static inline var REF_BASE = 1000;

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

	// per-stop state, cleared on every resume. All threads are frozen at a stop,
	// so any thread's stack is walked lazily on first request and cached.
	final frameCaches:Map<Int, Array<CachedFrame>> = new Map(); // threadId -> its frames (with ids)
	final frameHandles:Map<Int, CachedFrame> = new Map(); // frameId -> the frame it names
	final references:Map<Int, RefTarget> = new Map();
	// Frame ids AND variablesReferences draw from ONE monotonic counter that is
	// never reset: a stale handle from before a resume resolves to nothing, never
	// aliases a new stop's allocation, and the two id spaces can't collide.
	var nextHandle:Int = REF_BASE;
	// The thread the stop landed in — writes and eval-call run only here.
	var stoppedThreadId:Int = 0;

	// Set by DebugSession: walks a thread's stack (StackWalker) on demand.
	public var frameWalker:Null<Int->Array<StackFrameLocation>> = null;

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
		valueReader.referenceAllocator = (pointer, type) -> allocReference(RefObject(pointer, type));
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
	}

	/**
	 * Begins a new stop: drops all per-thread frame caches, frame handles, and
	 * references (their NUMBERS are never reused — see nextHandle). `threadId` is
	 * the thread the stop landed in, the only one writes/eval-call may touch.
	 */
	public function startStop(threadId:Int):Void {
		frameCaches.clear();
		frameHandles.clear();
		references.clear();
		stoppedThreadId = threadId;
	}

	/** Clears every per-stop cache (on resume). */
	public function invalidate():Void {
		frameCaches.clear();
		frameHandles.clear();
		references.clear();
	}

	/** True once a stop has produced at least one frame (any thread walked). */
	public function hasFrames():Bool {
		return frameCaches.iterator().hasNext();
	}

	/**
	 * The frames of `threadId` (walked+cached on first request; all threads are
	 * frozen at a stop). Each carries the globally-unique frame id the client
	 * uses for scopes/variables/evaluate.
	 */
	public function framesFor(threadId:Int):Array<CachedFrame> {
		var cached = frameCaches.get(threadId);
		if (cached != null) {
			return cached;
		}
		var walked = frameWalker == null ? [] : frameWalker(threadId);
		var withIds:Array<CachedFrame> = [];
		for (i in 0...walked.length) {
			var frame:CachedFrame = {frameId: nextHandle++, threadId: threadId, index: i, location: walked[i]};
			frameHandles.set(frame.frameId, frame);
			withIds.push(frame);
		}
		frameCaches.set(threadId, withIds);
		return withIds;
	}

	inline function frameAt(frameId:Int):Null<CachedFrame> {
		return frameHandles.get(frameId);
	}

	/** The scopes of a cached frame: Locals, plus Statics when the owning class has static data. */
	public function scopesFor(frameId:Int):Array<ScopeInfo> {
		var frame = frameAt(frameId);
		if (frame == null) {
			return [];
		}
		var scopes:Array<ScopeInfo> = [];
		scopes.push({name: "Locals", reference: allocReference(RefLocals(frameId))});
		var statics = staticsScope(frame.location.fidx);
		if (statics != null) {
			scopes.push(statics);
		}
		scopes.push({name: "Registers", reference: allocReference(RefRegisters(frameId)), hint: "registers"});
		return scopes;
	}

	/**
	 * Evaluates a VARIABLE PATH (`name`, `obj.field`, `arr[3]`, ...) in a
	 * cached frame. Root resolution order: the frame's locals, then fields of
	 * `this`, then the owning class's statics. Throws debug.DebugError with a
	 * user-facing message when the path cannot be resolved.
	 */
	public function evaluate(frameId:Int, expression:String):VariableInfo {
		// a single-line expression may carry a trailing ';' (e.g. copied from
		// source); it is not part of the expression grammar, so drop it
		expression = StringTools.trim(expression);
		while (StringTools.endsWith(expression, ";")) {
			expression = StringTools.rtrim(expression.substr(0, expression.length - 1));
		}
		var call = CallExpr.parse(expression);
		if (call != null) {
			return call.isConstruction
				? decodeReturn("new " + call.callee + "()", construct(frameId, call.callee, call.args), constructedType(call.callee))
				: evaluateCall(frameId, call.callee, call.args);
		}
		var assignAt = assignmentEquals(expression);
		if (assignAt >= 0) {
			return assign(frameId, expression.substr(0, assignAt), expression.substr(assignAt + 1));
		}
		// `map[key]` — bracket access on a map is sugar for `map.get(key)` (the
		// compiler inlines the abstract's @:arrayAccess; there is no runtime
		// operator). Arrays fall through: `arr[i]` is a real indexed slot.
		var bracket = splitBracketTail(expression);
		if (bracket != null && mapReceiverType(frameId, bracket.receiver) != null) {
			var call = callRaw(frameId, bracket.receiver + ".get", [bracket.key]);
			return decodeReturn(bracket.receiver + "[" + bracket.key + "]", call.raw, call.type);
		}
		var path = ValuePath.parse(expression);
		if (path == null) {
			throw new debug.DebugError("Only variable paths and assignments can be evaluated (e.g. name, obj.field, x = 5)");
		}
		var current = resolveRoot(frameId, path.root);
		if (current == null) {
			throw new debug.DebugError('Unknown variable "' + path.root + '"');
		}
		for (accessor in path.accessors) {
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
	 * Sets a named child of a variablesReference (DAP `setVariable`) to a
	 * literal or another variable's value, and returns the child's new decoded
	 * value. Throws DebugError with a user-facing message on any failure.
	 */
	public function setVariable(reference:Int, name:String, valueExpr:String):VariableInfo {
		var target = targetInReference(reference, name);
		applyWrite(target, valueExpr);
		fixupAfterWrite(target);
		var decoded = valueReader.read(target.address, target.type);
		return {name: name, value: decoded.value, type: decoded.type, reference: decoded.reference};
	}

	/**
	 * Assigns to a variable PATH (`x`, `obj.field`, `arr[3]`) from the evaluate
	 * request (`path = expr`), returning the new decoded value.
	 */
	public function assign(frameId:Int, lhsExpr:String, rhsExpr:String):VariableInfo {
		// `map[key] = value` — sugar for `map.set(key, value)` (see evaluate).
		// Arrays fall through: an array element IS a writable slot.
		var bracket = splitBracketTail(lhsExpr);
		if (bracket != null && mapReceiverType(frameId, bracket.receiver) != null) {
			callRaw(frameId, bracket.receiver + ".set", [bracket.key, rhsExpr]); // set returns Void
			var read = callRaw(frameId, bracket.receiver + ".get", [bracket.key]);
			return decodeReturn(StringTools.trim(lhsExpr), read.raw, read.type);
		}
		var path = ValuePath.parse(lhsExpr);
		if (path == null) {
			throw new debug.DebugError('The left side of "=" must be a variable path (e.g. name, obj.field, arr[0])');
		}
		var target = targetOfPath(frameId, path);
		applyWrite(target, rhsExpr);
		fixupAfterWrite(target);
		var decoded = valueReader.read(target.address, target.type);
		return {name: target.name, value: decoded.value, type: decoded.type, reference: decoded.reference};
	}

	// Splits `<receiver>[<key>]` at the FINAL balanced bracket, honouring nested
	// brackets in the key; null when the expression does not end in `]`. Pure
	// string work — whether the receiver is actually a map is decided separately.
	static function splitBracketTail(expr:String):Null<{receiver:String, key:String}> {
		var s = StringTools.trim(expr);
		if (!StringTools.endsWith(s, "]")) {
			return null;
		}
		var depth = 0;
		var i = s.length - 1;
		while (i >= 0) {
			var c = StringTools.fastCodeAt(s, i);
			if (c == "]".code) {
				depth++;
			} else if (c == "[".code) {
				depth--;
				if (depth == 0) {
					break;
				}
			}
			i--;
		}
		if (i <= 0) {
			return null; // no matching '[' or an empty receiver
		}
		var receiver = StringTools.trim(s.substring(0, i));
		var key = StringTools.trim(s.substring(i + 1, s.length - 1));
		return (receiver.length == 0 || key.length == 0) ? null : {receiver: receiver, key: key};
	}

	// The map type of `receiverExpr` if it resolves to one of the map classes
	// (StringMap/IntMap/ObjectMap or a BalancedTree), else null — the signal to
	// route `[]` to get/set rather than treat it as an array index.
	function mapReceiverType(frameId:Int, receiverExpr:String):Null<HLType> {
		var p = ValuePath.parse(receiverExpr);
		if (p == null) {
			return null;
		}
		var target = try targetOfPath(frameId, p) catch (e:Dynamic) return null;
		var t = target.type;
		switch (t) {
			case HObj(_):
				var base = memory.readPointer(target.address);
				if (!Int64.eq(base, Int64.ofInt(0))) {
					t = refineObjectType(base, t);
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

	// Set by DebugSession: runs a function inside the debuggee. Null until the
	// eval-call machinery is enabled.
	public var functionCaller:Null<(Pointer, Array<debug.eval.CallEmitter.CallArg>, Bool)->Pointer> = null;


	/**
	 * Evaluates a function call `callee(args...)` by running the callee in the
	 * debuggee (M13). `callee` must resolve to a function value (an unbound
	 * closure / function reference); args are literals or variable paths lowered
	 * to the callee's declared parameter types. Returns the decoded result.
	 */
	function evaluateCall(frameId:Int, callee:String, argExprs:Array<String>):VariableInfo {
		var call = callRaw(frameId, callee, argExprs);
		return decodeReturn(callee + "()", call.raw, call.type);
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
	function construct(frameId:Int, className:String, argExprs:Array<String>):Pointer {
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
		if (argExprs.length != paramTypes.length) {
			throw new debug.DebugError('new ' + className + " takes " + paramTypes.length
				+ " argument(s), got " + argExprs.length);
		}
		// allocate: hl_alloc_obj(classType) -> fresh zeroed instance
		var instance = functionCaller(site.allocFunction, [{isFloat: false, bits: site.typePointer}], false);
		if (Int64.eq(instance, Int64.ofInt(0))) {
			throw new debug.DebugError("Allocation returned null while constructing " + className);
		}
		// run the constructor: new(this, args...) -> void, initialising `instance`
		var ctorArgs:Array<debug.eval.CallEmitter.CallArg> = [{isFloat: false, bits: instance}];
		for (i in 0...argExprs.length) {
			ctorArgs.push(lowerArgument(frameId, argExprs[i], paramTypes[i]));
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
	// (for display) and assign (to write the result into a slot).
	function callRaw(frameId:Int, callee:String, argExprs:Array<String>):{raw:Pointer, type:format.hl.Data.HLType} {
		if (functionCaller == null) {
			throw new debug.DebugError("Calling functions is not available in this session");
		}
		var path = ValuePath.parse(callee);
		if (path == null) {
			throw new debug.DebugError('Cannot call "' + callee + '": the callee must be a variable path');
		}
		// `recv.method(args)` — the last segment is an instance method on the
		// receiver (proto method), not a closure-valued field. Try that first;
		// fall through to the closure-field call when it isn't a method.
		var method = tryMethodCall(frameId, path, argExprs);
		if (method != null) {
			return method;
		}
		var target = targetOfPath(frameId, path);
		var fn = switch (target.type) {
			case HFun(f): f;
			default: throw new debug.DebugError('"' + callee + '" is not a function');
		};
		// the slot holds a vclosure; its function pointer is at +ptr. A bound
		// closure (captured environment) needs the env threaded through as a
		// leading argument, which is out of scope for now.
		var closurePtr = memory.readPointer(target.address);
		if (Int64.eq(closurePtr, Int64.ofInt(0))) {
			throw new debug.DebugError('"' + callee + '" is null');
		}
		if (memory.readI32(offset(closurePtr, align.ptr * 2)) == 1) {
			throw new debug.DebugError("Cannot call a bound closure yet (it captures local state)");
		}
		var funcAddr = memory.readPointer(offset(closurePtr, align.ptr));
		if (argExprs.length != fn.args.length) {
			throw new debug.DebugError('"' + callee + '" takes ' + fn.args.length + " argument(s), got " + argExprs.length);
		}
		var args:Array<debug.eval.CallEmitter.CallArg> = [];
		for (i in 0...argExprs.length) {
			args.push(lowerArgument(frameId, argExprs[i], fn.args[i]));
		}
		var floatReturn = fn.ret.match(HF64) || fn.ret.match(HF32);
		return {raw: functionCaller(funcAddr, args, floatReturn), type: fn.ret};
	}

	/**
	 * If `path` is `receiver.method` and `method` is an instance method on the
	 * receiver's runtime class, calls it with the receiver threaded as `this`
	 * (M16). Returns null when it isn't a method call (the caller then treats
	 * the path as a closure-valued field). Enables `map.set(k,v)`, `arr.push(x)`,
	 * getters, and any other mutation/query the program's own methods provide.
	 */
	function tryMethodCall(frameId:Int, path:ValuePath, argExprs:Array<String>):Null<{raw:Pointer, type:format.hl.Data.HLType}> {
		if (path.accessors.length == 0) {
			return null; // a bare name: not `recv.method`
		}
		var last = path.accessors[path.accessors.length - 1];
		var methodName = switch (last) {
			case Field(name): name;
			default: return null; // `recv[i](...)` is not a method call
		};
		// resolve the receiver = the path without its last segment
		var receiver = targetOfPath(frameId, new ValuePath(path.root, path.accessors.slice(0, path.accessors.length - 1)));
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
				runtimeType = refineObjectType(base, receiver.type);
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
		if (argExprs.length != paramTypes.length) {
			throw new debug.DebugError('"' + methodName + '" takes ' + paramTypes.length
				+ " argument(s), got " + argExprs.length);
		}
		var args:Array<debug.eval.CallEmitter.CallArg> = [{isFloat: false, bits: base}];
		for (i in 0...argExprs.length) {
			args.push(lowerArgument(frameId, argExprs[i], paramTypes[i]));
		}
		var floatReturn = fn.ret.match(HF64) || fn.ret.match(HF32);
		return {raw: functionCaller(jit.functionEntry(arrayIndex), args, floatReturn), type: fn.ret};
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

	// Lowers an argument expression to the raw 64-bit value its register needs,
	// coercing to the callee's declared parameter type.
	function lowerArgument(frameId:Int, argExpr:String, paramType:format.hl.Data.HLType):debug.eval.CallEmitter.CallArg {
		var literal = ValueLiteralParser.parse(argExpr);
		if (literal == null) {
			throw new debug.DebugError('Cannot parse argument "' + argExpr + '"');
		}
		// A primitive going into a `Dynamic` parameter must be BOXED into a
		// vdynamic: passing the raw bits would be read as a pointer and stored
		// as garbage. Pointers (strings, objects, paths) are dynamic-compatible
		// and pass as-is; primitive literals are boxed here (M17).
		if (paramType.match(HDyn)) {
			switch (literal) {
				case LInt(v):
					return boxPrimitive(Type.enumIndex(HI32), box -> memWriter.writeI32(box, Int64.getLow(v)));
				case LFloat(f):
					return boxPrimitive(Type.enumIndex(HF64), box -> memWriter.writeF64(box, f));
				case LBool(b):
					return boxPrimitive(Type.enumIndex(HBool), box -> memWriter.writeU8(box, b ? 1 : 0));
				default:
			}
		}
		switch (literal) {
			case LPath(argPath):
				// read the current value at the path's slot as the parameter type
				var src = targetOfPath(frameId, argPath);
				return lowerFromMemory(src.address, src.type, paramType);
			case LInt(v):
				return isFloatSlot(paramType)
					? {isFloat: true, bits: haxe.io.FPHelper.doubleToI64(Int64.toInt(v))}
					: {isFloat: false, bits: v};
			case LFloat(f):
				if (!isFloatSlot(paramType)) {
					throw new debug.DebugError("A float argument does not fit an integer parameter");
				}
				return {isFloat: true, bits: haxe.io.FPHelper.doubleToI64(f)};
			case LBool(b):
				return {isFloat: false, bits: Int64.ofInt(b ? 1 : 0)};
			case LNull:
				return {isFloat: false, bits: Int64.ofInt(0)};
			case LString(text):
				if (isFloatSlot(paramType)) {
					throw new debug.DebugError("A string argument does not fit a float parameter");
				}
				return {isFloat: false, bits: makeString(text)};
		}
	}

	function lowerFromMemory(address:Pointer, type:format.hl.Data.HLType, paramType:format.hl.Data.HLType):debug.eval.CallEmitter.CallArg {
		return switch (type) {
			case HUi8: {isFloat: false, bits: Int64.ofInt(memory.readU8(address))};
			case HUi16: {isFloat: false, bits: Int64.ofInt(memory.readU16(address))};
			case HI32, HBool: {isFloat: false, bits: Int64.ofInt(memory.readI32(address))};
			case HI64: {isFloat: false, bits: memory.readI64(address)};
			case HF64: {isFloat: true, bits: haxe.io.FPHelper.doubleToI64(memory.readF64(address))};
			case HF32: {isFloat: true, bits: haxe.io.FPHelper.doubleToI64(memory.readF32(address))};
			default:
				// a pointer type: pass the pointer value itself
				{isFloat: false, bits: memory.readPointer(address)};
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
		var stoppedFrames = frameCaches.get(stoppedThreadId);
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

	// The index of the assignment `=`, or -1. Skips the comparison operators
	// `==`, `!=`, `<=`, `>=` so `x == y` is still a (rejected) expression, not
	// an assignment.
	static function assignmentEquals(expression:String):Int {
		var i = 0;
		while (i < expression.length) {
			if (StringTools.fastCodeAt(expression, i) == "=".code) {
				var next = i + 1 < expression.length ? StringTools.fastCodeAt(expression, i + 1) : -1;
				var prev = i > 0 ? StringTools.fastCodeAt(expression, i - 1) : -1;
				if (next != "=".code && prev != "=".code && prev != "!".code && prev != "<".code && prev != ">".code) {
					return i;
				}
			}
			i++;
		}
		return -1;
	}

	function applyWrite(target:WriteTarget, valueExpr:String):Void {
		if (writer == null) {
			throw new debug.DebugError("Value modification is not available in this session");
		}
		// RHS is a function call or construction: run it, write its result (M13b/M15)
		var call = CallExpr.parse(valueExpr);
		if (call != null) {
			if (call.isConstruction) {
				writer.assignRaw(target, construct(writeFrame, call.callee, call.args), constructedType(call.callee));
			} else {
				var result = callRaw(writeFrame, call.callee, call.args);
				writer.assignRaw(target, result.raw, result.type);
			}
			return;
		}
		var literal = ValueLiteralParser.parse(valueExpr);
		if (literal == null) {
			throw new debug.DebugError('Cannot parse "' + valueExpr
				+ '": expected a number, "string", true/false, null, or another variable');
		}
		switch (literal) {
			case LPath(rhsPath):
				writer.copy(target, targetOfPath(currentFrameOf(target), rhsPath));
			case LString(text):
				writer.assignRaw(target, makeString(text), stringType());
			default:
				writer.write(target, literal);
		}
	}

	function stringType():format.hl.Data.HLType {
		var t = module.typeByName("String");
		return t == null ? HDyn : t;
	}

	// The RHS of an assignment resolves in the same frame as the LHS; both
	// assign() and setVariable() stash it so a `path = otherPath` works.
	var writeFrame:Int = 0;

	function currentFrameOf(_:WriteTarget):Int {
		return writeFrame;
	}

	function targetInReference(reference:Int, name:String):WriteTarget {
		var container = references.get(reference);
		if (container == null) {
			throw new debug.DebugError("This value can no longer be modified (the debuggee has moved on)");
		}
		return switch (container) {
			case RefLocals(frameId):
				writeFrame = frameId;
				var local = localTarget(frameId, name);
				if (local == null) {
					throw new debug.DebugError('No local named "' + name + '"');
				}
				local;
			case RefObject(pointer, type):
				writeFrame = 0;
				childTargetFromBase(name, pointer, type, name);
			case RefStatics(pointer, proto):
				writeFrame = 0;
				childTargetFromBase(name, pointer, HObj(proto), name);
			case RefRegisters(_):
				throw new debug.DebugError("CPU/VM registers cannot be edited");
		}
	}

	function targetOfPath(frameId:Int, path:ValuePath):WriteTarget {
		writeFrame = frameId;
		var current = rootTarget(frameId, path.root);
		for (accessor in path.accessors) {
			var childName = switch (accessor) {
				case Field(name): name;
				case Index(index): Std.string(index);
			}
			current = childTarget(current, childName);
		}
		return current;
	}

	function rootTarget(frameId:Int, name:String):WriteTarget {
		var local = localTarget(frameId, name);
		if (local != null) {
			return local;
		}
		// implicit this.field
		var self = localTarget(frameId, "this");
		if (self != null) {
			var member = tryChildTarget(self, name);
			if (member != null) {
				return member;
			}
		}
		// static of the owning class
		var frame = frameAt(frameId);
		if (frame != null) {
			var proto = module.staticsProtoForFunction(frame.location.fidx);
			if (proto != null) {
				var globalIndex = module.staticsGlobalIndex(proto);
				if (globalIndex >= 0) {
					var slot = Int64.add(jit.globalsPtr, Int64.ofInt(globalTable.offsetOf(globalIndex)));
					var singleton = memory.readPointer(slot);
					if (!Int64.eq(singleton, Int64.ofInt(0))) {
						var child = valueChildren.targetOf(singleton, HObj(proto), name);
						if (child != null) {
							return {name: name, address: child.address, type: child.type};
						}
					}
				}
			}
		}
		throw new debug.DebugError('Unknown variable "' + name + '"');
	}

	// A local/argument slot: ebp + FrameLayout offset, typed by the register.
	function localTarget(frameId:Int, name:String):Null<WriteTarget> {
		var handle = frameAt(frameId);
		if (handle == null) {
			return null;
		}
		var frame = handle.location;
		var local = findLocal(localsResolver.localsAt(frame.fidx, frame.op), name);
		if (local == null) {
			return null;
		}
		var offsets = frameLayout.registerOffsets(module.registers(frame.fidx), module.argCount(frame.fidx));
		if (local.register < 0 || local.register >= offsets.length) {
			return null;
		}
		var slot = offsets[local.register];
		return {name: name, address: Int64.add(frame.ebp, Int64.ofInt(slot.offset)), type: slot.t};
	}

	static function findLocal(locals:Array<debug.module.LocalVar>, name:String):Null<debug.module.LocalVar> {
		for (local in locals) {
			if (local.name == name) {
				return local;
			}
		}
		return null;
	}

	function childTarget(parent:WriteTarget, childName:String):WriteTarget {
		var child = tryChildTarget(parent, childName);
		if (child == null) {
			throw new debug.DebugError('"' + parent.name + '" has no member "' + childName + '"');
		}
		return child;
	}

	// Resolves a child by first finding the parent's object BASE: a struct is
	// inline (its slot IS the base), a pointer type is dereferenced. Objects
	// are refined to their runtime class so a Base-typed slot holding a Sub
	// resolves Sub's fields.
	function tryChildTarget(parent:WriteTarget, childName:String):Null<WriteTarget> {
		var base:Pointer;
		var effectiveType:HLType;
		switch (parent.type) {
			case HStruct(_):
				base = parent.address;
				effectiveType = parent.type;
			case HObj(_), HArray, HDynObj, HVirtual(_):
				base = memory.readPointer(parent.address);
				if (Int64.eq(base, Int64.ofInt(0))) {
					throw new debug.DebugError('"' + parent.name + '" is null');
				}
				effectiveType = parent.type.match(HObj(_)) ? refineObjectType(base, parent.type) : parent.type;
			default:
				return null;
		}
		return childTargetFromBase(parent.name + "." + childName, base, effectiveType, childName);
	}

	function childTargetFromBase(displayName:String, base:Pointer, type:HLType, childName:String):WriteTarget {
		var child = valueChildren.targetOf(base, type, childName);
		if (child == null) {
			throw new debug.DebugError('"' + displayName + '" cannot be resolved to a writable location');
		}
		return {name: displayName, address: child.address, type: child.type};
	}

	function refineObjectType(base:Pointer, staticType:HLType):HLType {
		var runtime = runtimeTypes.typeAt(memory.readPointer(base));
		return switch (runtime) {
			case HObj(_), HStruct(_): runtime;
			default: staticType;
		}
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
		var target = references.get(reference);
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
		return {name: "Statics (" + className + ")", reference: allocReference(RefStatics(address, proto))};
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

	function allocReference(target:RefTarget):Int {
		var reference = nextHandle++;
		references.set(reference, target);
		return reference;
	}
}

/** A walked stack frame plus the globally-unique id the client refers to it by. */
typedef CachedFrame = {
	var frameId:Int;
	var threadId:Int;
	var index:Int; // position in its thread's stack (0 = top)
	var location:debug.target.StackFrameLocation;
}
