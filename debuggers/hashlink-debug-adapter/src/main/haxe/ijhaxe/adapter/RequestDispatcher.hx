package ijhaxe.adapter;
import ijhaxe.debug.target.ThreadInfo;

import ijhaxe.debug.breakpoints.BreakpointResult;
import ijhaxe.debug.session.DebugEvent;
import ijhaxe.debug.session.FrameInfo;
import ijhaxe.debug.session.LaunchConfig;
import ijhaxe.debug.breakpoints.RequestedBreakpoint;
import ijhaxe.debug.inspect.ScopeInfo;
import ijhaxe.debug.session.SessionCommand;
import ijhaxe.debug.session.StepMode;
import ijhaxe.debug.values.VariableInfo;

import ijhaxe.dap.protocol.Capabilities;
import ijhaxe.dap.protocol.requests.ContinueArguments;
import ijhaxe.dap.protocol.responses.ContinueResponseBody;
import ijhaxe.dap.protocol.responses.ErrorResponseBody;
import ijhaxe.dap.protocol.Event;
import ijhaxe.dap.protocol.requests.LaunchRequestArguments;
import ijhaxe.dap.protocol.responses.Message;
import ijhaxe.dap.protocol.requests.PauseArguments;
import ijhaxe.dap.protocol.ProtocolMessage;
import ijhaxe.dap.protocol.Request;
import ijhaxe.dap.protocol.Response;
import ijhaxe.dap.protocol.requests.ScopesArguments;
import ijhaxe.dap.protocol.requests.SetBreakpointsArguments;
import ijhaxe.dap.protocol.requests.SetExceptionBreakpointsArguments;
import ijhaxe.dap.protocol.requests.SetVariableArguments;
import ijhaxe.dap.protocol.requests.EvaluateArguments;
import ijhaxe.dap.protocol.requests.VariablesArguments;
import ijhaxe.dap.protocol.SourceBreakpoint;
import ijhaxe.dap.protocol.requests.StackTraceArguments;
import ijhaxe.dap.protocol.requests.StepInTargetsArguments;
import ijhaxe.dap.protocol.responses.StepInTargetsResponseBody;
import ijhaxe.dap.protocol.responses.ThreadsResponseBody;
import haxe.Json;

/**
	Translates DAP requests into session commands, and session DebugEvents into
	DAP responses and events.

	It performs no I/O and owns the `seq` counter of the adapter's outgoing
	messages. A request that the session thread handles gets a deferred
	response: the command carries the request seq, and the matching DebugEvent
	completes the response later. Only the worker thread calls into this class,
	so messages keep their order.
**/
class RequestDispatcher {
	static inline var ERROR_UNRECOGNIZED_COMMAND = 1000;
	static inline var ERROR_INVALID_REQUEST = 1001;
	static inline var ERROR_LAUNCH_FAILED = 1002;

	final sendToClient:ProtocolMessage->Void;
	final sendToSession:SessionCommand->Void;

	var nextSeq:Int = 1;
	var nextBreakpointId:Int = 1;
	var launched:Bool = false;
	var currentThreadId:Int = 1;

	// the command name of each request awaiting a deferred response, by request
	// seq: the response has to echo it
	final deferredCommands:Map<Int, String> = new Map();
	// breakpoints requested before launch, replayed for re-verification afterwards
	final preLaunchBreakpoints:Array<{sourceKey:String, sourcePath:String, requested:Array<RequestedBreakpoint>}> = [];

	/**
		Set once the session has ended, or a disconnect arrived before any launch.
	**/
	public var shutdownRequested(default, null):Bool = false;

	public function new(sendToClient:ProtocolMessage->Void, sendToSession:SessionCommand->Void) {
		this.sendToClient = sendToClient;
		this.sendToSession = sendToSession;
	}

	/**
		Parses one frame payload and dispatches it. Never throws on bad input.
	**/
	public function handleRawPayload(payload:String):Void {
		var parsed:Dynamic;
		try {
			parsed = Json.parse(payload);
		} catch (e:Dynamic) {
			sendError(0, "", ERROR_INVALID_REQUEST, "Invalid JSON payload");
			return;
		}
		handleRequest(parsed);
	}

