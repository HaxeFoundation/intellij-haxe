package ijhaxe.debug.inspect;
import haxe.io.FPHelper;
import ijhaxe.debug.DebugError;
import ijhaxe.debug.DebugErrorCode;

import ijhaxe.debug.values.*;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.eval.EvalValue;
import ijhaxe.debug.eval.ExprAst.Expr;
import ijhaxe.debug.eval.ExprParser;
import ijhaxe.debug.eval.Operators;
import ijhaxe.debug.layout.Align;
import ijhaxe.debug.module.ModuleDebugInfo;
import ijhaxe.debug.target.MemoryReader;
import format.hl.Data.HLType;
import haxe.Int64;

/**
	The interpreter for evaluate expressions. The leaves of a parsed
	expression resolve through the SAME machinery as paths and writes: typed
	reads at PathResolver addresses, calls through DebuggeeCallService, `new`
	through its construct, and map brackets through get/set. Operators are
	computed inside the adapter on EvalValues, so no debuggee code runs for
	arithmetic. The result is either an EvalValue (for operands, conditions
	and call arguments) or a rendered VariableInfo (for the watches view).

	A pure variable path is read by walking the variablesReference listings of
	the Variables view (`evaluatePath`), so a watch renders exactly like the
	Variables view, with map entries, enum parameters and expandable
	references.
**/
class ExpressionEvaluator {
	final resolver:PathResolver;
	final calls:DebuggeeCallService;
	final view:VariablesView;
	final valueReader:ValueReader;
	final memory:MemoryReader;
	final module:ModuleDebugInfo;
	final align:Align;
	final runtimeTypes:RuntimeTypes;
	final stops:StopState;

	public function new(resolver:PathResolver, calls:DebuggeeCallService, view:VariablesView,
			valueReader:ValueReader, memory:MemoryReader, module:ModuleDebugInfo, align:Align,
			runtimeTypes:RuntimeTypes, stops:StopState) {
		this.resolver = resolver;
		this.calls = calls;
		this.view = view;
		this.valueReader = valueReader;
		this.memory = memory;
		this.module = module;
		this.align = align;
		this.runtimeTypes = runtimeTypes;
		this.stops = stops;
	}

	/**
		Evaluates a NON-assignment expression to a displayed value; the caller
		handles a top-level assignment itself. A pure path uses the
		Variables-view walk, a call or `new` returns the decoded result, and
		anything else is interpreted and rendered.
	**/
	public function evaluateForDisplay(frameId:Int, e:Expr, exprText:String):VariableInfo {
		switch (e) {
			case ECall(callee, args):
				var calleePath = chainToPath(callee);
				if (calleePath == null) {
					throw new DebugError("The callee must be a function name or a variable path");
				}
				var values = [for (a in args) valueOf(frameId, a)];
				return evaluateCall(frameId, calleePath, values);
			case ENew(className, args):
				var values = [for (a in args) valueOf(frameId, a)];
				return decodeReturn('new $className()', calls.construct(frameId, className, values), constructedType(className));
			case EIndex(_, _):
				// the interpreter decides between a map get and an array element,
				// including computed keys
				return renderValue(exprText, valueOf(frameId, e));
			default:
		}
		// a pure variable path renders exactly like the Variables view
		var path = chainToPath(e);
		if (path != null) {
			try {
				return evaluatePath(frameId, path);
			} catch (walkError:DebugError) {
				// The view shows some REAL objects as leaves without children (a
				// String shows its content, not bytes and length), so the walk
				// cannot descend into them. The typed resolver can: `s.length` is a
				// real I32 field of the String object. A pure path has no side
				// effects, so retrying through the interpreter is safe. When that
				// fails too, the walk's error is surfaced, because its UnresolvedName
				// code lets the client qualify class names.
				try {
					return renderValue(exprText, valueOf(frameId, e));
				} catch (_:DebugError) {
					throw walkError;
				}
			}
		}
		// anything else is an operator expression: interpret it
		return renderValue(exprText, valueOf(frameId, e));
	}

