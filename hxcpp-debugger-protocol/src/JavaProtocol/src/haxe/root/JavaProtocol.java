package haxe.root;
import haxe.root.*;

@SuppressWarnings(value={"rawtypes", "unchecked"})
public  class JavaProtocol extends haxe.lang.HxObject
{
	public static void main(String[] args)
	{
		main();
	}
	static 
	{
		haxe.root.JavaProtocol.IdErrorInternal = 0;
		haxe.root.JavaProtocol.IdErrorNoSuchThread = 1;
		haxe.root.JavaProtocol.IdErrorNoSuchFile = 2;
		haxe.root.JavaProtocol.IdErrorNoSuchBreakpoint = 3;
		haxe.root.JavaProtocol.IdErrorBadClassNameRegex = 4;
		haxe.root.JavaProtocol.IdErrorBadFunctionNameRegex = 5;
		haxe.root.JavaProtocol.IdErrorNoMatchingFunctions = 6;
		haxe.root.JavaProtocol.IdErrorBadCount = 7;
		haxe.root.JavaProtocol.IdErrorCurrentThreadNotStopped = 8;
		haxe.root.JavaProtocol.IdErrorEvaluatingExpression = 9;
		haxe.root.JavaProtocol.IdOK = 10;
		haxe.root.JavaProtocol.IdExited = 11;
		haxe.root.JavaProtocol.IdDetached = 12;
		haxe.root.JavaProtocol.IdFiles = 13;
		haxe.root.JavaProtocol.IdAllClasses = 14;
		haxe.root.JavaProtocol.IdClasses = 15;
		haxe.root.JavaProtocol.IdMemBytes = 16;
		haxe.root.JavaProtocol.IdCompacted = 17;
		haxe.root.JavaProtocol.IdCollected = 18;
		haxe.root.JavaProtocol.IdThreadLocation = 19;
		haxe.root.JavaProtocol.IdFileLineBreakpointNumber = 20;
		haxe.root.JavaProtocol.IdClassFunctionBreakpointNumber = 21;
		haxe.root.JavaProtocol.IdBreakpoints = 22;
		haxe.root.JavaProtocol.IdBreakpointDescription = 23;
		haxe.root.JavaProtocol.IdBreakpointStatuses = 24;
		haxe.root.JavaProtocol.IdThreadsWhere = 25;
		haxe.root.JavaProtocol.IdVariables = 26;
		haxe.root.JavaProtocol.IdValue = 27;
		haxe.root.JavaProtocol.IdStructured = 28;
		haxe.root.JavaProtocol.IdThreadCreated = 29;
		haxe.root.JavaProtocol.IdThreadTerminated = 30;
		haxe.root.JavaProtocol.IdThreadStarted = 31;
		haxe.root.JavaProtocol.IdThreadStopped = 32;
	}
	public    JavaProtocol(haxe.lang.EmptyObject empty)
	{
		{
		}
		
	}
	
	
	public    JavaProtocol()
	{
		haxe.root.JavaProtocol.__hx_ctor__JavaProtocol(this);
	}
	
	
	public static   void __hx_ctor__JavaProtocol(haxe.root.JavaProtocol __temp_me12)
	{
		{
		}
		
	}
	
	
	public static  int IdErrorInternal;
	
	public static  int IdErrorNoSuchThread;
	
	public static  int IdErrorNoSuchFile;
	
	public static  int IdErrorNoSuchBreakpoint;
	
	public static  int IdErrorBadClassNameRegex;
	
	public static  int IdErrorBadFunctionNameRegex;
	
	public static  int IdErrorNoMatchingFunctions;
	
	public static  int IdErrorBadCount;
	
	public static  int IdErrorCurrentThreadNotStopped;
	
	public static  int IdErrorEvaluatingExpression;
	
	public static  int IdOK;
	
	public static  int IdExited;
	
	public static  int IdDetached;
	
	public static  int IdFiles;
	
	public static  int IdAllClasses;
	
	public static  int IdClasses;
	
	public static  int IdMemBytes;
	
	public static  int IdCompacted;
	
	public static  int IdCollected;
	
	public static  int IdThreadLocation;
	
	public static  int IdFileLineBreakpointNumber;
	
	public static  int IdClassFunctionBreakpointNumber;
	
	public static  int IdBreakpoints;
	
	public static  int IdBreakpointDescription;
	
	public static  int IdBreakpointStatuses;
	
	public static  int IdThreadsWhere;
	
	public static  int IdVariables;
	
	public static  int IdValue;
	
	public static  int IdStructured;
	
