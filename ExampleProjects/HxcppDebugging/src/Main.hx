package ;

#if (debug && cpp)
import debugger.Local;
import debugger.HaxeRemote;
#end

class Main {

/*
 *   NOTE: Do not add your application's properties or
 *         initialization in this class.  Add it to
 *         ApplicationMain, so that the debugger is
 *         fully started *before* your application begins
 *         to load.
 *
 *         If you put properties here, they will be
 *         initialized (and some of your application
 *         code will run) before the debugger gets a
 *         chance to start.
 */

#if (debug && cpp)
    static public var remoteDebugger(default,default) : HaxeRemote;
    static public var localDebugger(default,default) : Local;
#end

    public function new() {
        trace('Running Main.new()');
    }

    public static function main():Void {

        try {
            var a : Int = 3;

            startDebugger();
            MyApp.main();
        } catch (e:Dynamic) {
            trace("Caught Dynamic:");
            trace(e);
        }
    }

    public static function startDebugger():Void {
        #if (debug && cpp)
            var startDebugger:Bool = false;
            var debuggerHost:String = "";
            var argStartDebugger:String = "-start_debugger";
            var pDebuggerHost:EReg = ~/-debugger_host=(.+)/;

            for (arg in Sys.args()) {
                if(arg == argStartDebugger){
                    startDebugger = true;
                }
                else if(pDebuggerHost.match(arg)){
                    debuggerHost = pDebuggerHost.matched(1);
                }
            }

            if(startDebugger){
                if(debuggerHost != "") {
                    trace("Connecting to remote debug session at " + debuggerHost);
                    var args:Array<String> = debuggerHost.split(":");
                    remoteDebugger = new debugger.HaxeRemote(true, args[0], Std.parseInt(args[1]));
                }
                else {
                    trace("Starting local debug session.");
                    localDebugger = new debugger.Local(true);
                }
                trace("Debugger is ready.");
            } else {
                trace("No debug session requested.");
            }
        #else
            #if debug
                trace("Not a C++ target - skipping hxcpp debugger.");
            #end
        #end
    }

}
