package tests;

import ijhaxe.hxcpp.debug.DebuggerApi;

/**
	Scriptable DebuggerApi for the interpreter-run tests: canned thread lists,
	recorded calls, and manual event firing through the captured handler.
**/
class FakeDebuggerApi implements DebuggerApi {
	public var cannedThreads:Array<DebugThread> = [];
	public var continueCalls:Array<{threadNumber:Int, count:Int}> = [];
	public var excludedCurrentThread:Bool = false;
	public var handler:Null<DebugEvent->Void> = null;

	public function new() {}

	public function excludeCurrentThread():Void {
		excludedCurrentThread = true;
	}

	public var enabledCurrentThread:Bool = false;

	public function enableCurrentThread():Void {
		enabledCurrentThread = true;
	}

	public function setEventHandler(handler:DebugEvent->Void):Void {
		this.handler = handler;
	}

	public function threads():Array<DebugThread> {
		return cannedThreads;
	}

	public function continueThreads(threadNumber:Int, count:Int):Void {
		continueCalls.push({threadNumber: threadNumber, count: count});
	}

	public var breakNowCalls:Int = 0;

	public function breakNow(wait:Bool):Void {
		breakNowCalls++;
	}

	public var stepCalls:Array<{threadNumber:Int, stepType:Int}> = [];

	public function stepThread(threadNumber:Int, stepType:Int):Void {
		stepCalls.push({threadNumber: threadNumber, stepType: stepType});
	}

	// breakpoint engine: canned file tables, recorded installs, monotonic numbers
	public var cannedFiles:Array<String> = [];
	public var cannedFilesFullPath:Array<String> = [];
	public var installedBreakpoints:Array<{file:String, line:Int, number:Int}> = [];
	public var deletedBreakpoints:Array<Int> = [];
	var nextBreakpointNumber:Int = 100;

	public function files():Array<String> {
		return cannedFiles;
	}

	public function filesFullPath():Array<String> {
		return cannedFilesFullPath;
	}

	public function addFileLineBreakpoint(file:String, line:Int):Int {
		var number = nextBreakpointNumber++;
		installedBreakpoints.push({file: file, line: line, number: number});
		return number;
	}

	public var installedFunctionBreakpoints:Array<{className:String, functionName:String, number:Int}> = [];
	// when set, class names NOT in this list are rejected with -1, mirroring
	// hxcpp's Add() which validates against the compiled-in class table
	public var knownClasses:Null<Array<String>> = null;

	public function addClassFunctionBreakpoint(className:String, functionName:String):Int {
		if (knownClasses != null && knownClasses.indexOf(className) < 0) {
			return -1;
		}
		var number = nextBreakpointNumber++;
		installedFunctionBreakpoints.push({className: className, functionName: functionName, number: number});
		return number;
	}

	public function deleteBreakpoint(number:Int):Void {
		deletedBreakpoints.push(number);
	}

	// stack variables: an ordered name list + values for a single test frame
	public var localNames:Array<String> = [];
	public var localValues:Map<String, Dynamic> = new Map();
	public var setVarCalls:Array<{thread:Int, frame:Int, name:String, value:Dynamic}> = [];
	// names whose value READ throws — simulates a corrupt frame slot, which
	// hxcpp re-raises on the debug thread ("Critical Error in the debugger
	// thread")
	public var corruptLocals:Array<String> = [];

	public function stackVariables(threadNumber:Int, frame:Int):Array<String> {
		return localNames;
	}

	public function stackVariableValue(threadNumber:Int, frame:Int, name:String):Dynamic {
		if (corruptLocals.indexOf(name) >= 0) {
			throw "Critical Error in the debugger thread";
		}
		return localValues.get(name);
	}

	public function setStackVariableValue(threadNumber:Int, frame:Int, name:String, value:Dynamic):Dynamic {
		setVarCalls.push({thread: threadNumber, frame: frame, name: name, value: value});
		localValues.set(name, value);
		return value;
	}
}