	public function handleRequest(message:Dynamic):Void {
		var seq = readInt(message, "seq");
		var type = readString(message, "type");
		var command = readString(message, "command");
		if (type != "request" || command == null) {
			sendError(seq, command == null ? "" : command, ERROR_INVALID_REQUEST, "Not a valid DAP request");
			return;
		}
		var request:Request = message;
		switch (command) {
			case "initialize":
				handleInitialize(request);
			case "launch":
				handleLaunch(request);
			case "setBreakpoints":
				handleSetBreakpoints(request);
			case "setExceptionBreakpoints":
				handleSetExceptionBreakpoints(request);
			case "custom/setToStringRendering":
				handleSetToStringRendering(request);
			case "configurationDone":
				handleConfigurationDone(request);
			case "continue":
				handleContinue(request);
			case "next":
				handleStep(request, Next);
			case "stepIn":
				handleStep(request, StepIn);
			case "stepInTargets":
				handleStepInTargets(request);
			case "stepOut":
				handleStep(request, StepOut);
			case "pause":
				handlePause(request);
			case "stackTrace":
				handleStackTrace(request);
			case "scopes":
				handleScopes(request);
			case "variables":
				handleVariables(request);
			case "setVariable":
				handleSetVariable(request);
			case "evaluate":
				handleEvaluate(request);
			case "threads":
				handleThreads(request);
			case "disconnect":
				handleDisconnect(request);
			default:
				sendError(request.seq, command, ERROR_UNRECOGNIZED_COMMAND, "Unrecognized command: " + command);
		}
	}

	// --- request handlers ---

	function handleInitialize(request:Request):Void {
		var capabilities:Capabilities = {
			supportsConfigurationDoneRequest: true, supportsVariableType: true,
			supportsEvaluateForHovers: true, supportsSetVariable: true,
			supportsConditionalBreakpoints: true,
			supportsStepInTargetsRequest: true,
			exceptionBreakpointFilters: [
				{filter: "all", label: "All Exceptions"},
				{filter: "uncaught", label: "Uncaught Exceptions"}
			]
		};
		sendSuccess(request.seq, request.command, capabilities);
		// the spec requires the initialized event strictly after the initialize response
		sendEvent("initialized");
	}

	function handleLaunch(request:Request):Void {
		var args:LaunchRequestArguments = request.arguments;
		if (args == null || args.program == null || args.program == "") {
			sendError(request.seq, request.command, ERROR_LAUNCH_FAILED, "launch requires a 'program' (.hl file)");
			return;
		}
		if (args.attachPid != null && (args.debugPort == null || args.debugPort <= 0)) {
			sendError(request.seq, request.command, ERROR_LAUNCH_FAILED, "launch with 'attachPid' requires a positive 'debugPort'");
			return;
		}
		var config:LaunchConfig = {
			program: args.program,
			args: args.args != null ? args.args : [],
			cwd: args.cwd,
			hlPath: (args.hlPath != null && args.hlPath != "") ? args.hlPath : defaultHlExecutable(),
			stopOnEntry: args.stopOnEntry == true,
			attachPid: args.attachPid,
			debugPort: args.debugPort
		};
		deferResponse(request);
		sendToSession(CmdLaunch(request.seq, config));
	}

	function handleSetBreakpoints(request:Request):Void {
		var args:SetBreakpointsArguments = request.arguments;
		var sourcePath = (args != null && args.source != null && args.source.path != null) ? args.source.path : "";
		var sourceKey = sourcePath.toLowerCase();
		var sourceBreakpoints:Array<SourceBreakpoint> = (args != null && args.breakpoints != null) ? args.breakpoints : [];
		var requested:Array<RequestedBreakpoint> = [
			for (sb in sourceBreakpoints) {id: nextBreakpointId++, line: sb.line, condition: sb.condition}
		];

		if (!launched) {
			// answer provisionally; re-verified after launch via breakpoint events
			preLaunchBreakpoints.push({sourceKey: sourceKey, sourcePath: sourcePath, requested: requested});
			var provisional:Array<BreakpointResult> = [
				for (r in requested) {id: r.id, verified: false, line: r.line, message: "breakpoint will be resolved at launch", sourcePath: sourcePath}
			];
			sendSuccess(request.seq, request.command, setBreakpointsBody(provisional));
			return;
		}
		deferResponse(request);
		sendToSession(CmdSetBreakpoints(request.seq, sourceKey, sourcePath, requested, false));
	}

	function handleSetExceptionBreakpoints(request:Request):Void {
		var args:SetExceptionBreakpointsArguments = request.arguments;
		var filters = (args != null && args.filters != null) ? args.filters : [];
		var filterTypes = (args != null && args.filterTypes != null) ? args.filterTypes : [];
		// Forwarded before and after launch alike: the session stores the filters
		// and arms the throw sites as soon as it has launched.
		deferResponse(request);
		sendToSession(CmdSetExceptionBreakpoints(request.seq, filters, filterTypes));
	}