	public static  int IdThreadCreated;
	
	public static  int IdThreadTerminated;
	
	public static  int IdThreadStarted;
	
	public static  int IdThreadStopped;
	
	public static   void writeServerIdentification(java.io.OutputStream output)
	{
		debugger.HaxeProtocol.writeServerIdentification(new _JavaProtocol.OutputAdapter(((java.io.OutputStream) (output) )));
	}
	
	
	public static   void readClientIdentification(java.io.InputStream input)
	{
		debugger.HaxeProtocol.readClientIdentification(new _JavaProtocol.InputAdapter(((java.io.InputStream) (input) )));
	}
	
	
	public static   void writeCommand(java.io.OutputStream output, debugger.Command command)
	{
		debugger.HaxeProtocol.writeCommand(new _JavaProtocol.OutputAdapter(((java.io.OutputStream) (output) )), command);
	}
	
	
	public static   debugger.Message readMessage(java.io.InputStream input)
	{
		return debugger.HaxeProtocol.readMessage(new _JavaProtocol.InputAdapter(((java.io.InputStream) (input) )));
	}
	
	
	public static   int getMessageId(debugger.Message message)
	{
		switch (haxe.root.Type.enumIndex(message))
		{
			case 0:
			{
				java.lang.String details = haxe.lang.Runtime.toString(message.params.__get(0));
				return haxe.root.JavaProtocol.IdErrorInternal;
			}
			
			
			case 1:
			{
				int number = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdErrorNoSuchThread;
			}
			
			
			case 2:
			{
				java.lang.String fileName = haxe.lang.Runtime.toString(message.params.__get(0));
				return haxe.root.JavaProtocol.IdErrorNoSuchFile;
			}
			
			
			case 3:
			{
				int number = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdErrorNoSuchBreakpoint;
			}
			
			
			case 4:
			{
				java.lang.String details = haxe.lang.Runtime.toString(message.params.__get(0));
				return haxe.root.JavaProtocol.IdErrorBadClassNameRegex;
			}
			
			
			case 5:
			{
				java.lang.String details = haxe.lang.Runtime.toString(message.params.__get(0));
				return haxe.root.JavaProtocol.IdErrorBadFunctionNameRegex;
			}
			
			
			case 6:
			{
				debugger.StringList u = ((debugger.StringList) (message.params.__get(2)) );
				java.lang.String f = haxe.lang.Runtime.toString(message.params.__get(1));
				java.lang.String className = haxe.lang.Runtime.toString(message.params.__get(0));
				return haxe.root.JavaProtocol.IdErrorNoMatchingFunctions;
			}
			
			
			case 7:
			{
				int count = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdErrorBadCount;
			}
			
			
			case 8:
			{
				int threadNumber = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdErrorCurrentThreadNotStopped;
			}
			
			
			case 9:
			{
				java.lang.String details = haxe.lang.Runtime.toString(message.params.__get(0));
				return haxe.root.JavaProtocol.IdErrorEvaluatingExpression;
			}
			
			
			case 10:
			{
				return haxe.root.JavaProtocol.IdOK;
			}
			
			
			case 11:
			{
				return haxe.root.JavaProtocol.IdExited;
			}
			
			
			case 12:
			{
				return haxe.root.JavaProtocol.IdDetached;
			}
			
			
			case 13:
			{
				debugger.StringList list = ((debugger.StringList) (message.params.__get(0)) );
				return haxe.root.JavaProtocol.IdFiles;
			}
			
			
			case 14:
			{
				debugger.StringList list = ((debugger.StringList) (message.params.__get(0)) );
				return haxe.root.JavaProtocol.IdAllClasses;
			}
			
			
			case 15:
			{
				debugger.ClassList list = ((debugger.ClassList) (message.params.__get(0)) );
				return haxe.root.JavaProtocol.IdClasses;
			}
			
			
			case 16:
			{
				int bytes = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdMemBytes;
			}
			
			
			case 17:
			{
				int a = ((int) (haxe.lang.Runtime.toInt(message.params.__get(1))) );
				int bytesBefore = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdCompacted;
			}
			
			
			case 18:
			{
				int a = ((int) (haxe.lang.Runtime.toInt(message.params.__get(1))) );
				int bytesBefore = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdCollected;
			}
			
			
			case 19:
			{
				int l = ((int) (haxe.lang.Runtime.toInt(message.params.__get(5))) );
				java.lang.String fi = haxe.lang.Runtime.toString(message.params.__get(4));
				java.lang.String f = haxe.lang.Runtime.toString(message.params.__get(3));
				java.lang.String c = haxe.lang.Runtime.toString(message.params.__get(2));
				int s = ((int) (haxe.lang.Runtime.toInt(message.params.__get(1))) );
				int number = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdThreadLocation;
			}
			
			
			case 20:
			{
				int number = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdFileLineBreakpointNumber;
			}
			
			
			case 21:
			{
				debugger.StringList u = ((debugger.StringList) (message.params.__get(1)) );
				int number = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdClassFunctionBreakpointNumber;
			}
			
			
			case 22:
			{
				debugger.BreakpointList list = ((debugger.BreakpointList) (message.params.__get(0)) );
				return haxe.root.JavaProtocol.IdBreakpoints;
			}
			
			
			case 23:
			{
				debugger.BreakpointLocationList l = ((debugger.BreakpointLocationList) (message.params.__get(1)) );
				int number = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdBreakpointDescription;
			}
			
			
			case 24:
			{
				debugger.BreakpointStatusList list = ((debugger.BreakpointStatusList) (message.params.__get(0)) );
				return haxe.root.JavaProtocol.IdBreakpointStatuses;
			}
			
			
			case 25:
			{
				debugger.ThreadWhereList list = ((debugger.ThreadWhereList) (message.params.__get(0)) );
				return haxe.root.JavaProtocol.IdThreadsWhere;
			}
			
			
			case 26:
			{
				debugger.StringList list = ((debugger.StringList) (message.params.__get(0)) );
				return haxe.root.JavaProtocol.IdVariables;
			}
			
			
			case 28:
			{
				debugger.StructuredValue structuredValue = ((debugger.StructuredValue) (message.params.__get(0)) );
				return haxe.root.JavaProtocol.IdStructured;
			}
			
			
			case 27:
			{
				java.lang.String v = haxe.lang.Runtime.toString(message.params.__get(2));
				java.lang.String t = haxe.lang.Runtime.toString(message.params.__get(1));
				java.lang.String expression = haxe.lang.Runtime.toString(message.params.__get(0));
				return haxe.root.JavaProtocol.IdValue;
			}
			
			
			case 29:
			{
				int number = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdThreadCreated;
			}
			
			
			case 30:
			{
				int number = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdThreadTerminated;
			}
			
			
			case 31:
			{
				int number = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdThreadStarted;
			}
			
			
			case 32:
			{
				int l = ((int) (haxe.lang.Runtime.toInt(message.params.__get(5))) );
				java.lang.String fi = haxe.lang.Runtime.toString(message.params.__get(4));
				java.lang.String f = haxe.lang.Runtime.toString(message.params.__get(3));
				java.lang.String c = haxe.lang.Runtime.toString(message.params.__get(2));
				int s = ((int) (haxe.lang.Runtime.toInt(message.params.__get(1))) );
				int number = ((int) (haxe.lang.Runtime.toInt(message.params.__get(0))) );
				return haxe.root.JavaProtocol.IdThreadStopped;
			}
			
			
		}
		
		return 0;
	}
	
	
	public static   java.lang.String commandToString(debugger.Command command)
	{
		return haxe.root.Std.string(command);
	}
	
	
	public static   java.lang.String messageToString(debugger.Message message)
	{
		return haxe.root.Std.string(message);
	}
	
	
	public static   void main()
	{
		java.io.OutputStream stdout = System.out;
		debugger.HaxeProtocol.writeMessage(new _JavaProtocol.OutputAdapter(((java.io.OutputStream) (stdout) )), debugger.Message.ThreadsWhere(debugger.ThreadWhereList.Where(0, debugger.ThreadStatus.Running, debugger.FrameList.Frame(true, 0, "h", "i", "p", 10, debugger.FrameList.Terminator), debugger.ThreadWhereList.Terminator)));
		haxe.root.Sys.stderr().writeString("Reading message\n");
		debugger.Message msg = haxe.root.JavaProtocol.readMessage(System.in);
		haxe.root.Sys.stderr().writeString("Read message\n");
		haxe.root.Sys.stderr().writeString(( ( "Message is: " + haxe.root.Std.string(msg) ) + "\n" ));
	}
	
	
	public static   java.lang.Object __hx_createEmpty()
	{
		return new haxe.root.JavaProtocol(((haxe.lang.EmptyObject) (haxe.lang.EmptyObject.EMPTY) ));
	}
	
	
	public static   java.lang.Object __hx_create(haxe.root.Array arr)
	{
		return new haxe.root.JavaProtocol();
	}
	
	
}


