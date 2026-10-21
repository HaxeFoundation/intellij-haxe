
import debugger.IController;
import debugger.HaxeProtocol;

/**
 * Serializes and deserializes the Command and Message enums of the legacy
 * hxcpp debugger protocol. Compiled to Java, it gives the IDE an API that
 * writes commands to a Java OutputStream and reads messages from a Java
 * InputStream.
 *
 * The haxe compiler turns it into the Java class haxe.root.JavaProtocol.
 * The gradle task generateDebuggerJavaSource runs that compile (with the
 * hxcpp-debugger and hxjava haxelibs) into src/gen, but only when the
 * generateHxcppDebugger property is true; otherwise the build uses the
 * checked-in Java sources under src/fallback/java.
 **/
class JavaProtocol
{
    // Haxe enums are awkward to test from Java, so each Message constructor
    // also has an integer id (see getMessageId).
    public static var IdErrorInternal : Int = 0;
    public static var IdErrorNoSuchThread : Int = 1;
    public static var IdErrorNoSuchFile : Int = 2;
    public static var IdErrorNoSuchBreakpoint : Int = 3;
    public static var IdErrorBadClassNameRegex : Int = 4;
    public static var IdErrorBadFunctionNameRegex : Int = 5;
    public static var IdErrorNoMatchingFunctions : Int = 6;
    public static var IdErrorBadCount : Int = 7;
    public static var IdErrorCurrentThreadNotStopped : Int = 8;
    public static var IdErrorEvaluatingExpression : Int = 9;
    public static var IdOK : Int = 10;
    public static var IdExited : Int = 11;
    public static var IdDetached : Int = 12;
    public static var IdFiles : Int = 13;
    public static var IdAllClasses : Int = 14;
    public static var IdClasses : Int = 15;
    public static var IdMemBytes : Int = 16;
    public static var IdCompacted : Int = 17;
    public static var IdCollected : Int = 18;
    public static var IdThreadLocation : Int = 19;
    public static var IdFileLineBreakpointNumber : Int = 20;
    public static var IdClassFunctionBreakpointNumber : Int = 21;
    public static var IdBreakpoints : Int = 22;
    public static var IdBreakpointDescription : Int = 23;
    public static var IdBreakpointStatuses : Int = 24;
    public static var IdThreadsWhere : Int = 25;
    public static var IdVariables : Int = 26;
    public static var IdValue : Int = 27;
    public static var IdStructured : Int = 28;
    public static var IdThreadCreated : Int = 29;
    public static var IdThreadTerminated : Int = 30;
    public static var IdThreadStarted : Int = 31;
    public static var IdThreadStopped : Int = 32;

    public static function writeServerIdentification(output : java.io.OutputStream)
    {
        HaxeProtocol.writeServerIdentification(new OutputAdapter(output));
    }

    public static function readClientIdentification(input : java.io.InputStream)
    {
        HaxeProtocol.readClientIdentification(new InputAdapter(input));
    }

    public static function writeCommand(output : java.io.OutputStream,
                                        command : Command)
    {
        HaxeProtocol.writeCommand(new OutputAdapter(output), command);
    }

    public static function readMessage(input : java.io.InputStream) : Message
    {
        return HaxeProtocol.readMessage(new InputAdapter(input));
    }

    public static function getMessageId(message : Message)
    {
        switch (message) {
        case ErrorInternal(details):
            return IdErrorInternal;
        case ErrorNoSuchThread(number):
            return IdErrorNoSuchThread;
        case ErrorNoSuchFile(fileName):
            return IdErrorNoSuchFile;
        case ErrorNoSuchBreakpoint(number):
            return IdErrorNoSuchBreakpoint;
        case ErrorBadClassNameRegex(details):
            return IdErrorBadClassNameRegex;
        case ErrorBadFunctionNameRegex(details):
            return IdErrorBadFunctionNameRegex;
        case ErrorNoMatchingFunctions(className, f, u):
            return IdErrorNoMatchingFunctions;
        case ErrorBadCount(count):
            return IdErrorBadCount;
        case ErrorCurrentThreadNotStopped(threadNumber):
            return IdErrorCurrentThreadNotStopped;
        case ErrorEvaluatingExpression(details):
            return IdErrorEvaluatingExpression;
        case OK:
            return IdOK;
        case Exited:
            return IdExited;
        case Detached:
            return IdDetached;
        case Files(list):
            return IdFiles;
        case AllClasses(list):
            return IdAllClasses;
        case Classes(list):
            return IdClasses;
        case MemBytes(bytes):
            return IdMemBytes;
        case Compacted(bytesBefore, a):
            return IdCompacted;
        case Collected(bytesBefore, a):
            return IdCollected;
        case ThreadLocation(number, s, c, f, fi, l):
            return IdThreadLocation;
        case FileLineBreakpointNumber(number):
            return IdFileLineBreakpointNumber;
        case ClassFunctionBreakpointNumber(number, u):
            return IdClassFunctionBreakpointNumber;
        case Breakpoints(list):
            return IdBreakpoints;
        case BreakpointDescription(number, l):
            return IdBreakpointDescription;
        case BreakpointStatuses(list):
            return IdBreakpointStatuses;
        case ThreadsWhere(list):
            return IdThreadsWhere;
        case Variables(list):
            return IdVariables;
        case Structured(structuredValue):
            return IdStructured;
        case Value(expression, t, v):
            return IdValue;
        case ThreadCreated(number):
            return IdThreadCreated;
        case ThreadTerminated(number):
            return IdThreadTerminated;
        case ThreadStarted(number):
            return IdThreadStarted;
        case ThreadStopped(number, s, c, f, fi, l):
            return IdThreadStopped;
        }
    }

    public static function commandToString(command : Command) : String
    {
        return Std.string(command);
    }

    public static function messageToString(message : Message) : String
    {
        return Std.string(message);
    }

    // A manual round-trip check: writes a ThreadsWhere message to stdout,
    // then reads one message from stdin and prints it to stderr.
    public static function main()
    {
        var stdout = untyped __java__('System.out');
        HaxeProtocol.writeMessage(new OutputAdapter(stdout),
                                  Message.ThreadsWhere
                     (Where(0, Running, 
                            Frame(true, 0, "h", "i", "p",
                                  10, Terminator),
                            Terminator)));
        Sys.stderr().writeString("Reading message\n");
        var msg = readMessage(untyped __java__('System.in'));
        Sys.stderr().writeString("Read message\n");
        Sys.stderr().writeString("Message is: " + msg + "\n");
    }
}


// A haxe.io.Output over a java.io.OutputStream.
private class OutputAdapter extends haxe.io.Output
{
    public function new(os : java.io.OutputStream)
    {
        mOs = os;
    }

    public override function writeBytes(bytes : haxe.io.Bytes, pos : Int,
                                        len : Int) : Int
    {
        try {
            mOs.write(bytes.getData(), pos, len);
            return len;
        }
        catch (e : java.io.IOException) {
            throw "IOException: " + Std.string(e);
        }
    }

    private var mOs : java.io.OutputStream;
}


// A haxe.io.Input over a java.io.InputStream.
private class InputAdapter extends haxe.io.Input
{
    public function new(is : java.io.InputStream)
    {
        mIs = is;
    }

    public override function readBytes(bytes : haxe.io.Bytes, pos : Int,
                                       len : Int) : Int
    {
        try {
            return mIs.read(bytes.getData(), pos, len);
        }
        catch (e : java.io.IOException) {
            throw "IOException: " + Std.string(e);
        }
    }

    private var mIs : java.io.InputStream;
}