	// Custom request: the user's live opt-in for toString object labels. The
	// session only stores the flag; objects keep their class-name labels either
	// way (see DebugSession).
	function handleSetToStringRendering(request:Request):Void {
		var args:Dynamic = request.arguments;
		var enabled = args != null && args.enabled == true;
		deferResponse(request);
		sendToSession(CmdSetToStringRendering(request.seq, enabled));
	}

	function handleConfigurationDone(request:Request):Void {
		if (!launched) {
			sendSuccess(request.seq, request.command, null);
			return;
		}
		deferResponse(request);
		sendToSession(CmdConfigurationDone(request.seq));
	}

	function handleContinue(request:Request):Void {
		if (!launched) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Cannot continue: nothing is running");
			return;
		}
		var args:ContinueArguments = request.arguments;
		var threadId = args != null ? args.threadId : currentThreadId;
		deferResponse(request);
		sendToSession(CmdContinue(request.seq, threadId));
	}

	function handleStep(request:Request, mode:StepMode):Void {
		if (!launched) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Cannot step: nothing is running");
			return;
		}
		// stepIn may also carry a targetId: the opcode id of one call listed by
		// stepInTargets, which the step then enters
		var threadId = request.arguments != null && Reflect.hasField(request.arguments, "threadId") ? request.arguments.threadId : currentThreadId;
		var targetId:Null<Int> = null;
		if (mode == StepIn && request.arguments != null) {
			var raw:Dynamic = Reflect.field(request.arguments, "targetId");
			if (Std.isOfType(raw, Int)) {
				targetId = raw;
			}
		}
		deferResponse(request);
		sendToSession(CmdStep(request.seq, threadId, mode, targetId));
	}

	function handleStepInTargets(request:Request):Void {
		if (!launched) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Cannot list step-in targets: nothing is running");
			return;
		}
		var args:StepInTargetsArguments = request.arguments;
		if (args == null || !Reflect.hasField(args, "frameId")) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Missing frameId");
			return;
		}
		deferResponse(request);
		sendToSession(CmdStepInTargets(request.seq, args.frameId));
	}

	function handlePause(request:Request):Void {
		if (!launched) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Cannot pause: nothing is running");
			return;
		}
		var args:PauseArguments = request.arguments;
		var threadId = args != null ? args.threadId : currentThreadId;
		deferResponse(request);
		sendToSession(CmdPause(request.seq, threadId));
	}

	function handleThreads(request:Request):Void {
		if (!launched) {
			// clients poll threads before launch too: answer with a placeholder
			sendSuccess(request.seq, request.command, {threads: [{id: 1, name: "main"}]});
			return;
		}
		deferResponse(request);
		sendToSession(CmdThreads(request.seq));
	}

	function handleStackTrace(request:Request):Void {
		if (!launched) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Cannot get a stack trace: nothing is running");
			return;
		}
		var args:StackTraceArguments = request.arguments;
		var threadId = args != null ? args.threadId : currentThreadId;
		deferResponse(request);
		sendToSession(CmdStackTrace(request.seq, threadId));
	}

	function handleScopes(request:Request):Void {
		if (!launched) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Cannot get scopes: nothing is running");
			return;
		}
		var args:ScopesArguments = request.arguments;
		deferResponse(request);
		sendToSession(CmdScopes(request.seq, args != null ? args.frameId : 0));
	}

	function handleVariables(request:Request):Void {
		if (!launched) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Cannot get variables: nothing is running");
			return;
		}
		var args:VariablesArguments = request.arguments;
		deferResponse(request);
		sendToSession(CmdVariables(request.seq, args != null ? args.variablesReference : 0));
	}

	function handleSetVariable(request:Request):Void {
		if (!launched) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Cannot set a value: nothing is running");
			return;
		}
		var args:SetVariableArguments = request.arguments;
		if (args == null || args.name == null || args.value == null) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Missing name or value");
			return;
		}
		deferResponse(request);
		sendToSession(CmdSetVariable(request.seq, args.variablesReference, args.name, args.value));
	}

	function handleEvaluate(request:Request):Void {
		if (!launched) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Cannot evaluate: nothing is running");
			return;
		}
		var args:EvaluateArguments = request.arguments;
		if (args == null || args.expression == null) {
			sendError(request.seq, request.command, ERROR_INVALID_REQUEST, "Missing expression");
			return;
		}
		deferResponse(request);
		sendToSession(CmdEvaluate(request.seq, args.frameId != null ? args.frameId : 0, args.expression));
	}

	function handleDisconnect(request:Request):Void {
		if (!launched) {
			sendSuccess(request.seq, request.command, null);
			shutdownRequested = true;
			return;
		}
		deferResponse(request);
		sendToSession(CmdDisconnect(request.seq));
	}

	// --- session events -> responses/events ---

	public function handleSessionEvent(event:DebugEvent):Void {
		switch (event) {
			case EvLaunched(seq):
				launched = true;
				completeSuccess(seq, null);
				flushPreLaunchBreakpoints();
			case EvLaunchFailed(seq, message):
				completeError(seq, ERROR_LAUNCH_FAILED, message);
			case EvBreakpoints(seq, results):
				completeSuccess(seq, setBreakpointsBody(results));
			case EvConfigurationDone(seq):
				completeSuccess(seq, null);
			case EvContinued(seq):
				var body:ContinueResponseBody = {allThreadsContinued: true};
				completeSuccess(seq, body);
			case EvStepStarted(seq):
				completeSuccess(seq, null);
			case EvPaused(seq):
				completeSuccess(seq, null);
			case EvExceptionBreakpointsSet(seq):
				completeSuccess(seq, null);
			case EvToStringRenderingSet(seq):
				completeSuccess(seq, null);
			case EvThreads(seq, threads):
				completeSuccess(seq, threadsBody(threads));
			case EvStepInTargets(seq, targets):
				var body:StepInTargetsResponseBody = {targets: [for (t in targets) {id: t.id, label: t.label}]};
				completeSuccess(seq, body);
			case EvStackTrace(seq, frames):
				completeSuccess(seq, stackTraceBody(frames));
			case EvScopes(seq, scopes):
				completeSuccess(seq, scopesBody(scopes));
			case EvVariables(seq, variables):
				completeSuccess(seq, variablesBody(variables));
			case EvVariableSet(seq, result):
				completeSuccess(seq, {value: result.value, type: result.type, variablesReference: result.reference});
			case EvEvaluated(seq, result):
				completeSuccess(seq, {result: result.value, type: result.type, variablesReference: result.reference});
			case EvRejected(seq, message, code, variables):
				completeError(seq, code, message, variables);
			case EvSessionEnded(seq):
				completeSuccess(seq, null);
				shutdownRequested = true;

			case EvBreakpointChanged(result):
				sendEvent("breakpoint", {reason: "changed", breakpoint: breakpointStruct(result)});
			case EvStoppedBreakpoint(threadId, hitBreakpointIds):
				currentThreadId = threadId;
				sendEvent("stopped", {reason: "breakpoint", threadId: threadId, allThreadsStopped: true, hitBreakpointIds: hitBreakpointIds});
			case EvStoppedStep(threadId):
				currentThreadId = threadId;
				sendEvent("stopped", {reason: "step", threadId: threadId, allThreadsStopped: true});
			case EvStoppedException(threadId, description):
				currentThreadId = threadId;
				sendEvent("stopped", {reason: "exception", threadId: threadId, allThreadsStopped: true, description: description});
			case EvStoppedPause(threadId):
				currentThreadId = threadId;
				sendEvent("stopped", {reason: "pause", threadId: threadId, allThreadsStopped: true});
			case EvResumed(threadId):
				currentThreadId = threadId;
				sendEvent("continued", {threadId: threadId, allThreadsContinued: true});

			case EvOutput(category, text):
				sendEvent("output", {category: category, output: text});
			case EvExited(exitCode):
				sendEvent("exited", {exitCode: exitCode});
				sendEvent("terminated");
		}
	}

	function flushPreLaunchBreakpoints():Void {
		for (entry in preLaunchBreakpoints) {
			sendToSession(CmdSetBreakpoints(-1, entry.sourceKey, entry.sourcePath, entry.requested, true));
		}
		preLaunchBreakpoints.resize(0);
	}

	// --- response/event helpers ---

	function deferResponse(request:Request):Void {
		deferredCommands.set(request.seq, request.command);
	}

	function completeSuccess(requestSeq:Int, body:Dynamic):Void {
		var command = deferredCommands.get(requestSeq);
		deferredCommands.remove(requestSeq);
		sendSuccess(requestSeq, command == null ? "" : command, body);
	}

	function completeError(requestSeq:Int, errorId:Int, message:String, ?variables:Null<Map<String, String>>):Void {
		var command = deferredCommands.get(requestSeq);
		deferredCommands.remove(requestSeq);
		sendError(requestSeq, command == null ? "" : command, errorId, message, variables);
	}

	function sendSuccess(requestSeq:Int, command:String, body:Dynamic):Void {
		var response:Response = {
			seq: nextSeq++,
			type: "response",
			request_seq: requestSeq,
			success: true,
			command: command
		};
		if (body != null) {
			response.body = body;
		}
		sendToClient(response);
	}

	function sendError(requestSeq:Int, command:String, errorId:Int, message:String, ?variables:Null<Map<String, String>>):Void {
		var error:Message = {id: errorId, format: message, showUser: false};
		if (variables != null) {
			// DAP Message.variables is a plain JSON object; copy the Map into one
			var details = new haxe.DynamicAccess<String>();
			for (key => value in variables) {
				details.set(key, value);
			}
			error.variables = details;
		}
		var body:ErrorResponseBody = {error: error};
		var response:Response = {
			seq: nextSeq++,
			type: "response",
			request_seq: requestSeq,
			success: false,
			command: command,
			message: message,
			body: body
		};
		sendToClient(response);
	}

	function sendEvent(name:String, ?body:Dynamic):Void {
		var event:Event = {
			seq: nextSeq++,
			type: "event",
			event: name
		};
		if (body != null) {
			event.body = body;
		}
		sendToClient(event);
	}

	function threadsBody(threads:Array<ThreadInfo>):ThreadsResponseBody {
		return {threads: [for (t in threads) {id: t.id, name: t.name}]};
	}

	function setBreakpointsBody(results:Array<BreakpointResult>):Dynamic {
		return {breakpoints: [for (r in results) breakpointStruct(r)]};
	}

	function breakpointStruct(result:BreakpointResult):Dynamic {
		var breakpoint:Dynamic = {id: result.id, verified: result.verified, line: result.line};
		if (result.message != null) {
			breakpoint.message = result.message;
		}
		if (result.sourcePath != null) {
			breakpoint.source = {name: baseName(result.sourcePath), path: result.sourcePath};
		}
		return breakpoint;
	}

	function stackTraceBody(frames:Array<FrameInfo>):Dynamic {
		var stackFrames:Array<Dynamic> = [];
		for (frame in frames) {
			var stackFrame:Dynamic = {id: frame.id, name: frame.name, line: frame.line, column: 1};
			if (frame.file != null) {
				stackFrame.source = {name: baseName(frame.file), path: frame.file};
			}
			stackFrames.push(stackFrame);
		}
		return {stackFrames: stackFrames, totalFrames: stackFrames.length};
	}

	function scopesBody(scopes:Array<ScopeInfo>):Dynamic {
		return {
			scopes: [
				for (s in scopes) {
					var scope:Dynamic = {name: s.name, variablesReference: s.reference};
					if (s.hint != null) {
						scope.presentationHint = s.hint;
					}
					scope;
				}
			]
		};
	}

	function variablesBody(variables:Array<VariableInfo>):Dynamic {
		return {
			variables: [
				for (v in variables) {
					var variable:Dynamic = {name: v.name, value: v.value, type: v.type, variablesReference: v.reference};
					if (v.kind != null) {
						variable.kind = v.kind;
					}
					variable;
				}
			]
		};
	}

	// The HashLink VM running this adapter; the debuggee runs on it unless the
	// client names another. The deprecated Sys.executablePath() points at
	// Sys.programPath(), but on HL that returns the adapter's .hl file instead
	// of the VM, so the warning is silenced for this function only.
	@:haxe.warning("-WDeprecated")
	static function defaultHlExecutable():String {
		return Sys.executablePath();
	}

	static function baseName(path:String):String {
		var normalized = StringTools.replace(path, "\\", "/");
		var slash = normalized.lastIndexOf("/");
		return slash < 0 ? normalized : normalized.substr(slash + 1);
	}

	static function readString(object:Dynamic, field:String):Null<String> {
		var value:Dynamic = Reflect.field(object, field);
		return Std.isOfType(value, String) ? (value : String) : null;
	}

	static function readInt(object:Dynamic, field:String):Int {
		var value:Dynamic = Reflect.field(object, field);
		return Std.isOfType(value, Int) ? (value : Int) : 0;
	}
}
