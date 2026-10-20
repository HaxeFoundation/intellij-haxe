package ijhaxe.debug.session;

import ijhaxe.debug.Pointer;
import ijhaxe.debug.inspect.VariableInspector;
import ijhaxe.debug.module.ModuleDebugInfo;

/**
	Builds the user-facing descriptions of exception stops
	(stopped(reason:"exception")): low-level runtime faults, VM-raised
	exceptions and bytecode throws. It only assembles text from the inspector's
	frame cache and the module's debug tables, and never controls execution.
**/
class StopDescriptions {
	final module:ModuleDebugInfo;
	final inspector:VariableInspector;

	public function new(module:ModuleDebugInfo, inspector:VariableInspector) {
		this.module = module;
		this.inspector = inspector;
	}

	/**
		Describes a low-level runtime fault (an Error or StackOverflow wait
		outcome). Unlike a Haxe throw, it has no exception value to read, and
		HL's debug API exposes no OS exception record, so the exact cause (null
		access, bad arithmetic, wild pointer) is unknown. The text names the
		kind, the faulting function and the source line. It also warns that
		execution cannot get past the fault: hl_debug_resume cannot pass the
		exception to the program, so resuming re-executes the faulting
		instruction.
	**/
	public function runtimeError(threadId:Int, stackOverflow:Bool):String {
		var what = stackOverflow
			? "Stack overflow"
			: "Low-level runtime error (such as a null access or invalid arithmetic; the VM reports no further detail)";

		var where = topFrameWhere(threadId);
		return what + (where != null ? " in " + where : "") + ". Execution cannot continue past this instruction.";
	}

	/**
		Describes a VM-raised error. `thrown` is the vdynamic in the thread's
		exc_value, available when the stop is hl_throw's own break. For errors
		raised through hl_error_msg it decodes to the exact runtime message
		("Null access .length", "Out of bounds 5/3"). Without it (the thread
		registry is unreadable, or the value does not decode) the text is
		generic.
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
		Describes the value being thrown, which is in register `reg` of the top
		frame; a generic message when it cannot be read.
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
		var source = module.sourceLineAt(location.fidx, location.op);
		return module.functionName(location.fidx)
			+ (source != null ? " (" + source.file + ":" + source.line + ")" : "");
	}
}
