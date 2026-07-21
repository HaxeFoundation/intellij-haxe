package debug.inspect;
import haxe.io.FPHelper;
import debug.DebugError;
import debug.DebugErrorCode;

import debug.values.*;

import debug.Pointer;
import debug.eval.EvalValue;
import debug.eval.ExprAst.Expr;
import debug.eval.ExprParser;
import debug.eval.Operators;
import debug.layout.Align;
import debug.module.ModuleDebugInfo;
import debug.target.MemoryReader;
import format.hl.Data.HLType;
import haxe.Int64;

/**
	The evaluate-expression interpreter. A parsed expression's leaves
	resolve through the SAME machinery as paths/writes (typed reads at
	SymbolResolver addresses, calls via DebuggeeCallService, `new` via construct,
	map brackets via get/set); operators fold ADAPTER-SIDE on EvalValue — no
	debuggee code runs for arithmetic. Produces either a raw EvalValue (for
	operands / conditions / call arguments) or a rendered VariableInfo (for
	display in the watches view).

	A pure variable path keeps the direct reference walk (`evaluatePath`) so a
	watch renders exactly like the Variables view — map entries, enum params,
	expandable references and all.
**/
class ExpressionEvaluator {
	final resolver:SymbolResolver;
	final calls:DebuggeeCallService;
	final view:VariablesView;
	final valueReader:ValueReader;
	final memory:MemoryReader;
	final module:ModuleDebugInfo;
	final align:Align;
	final runtimeTypes:RuntimeTypes;
	final stops:StopState;

