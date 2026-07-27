package tests.adapter;

import ijhaxe.debug.breakpoints.BreakpointResult;
import ijhaxe.debug.session.FrameInfo;
import ijhaxe.debug.session.SessionCommand;

import ijhaxe.adapter.RequestDispatcher;
import ijhaxe.dap.protocol.Event;
import ijhaxe.dap.protocol.ProtocolMessage;
import ijhaxe.dap.protocol.Response;

class DispatcherTest {
	public static function run(assert:Assert):Void {
		initializeRepliesWithCapabilitiesThenInitializedEvent(assert);
		unknownCommandYieldsErrorResponse(assert);
		invalidJsonYieldsErrorResponse(assert);
		launchIsDeferredUntilSessionCompletes(assert);
		badLaunchArgsFailImmediately(assert);
		setBreakpointsBeforeLaunchAnswersUnverifiedThenReverifies(assert);
		stoppedEventUpdatesThreadsAndStackTraceFlows(assert);
		exitEmitsExitedThenTerminated(assert);
		disconnectWithoutLaunchRespondsImmediately(assert);
	}

	static function makeCollectors():{out:Array<ProtocolMessage>, cmds:Array<SessionCommand>, dispatcher:RequestDispatcher} {
		var out:Array<ProtocolMessage> = [];
		var cmds:Array<SessionCommand> = [];
		var dispatcher = new RequestDispatcher(m -> out.push(m), c -> cmds.push(c));
		return {out: out, cmds: cmds, dispatcher: dispatcher};
	}

	static function request(seq:Int, command:String, ?arguments:Dynamic):Dynamic {
		var result:Dynamic = {seq: seq, type: "request", command: command};
		if (arguments != null) {
			result.arguments = arguments;
		}
		return result;
	}

	static function lastResponse(out:Array<ProtocolMessage>):Response {
		return cast out[out.length - 1];
	}

	static function initializeRepliesWithCapabilitiesThenInitializedEvent(assert:Assert):Void {
		var c = makeCollectors();
		c.dispatcher.handleRequest(request(1, "initialize", {adapterID: "test"}));

		assert.equals(2, c.out.length, "initialize produces response + event");
		var response:Response = cast c.out[0];
		assert.isTrue(response.success, "initialize succeeds");
		assert.isTrue(response.body.supportsConfigurationDoneRequest, "capabilities advertised");
		var event:Event = cast c.out[1];
		assert.equals("initialized", event.event, "initialized event after response");
	}

	static function unknownCommandYieldsErrorResponse(assert:Assert):Void {
		var c = makeCollectors();
		c.dispatcher.handleRequest(request(5, "somethingWeird"));
		var response = lastResponse(c.out);
		assert.isFalse(response.success, "unknown command fails");
		assert.equals("somethingWeird", response.command, "command echoed");
		assert.isTrue(response.message.indexOf("somethingWeird") >= 0, "message names the command");
	}

	static function invalidJsonYieldsErrorResponse(assert:Assert):Void {
		var c = makeCollectors();
		c.dispatcher.handleRawPayload("not json {");
		var response = lastResponse(c.out);
		assert.isFalse(response.success, "invalid json fails");
		assert.equals(0, response.request_seq, "request_seq falls back to 0");
	}

	static function launchIsDeferredUntilSessionCompletes(assert:Assert):Void {
		var c = makeCollectors();
		c.dispatcher.handleRequest(request(2, "launch", {program: "/x/app.hl"}));

		assert.equals(0, c.out.length, "launch produces no immediate response");
		assert.equals(1, c.cmds.length, "launch enqueues a session command");
		assert.isTrue(c.cmds[0].match(CmdLaunch(2, _)), "CmdLaunch with request seq");

		c.dispatcher.handleSessionEvent(EvLaunched(2));
		assert.equals(1, c.out.length, "launch response emitted on completion");
		var response = lastResponse(c.out);
		assert.isTrue(response.success, "launch succeeds");
		assert.equals("launch", response.command, "launch command echoed on deferred response");
		assert.equals(2, response.request_seq, "deferred response echoes request seq");
	}

	static function badLaunchArgsFailImmediately(assert:Assert):Void {
		var c = makeCollectors();
		c.dispatcher.handleRequest(request(3, "launch", {}));
		assert.equals(1, c.out.length, "bad launch answered immediately");
		assert.equals(0, c.cmds.length, "no session command for bad launch");
		assert.isFalse(lastResponse(c.out).success, "bad launch fails");
	}

