package ijhaxe.debug.session;

import ijhaxe.debug.inspect.VariableInspector;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.module.TryRegions;
import ijhaxe.debug.target.DebugApi;
import ijhaxe.debug.target.MemoryReader;
import ijhaxe.debug.target.StackWalker;

/**
	Classifies a throw at stop time: whether anything will catch it, whether the
	VM's C runtime raised it rather than a bytecode OThrow, and whether the
	thrown value matches the exception-type filters. It only reads the stopped
	debuggee and never controls execution.
**/
class ThrowClassifier {
	final api:DebugApi;
	final debuggeePid:Int;
	final jit:JitInfo;
	final memReader:MemoryReader;
	final stackWalker:StackWalker;
	final tryRegions:TryRegions;
	final inspector:VariableInspector;

	public function new(api:DebugApi, debuggeePid:Int, jit:JitInfo, memReader:MemoryReader,
			stackWalker:StackWalker, tryRegions:TryRegions, inspector:VariableInspector) {
		this.api = api;
		this.debuggeePid = debuggeePid;
		this.jit = jit;
		this.memReader = memReader;
		this.stackWalker = stackWalker;
		this.tryRegions = tryRegions;
		this.inspector = inspector;
	}

	/**
		True when no live frame's current op sits inside a `try` block, so only
		HL's root handler would catch the throw. Any enclosing `try` counts as
		catching, whatever its catch types. A throw whose only enclosing catch
		has a non-matching type is therefore reported as caught.
	**/
	public function isUncaught(threadId:Int):Bool {
		for (frame in stackWalker.walk(threadId)) {
			if (tryRegions.isProtected(frame.fidx, frame.op)) {
				return false;
			}
		}
		return true;
	}

	/**
		At hl_throw's entry, true when the immediate caller is C runtime code: the
		return address on the stack is not in JIT code. The VM raised the throw
		then (null access, out of bounds, invalid cast, division by zero). A
		bytecode OThrow has a jitted caller and is left to the OThrow-site
		breakpoints.
	**/
	public function raisedByRuntime(threadId:Int):Bool {
		var esp = api.readRegister(debuggeePid, threadId, Esp);
		var returnAddress = memReader.readPointer(esp);
		return !jit.isCodePtr(returnAddress);
	}

	/**
		True when the thrown value (register `reg` of the throwing frame) is an
		instance of a class named in `wanted`, by full or simple name, including
		subclasses. False for an empty `wanted`.
	**/
	public function throwMatchesTypes(threadId:Int, reg:Int, wanted:Array<String>):Bool {
		if (wanted.length == 0) {
			return false;
		}
		var frames = inspector.framesFor(threadId);
		if (frames.length == 0) {
			return false;
		}
		return inspector.registerValueMatchesType(frames[0].frameId, reg, wanted);
	}
}
