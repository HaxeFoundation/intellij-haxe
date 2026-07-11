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
	final frameLayout:FrameLayout;
	final localsResolver:LocalsResolver;
	final objectLayout:ObjectLayout;
	final globalTable:GlobalTable;
	final valueReader:ValueReader;
	final valueChildren:ValueChildren;

	// per-stop state, cleared on every resume
	var frameCache:Array<StackFrameLocation> = [];
	final references:Map<Int, RefTarget> = new Map();
	var nextReference:Int = REF_BASE;

	public function new(module:ModuleDebugInfo, jit:JitInfo, memory:MemoryReader) {
		this.module = module;
		this.jit = jit;
		this.memory = memory;

		var align = new Align(jit.is64, jit.boolSize4);
		align.structSizes = jit.structSizes;
		frameLayout = new FrameLayout(align, jit.winCall);
		localsResolver = new LocalsResolver(module);
		objectLayout = new ObjectLayout(align);
		globalTable = new GlobalTable(align, module.globals());

		var runtimeTypes = new RuntimeTypes(memory, name -> module.typeByName(name));
		var enumLayout = new EnumLayout(align);
		valueReader = new ValueReader(memory, align);
		valueReader.referenceAllocator = (pointer, type) -> allocReference(RefObject(pointer, type));
		valueReader.runtimeTypes = runtimeTypes;
		valueReader.enumLayout = enumLayout;
		valueReader.functionNameResolver = funPtr -> {
			var location = jit.resolveAddress(funPtr);
			return location == null ? null : module.functionName(location.fidx);
		};
		valueChildren = new ValueChildren(memory, align, valueReader, objectLayout);
		valueChildren.runtimeTypes = runtimeTypes;
		valueChildren.enumLayout = enumLayout;
	}

	/** The frames of the current stop (empty after invalidate). */
	public function frames():Array<StackFrameLocation> {
		return frameCache;
	}

	/** Installs the frames walked at a stop, dropping all previous references. */
	public function setFrames(frames:Array<StackFrameLocation>):Void {
		frameCache = frames;
		references.clear();
		nextReference = REF_BASE;
	}

	/** Clears the frame cache and every reference handed out for the last stop. */
	public function invalidate():Void {
		setFrames([]);
	}

	/** The scopes of a cached frame: Locals, plus Statics when the owning class has static data. */
	public function scopesFor(frameId:Int):Array<ScopeInfo> {
		if (frameId < 0 || frameId >= frameCache.length) {
			return [];
		}
		var scopes:Array<ScopeInfo> = [];
		scopes.push({name: "Locals", reference: allocReference(RefLocals(frameId))});
		var statics = staticsScope(frameCache[frameId].fidx);
		if (statics != null) {
			scopes.push(statics);
		}
		return scopes;
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
		}
	}

	function readLocals(frameId:Int):Array<VariableInfo> {
		if (frameId < 0 || frameId >= frameCache.length) {
			return [];
		}
		var frame = frameCache[frameId];
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

	// A statics container also holds its static methods as function-typed fields;
	// only show the scope when there is at least one non-method (data) field.
	function hasStaticData(proto:ObjPrototype):Bool {
		for (field in proto.fields) {
			switch (field.t) {
				case HFun(_):
				default:
					return true;
			}
		}
		return false;
	}

	// Like object expansion but for a statics singleton: the container's function
	// fields are its static methods (deferred), so only data fields are listed.
	function readStaticFields(pointer:Pointer, proto:ObjPrototype):Array<VariableInfo> {
		var variables:Array<VariableInfo> = [];
		for (field in objectLayout.fields(proto)) {
			switch (field.type) {
				case HFun(_):
					continue;
				default:
			}
			var address = Int64.add(pointer, Int64.ofInt(field.offset));
			var decoded = valueReader.read(address, field.type);
			variables.push({name: field.name, value: decoded.value, type: decoded.type, reference: decoded.reference});
		}
		return variables;
	}

	function allocReference(target:RefTarget):Int {
		var reference = nextReference++;
		references.set(reference, target);
		return reference;
	}
}