	static function setBreakpointsBeforeLaunchAnswersUnverifiedThenReverifies(assert:Assert):Void {
		var c = makeCollectors();
		var args = {source: {name: "Main.hx", path: "/p/Main.hx"}, breakpoints: [{line: 14}, {line: 20}]};
		c.dispatcher.handleRequest(request(4, "setBreakpoints", args));

		var response = lastResponse(c.out);
		assert.isTrue(response.success, "pre-launch setBreakpoints succeeds");
		var bps:Array<Dynamic> = response.body.breakpoints;
		assert.equals(2, bps.length, "two breakpoints echoed");
		assert.isFalse(bps[0].verified, "pre-launch breakpoints unverified");
		assert.equals(0, c.cmds.length, "no session command before launch");

		// launch, then the dispatcher replays breakpoints for re-verification
		c.dispatcher.handleRequest(request(5, "launch", {program: "/p/app.hl"}));
		c.dispatcher.handleSessionEvent(EvLaunched(5));
		var reverify = c.cmds.filter(cmd -> cmd.match(CmdSetBreakpoints(_, _, _, _, true)));
		assert.equals(1, reverify.length, "pre-launch breakpoints replayed as reverify");

		// session reports one now verified -> a breakpoint changed event
		var before = c.out.length;
		var result:BreakpointResult = {id: bps[0].id, verified: true, line: 14, sourcePath: "/p/Main.hx"};
		c.dispatcher.handleSessionEvent(EvBreakpointChanged(result));
		var event:Event = cast c.out[c.out.length - 1];
		assert.equals("breakpoint", event.event, "breakpoint changed event emitted");
		assert.equals("changed", event.body.reason, "reason is changed");
		assert.isTrue(event.body.breakpoint.verified, "breakpoint now verified");
		assert.isTrue(c.out.length > before, "an event was produced");
	}

	static function stoppedEventUpdatesThreadsAndStackTraceFlows(assert:Assert):Void {
		var c = makeCollectors();
		c.dispatcher.handleRequest(request(1, "launch", {program: "/p/app.hl"}));
		c.dispatcher.handleSessionEvent(EvLaunched(1));

		c.dispatcher.handleSessionEvent(EvStoppedBreakpoint(77, [1]));
		var stopped:Event = cast c.out[c.out.length - 1];
		assert.equals("stopped", stopped.event, "stopped event emitted");
		assert.equals(77, stopped.body.threadId, "stopped thread id");

		// threads is deferred to the session (it must read the runtime registry)
		c.dispatcher.handleRequest(request(2, "threads"));
		assert.isTrue(c.cmds[c.cmds.length - 1].match(CmdThreads(2)), "threads deferred to session");
		c.dispatcher.handleSessionEvent(EvThreads(2, [{id: 77, name: "main"}]));
		var threadsResponse = lastResponse(c.out);
		assert.equals(77, threadsResponse.body.threads[0].id, "threads reflects the session's list");

		// stackTrace is deferred to the session
		var before = c.cmds.length;
		c.dispatcher.handleRequest(request(3, "stackTrace", {threadId: 77}));
		assert.isTrue(c.cmds[c.cmds.length - 1].match(CmdStackTrace(3, 77)), "stackTrace deferred to session");

		var frames:Array<FrameInfo> = [{id: 0, name: "Main.add", file: "src/Main.hx", line: 20}];
		c.dispatcher.handleSessionEvent(EvStackTrace(3, frames));
		var stackResponse = lastResponse(c.out);
		assert.isTrue(stackResponse.success, "stackTrace succeeds");
		assert.equals("Main.add", stackResponse.body.stackFrames[0].name, "frame name");
		assert.equals(20, stackResponse.body.stackFrames[0].line, "frame line");
	}

	static function exitEmitsExitedThenTerminated(assert:Assert):Void {
		var c = makeCollectors();
		c.dispatcher.handleSessionEvent(EvExited(0));
		assert.equals(2, c.out.length, "exit produces two events");
		var exited:Event = cast c.out[0];
		var terminated:Event = cast c.out[1];
		assert.equals("exited", exited.event, "exited first");
		assert.equals(0, exited.body.exitCode, "exit code forwarded");
		assert.equals("terminated", terminated.event, "terminated second");
	}

	static function disconnectWithoutLaunchRespondsImmediately(assert:Assert):Void {
		var c = makeCollectors();
		c.dispatcher.handleRequest(request(9, "disconnect"));
		var response = lastResponse(c.out);
		assert.isTrue(response.success, "disconnect succeeds");
		assert.isTrue(c.dispatcher.shutdownRequested, "shutdown requested");
		assert.equals(0, c.cmds.length, "no session command without a launch");
	}
}
