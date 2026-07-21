package debug.inspect;

import debug.values.*;

import debug.Pointer;
import debug.layout.FrameLayout;
import debug.layout.GlobalTable;
import debug.layout.ObjectLayout;
import debug.module.JitInfo;
import debug.module.LocalsResolver;
import debug.module.ModuleDebugInfo;
import debug.target.MemoryReader;
import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;
import haxe.Int64;

/**
	Turns a stopped frame (or a variablesReference) into the DAP variable lists
	the client renders: the frame's scopes (Locals / Statics / Registers), the
	decoded locals, the HL bytecode registers, and a class's static fields.

	Read-only over the frozen debuggee — every value it produces is decoded via
	ValueReader/ValueChildren and is valid only for the current stop.
**/
class VariablesView {
	final stops:StopState;
	final memory:MemoryReader;
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final frameLayout:FrameLayout;
	final localsResolver:LocalsResolver;
	final globalTable:GlobalTable;
	final objectLayout:ObjectLayout;
	final valueReader:ValueReader;
	final valueChildren:ValueChildren;

	// Set by DebugSession: a thread's CPU registers (the architecture-neutral
	// SP/BP/IP/FLAGS subset), shown on that thread's top frame.
	public var cpuRegistersFor:Null<Int->Array<VariableInfo>> = null;

	public function new(stops:StopState, memory:MemoryReader, module:ModuleDebugInfo, jit:JitInfo,
			frameLayout:FrameLayout, localsResolver:LocalsResolver, globalTable:GlobalTable,
			objectLayout:ObjectLayout, valueReader:ValueReader, valueChildren:ValueChildren) {
		this.stops = stops;
		this.memory = memory;
		this.module = module;
		this.jit = jit;
		this.frameLayout = frameLayout;
		this.localsResolver = localsResolver;
		this.globalTable = globalTable;
		this.objectLayout = objectLayout;
		this.valueReader = valueReader;
		this.valueChildren = valueChildren;
	}

