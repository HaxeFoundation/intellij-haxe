/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package debugger;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class Message extends haxe.lang.Enum
{
	static 
	{
		debugger.Message.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"ErrorInternal", "ErrorNoSuchThread", "ErrorNoSuchFile", "ErrorNoSuchBreakpoint", "ErrorBadClassNameRegex", "ErrorBadFunctionNameRegex", "ErrorNoMatchingFunctions", "ErrorBadCount", "ErrorCurrentThreadNotStopped", "ErrorEvaluatingExpression", "OK", "Exited", "Detached", "Files", "AllClasses", "Classes", "MemBytes", "Compacted", "Collected", "ThreadLocation", "FileLineBreakpointNumber", "ClassFunctionBreakpointNumber", "Breakpoints", "BreakpointDescription", "BreakpointStatuses", "ThreadsWhere", "Variables", "Value", "Structured", "ThreadCreated", "ThreadTerminated", "ThreadStarted", "ThreadStopped"});
		debugger.Message.OK = new debugger.Message(((int) (10) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Message.Exited = new debugger.Message(((int) (11) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.Message.Detached = new debugger.Message(((int) (12) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    Message(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static   debugger.Message ErrorInternal(java.lang.String details)
	{
		return new debugger.Message(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{details})) ));
	}
	
	
	public static   debugger.Message ErrorNoSuchThread(int number)
	{
		return new debugger.Message(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static   debugger.Message ErrorNoSuchFile(java.lang.String fileName)
	{
		return new debugger.Message(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{fileName})) ));
	}
	
	
	public static   debugger.Message ErrorNoSuchBreakpoint(int number)
	{
		return new debugger.Message(((int) (3) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static   debugger.Message ErrorBadClassNameRegex(java.lang.String details)
	{
		return new debugger.Message(((int) (4) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{details})) ));
	}
	
	
	public static   debugger.Message ErrorBadFunctionNameRegex(java.lang.String details)
	{
		return new debugger.Message(((int) (5) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{details})) ));
	}
	
	
	public static   debugger.Message ErrorNoMatchingFunctions(java.lang.String className, java.lang.String functionName, debugger.StringList unresolvableClasses)
	{
		return new debugger.Message(((int) (6) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{className, functionName, unresolvableClasses})) ));
	}
	
	
	public static   debugger.Message ErrorBadCount(int count)
	{
		return new debugger.Message(((int) (7) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{count})) ));
	}
	
	
	public static   debugger.Message ErrorCurrentThreadNotStopped(int threadNumber)
	{
		return new debugger.Message(((int) (8) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{threadNumber})) ));
	}
	
	
	public static   debugger.Message ErrorEvaluatingExpression(java.lang.String details)
	{
		return new debugger.Message(((int) (9) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{details})) ));
	}
	
	
	public static  debugger.Message OK;
	
	public static  debugger.Message Exited;
	
	public static  debugger.Message Detached;
	
	public static   debugger.Message Files(debugger.StringList list)
	{
		return new debugger.Message(((int) (13) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{list})) ));
	}
	
	
	public static   debugger.Message AllClasses(debugger.StringList list)
	{
		return new debugger.Message(((int) (14) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{list})) ));
	}
	
	
	public static   debugger.Message Classes(debugger.ClassList list)
	{
		return new debugger.Message(((int) (15) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{list})) ));
	}
	
	
	public static   debugger.Message MemBytes(int bytes)
	{
		return new debugger.Message(((int) (16) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{bytes})) ));
	}
	
	
	public static   debugger.Message Compacted(int bytesBefore, int bytesAfter)
	{
		return new debugger.Message(((int) (17) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{bytesBefore, bytesAfter})) ));
	}
	
	
	public static   debugger.Message Collected(int bytesBefore, int bytesAfter)
	{
		return new debugger.Message(((int) (18) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{bytesBefore, bytesAfter})) ));
	}
	
	
	public static   debugger.Message ThreadLocation(int number, int stackFrame, java.lang.String className, java.lang.String functionName, java.lang.String fileName, int lineNumber)
	{
		return new debugger.Message(((int) (19) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, stackFrame, className, functionName, fileName, lineNumber})) ));
	}
	
	
	public static   debugger.Message FileLineBreakpointNumber(int number)
	{
		return new debugger.Message(((int) (20) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static   debugger.Message ClassFunctionBreakpointNumber(int number, debugger.StringList unresolvableClasses)
	{
		return new debugger.Message(((int) (21) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, unresolvableClasses})) ));
	}
	
	
	public static   debugger.Message Breakpoints(debugger.BreakpointList list)
	{
		return new debugger.Message(((int) (22) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{list})) ));
	}
	
	
	public static   debugger.Message BreakpointDescription(int number, debugger.BreakpointLocationList list)
	{
		return new debugger.Message(((int) (23) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, list})) ));
	}
	
	
	public static   debugger.Message BreakpointStatuses(debugger.BreakpointStatusList list)
	{
		return new debugger.Message(((int) (24) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{list})) ));
	}
	
	
	public static   debugger.Message ThreadsWhere(debugger.ThreadWhereList list)
	{
		return new debugger.Message(((int) (25) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{list})) ));
	}
	
	
	public static   debugger.Message Variables(debugger.StringList list)
	{
		return new debugger.Message(((int) (26) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{list})) ));
	}
	
	
	public static   debugger.Message Value(java.lang.String expression, java.lang.String type, java.lang.String value)
	{
		return new debugger.Message(((int) (27) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{expression, type, value})) ));
	}
	
	
	public static   debugger.Message Structured(debugger.StructuredValue structuredValue)
	{
		return new debugger.Message(((int) (28) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{structuredValue})) ));
	}
	
	
	public static   debugger.Message ThreadCreated(int number)
	{
		return new debugger.Message(((int) (29) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static   debugger.Message ThreadTerminated(int number)
	{
		return new debugger.Message(((int) (30) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static   debugger.Message ThreadStarted(int number)
	{
		return new debugger.Message(((int) (31) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static   debugger.Message ThreadStopped(int number, int stackFrame, java.lang.String className, java.lang.String functionName, java.lang.String fileName, int lineNumber)
	{
		return new debugger.Message(((int) (32) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number, stackFrame, className, functionName, fileName, lineNumber})) ));
	}
	
	
}


