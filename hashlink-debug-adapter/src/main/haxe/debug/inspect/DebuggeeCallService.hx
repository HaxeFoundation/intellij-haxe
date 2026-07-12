package debug.inspect;

import debug.values.*;

import debug.Pointer;
import debug.eval.call.CallEmitter.CallArg;
import debug.eval.EvalValue;
import debug.layout.Align;
import debug.module.JitInfo;
import debug.module.ModuleDebugInfo;
import debug.target.MemoryReader;
import debug.target.MemoryWriter;
import format.hl.Data.HLType;
import haxe.Int64;

/**
 * Runs code INSIDE the stopped debuggee (M13–M20): calls functions/methods,
 * constructs objects, materializes strings, and boxes primitives — everything
 * that needs the debuggee's own machinery rather than adapter-side reads.
 *
 * The actual injection is done by `functionCaller` (a trampoline the session
 * installs); this class resolves what to call (via SymbolResolver), lowers the
 * arguments to the calling convention, and returns the RAW result. Turning that
 * raw result into a displayed value is the caller's job (decodeReturn).
 *
 * DANGEROUS by nature — it executes arbitrary debuggee code on the session
 * thread — but that is the accepted trade for steering execution. The
 * constructor/native/box resolvers it uses disassemble raw JIT output (see
 * ConstructorResolver / NativeResolver / BoxResolver) and are x86-64 only.
 */
class DebuggeeCallService {
	final resolver:SymbolResolver;
	final memory:MemoryReader;
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final align:Align;

	// Installed by DebugSession: injects a trampoline that calls `addr(args)` in
	// the debuggee and returns its result. Null until eval-call is enabled.
	public var functionCaller:Null<(Pointer, Array<CallArg>, Bool)->Pointer> = null;
	// Installed alongside writes: lets makeString/boxPrimitive write into the
	// buffers/boxes they allocate in the debuggee.
	public var memWriter:Null<MemoryWriter> = null;

	// Recover recipes by disassembling JIT sites (hacks); created lazily.
	var constructors:Null<debug.eval.call.ConstructorResolver> = null;
	var natives:Null<debug.eval.call.NativeResolver> = null;
	var boxer:Null<debug.eval.call.BoxResolver> = null;

	public function new(resolver:SymbolResolver, memory:MemoryReader, module:ModuleDebugInfo, jit:JitInfo, align:Align) {
		this.resolver = resolver;
		this.memory = memory;
		this.module = module;
		this.jit = jit;
		this.align = align;
	}

	/**
	 * Runs `callee(args)` in the debuggee and returns the raw result (RAX, or
	 * XMM0-as-RAX for a float return) plus the return type. `callee` resolves to
	 * a function value (an unbound or bound closure — M20); args are ALREADY
	 * evaluated and lowered to the callee's declared parameter types. Tries an
	 * instance-method call first (`recv.method(args)` — M16), falling back to the
	 * closure-field call.
	 */
	public function callRaw(frameId:Int, path:ValuePath, args:Array<EvalValue>):{raw:Pointer, type:HLType} {
		if (functionCaller == null) {
			throw new debug.DebugError("Calling functions is not available in this session");
		}
		var callee = path.display();
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
		var callArgs:Array<CallArg> = [];
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
	function tryMethodCall(frameId:Int, path:ValuePath, args:Array<EvalValue>):Null<{raw:Pointer, type:HLType}> {
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
		var callArgs:Array<CallArg> = [{isFloat: false, bits: base}];
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

	/**
	 * Constructs `new className(args)` in the debuggee (M15) and returns the new
	 * instance pointer. Allocates via the recovered `hl_alloc_obj` + class type
	 * pointer (see ConstructorResolver — a disassembly hack), then runs the
	 * constructor `(this, args...)`. Construction is EXPERIMENTAL: if the
	 * allocator/ONew pattern can't be mined (non-x86-64, an unrecognised JIT, or
	 * the class is never constructed in the program so its `new` was stripped by
	 * DCE) it fails with a clear message rather than guessing.
	 */
	public function construct(frameId:Int, className:String, args:Array<EvalValue>):Pointer {
		if (functionCaller == null) {
			throw new debug.DebugError("Constructing objects is not available in this session");
		}
		if (constructors == null) {
			constructors = new debug.eval.call.ConstructorResolver(module, jit, memory);
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
		var ctorArgs:Array<CallArg> = [{isFloat: false, bits: instance}];
		for (i in 0...args.length) {
			ctorArgs.push(lowerValue(args[i], paramTypes[i]));
		}
		functionCaller(jit.functionEntry(site.ctorFindex), ctorArgs, false);
		return instance;
	}

	// Calls a bytecode function resolved by qualified name (a runtime helper).
	// Returns the raw result.
	function callByName(name:String, args:Array<CallArg>, floatReturn:Bool):Pointer {
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
	 * program's own `alloc_bytes` native, writes the UTF-8 bytes into it, then
	 * calls `String.fromUTF8` — both through the eval-call machinery. Heap
	 * (not stack) because on Windows there is no red zone: a buffer below Esp
	 * plus the callee's own stack use faults on the guard page. GC-safe: no
	 * allocation happens between reading the buffer pointer and the fromUTF8
	 * call that consumes it, and the buffer is fromUTF8's argument (kept live by
	 * the conservative stack scan) during its internal allocation.
	 */
	public function makeString(text:String):Pointer {
		if (memWriter == null || functionCaller == null) {
			throw new debug.DebugError("Unable to create a string: value modification is not available in this session");
		}
		if (natives == null) {
			natives = new debug.eval.call.NativeResolver(module, jit, memory);
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

	// Boxes a primitive literal into a fresh vdynamic so it can be passed to a
	// `Dynamic` parameter (M17). `alloc_dynamic(typePtr)` gives a GC-tracked
	// vdynamic tagged with the primitive's runtime type; `writePayload` writes
	// the value into its payload slot (HDYN_VALUE = one pointer past the type).
	function boxPrimitive(kind:Int, writePayload:Pointer->Void):CallArg {
		if (memWriter == null || functionCaller == null) {
			throw new debug.DebugError("Unable to box a value: value modification is not available in this session");
		}
		if (boxer == null) {
			boxer = new debug.eval.call.BoxResolver(module, jit, memory);
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
	function lowerValue(v:EvalValue, paramType:HLType):CallArg {
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

	static inline function offset(p:Pointer, n:Int):Pointer {
		return Int64.add(p, Int64.ofInt(n));
	}

	static function isFloatSlot(t:HLType):Bool {
		return t.match(HF32) || t.match(HF64);
	}
}
