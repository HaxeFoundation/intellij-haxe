package debug.breakpoints;

import debug.module.JitInfo;
import debug.module.ModuleDebugInfo;

/**
	Resolves requested source breakpoints to installable machine locations plus
	their DAP verification results. Pure planning over the module's debug tables
	and the jit map — installation (and the running-debuggee freeze around it)
	stays with the session.
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
