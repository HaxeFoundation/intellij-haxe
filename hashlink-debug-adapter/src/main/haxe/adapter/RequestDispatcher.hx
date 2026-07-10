package adapter;

import dap.protocol.Breakpoint;
import dap.protocol.Capabilities;
import dap.protocol.ErrorResponseBody;
import dap.protocol.Event;
import dap.protocol.ProtocolMessage;
import dap.protocol.Request;
import dap.protocol.Response;
import dap.protocol.SetBreakpointsArguments;
import dap.protocol.SetBreakpointsResponseBody;
import dap.protocol.SourceBreakpoint;
import dap.protocol.ThreadsResponseBody;
import haxe.Json;

/**
 * Turns incoming DAP request payloads into outgoing responses/events.
 *
 * Performs no I/O: outgoing messages are handed to the `sink` callback,
 * which in production pushes onto the writer queue and in tests collects
 * into an array. Owns the adapter-side `seq` counter (one monotonic
 * counter shared by responses and events, as the DAP spec requires).
 */
class RequestDispatcher {
	static inline var ERROR_UNRECOGNIZED_COMMAND = 1000;
	static inline var ERROR_INVALID_REQUEST = 1001;

	final sink:ProtocolMessage->Void;
	var nextSeq:Int = 1;
	var nextBreakpointId:Int = 1;

	/** Set after a "disconnect" request has been answered; the owner should shut down. */
	public var shutdownRequested(default, null):Bool = false;

	public function new(sink:ProtocolMessage->Void) {
		this.sink = sink;
	}

	/** Parses one frame payload and dispatches it. Never throws on bad input. */
	public function handleRawPayload(payload:String):Void {
		var parsed:Dynamic;
		try {
			parsed = Json.parse(payload);
		} catch (e:Dynamic) {
			sendErrorResponse(0, "", ERROR_INVALID_REQUEST, "Invalid JSON payload");
			return;
		}
		handleRequest(parsed);
	}

	/** Dispatches an already-parsed message. */
	public function handleRequest(message:Dynamic):Void {
		var seq = readInt(message, "seq");
		var type = readString(message, "type");
		var command = readString(message, "command");
		if (type != "request" || command == null) {
			sendErrorResponse(seq, command == null ? "" : command, ERROR_INVALID_REQUEST, "Not a valid DAP request");
			return;
		}
		var request:Request = message;
		switch (command) {
			case "initialize":
				handleInitialize(request);
			case "setBreakpoints":
				handleSetBreakpoints(request);
			case "configurationDone":
				sendSuccess(request);
			case "launch":
				// milestone 1 stub: accepted, but no debuggee is started yet
				sendSuccess(request);
			case "threads":
				var body:ThreadsResponseBody = {threads: [{id: 1, name: "main"}]};
				sendSuccess(request, body);
			case "disconnect":
				sendSuccess(request);
				shutdownRequested = true;
			default:
				sendErrorResponse(request.seq, command, ERROR_UNRECOGNIZED_COMMAND, "Unrecognized command: " + command);
		}
	}

	function handleInitialize(request:Request):Void {
		var capabilities:Capabilities = {supportsConfigurationDoneRequest: true};
		sendSuccess(request, capabilities);
		// the spec requires the initialized event strictly after the initialize response
		sendEvent("initialized");
	}

	function handleSetBreakpoints(request:Request):Void {
		var args:SetBreakpointsArguments = request.arguments;
		var requested:Array<SourceBreakpoint> = (args != null && args.breakpoints != null) ? args.breakpoints : [];
		var accepted:Array<Breakpoint> = [];
		for (sourceBreakpoint in requested) {
			var breakpoint:Breakpoint = {
				id: nextBreakpointId++,
				verified: true,
				line: sourceBreakpoint.line
			};
			if (args.source != null) {
				breakpoint.source = args.source;
			}
			accepted.push(breakpoint);
		}
		var body:SetBreakpointsResponseBody = {breakpoints: accepted};
		sendSuccess(request, body);
	}

	function sendSuccess(request:Request, ?body:Dynamic):Void {
		var response:Response = {
			seq: nextSeq++,
			type: "response",
			request_seq: request.seq,
			success: true,
			command: request.command
		};
		if (body != null) {
			response.body = body;
		}
		sink(response);
	}

	function sendErrorResponse(requestSeq:Int, command:String, errorId:Int, message:String):Void {
		var body:ErrorResponseBody = {error: {id: errorId, format: message, showUser: false}};
		var response:Response = {
			seq: nextSeq++,
			type: "response",
			request_seq: requestSeq,
			success: false,
			command: command,
			message: message,
			body: body
		};
		sink(response);
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
		sink(event);
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
