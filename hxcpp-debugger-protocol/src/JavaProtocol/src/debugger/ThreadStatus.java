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
public  class ThreadStatus extends haxe.lang.Enum
{
	static 
	{
		debugger.ThreadStatus.constructs = new haxe.root.Array<java.lang.String>(new java.lang.String[]{"Running", "StoppedImmediate", "StoppedBreakpoint", "StoppedUncaughtException", "StoppedCriticalError"});
		debugger.ThreadStatus.Running = new debugger.ThreadStatus(((int) (0) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.ThreadStatus.StoppedImmediate = new debugger.ThreadStatus(((int) (1) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
		debugger.ThreadStatus.StoppedUncaughtException = new debugger.ThreadStatus(((int) (3) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{})) ));
	}
	public    ThreadStatus(int index, haxe.root.Array<java.lang.Object> params)
	{
		super(index, params);
	}
	
	
	public static  haxe.root.Array<java.lang.String> constructs;
	
	public static  debugger.ThreadStatus Running;
	
	public static  debugger.ThreadStatus StoppedImmediate;
	
	public static   debugger.ThreadStatus StoppedBreakpoint(int number)
	{
		return new debugger.ThreadStatus(((int) (2) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{number})) ));
	}
	
	
	public static  debugger.ThreadStatus StoppedUncaughtException;
	
	public static   debugger.ThreadStatus StoppedCriticalError(java.lang.String description)
	{
		return new debugger.ThreadStatus(((int) (4) ), ((haxe.root.Array<java.lang.Object>) (new haxe.root.Array<java.lang.Object>(new java.lang.Object[]{description})) ));
	}
	
	
}


