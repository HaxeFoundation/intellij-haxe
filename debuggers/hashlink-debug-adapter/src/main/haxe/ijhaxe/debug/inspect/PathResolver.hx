package ijhaxe.debug.inspect;
import ijhaxe.debug.DebugError;
import ijhaxe.debug.DebugErrorCode;

import ijhaxe.debug.values.*;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.layout.FrameLayout;
import ijhaxe.debug.layout.GlobalTable;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.module.LocalsResolver;
import ijhaxe.debug.module.LocalVar;
import ijhaxe.debug.module.ModuleDebugInfo;
import ijhaxe.debug.target.MemoryReader;
import format.hl.Data.HLType;
import format.hl.Data.ObjPrototype;
import haxe.Int64;

/**
	Resolves a variable PATH (`x`, `obj.field`, `arr[3]`, `MyClass.member`) to
	a writable location `{name, address, type}` in the stopped debuggee. It is
	the ONE place that navigates the frame layout and the object graph, shared
	by reads (the expression interpreter) and writes (setVariable and
	assignment).

	Roots resolve in this order: the frame's locals, fields of `this`
	(implicit member access), the statics of the class owning the frame, and
	finally a class named by a leading dotted prefix (`MyClass.member`,
	`pkg.Cls.member`). Object-typed slots are refined to their runtime class,
	so a `Base`-typed slot holding a `Sub` resolves `Sub`'s fields.

	It keeps no per-stop caches of its own: frames come from StopState and
	child addresses from ValueChildren. Every address it returns is valid only
	for the current stop.
**/
class PathResolver {
	final stops:StopState;
	final memory:MemoryReader;
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final frameLayout:FrameLayout;
	final localsResolver:LocalsResolver;
	final globalTable:GlobalTable;
	final valueChildren:ValueChildren;
	final runtimeTypes:RuntimeTypes;

	// The frame of the last resolved target, recorded by targetOfPath and
	// targetInReference so the right-hand side of a setVariable is evaluated in
	// the same frame. 0 for a target inside an object or statics reference.
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

	/**
		Resolves the child named `name` of a variablesReference (DAP setVariable).
	**/
	public function targetInReference(reference:Int, name:String):WriteTarget {
		var container = stops.referenceTarget(reference);
		if (container == null) {
			throw new DebugError("This value can no longer be modified (the debuggee has moved on)");
		}
		return switch (container) {
			case RefLocals(frameId):
				writeFrame = frameId;
				var local = localTarget(frameId, name);
				if (local == null) {
					throw new DebugError('No local named "' + name + '"');
				}
				local;
			case RefObject(pointer, type):
				writeFrame = 0;
				childTargetFromBase(name, pointer, type, name);
			case RefStatics(pointer, proto):
				writeFrame = 0;
				childTargetFromBase(name, pointer, HObj(proto), name);
			case RefRegisters(_):
				throw new DebugError("CPU/VM registers cannot be edited");
		}
	}

