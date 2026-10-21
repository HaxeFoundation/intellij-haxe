package ijhaxe.hxcpp.debug;

#if cpp
import cpp.vm.Debugger;
import ijhaxe.hxcpp.debug.DebuggerApi;

/**
	The real DebuggerApi over `cpp.vm.Debugger`. It is a thin wrapper: the
	data types are aliases of the std classes, so runtime results pass
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
				// Runs on the thread the event concerns. A stop's STATUS is read
				// here, where the thread is certain to be stopped (safe=false),
				// and not later from the server thread: a read from another
				// thread races the thread's state and returns RUNNING. The
				// handler then hands the event over and returns; hxcpp blocks a
				// stopped thread in DoBreak until continueThreads.
				if (event == Debugger.THREAD_CREATED) {
					// Runs ON the newly attached thread. hxcpp debugs a thread only
					// after it opts in, and only a thread can opt ITSELF in.
					// Without this, code on threads spawned after startup verifies
					// breakpoints but never hits them; nme runs its whole
					// application loop on such a thread, and user worker threads
					// are affected too. The server thread never reaches this
					// branch: it attaches before registering this handler and
					// excludes itself right after.
					Debugger.enableCurrentThreadDebugging(true);
					handler(ThreadCreated(threadNumber));
				} else if (event == Debugger.THREAD_TERMINATED) {
					handler(ThreadTerminated(threadNumber));
				} else if (event == Debugger.THREAD_STARTED) {
					handler(ThreadStarted(threadNumber));
				} else if (event == Debugger.THREAD_STOPPED) {
					// Capture the status, the hit breakpoint and the stack while
					// the thread is stopped. The captured stack has THIS handler's
					// own frames on top (getThreadInfo, the closure). Everything
					// above the reported stop location, the innermost user frame,
					// is trimmed, leaving only user frames (innermost last).
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