	/**
		Evaluates a breakpoint condition to a Bool in the given frame. Any other
		result is a user error with a clear message, so the caller can fail safe
		and stop.
	**/
	public function evaluateBool(frameId:Int, expression:String):Bool {
		var e = ExprParser.parse(StringTools.trim(expression));
		if (e.match(EAssign(_, _))) {
			throw new DebugError("A breakpoint condition cannot be an assignment");
		}
		return switch (valueOf(frameId, e)) {
			case VBool(b): b;
			case other: throw new DebugError("A breakpoint condition must be true/false, got "
				+ Operators.describe(other));
		}
	}

	// The Variables-view walk: resolves the root, then follows each accessor
	// through the same variablesReference listings the Variables view uses.
	function evaluatePath(frameId:Int, path:ValuePath):VariableInfo {
		var start = 0;
		var current = resolveRoot(frameId, path.root);

		if (current == null) {
			// `MyClass.member`: a leading prefix that names a class resolves to the
			// class's statics container. Locals, `this` and the frame's statics
			// were tried first.
			var cls = resolver.staticsPrefix(path);
			if (cls != null) {
				current = {
					name: cls.className,
					value: 'class ${cls.className}',
					type: PathResolver.staticsContainerName(cls.className),
					reference: stops.allocReference(RefStatics(cls.singleton, cls.proto)),
				};
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
			var accessor = path.accessors[i];
			var childName = switch (accessor) {
				case Field(name): name;
				case Index(index): Std.string(index);
			}
			if (current.reference <= 0) {
				throw new DebugError('"' + current.name + '" has no members');
			}
			var next = findByName(view.variablesFor(current.reference), childName);
			if (next == null) {
				var what = accessor.match(Index(_)) ? 'index [$childName]' : 'field "$childName"';
				throw new DebugError('"${current.name}" has no $what');
			}
			current = next;
		}
		return current;
	}

	function resolveRoot(frameId:Int, name:String):Null<VariableInfo> {
		var locals = view.readLocals(frameId);
		var local = findByName(locals, name);
		if (local != null) {
			return local;
		}
		// implicit this.field
		var self = findByName(locals, "this");
		if (self != null && self.reference > 0) {
			var member = findByName(view.variablesFor(self.reference), name);
			if (member != null) {
				return member;
			}
		}
		// static of the class owning the frame
		var frame = stops.frameAt(frameId);
		if (frame != null) {
			var statics = view.staticsScope(frame.location.fidx);
			if (statics != null) {
				return findByName(view.variablesFor(statics.reference), name);
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

	// --- the expression interpreter ---

	/**
		Evaluates an expression node to a typed value.
	**/
	public function valueOf(frameId:Int, e:Expr):EvalValue {
		return switch (e) {
			case EInt(v): VInt(v);
			case EFloat(f): VFloat(f);
			case EBool(b): VBool(b);
			case ENull: VNull;
			case EString(s): VString(s, null);
			case EIdent(_), EField(_, _):
				var path = chainToPath(e);
				if (path == null) {
					throw new DebugError("This value cannot be resolved as a variable path");
				}
				valueOfPath(frameId, path);
			case EIndex(recv, key):
				indexValue(frameId, recv, key);
			case ECall(callee, args):
				var path = chainToPath(callee);
				if (path == null) {
					throw new DebugError("The callee must be a function name or a variable path");
				}
				var values = [for (a in args) valueOf(frameId, a)];
				var ret = calls.callRaw(frameId, path, values);
				if (ret.type.match(HVoid)) {
					throw new DebugError('"' + path.display() + '" returns Void and cannot be used inside an expression');
				}
				toEvalValue(ret.raw, ret.type);
			case ENew(className, args):
				var values = [for (a in args) valueOf(frameId, a)];
				VObject(calls.construct(frameId, className, values), constructedType(className));
			case EUnop(op, inner):
				Operators.unop(op, valueOf(frameId, inner));
			case EBinop("&&", l, r):
				// the adapter's own && and || short-circuit, so the right side runs only when needed
				VBool(Operators.asBool(valueOf(frameId, l), "&&")
					&& Operators.asBool(valueOf(frameId, r), "&&"));
			case EBinop("||", l, r):
				VBool(Operators.asBool(valueOf(frameId, l), "||")
					|| Operators.asBool(valueOf(frameId, r), "||"));
			case EBinop(op, l, r):
				Operators.binop(op, valueOf(frameId, l), valueOf(frameId, r));
			case ETernary(cond, thenE, elseE):
				// only the taken branch runs (a branch may call a function)
				Operators.asBool(valueOf(frameId, cond), "?:")
					? valueOf(frameId, thenE) : valueOf(frameId, elseE);
			case EIs(inner, typeName):
				VBool(valueIsOfType(valueOf(frameId, inner), typeName));
			case EAssign(_, _):
				throw new DebugError("Assignment is only allowed at the top level of an expression");
		}
	}

	// `value is Type`, following Haxe's Std.isOfType for a supported subset.
	// null is never an instance. Int, Float, Bool, String and Dynamic match by
	// kind, and an Int also satisfies Float, as in Haxe. A class, enum or struct
	// name matches an object whose runtime class is that class or a subclass of
	// it, by full or simple name. Interfaces are not resolved. A type name that
	// names nothing is a user error, so a typo does not silently yield false.
	function valueIsOfType(v:EvalValue, typeName:String):Bool {
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
			throw new DebugError('Unknown type "' + typeName + '" in an `is` check');
		}
		return switch (v) {
			case VObject(ptr, type):
				var runtime = switch (type) {
					case HObj(_), HStruct(_): type;
					default: runtimeTypes.typeAt(memory.readPointer(ptr));
				}
				ClassChain.matches(runtime, typeName);
			default:
				false; // a primitive or string against a (real) class name
		}
	}

	/**
		The ValuePath spelled by a chain of identifiers, fields and constant
		non-negative indexes; null for any other expression.
	**/
	public static function chainToPath(e:Expr):Null<ValuePath> {
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

	/**
		Evaluates `key` to an array index, a non-negative Int.
	**/
	public function arrayIndex(frameId:Int, key:Expr):Int {
		return switch (valueOf(frameId, key)) {
			case VInt(v):
				var i = Int64.toInt(v);
				if (i < 0) {
					throw new DebugError("An index must be >= 0");
				}
				i;
			default:
				throw new DebugError("An array index must be an Int");
		}
	}

	// `recv[key]`: on a map this calls get(key); on anything else it reads the
	// element at a constant or computed index.
	function indexValue(frameId:Int, recv:Expr, key:Expr):EvalValue {
		var recvPath = chainToPath(recv);
		if (recvPath == null) {
			throw new DebugError("The receiver of [...] must be a variable path");
		}
		var target = resolver.targetOfPath(frameId, recvPath);
		if (mapTypeOfTarget(target) != null) {
			var ret = calls.callRaw(frameId, recvPath.plus("get"), [valueOf(frameId, key)]);
			return toEvalValue(ret.raw, ret.type);
		}
		var element = resolver.childTarget(target, Std.string(arrayIndex(frameId, key)));
		return evalValueAt(element.address, element.type);
	}

	function valueOfPath(frameId:Int, path:ValuePath):EvalValue {
		var target = resolver.targetOfPath(frameId, path);
		return evalValueAt(target.address, target.type);
	}

	// Reads the slot at `address` as type `t`.
	function evalValueAt(address:Pointer, t:HLType):EvalValue {
		return switch (t) {
			case HUi8: VInt(Int64.ofInt(memory.readU8(address)));
			case HUi16: VInt(Int64.ofInt(memory.readU16(address)));
			case HI32: VInt(Int64.ofInt(memory.readI32(address)));
			case HI64: VInt(memory.readI64(address));
			case HF32: VFloat(memory.readF32(address));
			case HF64: VFloat(memory.readF64(address));
			case HBool: VBool(memory.readU8(address) != 0);
			case HVoid: VNull;
			case HStruct(_), HPacked(_): VObject(address, t); // stored inline: the slot IS the struct
			default: pointerValue(memory.readPointer(address), t);
		}
	}

	function pointerValue(ptr:Pointer, t:HLType):EvalValue {
		if (Int64.eq(ptr, Int64.ofInt(0))) {
			return VNull;
		}
		return switch (t) {
			case HNull(inner): evalValueAt(ptr.offset(align.dynPayload), inner); // the box payload sits at +8 on BOTH bitnesses
			case HDyn: dynamicValue(ptr);
			case HObj(p) if (p != null && p.name == "String"): VString(valueReader.stringContentAt(ptr), ptr);
			case HObj(_): VObject(ptr, resolver.refineObjectType(ptr, t));
			default: VObject(ptr, t);
		}
	}

	// Reads a Dynamic. A primitive lives in a vdynamic box: hl_type* at +0, the
	// payload at +8 on BOTH bitnesses. For a pointer kind the pointer IS the
	// value, and its own header carries the type.
	function dynamicValue(ptr:Pointer):EvalValue {
		var runtime = runtimeTypes.typeAt(memory.readPointer(ptr));
		if (runtime == null) {
			return VObject(ptr, HDyn);
		}
		return switch (runtime) {
			case HUi8: VInt(Int64.ofInt(memory.readU8(ptr.offset(align.dynPayload))));
			case HUi16: VInt(Int64.ofInt(memory.readU16(ptr.offset(align.dynPayload))));
			case HI32: VInt(Int64.ofInt(memory.readI32(ptr.offset(align.dynPayload))));
			case HI64: VInt(memory.readI64(ptr.offset(align.dynPayload)));
			case HF32: VFloat(memory.readF32(ptr.offset(align.dynPayload)));
			case HF64: VFloat(memory.readF64(ptr.offset(align.dynPayload)));
			case HBool: VBool(memory.readU8(ptr.offset(align.dynPayload)) != 0);
			case HObj(p) if (p != null && p.name == "String"): VString(valueReader.stringContentAt(ptr), ptr);
			default: VObject(ptr, runtime);
		}
	}

	// Converts the raw return bits of a call into an EvalValue.
	function toEvalValue(raw:Pointer, t:HLType):EvalValue {
		return switch (t) {
			case HVoid: VNull;
			case HUi8, HUi16, HI32: VInt(Int64.ofInt(raw.low));
			case HI64: VInt(raw);
			case HBool: VBool(raw.low != 0);
			case HF64: VFloat(FPHelper.i64ToDouble(raw.low, raw.high));
			case HF32: VFloat(FPHelper.i32ToFloat(raw.low));
			default: pointerValue(raw, t);
		}
	}

	/**
		Renders an EvalValue as a displayed VariableInfo named `name`.
	**/
	public function renderValue(name:String, v:EvalValue):VariableInfo {
		return switch (v) {
			case VInt(i): {name: name, value: Int64.toStr(i), type: "Int", reference: 0};
			case VFloat(f): {name: name, value: Std.string(f), type: "Float", reference: 0};
			case VBool(b): {name: name, value: b ? "true" : "false", type: "Bool", reference: 0};
			case VNull: {name: name, value: "null", type: "Dynamic", reference: 0};
			case VString(s, _): {name: name, value: '"$s"', type: "String", reference: 0};
			case VObject(raw, t): decodeReturn(name, raw, t);
		}
	}

	function evaluateCall(frameId:Int, callee:ValuePath, args:Array<EvalValue>):VariableInfo {
		var call = calls.callRaw(frameId, callee, args);
		return decodeReturn(callee.display() + "()", call.raw, call.type);
	}

	// The HLType of the class `new` constructed; HDyn when the module has no such type.
	function constructedType(className:String):HLType {
		var t = module.typeByName(className);
		return t == null ? HDyn : t;
	}

	/**
		Decodes raw bits of type `retType` for display: a call's return value
		(RAX, or XMM0 copied to RAX for a float) or a pointer.
	**/
	public function decodeReturn(name:String, raw:Pointer, retType:HLType):VariableInfo {
		return switch (retType) {
			case HVoid: {name: name, value: "void", type: "Void", reference: 0};
			case HUi8, HUi16, HI32: {name: name, value: Std.string(raw.low), type: "Int", reference: 0};
			case HI64: {name: name, value: Int64.toStr(raw), type: "Int64", reference: 0};
			case HBool: {name: name, value: raw.low != 0 ? "true" : "false", type: "Bool", reference: 0};
			case HF64: {name: name, value: Std.string(FPHelper.i64ToDouble(raw.low, raw.high)), type: "Float", reference: 0};
			case HF32: {name: name, value: Std.string(FPHelper.i32ToFloat(raw.low)), type: "Float", reference: 0};
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

	// The runtime type of `target` when it is a map class (StringMap, IntMap,
	// ObjectMap or a BalancedTree), else null. A non-null result routes `[]` to
	// get/set instead of an array index.
	public function mapTypeOfTarget(target:WriteTarget):Null<HLType> {
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
}