	/**
		Resolves a full path to a writable target; throws when it cannot be resolved.
	**/
	public function targetOfPath(frameId:Int, path:ValuePath):WriteTarget {
		writeFrame = frameId;
		var start = 0;
		var current = tryRootTarget(frameId, path.root);

		if (current == null) {
			// `MyClass.member` or `pkg.MyClass.member`: a leading prefix names a
			// class. Its statics container then acts as an object variable whose
			// slot is the container's global, which holds the singleton pointer.
			var cls = staticsPrefix(path);
			if (cls != null) {
				current = {name: cls.className, address: cls.slot, type: HObj(cls.proto)};
				start = cls.consumed;
			}
		}
		if (current == null) {
			// UnresolvedName with the unknown name lets the client resolve it against
			// its own source (imports) and retry with a fully qualified expression.
			throw new DebugError('Unknown variable "' + path.root + '"',
				DebugErrorCode.UnresolvedName, ["name" => path.root]);
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
	// ($ prefixes the LAST segment, not the whole qualified name).
	public static function staticsContainerName(className:String):String {
		return ModuleDebugInfo.staticsContainerName(className);
	}

	// The live statics singleton of the class named `className`, or null when
	// there is no such class, no statics global, or the singleton is not
	// allocated yet.
	function staticsByClassName(className:String):Null<StaticsContainer> {
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
		Matches a leading dotted prefix of `path` against a class name: the root
		alone (`MyClass`) or the root extended by field accessors (`pkg.MyClass`,
		`pkg.sub.MyClass`). The FIRST (shortest) match wins, and `consumed` counts
		the accessors that became part of the class name. Callers must try
		frame-local resolution first, so a class never shadows a local.
	**/
	public function staticsPrefix(path:ValuePath):Null<StaticsPrefix> {
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
					name += '.$segment';
					i++;
				default:
					return null;
			}
		}
	}

	// A local or argument slot: ebp plus its FrameLayout offset, typed by the register.
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

	/**
		Resolves a child of an already resolved target; throws when it has none.
	**/
	public function childTarget(parent:WriteTarget, childName:String):WriteTarget {
		var child = tryChildTarget(parent, childName);
		if (child == null) {
			throw new DebugError('"' + parent.name + '" has no member "' + childName + '"');
		}
		return child;
	}

	// Resolves a child by first finding the parent's object BASE. A struct is
	// inline, so its slot IS the base; a pointer type is dereferenced. Objects
	// are refined to their runtime class, so a Base-typed slot holding a Sub
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
					throw new DebugError('"' + parent.name + '" is null');
				}
				effectiveType = parent.type.match(HObj(_)) ? refineObjectType(base, parent.type) : parent.type;
			case HDyn:
				// A Dynamic slot: pointer kinds carry their own type header, so the
				// value is refined and descended as its runtime type. That lets
				// `dynArray[1].length` reach the String fields although the STATIC
				// element type is Dynamic. A boxed primitive refines to a non-object
				// type and yields no child, like any other value without members.
				base = memory.readPointer(parent.address);
				if (Int64.eq(base, Int64.ofInt(0))) {
					throw new DebugError('"' + parent.name + '" is null');
				}
				effectiveType = refineObjectType(base, parent.type);
			default:
				return null;
		}
		// This is a PROBE. While resolving a root, callers ask "is childName a
		// member of this?", and an unknown member answers null instead of
		// throwing. A throw would make `Cls.member + x` in an instance frame fail
		// on "is Cls a field of this?" instead of falling through to the
		// class-prefix resolution.
		var child = valueChildren.targetOf(base, effectiveType, childName);
		return child == null
			? null
			: {name: '${parent.name}.$childName', address: child.address, type: child.type};
	}

	function childTargetFromBase(displayName:String, base:Pointer, type:HLType, childName:String):WriteTarget {
		var child = valueChildren.targetOf(base, type, childName);
		if (child == null) {
			throw new DebugError('"' + displayName + '" cannot be resolved to a writable location');
		}
		return {name: displayName, address: child.address, type: child.type};
	}

	/**
		Refines an object pointer's static type to its runtime class, read from the object's header.
	**/
	public function refineObjectType(base:Pointer, staticType:HLType):HLType {
		var runtime = runtimeTypes.typeAt(memory.readPointer(base));
		return switch (runtime) {
			case HObj(_), HStruct(_): runtime;
			default: staticType;
		}
	}
}

/**
	A class's live statics: the global slot holding the singleton, the singleton pointer, and the container's prototype.
**/
typedef StaticsContainer = {slot:Pointer, singleton:Pointer, proto:ObjPrototype}

/**
	A StaticsContainer found through a dotted path prefix: the matched class name and the number of path accessors it consumed.
**/
typedef StaticsPrefix = {>StaticsContainer, className:String, consumed:Int}
