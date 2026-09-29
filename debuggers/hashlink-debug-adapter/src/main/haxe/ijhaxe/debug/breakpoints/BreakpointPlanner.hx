package ijhaxe.debug.breakpoints;

import ijhaxe.debug.module.JitInfo;
import ijhaxe.debug.module.ModuleDebugInfo;

/**
	Resolves requested source breakpoints to machine locations to install, and
	to the verification results reported to the client. It only reads the
	module's debug tables and the jit map; installing, and pausing a running
	debuggee for it, is left to the session.
**/
class BreakpointPlanner {
	public static function plan(module:ModuleDebugInfo, jit:JitInfo, sourcePath:String,
			requested:Array<RequestedBreakpoint>):{locations:Array<BreakpointLocation>, results:Array<BreakpointResult>} {
		var locations:Array<BreakpointLocation> = [];
		var results:Array<BreakpointResult> = [];

		for (request in requested) {
			var resolved = module.resolveLine(sourcePath, request.line);
			if (resolved.length == 0) {
				results.push(unresolved(request, sourcePath));
			} else {
				for (location in resolved) {
					locations.push({
						id: request.id,
						address: jit.addressOf(location.fidx, location.op),
						fidx: location.fidx, op: location.op, file: sourcePath, line: location.line,
						condition: request.condition
					});
				}
				results.push({id: request.id, verified: true, line: resolved[0].line, sourcePath: sourcePath});
			}
		}
		return {locations: locations, results: results};
	}

	public static function unresolved(request:RequestedBreakpoint, sourcePath:String):BreakpointResult {
		return {id: request.id, verified: false, line: request.line, message: "no executable code at this line (stale build?)", sourcePath: sourcePath};
	}
}
