package debug.inspect;

import debug.values.*;

import debug.Pointer;
import debug.eval.EvalValue;
import debug.eval.ExprAst.Expr;
import debug.eval.ExprParser;
import debug.layout.FrameLayout;
import debug.layout.RegisterSlot;
import debug.module.JitInfo;
import debug.module.ModuleDebugInfo;
import debug.target.MemoryReader;
import format.hl.Data.HLType;
import haxe.Int64;

/**
 * The value-modification path (M12): DAP `setVariable` and `path = expr` in
 * evaluate. Resolves the target slot (via SymbolResolver), evaluates the RHS
 * (via ExpressionEvaluator), and writes it (via ValueWriter) while the debuggee
 * is stopped — to steer execution.
 *
 * Non-null `writer` gates every write (value modification is opt-in per
 * session). String/object RHS values are materialized/passed through by the
 * call service; primitives go straight to the writer. After a write to a
 * register-passed argument, the arrival-register fixup (`fixupAfterWrite`)
 * patches XMM0 for the first float arg (the only arrival register the debug
 * API exposes) and warns for the rest.
 */
class VariableMutator {
	final resolver:SymbolResolver;
	final evaluator:ExpressionEvaluator;
	final calls:DebuggeeCallService;
	final valueReader:ValueReader;
	final memory:MemoryReader;
	final module:ModuleDebugInfo;
	final jit:JitInfo;
	final frameLayout:FrameLayout;
	final stops:StopState;

	// Non-null once value modification is enabled (a MemoryWriter is available).
	public var writer:Null<ValueWriter> = null;
	// Set by DebugSession: writes the low half of XMM0 (arrival-register fixup).
	public var xmm0Writer:Null<Float->Void> = null;
	// Set by DebugSession: surfaces a non-fatal warning (as a console output event).
	public var warnSink:Null<String->Void> = null;

	public function new(resolver:SymbolResolver, evaluator:ExpressionEvaluator, calls:DebuggeeCallService,
			valueReader:ValueReader, memory:MemoryReader, module:ModuleDebugInfo, jit:JitInfo,
			frameLayout:FrameLayout, stops:StopState) {
		this.resolver = resolver;
		this.evaluator = evaluator;
		this.calls = calls;
		this.valueReader = valueReader;
		this.memory = memory;
		this.module = module;
		this.jit = jit;
		this.frameLayout = frameLayout;
		this.stops = stops;
	}

	/**
	 * Sets a named child of a variablesReference (DAP `setVariable`) to any
	 * evaluate expression (literal, another variable, arithmetic, a call), and
	 * returns the child's new decoded value. Throws DebugError on any failure.
	 */
	public function setVariable(reference:Int, name:String, valueExpr:String):VariableInfo {
		var target = resolver.targetInReference(reference, name);
		var v = evaluator.evalExpr(resolver.writeFrame, ExprParser.parse(StringTools.trim(valueExpr)));
		writeValue(target, v);
		fixupAfterWrite(target);
		var decoded = valueReader.read(target.address, target.type);
		return {name: name, value: decoded.value, type: decoded.type, reference: decoded.reference};
	}

	/**
	 * `target = expr` from evaluate: the target is a variable path, an array
	 * element (any Int key expression), or a map bracket (sugar for set).
	 */
	public function assignExpr(frameId:Int, lhs:Expr, rhs:Expr):VariableInfo {
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

	// Writes an ALREADY-EVALUATED expression value into a target slot, mapping
	// each value kind onto the appropriate ValueWriter primitive.
	function writeValue(target:WriteTarget, v:EvalValue):Void {
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

	function stringType():HLType {
		var t = module.typeByName("String");
		return t == null ? HDyn : t;
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

	static function isFloatSlot(t:HLType):Bool {
		return t.match(HF32) || t.match(HF64);
	}

	// Whether argument `argIndex` arrives in a CPU register: win64 passes the
	// first 4 positionally; SysV the first 6 integer-class / 8 float-class.
	function registerPassed(argIndex:Int, offsets:Array<RegisterSlot>, argCount:Int):Bool {
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
}
