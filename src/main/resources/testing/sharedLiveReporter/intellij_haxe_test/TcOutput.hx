package intellij_haxe_test;

#if !macro
/**
	Output plumbing shared by every shipped live reporter: line printing on the
	target's live output channel, TeamCity value escaping, and the hosted-run
	completion sentinel. Compiled by the USER's haxe - stays at the 4.1
	language level.
**/
class TcOutput {
	/**
		Prints one line where the IDE captures it. `leadingBreak` additionally
		starts from a fresh line on sys targets, for runners whose own console
		client leaves an unfinished line - a service message must start at a
		line start to be parsed.
	**/
	public static function printLine(line:String, leadingBreak:Bool = false):Void {
		#if sys
		Sys.print(leadingBreak ? "\n" + line + "\n" : line + "\n");
		#elseif flash
		flash.Lib.trace(line);
		#elseif js
		// console.log reaches node's stdout and the browser console alike;
		// the trace fallback would prefix every line with its own position
		untyped console.log(line);
		#else
		trace(line);
		#end
	}

	// A BROWSER-hosted page has no process exit; the IDE ends the run when
	// this line arrives (node/sys runs exit by themselves, flash through adl).
	public static function announceHostedRunFinished(failed:Bool):Void {
		#if js
		var proc:Dynamic = js.Syntax.code("typeof process !== 'undefined' ? process : null");
		if (proc == null) printLine("##intellij-haxe[testRunFinished exit='" + (failed ? 1 : 0) + "']");
		#end
	}

	// TeamCity value escaping: https://www.jetbrains.com/help/teamcity/service-messages.html
	public static function escape(value:String):String {
		value = StringTools.replace(value, "|", "||");
		value = StringTools.replace(value, "'", "|'");
		value = StringTools.replace(value, "\n", "|n");
		value = StringTools.replace(value, "\r", "|r");
		value = StringTools.replace(value, "[", "|[");
		value = StringTools.replace(value, "]", "|]");
		return value;
	}
}
#end
