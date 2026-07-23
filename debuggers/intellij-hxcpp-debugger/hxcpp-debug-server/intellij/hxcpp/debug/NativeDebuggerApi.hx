package intellij.hxcpp.debug;

#if cpp
import cpp.vm.Debugger;
import intellij.hxcpp.debug.DebuggerApi;

/**
	The real DebuggerApi over `cpp.vm.Debugger`. Thin by design: the data
	types are typedef-aliased to the std classes, so runtime results pass
	through untouched.
**/
class NativeDebuggerApi implements DebuggerApi {
	public function new() {}

	public function excludeCurrentThread():Void {
		Debugger.enableCurrentThreadDebugging(false);
	}

	public function enableCurrentThread():Void {
		Debugger.enableCurrentThreadDebugging(true);
	}

	public function setEventHandler(handler:DebugEvent->Void):Void {
		Debugger.setEventNotificationHandler(
			(threadNumber:Int, event:Int, stackFrame:Int, className:String, functionName:String, fileName:String, lineNumber:Int) -> {
				// Runs on the STOPPING thread. The stop STATUS is read here, where
				// the thread is guaranteed stopped (safe=false), not later from the
				// server thread — a cross-thread read races the thread's state and
				// returns RUNNING. Then hand off and return; hxcpp blocks the
				// thread in DoBreak until continueThreads.
				if (event == Debugger.THREAD_CREATED) {
					handler(ThreadCreated(threadNumber));
				} else if (event == Debugger.THREAD_TERMINATED) {
					handler(ThreadTerminated(threadNumber));
				} else if (event == Debugger.THREAD_STARTED) {
					handler(ThreadStarted(threadNumber));
				} else if (event == Debugger.THREAD_STOPPED) {
					// Capture the thread here (safe read on the stopped thread):
					// status, hit breakpoint, and the stack. The captured stack has
					// THIS handler's own frames on top (getThreadInfo, the closure);
					// trim everything above the reported stop — the innermost user
					// frame — leaving a clean user stack (innermost last).
					var info = Debugger.getThreadInfo(threadNumber, false);
					if (info != null) {
						var stack = info.stack;
						var end = stack.length;
						for (i in 0...stack.length) {
							var f = stack[i];
							if (f.fileName == fileName && f.lineNumber == lineNumber && f.functionName == functionName) {
								end = i + 1; // last match = the innermost user frame
							}
						}
						handler(ThreadStopped(threadNumber, info.status, info.breakpoint, stack.slice(0, end), info.criticalErrorDescription));
					}
				}
			});
	}

	public function threads():Array<DebugThread> {
		return Debugger.getThreadInfos();
	}

public function continueThreads(threadNumber:Int, count:Int):Void {
		Debugger.continueThreads(threadNumber, count);
	}

	public function breakNow(wait:Bool):Void {
		Debugger.breakNow(wait);
	}

	public function stepThread(threadNumber:Int, stepType:Int):Void {
		Debugger.stepThread(threadNumber, stepType, 1);
	}

	public function files():Array<String> {
		return Debugger.getFiles();
	}

	public function filesFullPath():Array<String> {
		return Debugger.getFilesFullPath();
	}

	public function addFileLineBreakpoint(file:String, line:Int):Int {
		return Debugger.addFileLineBreakpoint(file, line);
	}

	public function addClassFunctionBreakpoint(className:String, functionName:String):Int {
		return Debugger.addClassFunctionBreakpoint(className, functionName);
	}

	public function deleteBreakpoint(number:Int):Void {
		Debugger.deleteBreakpoint(number);
	}

	public function stackVariables(threadNumber:Int, frame:Int):Array<String> {
		return Debugger.getStackVariables(threadNumber, frame, false);
	}

	public function stackVariableValue(threadNumber:Int, frame:Int, name:String):Dynamic {
		return Debugger.getStackVariableValue(threadNumber, frame, name, false);
	}

	public function setStackVariableValue(threadNumber:Int, frame:Int, name:String, value:Dynamic):Dynamic {
		return Debugger.setStackVariableValue(threadNumber, frame, name, value, false);
	}
}
#end
