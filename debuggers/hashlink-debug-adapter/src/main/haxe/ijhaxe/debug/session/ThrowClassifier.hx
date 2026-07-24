package ijhaxe.debug.session;

import ijhaxe.debug.inspect.VariableInspector;
import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.module.TryRegions;
import ijhaxe.debug.target.DebugApi;
import ijhaxe.debug.target.MemoryReader;
import ijhaxe.debug.target.StackWalker;

/**
	Classifies a throw at stop time: whether anything will catch it, whether it
	was raised by the VM's C runtime rather than a bytecode OThrow, and whether
	the thrown value matches the configured exception-type filters. Read-only
	over the stopped debuggee — no run control.
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
		True when no live frame's current op sits inside a `try` block — only HL's
		root handler would catch the throw. Typed catches are approximated as
		always matching (any active try counts as catching), so this can
		under-report an uncaught throw whose only enclosing catch has a
		non-matching type.
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
		At hl_throw's entry, true when the immediate caller is C runtime code (the
		return address on the stack is NOT in JIT code) — i.e. the throw was
		raised by the VM (null access, bounds, cast, div0), not by a bytecode
		OThrow whose caller is jitted. Bytecode throws are left to the
		OThrow-based breakpoints.
	**/
	public function raisedByRuntime(threadId:Int):Bool {
		var esp = api.readRegister(debuggeePid, threadId, Esp);
		var returnAddress = memReader.readPointer(esp);
		return !jit.isCodePtr(returnAddress);
	}

	/**
		True when the thrown value (register `reg` of the throwing frame) is an
		instance of one of the `wanted` type filters — by FQN or simple name,
		including subclasses (the tsuper chain). Empty filter set ⇒ false.
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
