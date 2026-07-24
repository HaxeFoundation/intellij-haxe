package ijhaxe.debug.session;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.inspect.VariableInspector;
import ijhaxe.debug.module.ModuleDebugInfo;

/**
	Builds the user-facing texts for exception stops (the
	stopped(reason:"exception") descriptions): low-level runtime faults,
	VM-raised exceptions and bytecode throws. Pure text assembly over the
	inspector's frame cache and the module's debug tables — no run control.
**/
class StopDescriptions {
	final module:ModuleDebugInfo;
	final inspector:VariableInspector;

	public function new(module:ModuleDebugInfo, inspector:VariableInspector) {
		this.module = module;
		this.inspector = inspector;
	}

	/**
		Describes a LOW-LEVEL runtime fault (Error/StackOverflow wait outcome):
		unlike a Haxe throw there is no exception value to read, and HL's debug
		API exposes no OS exception record, so the precise cause (null access,
		bad arithmetic, wild pointer, ...) is not knowable here. Give the user
		what we do know — the kind, the faulting function and source line — and
		warn that the fault is not steppable: resuming re-executes the faulting
		instruction (hl_debug_resume has no pass-to-program mode), so
		step/continue can never get past it.
	**/
	public function runtimeError(threadId:Int, stackOverflow:Bool):String {
		var what = stackOverflow
			? "Stack overflow"
			: "Low-level runtime error (such as a null access or invalid arithmetic; the VM reports no further detail)";
		var where = topFrameWhere(threadId);
		return what + (where != null ? " in " + where : "") + ". Execution cannot continue past this instruction.";
	}

	/**
		The description for a VM-raised error. `thrown` is the vdynamic parked in
		the thread's exc_value — available when the stop is hl_throw's own break;
		for hl_error_msg-raised errors it decodes to the exact runtime message
		("Null access .length", "Out of bounds 5/3", ...). Null (registry
		unreadable, or decode failure) degrades to a generic text.
	**/
	public function vmThrow(threadId:Int, thrown:Null<Pointer>):String {
		var message = thrown != null ? inspector.previewDynamicPointer(thrown) : null;
		var where = topFrameWhere(threadId);
		if (message != null) {
			return 'HashLink VM exception: $message' + (where != null ? ' — in $where' : '');
		}
		return 'HashLink VM exception' + (where != null ? ' in $where' : '')
			+ ' (such as a null access or out-of-bounds — inspect the locals to see the offending value).';
	}

	/**
		Describes the value being thrown (the exception in register `reg` of the
		top frame); a generic message if it can't be read.
	**/
	public function thrown(threadId:Int, reg:Int):String {
		var frames = inspector.framesFor(threadId);
		if (frames.length == 0) {
			return "Exception thrown";
		}
		var value = inspector.thrownRegisterPreview(frames[0].frameId, reg);
		if (value == null || value.value == null) {
			return "Exception thrown";
		}
		return value.type != null ? value.type + ": " + value.value : value.value;
	}

	// "Class.method (File.hx:12)" for the thread's top frame, or null without frames.
	function topFrameWhere(threadId:Int):Null<String> {
		var frames = inspector.framesFor(threadId);
		if (frames.length == 0) {
			return null;
		}
		var location = frames[0].location;
		var source = module.lookup(location.fidx, location.op);
		return module.functionName(location.fidx)
			+ (source != null ? " (" + source.file + ":" + source.line + ")" : "");
	}
}
