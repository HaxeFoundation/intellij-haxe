package ijhaxe.debug.inspect;
import ijhaxe.debug.DebugError;

import ijhaxe.debug.values.*;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.eval.EvalValue;
import ijhaxe.debug.eval.ExprAst.Expr;
import ijhaxe.debug.eval.ExprParser;
import ijhaxe.debug.layout.FrameLayout;
import ijhaxe.debug.layout.RegisterSlot;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.module.ModuleDebugInfo;
import ijhaxe.debug.target.MemoryReader;
import format.hl.Data.HLType;
import haxe.Int64;

/**
	The value-modification path: DAP `setVariable` and `path = expr` in
	evaluate. It resolves the target slot (PathResolver), evaluates the
	right-hand side (ExpressionEvaluator) and writes it (ValueWriter) while the
	debuggee is stopped, so the user can steer execution.

	Writes happen only once `writer` is set, because value modification is
	enabled per session. A new string is created through the call service;
	existing strings and objects are written as pointers, and primitives go
	straight to the writer. After a write to a register-passed argument,
	`fixupAfterWrite` patches XMM0 for the first float argument and warns for
	the others.
**/
class VariableMutator {
	final resolver:PathResolver;
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
	// Set by DebugSession: reports a non-fatal warning as a console output event.
	public var warnSink:Null<String->Void> = null;

	public function new(resolver:PathResolver, evaluator:ExpressionEvaluator, calls:DebuggeeCallService,
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
		Sets a named child of a variablesReference (DAP `setVariable`) to any
		evaluate expression (a literal, another variable, arithmetic, a call) and
		returns the child's new decoded value. Throws DebugError on any failure.
	**/
	public function setVariable(reference:Int, name:String, valueExpr:String):VariableInfo {
		var target = resolver.targetInReference(reference, name);
		var v = evaluator.valueOf(resolver.writeFrame, ExprParser.parse(StringTools.trim(valueExpr)));

		writeValue(target, v);
		fixupAfterWrite(target);

		var decoded = valueReader.read(target.address, target.type);
		return {name: name, value: decoded.value, type: decoded.type, reference: decoded.reference};
	}

	/**
		`target = expr` from evaluate. The target is a variable path, an array
		element (any Int index expression), or a map bracket (sugar for set).
	**/
	public function assignExpr(frameId:Int, lhs:Expr, rhs:Expr):VariableInfo {
		switch (lhs) {
			case EIndex(recv, key):
				var recvPath = ExpressionEvaluator.chainToPath(recv);
				if (recvPath == null) {
					throw new DebugError("The receiver of [...] must be a variable path");
				}
				var target = resolver.targetOfPath(frameId, recvPath);
				var display = recvPath.display() + "[...]";
				if (evaluator.mapTypeOfTarget(target) != null) {
					// map bracket: sugar for set(key, value), read back via get
					var keyVal = evaluator.valueOf(frameId, key);
					var rhsVal = evaluator.valueOf(frameId, rhs);
					calls.callRaw(frameId, recvPath.plus("set"), [keyVal, rhsVal]); // set returns Void
					var read = calls.callRaw(frameId, recvPath.plus("get"), [keyVal]);
					return evaluator.decodeReturn(display, read.raw, read.type);
				}
				// array element (constant or computed index): a writable slot
				var element = resolver.childTarget(target, Std.string(evaluator.arrayIndex(frameId, key)));
				writeValue(element, evaluator.valueOf(frameId, rhs));
				var decoded = valueReader.read(element.address, element.type);
				return {name: element.name, value: decoded.value, type: decoded.type, reference: decoded.reference};
			default:
		}
		var path = ExpressionEvaluator.chainToPath(lhs);
		if (path == null) {
			throw new DebugError('The left side of "=" must be a variable path (e.g. name, obj.field, arr[0])');
		}
		var target = resolver.targetOfPath(frameId, path);
		writeValue(target, evaluator.valueOf(frameId, rhs));
		fixupAfterWrite(target);
		var decoded = valueReader.read(target.address, target.type);
		return {name: target.name, value: decoded.value, type: decoded.type, reference: decoded.reference};
	}

	// Writes an ALREADY evaluated value into a target slot through the
	// ValueWriter call that matches the value's kind.
	function writeValue(target:WriteTarget, v:EvalValue):Void {
		if (writer == null) {
			throw new DebugError("Value modification is not available in this session");
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
					throw new DebugError("Assigning a whole struct is not supported");
				}
				writer.assignRaw(target, raw, t);
		}
	}

	function stringType():HLType {
		var t = module.typeByName("String");
		return t == null ? HDyn : t;
	}

	// A register-passed argument ARRIVES in a CPU register, its arrival
	// register, and the prologue copies it to its stack slot. The code for the
	// argument's early uses may still read the arrival register instead of the
	// updated slot; writing only the slot leaves a traced Float parameter
	// unchanged. The first float argument arrives in XMM0 on both conventions
	// (win64 assigns XMM registers by position, so there it must also be
	// argument 0), and XMM0 is the one arrival register hl_debug_write_register
	// exposes, so it is patched too. No other register-passed argument can be
	// fixed up; a console warning explains a write that does not take effect on
	// the current line.
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
		if (isRegisterPassed(argIndex, offsets, argCount) && warnSink != null) {
			warnSink('[debugger] note: "${target.name}" is a register-passed argument; code on the '
				+ 'current line may still use the value it arrived with. The new value applies to later uses; '
				+ 'to steer this line, set the value in the caller before the call.' + String.fromCharCode(10));
		}
	}

	static function isFloatSlot(t:HLType):Bool {
		return t.match(HF32) || t.match(HF64);
	}

	// Whether argument `argIndex` arrives in a CPU register. win64 passes the
	// first 4 arguments by position; SysV passes the first 6 integer and the
	// first 8 float arguments.
	function isRegisterPassed(argIndex:Int, offsets:Array<RegisterSlot>, argCount:Int):Bool {
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
