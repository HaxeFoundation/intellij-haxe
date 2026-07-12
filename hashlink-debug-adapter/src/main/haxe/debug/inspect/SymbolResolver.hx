package debug.inspect;

import debug.values.*;

import debug.Pointer;
import debug.layout.FrameLayout;
import debug.layout.GlobalTable;
import debug.module.JitInfo;
import debug.module.LocalsResolver;
import debug.module.LocalVar;
import debug.module.ModuleDebugInfo;
import debug.target.MemoryReader;
import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;
import haxe.Int64;

/**
 * Resolves a variable PATH (`x`, `obj.field`, `arr[3]`, `MyClass.member`) to a
 * writable location `{name, address, type}` in the stopped debuggee — the
 * SINGLE navigation of the frame layout + object graph shared by reads (the
 * expression interpreter) and writes (setVariable / assignment).
 *
 * Root resolution order: the frame's locals → fields of `this` (implicit
 * member access) → the owning class's statics → a class named by a leading
 * dotted prefix (`MyClass.member`, `pkg.Cls.member`). Object-typed slots are
 * refined to their runtime class so a `Base`-typed slot holding a `Sub`
 * resolves `Sub`'s fields.
 *
 * Owns no per-stop caches of its own — it reads through StopState (frames) and
 * ValueChildren (child addresses); everything it returns is an address+type
 * valid only for the current stop.
 */
class SymbolResolver {
	final stops:StopState;
	final memory:MemoryReader;
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final frameLayout:FrameLayout;
	final localsResolver:LocalsResolver;
	final globalTable:GlobalTable;
	final valueChildren:ValueChildren;
	final runtimeTypes:RuntimeTypes;

	// The frame a resolved target lives in: targetOfPath/targetInReference stash
	// it so a setVariable RHS (`path = otherPath`) evaluates in the same frame.
	public var writeFrame(default, null):Int = 0;

	public function new(stops:StopState, memory:MemoryReader, module:ModuleDebugInfo, jit:JitInfo,
			frameLayout:FrameLayout, localsResolver:LocalsResolver, globalTable:GlobalTable,
			valueChildren:ValueChildren, runtimeTypes:RuntimeTypes) {
		this.stops = stops;
		this.memory = memory;
		this.module = module;
		this.jit = jit;
		this.frameLayout = frameLayout;
		this.localsResolver = localsResolver;
		this.globalTable = globalTable;
		this.valueChildren = valueChildren;
		this.runtimeTypes = runtimeTypes;
	}

	/** Resolves the child named `name` of a variablesReference (DAP setVariable). */
	public function targetInReference(reference:Int, name:String):WriteTarget {
		var container = stops.referenceTarget(reference);
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

	/** Resolves a full path to a writable target (throws if unresolvable). */
	public function targetOfPath(frameId:Int, path:ValuePath):WriteTarget {
		writeFrame = frameId;
		var start = 0;
		var current = tryRootTarget(frameId, path.root);
		if (current == null) {
			// `MyClass.member` / `pkg.MyClass.member`: a leading path prefix names
			// a class — its statics container behaves like an object variable
			// whose slot is the container's global (holding the singleton ptr)
			var cls = staticsPrefix(path);
			if (cls != null) {
				current = {name: cls.className, address: cls.slot, type: HObj(cls.proto)};
				start = cls.consumed;
			}
		}
		if (current == null) {
			throw new debug.DebugError('Unknown variable "' + path.root + '"');
		}
		for (i in start...path.accessors.length) {
			var childName = switch (path.accessors[i]) {
				case Field(name): name;
				case Index(index): Std.string(index);
			}
			current = childTarget(current, childName);
		}
		return current;
	}

	function tryRootTarget(frameId:Int, name:String):Null<WriteTarget> {
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
		var frame = stops.frameAt(frameId);
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
		return null;
	}

	// --- class-qualified statics (`MyClass.member`, `pkg.MyClass.member`) ---

	// A class `pkg.Cls` keeps its statics on a container type named `pkg.$Cls`
	// ($ prefixes the LAST segment — the M13c lesson).
	public static function staticsContainerName(className:String):String {
		var lastDot = className.lastIndexOf(".");
		return lastDot < 0 ? "$" + className : className.substr(0, lastDot + 1) + "$" + className.substr(lastDot + 1);
	}

	// The live statics singleton of the class named `className`, or null when
	// no such class / no statics global / the singleton isn't allocated yet.
	function staticsByClassName(className:String):Null<{slot:Pointer, singleton:Pointer, proto:ObjPrototype}> {
		var proto = switch (module.typeByName(staticsContainerName(className))) {
			case HObj(p): p;
			default: return null;
		}
		var globalIndex = module.staticsGlobalIndex(proto);
		if (globalIndex < 0) {
			return null;
		}
		var slot = Int64.add(jit.globalsPtr, Int64.ofInt(globalTable.offsetOf(globalIndex)));
		var singleton = memory.readPointer(slot);
		if (Int64.eq(singleton, Int64.ofInt(0))) {
			return null;
		}
		return {slot: slot, singleton: singleton, proto: proto};
	}

	/**
	 * Matches a leading dotted prefix of `path` against a class name — the root
	 * alone (`MyClass`) or the root extended by field accessors (`pkg.MyClass`,
	 * `pkg.sub.MyClass`). The FIRST (shortest) match wins; `consumed` is how
	 * many accessors the class name swallowed. Callers must try frame-local
	 * resolution first so a local can never be shadowed by a class.
	 */
	public function staticsPrefix(path:ValuePath):Null<{slot:Pointer, singleton:Pointer, proto:ObjPrototype, className:String, consumed:Int}> {
		var name = path.root;
		var i = 0;
		while (true) {
			var hit = staticsByClassName(name);
			if (hit != null) {
				return {slot: hit.slot, singleton: hit.singleton, proto: hit.proto, className: name, consumed: i};
			}
			if (i >= path.accessors.length) {
				return null;
			}
			switch (path.accessors[i]) {
				case Field(segment):
					name += "." + segment;
					i++;
				default:
					return null;
			}
		}
	}

	// A local/argument slot: ebp + FrameLayout offset, typed by the register.
	function localTarget(frameId:Int, name:String):Null<WriteTarget> {
		var handle = stops.frameAt(frameId);
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

	static function findLocal(locals:Array<LocalVar>, name:String):Null<LocalVar> {
		for (local in locals) {
			if (local.name == name) {
				return local;
			}
		}
		return null;
	}

	/** Resolves a child of an already-resolved target (throws if it has none). */
	public function childTarget(parent:WriteTarget, childName:String):WriteTarget {
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

	/** Refines an object pointer's static type to its runtime class (via its header). */
	public function refineObjectType(base:Pointer, staticType:HLType):HLType {
		var runtime = runtimeTypes.typeAt(memory.readPointer(base));
		return switch (runtime) {
			case HObj(_), HStruct(_): runtime;
			default: staticType;
		}
	}
}
