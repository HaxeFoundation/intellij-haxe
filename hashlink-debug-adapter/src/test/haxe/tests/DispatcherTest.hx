package tests;

import adapter.RequestDispatcher;
import dap.protocol.Event;
import dap.protocol.ProtocolMessage;
import dap.protocol.Response;

class DispatcherTest {
	public static function run(assert:Assert):Void {
		initializeRepliesWithCapabilitiesThenInitializedEvent(assert);
		setBreakpointsVerifiesEveryRequestedBreakpoint(assert);
		configurationDoneAndLaunchAreAcknowledged(assert);
		threadsReturnsStubMainThread(assert);
		unknownCommandYieldsErrorResponse(assert);
		invalidJsonYieldsErrorResponse(assert);
		nonRequestMessageYieldsErrorResponse(assert);
		disconnectAcknowledgesAndRequestsShutdown(assert);
		outgoingSeqIsMonotonicAcrossResponsesAndEvents(assert);
	}

	static function collectInto(out:Array<ProtocolMessage>):RequestDispatcher {
		return new RequestDispatcher(message -> out.push(message));
	}

	static function request(seq:Int, command:String, ?arguments:Dynamic):Dynamic {
		var result:Dynamic = {seq: seq, type: "request", command: command};
		if (arguments != null) {
			result.arguments = arguments;
		}
		return result;
	}

	static function initializeRepliesWithCapabilitiesThenInitializedEvent(assert:Assert):Void {
		var out:Array<ProtocolMessage> = [];
		collectInto(out).handleRequest(request(1, "initialize", {adapterID: "test"}));

		assert.equals(2, out.length, "initialize produces response + event");
		var response:Response = cast out[0];
		assert.equals("response", response.type, "first message is the response");
		assert.equals(1, response.request_seq, "response echoes request seq");
		assert.equals("initialize", response.command, "response echoes command");
		assert.isTrue(response.success, "initialize succeeds");
		assert.isTrue(response.body.supportsConfigurationDoneRequest, "capabilities advertised");

		var event:Event = cast out[1];
		assert.equals("event", event.type, "second message is an event");
		assert.equals("initialized", event.event, "initialized event after response");
	}

	static function setBreakpointsVerifiesEveryRequestedBreakpoint(assert:Assert):Void {
		var out:Array<ProtocolMessage> = [];
		var arguments = {
			source: {name: "Main.hx", path: "/project/src/Main.hx"},
			breakpoints: [{line: 10}, {line: 20}, {line: 30}]
		};
		collectInto(out).handleRequest(request(7, "setBreakpoints", arguments));

		assert.equals(1, out.length, "setBreakpoints produces one response");
		var response:Response = cast out[0];
		assert.isTrue(response.success, "setBreakpoints succeeds");
		var breakpoints:Array<Dynamic> = response.body.breakpoints;
		assert.equals(3, breakpoints.length, "one breakpoint per requested breakpoint");
		assert.equals(10, breakpoints[0].line, "first line echoed");
		assert.equals(30, breakpoints[2].line, "last line echoed");
		assert.isTrue(breakpoints[0].verified, "breakpoints are verified");
		assert.isTrue(breakpoints[0].id != breakpoints[1].id, "breakpoint ids are unique");
		assert.equals("/project/src/Main.hx", breakpoints[0].source.path, "source echoed");
	}

	static function configurationDoneAndLaunchAreAcknowledged(assert:Assert):Void {
		for (command in ["configurationDone", "launch"]) {
			var out:Array<ProtocolMessage> = [];
			collectInto(out).handleRequest(request(3, command));

			assert.equals(1, out.length, command + " produces one response");
			var response:Response = cast out[0];
			assert.isTrue(response.success, command + " succeeds");
			assert.isFalse(Reflect.hasField(response, "body"), command + " response has no body");
		}
	}

	static function threadsReturnsStubMainThread(assert:Assert):Void {
		var out:Array<ProtocolMessage> = [];
		collectInto(out).handleRequest(request(4, "threads"));

		var response:Response = cast out[0];
		assert.isTrue(response.success, "threads succeeds");
		var threads:Array<Dynamic> = response.body.threads;
		assert.equals(1, threads.length, "one stub thread");
		assert.equals(1, threads[0].id, "stub thread id");
		assert.equals("main", threads[0].name, "stub thread name");
	}

	static function unknownCommandYieldsErrorResponse(assert:Assert):Void {
		var out:Array<ProtocolMessage> = [];
		collectInto(out).handleRequest(request(5, "somethingWeird"));

		var response:Response = cast out[0];
		assert.isFalse(response.success, "unknown command fails");
		assert.equals("somethingWeird", response.command, "command echoed");
		assert.equals(5, response.request_seq, "request seq echoed");
		assert.isTrue(response.message.indexOf("somethingWeird") >= 0, "message names the command");
		assert.equals(1000, response.body.error.id, "error id set");
	}

	static function invalidJsonYieldsErrorResponse(assert:Assert):Void {
		var out:Array<ProtocolMessage> = [];
		collectInto(out).handleRawPayload("this is not json {");

		assert.equals(1, out.length, "invalid json produces one response");
		var response:Response = cast out[0];
		assert.isFalse(response.success, "invalid json fails");
		assert.equals(0, response.request_seq, "request_seq falls back to 0");
	}

	static function nonRequestMessageYieldsErrorResponse(assert:Assert):Void {
		var out:Array<ProtocolMessage> = [];
		collectInto(out).handleRequest({seq: 9, type: "event", event: "stopped"});

		var response:Response = cast out[0];
		assert.isFalse(response.success, "non-request message fails");
		assert.equals(9, response.request_seq, "request_seq taken from message");
	}

	static function disconnectAcknowledgesAndRequestsShutdown(assert:Assert):Void {
		var out:Array<ProtocolMessage> = [];
		var dispatcher = collectInto(out);
		assert.isFalse(dispatcher.shutdownRequested, "no shutdown before disconnect");

		dispatcher.handleRequest(request(6, "disconnect"));

		var response:Response = cast out[0];
		assert.isTrue(response.success, "disconnect succeeds");
		assert.isTrue(dispatcher.shutdownRequested, "shutdown requested after disconnect");
	}

	static function outgoingSeqIsMonotonicAcrossResponsesAndEvents(assert:Assert):Void {
		var out:Array<ProtocolMessage> = [];
		var dispatcher = collectInto(out);
		dispatcher.handleRequest(request(1, "initialize", {adapterID: "test"}));
		dispatcher.handleRequest(request(2, "configurationDone"));
		dispatcher.handleRequest(request(3, "threads"));

		assert.equals(4, out.length, "three requests produced four messages");
		for (i in 0...out.length) {
			assert.equals(i + 1, out[i].seq, "outgoing seq " + i + " is monotonic from 1");
		}
	}
}
