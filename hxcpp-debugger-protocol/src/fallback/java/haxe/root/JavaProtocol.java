// Generated by Haxe 4.3.0
package haxe.root;

import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public class JavaProtocol extends haxe.lang.HxObject
{
	public static void main(String[] args)
	{
		haxe.java.Init.init();
		haxe.root.JavaProtocol.main();
	}
	
	static
	{
		//line 24 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdErrorInternal = 0;
		//line 25 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdErrorNoSuchThread = 1;
		//line 26 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdErrorNoSuchFile = 2;
		//line 27 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdErrorNoSuchBreakpoint = 3;
		//line 28 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdErrorBadClassNameRegex = 4;
		//line 29 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdErrorBadFunctionNameRegex = 5;
		//line 30 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdErrorNoMatchingFunctions = 6;
		//line 31 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdErrorBadCount = 7;
		//line 32 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdErrorCurrentThreadNotStopped = 8;
		//line 33 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdErrorEvaluatingExpression = 9;
		//line 34 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdOK = 10;
		//line 35 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdExited = 11;
		//line 36 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdDetached = 12;
		//line 37 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdFiles = 13;
		//line 38 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdAllClasses = 14;
		//line 39 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdClasses = 15;
		//line 40 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdMemBytes = 16;
		//line 41 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdCompacted = 17;
		//line 42 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdCollected = 18;
		//line 43 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdThreadLocation = 19;
		//line 44 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdFileLineBreakpointNumber = 20;
		//line 45 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdClassFunctionBreakpointNumber = 21;
		//line 46 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdBreakpoints = 22;
		//line 47 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdBreakpointDescription = 23;
		//line 48 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdBreakpointStatuses = 24;
		//line 49 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdThreadsWhere = 25;
		//line 50 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdVariables = 26;
		//line 51 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdValue = 27;
		//line 52 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdStructured = 28;
		//line 53 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdThreadCreated = 29;
		//line 54 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdThreadTerminated = 30;
		//line 55 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdThreadStarted = 31;
		//line 56 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.IdThreadStopped = 32;
	}
	
	public JavaProtocol(haxe.lang.EmptyObject empty)
	{
	}
	
	
	public JavaProtocol()
	{
		//line 20 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.JavaProtocol.__hx_ctor__JavaProtocol(this);
	}
	
	
	protected static void __hx_ctor__JavaProtocol(haxe.root.JavaProtocol __hx_this)
	{
	}
	
	
	public static int IdErrorInternal;
	
	public static int IdErrorNoSuchThread;
	
	public static int IdErrorNoSuchFile;
	
	public static int IdErrorNoSuchBreakpoint;
	
	public static int IdErrorBadClassNameRegex;
	
	public static int IdErrorBadFunctionNameRegex;
	
	public static int IdErrorNoMatchingFunctions;
	
	public static int IdErrorBadCount;
	
	public static int IdErrorCurrentThreadNotStopped;
	
	public static int IdErrorEvaluatingExpression;
	
	public static int IdOK;
	
	public static int IdExited;
	
	public static int IdDetached;
	
	public static int IdFiles;
	
	public static int IdAllClasses;
	
	public static int IdClasses;
	
	public static int IdMemBytes;
	
	public static int IdCompacted;
	
	public static int IdCollected;
	
	public static int IdThreadLocation;
	
	public static int IdFileLineBreakpointNumber;
	
	public static int IdClassFunctionBreakpointNumber;
	
	public static int IdBreakpoints;
	
	public static int IdBreakpointDescription;
	
	public static int IdBreakpointStatuses;
	
	public static int IdThreadsWhere;
	
	public static int IdVariables;
	
	public static int IdValue;
	
	public static int IdStructured;
	
	public static int IdThreadCreated;
	
	public static int IdThreadTerminated;
	
	public static int IdThreadStarted;
	
	public static int IdThreadStopped;
	
	public static void writeServerIdentification(java.io.OutputStream output)
	{
		//line 60 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		debugger.HaxeProtocol.writeServerIdentification(new _JavaProtocol.OutputAdapter(((java.io.OutputStream) (output) )));
	}
	
	
	public static void readClientIdentification(java.io.InputStream input)
	{
		//line 65 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		debugger.HaxeProtocol.readClientIdentification(new _JavaProtocol.InputAdapter(((java.io.InputStream) (input) )));
	}
	
	
	public static void writeCommand(java.io.OutputStream output, debugger.Command command)
	{
		//line 71 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		debugger.HaxeProtocol.writeCommand(new _JavaProtocol.OutputAdapter(((java.io.OutputStream) (output) )), command);
	}
	
	
	public static debugger.Message readMessage(java.io.InputStream input)
	{
		//line 76 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		return debugger.HaxeProtocol.readMessage(new _JavaProtocol.InputAdapter(((java.io.InputStream) (input) )));
	}
	
	
	public static int getMessageId(debugger.Message message)
	{
		//line 81 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		switch (message.index)
		{
			case 0:
			{
				//line 82 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String details = haxe.lang.Runtime.toString(message.params[0]);
				//line 83 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdErrorInternal;
			}
			
			
			case 1:
			{
				//line 84 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int number = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 85 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdErrorNoSuchThread;
			}
			
			
			case 2:
			{
				//line 86 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String fileName = haxe.lang.Runtime.toString(message.params[0]);
				//line 87 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdErrorNoSuchFile;
			}
			
			
			case 3:
			{
				//line 88 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int number1 = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 89 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdErrorNoSuchBreakpoint;
			}
			
			
			case 4:
			{
				//line 90 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String details1 = haxe.lang.Runtime.toString(message.params[0]);
				//line 91 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdErrorBadClassNameRegex;
			}
			
			
			case 5:
			{
				//line 92 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String details2 = haxe.lang.Runtime.toString(message.params[0]);
				//line 93 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdErrorBadFunctionNameRegex;
			}
			
			
			case 6:
			{
				//line 94 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String className = haxe.lang.Runtime.toString(message.params[0]);
				//line 94 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String f = haxe.lang.Runtime.toString(message.params[1]);
				//line 94 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.StringList u = ((debugger.StringList) (message.params[2]) );
				//line 95 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdErrorNoMatchingFunctions;
			}
			
			
			case 7:
			{
				//line 96 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int count = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 97 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdErrorBadCount;
			}
			
			
			case 8:
			{
				//line 98 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int threadNumber = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 99 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdErrorCurrentThreadNotStopped;
			}
			
			
			case 9:
			{
				//line 100 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String details3 = haxe.lang.Runtime.toString(message.params[0]);
				//line 101 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdErrorEvaluatingExpression;
			}
			
			
			case 10:
			{
				//line 103 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdOK;
			}
			
			
			case 11:
			{
				//line 105 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdExited;
			}
			
			
			case 12:
			{
				//line 107 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdDetached;
			}
			
			
			case 13:
			{
				//line 108 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.StringList list = ((debugger.StringList) (message.params[0]) );
				//line 109 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdFiles;
			}
			
			
			case 14:
			{
				//line 110 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.StringList list1 = ((debugger.StringList) (message.params[0]) );
				//line 111 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdAllClasses;
			}
			
			
			case 15:
			{
				//line 112 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.ClassList list2 = ((debugger.ClassList) (message.params[0]) );
				//line 113 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdClasses;
			}
			
			
			case 16:
			{
				//line 114 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int bytes = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 115 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdMemBytes;
			}
			
			
			case 17:
			{
				//line 116 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int bytesBefore = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 116 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int a = ((int) (haxe.lang.Runtime.toInt(message.params[1])) );
				//line 117 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdCompacted;
			}
			
			
			case 18:
			{
				//line 118 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int bytesBefore1 = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 118 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int a1 = ((int) (haxe.lang.Runtime.toInt(message.params[1])) );
				//line 119 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdCollected;
			}
			
			
			case 19:
			{
				//line 120 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int number2 = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 120 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int s = ((int) (haxe.lang.Runtime.toInt(message.params[1])) );
				//line 120 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String c = haxe.lang.Runtime.toString(message.params[2]);
				//line 120 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String f1 = haxe.lang.Runtime.toString(message.params[3]);
				//line 120 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String fi = haxe.lang.Runtime.toString(message.params[4]);
				//line 120 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int l = ((int) (haxe.lang.Runtime.toInt(message.params[5])) );
				//line 121 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdThreadLocation;
			}
			
			
			case 20:
			{
				//line 122 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int number3 = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 123 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdFileLineBreakpointNumber;
			}
			
			
			case 21:
			{
				//line 124 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int number4 = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 124 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.StringList u1 = ((debugger.StringList) (message.params[1]) );
				//line 125 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdClassFunctionBreakpointNumber;
			}
			
			
			case 22:
			{
				//line 126 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.BreakpointList list3 = ((debugger.BreakpointList) (message.params[0]) );
				//line 127 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdBreakpoints;
			}
			
			
			case 23:
			{
				//line 128 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int number5 = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 128 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.BreakpointLocationList l1 = ((debugger.BreakpointLocationList) (message.params[1]) );
				//line 129 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdBreakpointDescription;
			}
			
			
			case 24:
			{
				//line 130 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.BreakpointStatusList list4 = ((debugger.BreakpointStatusList) (message.params[0]) );
				//line 131 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdBreakpointStatuses;
			}
			
			
			case 25:
			{
				//line 132 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.ThreadWhereList list5 = ((debugger.ThreadWhereList) (message.params[0]) );
				//line 133 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdThreadsWhere;
			}
			
			
			case 26:
			{
				//line 134 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.StringList list6 = ((debugger.StringList) (message.params[0]) );
				//line 135 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdVariables;
			}
			
			
			case 27:
			{
				//line 138 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String expression = haxe.lang.Runtime.toString(message.params[0]);
				//line 138 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String t = haxe.lang.Runtime.toString(message.params[1]);
				//line 138 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String v = haxe.lang.Runtime.toString(message.params[2]);
				//line 139 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdValue;
			}
			
			
			case 28:
			{
				//line 136 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				debugger.StructuredValue structuredValue = ((debugger.StructuredValue) (message.params[0]) );
				//line 137 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdStructured;
			}
			
			
			case 29:
			{
				//line 140 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int number6 = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 141 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdThreadCreated;
			}
			
			
			case 30:
			{
				//line 142 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int number7 = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 143 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdThreadTerminated;
			}
			
			
			case 31:
			{
				//line 144 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int number8 = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 145 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdThreadStarted;
			}
			
			
			case 32:
			{
				//line 146 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int number9 = ((int) (haxe.lang.Runtime.toInt(message.params[0])) );
				//line 146 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int s1 = ((int) (haxe.lang.Runtime.toInt(message.params[1])) );
				//line 146 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String c1 = haxe.lang.Runtime.toString(message.params[2]);
				//line 146 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String f2 = haxe.lang.Runtime.toString(message.params[3]);
				//line 146 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				java.lang.String fi1 = haxe.lang.Runtime.toString(message.params[4]);
				//line 146 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				int l2 = ((int) (haxe.lang.Runtime.toInt(message.params[5])) );
				//line 147 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
				return haxe.root.JavaProtocol.IdThreadStopped;
			}
			
			
		}
		
		//line 80 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		return 0;
	}
	
	
	public static java.lang.String commandToString(debugger.Command command)
	{
		//line 153 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		return haxe.root.Std.string(command);
	}
	
	
	public static java.lang.String messageToString(debugger.Message message)
	{
		//line 158 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		return haxe.root.Std.string(message);
	}
	
	
	public static void main()
	{
		//line 163 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		java.io.OutputStream stdout = System.out;
		//line 164 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		debugger.HaxeProtocol.writeMessage(new _JavaProtocol.OutputAdapter(((java.io.OutputStream) (stdout) )), debugger.Message.ThreadsWhere(debugger.ThreadWhereList.Where(0, debugger.ThreadStatus.Running, debugger.FrameList.Frame(true, 0, "h", "i", "p", 10, debugger.FrameList.Terminator), debugger.ThreadWhereList.Terminator)));
		//line 170 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.Sys.stderr().writeString("Reading message\n", null);
		//line 171 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		debugger.Message msg = haxe.root.JavaProtocol.readMessage(System.in);
		//line 172 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.Sys.stderr().writeString("Read message\n", null);
		//line 173 "P:\\Workspaces\\intellij-haxe-reboot\\intellij-haxe\\hxcpp-debugger-protocol\\src\\main\\haxe\\JavaProtocol.hx"
		haxe.root.Sys.stderr().writeString(( ( "Message is: " + haxe.root.Std.string(msg) ) + "\n" ), null);
	}
	
	
}