	public function new(resolver:SymbolResolver, calls:DebuggeeCallService, view:VariablesView,
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
		Evaluates a NON-assignment expression to a displayed value (the caller
		handles a top-level assignment before delegating here). A pure path uses
		the Variables-view reference walk; a call/new returns the decoded result;
		anything else is interpreted and rendered.
	**/
	public function evaluateExpr(frameId:Int, e:Expr, exprText:String):VariableInfo {
		switch (e) {
			case ECall(callee, args):
				var calleePath = chainToPath(callee);
				if (calleePath == null) {
					throw new DebugError("The callee must be a function name or a variable path");
				}
				var values = [for (a in args) evalExpr(frameId, a)];
				return evaluateCall(frameId, calleePath, values);
			case ENew(className, args):
				var values = [for (a in args) evalExpr(frameId, a)];
				return decodeReturn('new $className()', calls.construct(frameId, className, values), constructedType(className));
			case EIndex(_, _):
				// the interpreter decides map-get vs array element (incl. computed keys)
				return renderValue(exprText, evalExpr(frameId, e));
			default:
		}
		// a pure variable path keeps the direct reference walk: it renders
		// exactly like the Variables view (map entries, enum params, ...)
		var path = chainToPath(e);
		if (path != null) {
			try {
				return evaluatePath(frameId, path);
			} catch (walkError:DebugError) {
				// The view renders some REAL objects as childless leaves (a String
				// shows its content, not bytes/length), so the walk cannot descend
				// into them even though the typed resolver can (`s.length` is a real
				// I32 field of the String HObj). A pure path has no side effects, so
				// retrying through the interpreter is safe; if that fails too, the
				// walk's error (including its UnresolvedName code, which the client
				// uses to qualify class names) is the one to surface.
				try {
					return renderValue(exprText, evalExpr(frameId, e));
				} catch (_:DebugError) {
					throw walkError;
				}
			}
		}
		// anything else is an operator expression: interpret it
		return renderValue(exprText, evalExpr(frameId, e));
	}

	/**
		Evaluates a breakpoint condition to a Bool in the given frame. The
		expression must yield a Bool — a number/string/object condition is a user
		error, surfaced with a clear message so the caller can fail safe (stop).
	**/
	public function evaluateBool(frameId:Int, expression:String):Bool {
		var e = ExprParser.parse(StringTools.trim(expression));
		if (e.match(EAssign(_, _))) {
			throw new DebugError("A breakpoint condition cannot be an assignment");
		}
		return switch (evalExpr(frameId, e)) {
			case VBool(b): b;
			case other: throw new DebugError("A breakpoint condition must be true/false, got "
				+ Operators.describe(other));
		}
	}

	// The direct path walk: resolves the root, then follows accessors through
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
					value: 'class ${cls.className}',
					type: SymbolResolver.staticsContainerName(cls.className),
					reference: stops.allocReference(RefStatics(cls.singleton, cls.proto)),
				};
				start = cls.consumed;
			}
		}
		if (current == null) {
			// UnresolvedName + the offending token lets the client resolve it against
			// its own source (imports) and re-issue a fully-qualified expression.
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
		Evaluates an expression node to a typed adapter-side value.
	**/
	public function evalExpr(frameId:Int, e:Expr):EvalValue {
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
				var values = [for (a in args) evalExpr(frameId, a)];
				var ret = calls.callRaw(frameId, path, values);
				if (ret.type.match(HVoid)) {
					throw new DebugError('"' + path.display() + '" returns Void and cannot be used inside an expression');
				}
				toEvalValue(ret.raw, ret.type);
			case ENew(className, args):
				var values = [for (a in args) evalExpr(frameId, a)];
				VObject(calls.construct(frameId, className, values), constructedType(className));
			case EUnop(op, inner):
				Operators.unop(op, evalExpr(frameId, inner));
			case EBinop("&&", l, r):
				// Haxe && / || short-circuit natively, so the right side only runs when needed
				VBool(Operators.asBool(evalExpr(frameId, l), "&&")
					&& Operators.asBool(evalExpr(frameId, r), "&&"));
			case EBinop("||", l, r):
				VBool(Operators.asBool(evalExpr(frameId, l), "||")
					|| Operators.asBool(evalExpr(frameId, r), "||"));
			case EBinop(op, l, r):
				Operators.binop(op, evalExpr(frameId, l), evalExpr(frameId, r));
			case ETernary(cond, thenE, elseE):
				// only the taken branch runs (a branch may call a function)
				Operators.asBool(evalExpr(frameId, cond), "?:")
					? evalExpr(frameId, thenE) : evalExpr(frameId, elseE);
			case EIs(inner, typeName):
				VBool(valueIsOfType(evalExpr(frameId, inner), typeName));
			case EAssign(_, _):
				throw new DebugError("Assignment is only allowed at the top level of an expression");
		}
	}

	// `value is Type` (Haxe Std.isOfType semantics, the subset we support):
	// null is never an instance; Int/Float/Bool/String/Dynamic match by kind
	// (an Int satisfies Float, as in Haxe); a class/enum/struct name matches an
	// object whose runtime class equals it or descends from it (tsuper chain,
	// by full or simple name). Interfaces are not resolved. A type name that
	// names nothing is a user error (so a typo isn't a silent false).
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
				false; // a primitive/string against a (real) class name
		}
	}

	// (subtype matching moved to ClassChain — shared with exception
	// breakpoint type filters)

	/**
		A chain of EIdent/EField/EIndex(constant int) is exactly a ValuePath.
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
		Evaluates an index expression to a non-negative Int (for array elements).
	**/
	public function intKey(frameId:Int, key:Expr):Int {
		return switch (evalExpr(frameId, key)) {
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

	// `recv[key]`: a map routes to get(key); anything else is an indexed element
	// (constant or computed key).
	function indexValue(frameId:Int, recv:Expr, key:Expr):EvalValue {
		var recvPath = chainToPath(recv);
		if (recvPath == null) {
			throw new DebugError("The receiver of [...] must be a variable path");
		}
		var target = resolver.targetOfPath(frameId, recvPath);
		if (mapTypeOfTarget(target) != null) {
			var ret = calls.callRaw(frameId, recvPath.plus("get"), [evalExpr(frameId, key)]);
			return toEvalValue(ret.raw, ret.type);
		}
		var element = resolver.childTarget(target, Std.string(intKey(frameId, key)));
		return evalValueAt(element.address, element.type);
	}

	function valueOfPath(frameId:Int, path:ValuePath):EvalValue {
		var target = resolver.targetOfPath(frameId, path);
		return evalValueAt(target.address, target.type);
	}

	// Typed read of a slot into the interpreter's currency.
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
			case HStruct(_), HPacked(_): VObject(address, t); // inline: the slot IS the base
			default: pointerValue(memory.readPointer(address), t);
		}
	}

	function pointerValue(ptr:Pointer, t:HLType):EvalValue {
		if (Int64.eq(ptr, Int64.ofInt(0))) {
			return VNull;
		}
		return switch (t) {
			case HNull(inner): evalValueAt(ptr.offset(align.dynPayload), inner); // box payload (@ +8 on BOTH bitnesses)
			case HDyn: dynamicValue(ptr);
			case HObj(p) if (p != null && p.name == "String"): VString(valueReader.stringContentAt(ptr), ptr);
			case HObj(_): VObject(ptr, resolver.refineObjectType(ptr, t));
			default: VObject(ptr, t);
		}
	}

	// A Dynamic value: primitives live in a vdynamic box (hl_type* @0, payload
	// union @ +8 on BOTH bitnesses); pointer kinds ARE the value (their own
	// header says so).
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

	// A call's raw return (RAX bits) into the interpreter's currency.
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
		Renders an evaluated value (displayed VariableInfo).
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

	// The module HLType of a construction result (the class named).
	function constructedType(className:String):HLType {
		var t = module.typeByName(className);
		return t == null ? HDyn : t;
	}

	/**
		Decodes a raw call/pointer result (RAX, or XMM0-as-RAX for a float) for display.
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

	// The map type of a resolved receiver if it is one of the map classes
	// (StringMap/IntMap/ObjectMap or a BalancedTree), else null — the signal to
	// route `[]` to get/set rather than treat it as an array index.
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