	/**
		The scopes of a cached frame: Locals, plus Statics when the owning class has static data.
	**/
	public function scopesFor(frameId:Int):Array<ScopeInfo> {
		var frame = stops.frameAt(frameId);
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
		The children of a variablesReference ([] for an unknown/stale reference).
	**/
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

	public function readLocals(frameId:Int):Array<VariableInfo> {
		var handle = stops.frameAt(frameId);
		if (handle == null) {
			return [];
		}
		var frame = handle.location;
		var argCount = module.argCount(frame.fidx);
		var offsets = frameLayout.registerOffsets(module.registers(frame.fidx), argCount);
		var locals = localsResolver.localsAt(frame.fidx, frame.op);
		var variables:Array<VariableInfo> = [];
		for (local in locals) {
			if (local.register < 0 || local.register >= offsets.length) {
				continue;
			}
			var slot = offsets[local.register];
			var address = Int64.add(frame.ebp, Int64.ofInt(slot.offset));
			var decoded = valueReader.read(address, slot.t);
			// The first `argCount` registers are the function's parameters; `this`
			// (register 0 of a method) stays Unspecified so it keeps the plain value
			// icon rather than looking like a parameter.
			var kind = local.name == "this" ? VariableKind.Unspecified : (local.register < argCount ? VariableKind.Argument : VariableKind.Local);
			variables.push({name: local.name, value: decoded.value, type: decoded.type, reference: decoded.reference, kind: kind});
		}
		return variables;
	}

	/**
		The frame's HL bytecode registers r0..rN (every typed `ebp+offset` slot,
		including args and unnamed temporaries), each annotated with the local
		name currently bound to it. The stopped thread's CPU registers lead the
		list on the top frame (they are thread state, not frame state).
	**/
	function readRegisters(frameId:Int):Array<VariableInfo> {
		var handle = stops.frameAt(frameId);
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
			var name = bound == null ? 'r$i' : 'r$i ($bound)';
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

	/**
		The decoded value held in HL register `reg` of `frameId` — used to describe
		the value being thrown at a throw site, where it is a live bound value.
		Null when the frame is gone or the register is out of range.
	**/
	public function readRegisterValue(frameId:Int, reg:Int):Null<VariableInfo> {
		var handle = stops.frameAt(frameId);
		if (handle == null) {
			return null;
		}
		var frame = handle.location;
		var offsets = frameLayout.registerOffsets(module.registers(frame.fidx), module.argCount(frame.fidx));
		if (reg < 0 || reg >= offsets.length) {
			return null;
		}
		var slot = offsets[reg];
		var address = Int64.add(frame.ebp, Int64.ofInt(slot.offset));
		var decoded = try valueReader.read(address, slot.t) catch (e:Dynamic) rawSlot(address, slot.t);
		return {name: 'r$reg', value: decoded.value, type: decoded.type, reference: decoded.reference};
	}

	/**
		Display text for the vdynamic at `ptr` (a pointer already in hand — e.g.
		hl_throw's parked exc_value), resolved through its runtime type header.
		Null when it cannot be decoded.
	**/
	public function previewDynamicPointer(ptr:Pointer):Null<String> {
		return try valueReader.previewThrownDynamic(ptr) catch (e:Dynamic) null;
	}

	/**
		The value in register `reg` rendered for an EXCEPTION-STOP description.
		Same decoding as `readRegisterValue`, except a thrown haxe.Exception is
		unwrapped to the text it carries: haxe 5 wraps EVERY non-Exception throw
		in a haxe.ValueException at the throw site, so the raw preview
		degenerates to the wrapper's class name ("haxe.ValueException:
		haxe.ValueException") and the actual thrown value ends up one field
		deep. ValueException unwraps through `value`, any other haxe.Exception
		through `__exceptionMessage`; everything else renders as-is.
	**/
	public function thrownRegisterPreview(frameId:Int, reg:Int):Null<VariableInfo> {
		var base = readRegisterValue(frameId, reg);
		if (base == null || base.value == null) {
			return base;
		}
		var handle = stops.frameAt(frameId);
		var frame = handle.location;
		var offsets = frameLayout.registerOffsets(module.registers(frame.fidx), module.argCount(frame.fidx));
		var slot = offsets[reg];
		var address = Int64.add(frame.ebp, Int64.ofInt(slot.offset));
		var runtime = runtimeClassOf(address, slot.t);
		if (runtime == null) {
			return base;
		}
		var carried = exceptionMessageOf(memory.readPointer(address), runtime);
		if (carried != null) {
			base.value = carried;
		}
		return base;
	}

	// The text a thrown haxe.Exception-family object carries; null for any
	// other class (or when the field cannot be read — degrade to the raw preview).
	function exceptionMessageOf(objPtr:Pointer, runtime:HLType):Null<String> {
		var fieldName = ClassChain.matches(runtime, "haxe.ValueException") ? "value"
			: ClassChain.matches(runtime, "haxe.Exception") ? "__exceptionMessage"
			: null;
		if (fieldName == null || Int64.eq(objPtr, Int64.ofInt(0))) {
			return null;
		}
		var target = valueChildren.targetOf(objPtr, runtime, fieldName);
		if (target == null) {
			return null;
		}
		var read = try valueReader.read(target.address, target.type) catch (e:Dynamic) null;
		return read == null ? null : read.value;
	}

	/**
		True when the value in register `reg` of `frameId` is an object whose
		runtime class (or a superclass) matches one of `wanted` (FQN or simple
		name) — the type filter for exception breakpoints. False for a non-object
		slot or when `wanted` is empty.
	**/
	public function registerValueMatchesType(frameId:Int, reg:Int, wanted:Array<String>):Bool {
		if (wanted == null || wanted.length == 0) {
			return false;
		}
		var handle = stops.frameAt(frameId);
		if (handle == null) {
			return false;
		}
		var frame = handle.location;
		var offsets = frameLayout.registerOffsets(module.registers(frame.fidx), module.argCount(frame.fidx));
		if (reg < 0 || reg >= offsets.length) {
			return false;
		}
		var slot = offsets[reg];
		var address = Int64.add(frame.ebp, Int64.ofInt(slot.offset));
		var runtime = runtimeClassOf(address, slot.t);
		if (runtime == null) {
			return false;
		}
		for (name in wanted) {
			if (ClassChain.matches(runtime, name)) {
				return true;
			}
		}
		return false;
	}

	// The runtime class of the value at `address` given its slot type — for an
	// object the hl_type* sits at obj+0; for a Dynamic the boxed value's type sits
	// at the vdynamic's +0. Both read one pointer then its type header. Non-object
	// slots (primitives, structs without a header) have no class here.
	function runtimeClassOf(address:Pointer, t:HLType):Null<HLType> {
		var pointer = switch (t) {
			case HObj(_), HDyn: memory.readPointer(address);
			default: return null;
		}
		if (Int64.eq(pointer, Int64.ofInt(0)) || valueReader.runtimeTypes == null) {
			return null;
		}
		return valueReader.runtimeTypes.typeAt(memory.readPointer(pointer));
	}

	function rawSlot(address:Pointer, t:HLType):DecodedValue {
		return {
			value: ValueReader.hex(memory.readPointer(address)),
			type: ValueReader.typeName(t),
			reference: 0,
		};
	}

	static function chasesPointers(t:HLType):Bool {
		return switch (t) {
			case HVoid, HUi8, HUi16, HI32, HI64, HF32, HF64, HBool: false;
			default: true;
		}
	}

	// A "Statics" scope for the class owning `fidx`, or null when that class has no
	// statics container, no allocated global, or its singleton isn't live yet.
	public function staticsScope(fidx:Int):Null<ScopeInfo> {
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
		return {name: 'Statics ($className)', reference: stops.allocReference(RefStatics(address, proto))};
	}

	// A statics container also holds its static methods (function-typed fields)
	// and compiler bookkeeping like __name__/__constructs__/__meta__; only count
	// the actual static variables.
	function hasStaticData(proto:ObjPrototype):Bool {
		var methodFields = staticMethodFieldNames(proto);
		for (field in proto.fields) {
			if (isDisplayableStatic(field.name, methodFields)) {
				return true;
			}
		}
		return false;
	}

	// Like object expansion but for a statics singleton: static methods and the
	// compiler's __xx__ bookkeeping fields are hidden.
	function readStaticFields(pointer:Pointer, proto:ObjPrototype):Array<VariableInfo> {
		var methodFields = staticMethodFieldNames(proto);
		var variables:Array<VariableInfo> = [];
		for (field in objectLayout.fields(proto)) {
			if (!isDisplayableStatic(field.name, methodFields)) {
				continue;
			}
			var address = Int64.add(pointer, Int64.ofInt(field.offset));
			var decoded = valueReader.read(address, field.type);
			variables.push({name: field.name, value: decoded.value, type: decoded.type, reference: decoded.reference, kind: VariableKind.Static});
		}
		return variables;
	}

	// The names of static fields that are METHOD bindings, not data. A class's
	// static methods sit in its statics container as fields bound to a function
	// via `bindings` (binding.mid = the bound function's findex; the same handle
	// buildStaticsIndex keys on). Its "Class.method" name's last segment is the
	// field name to hide. A function-TYPED static var (e.g. `static var
	// cb:Dynamic->Void`, HFun at runtime) is NOT bound and must stay visible —
	// hiding it by type alone, as an earlier "drop every HFun" rule did, made it
	// unreadable in both the Statics view and `Class.member` evaluate.
	function staticMethodFieldNames(proto:ObjPrototype):Map<String, Bool> {
		var names = new Map<String, Bool>();
		if (proto.bindings != null) {
			for (binding in proto.bindings) {
				var qualified = module.functionNameByFindex(binding.mid);
				if (qualified != null) {
					var lastDot = qualified.lastIndexOf(".");
					names.set(lastDot < 0 ? qualified : qualified.substr(lastDot + 1), true);
				}
			}
		}
		return names;
	}

	static function isDisplayableStatic(name:String, methodFields:Map<String, Bool>):Bool {
		if (name == null) {
			return false;
		}
		if (methodFields.exists(name)) {
			return false; // a static method binding, not a data field
		}
		// compiler-generated metadata (__name__, __constructs__, __meta__, ...)
		if (name.length > 4 && StringTools.startsWith(name, "__") && StringTools.endsWith(name, "__")) {
			return false;
		}
		return true;
	}
}
