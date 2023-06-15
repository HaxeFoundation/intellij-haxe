Haxe plugin for Intellij IDEA and Android Studio
======================================

This plugin allows you to develop multi-platform programs using the [Haxe](http://haxe.org/) language with [Intellij IDEA](http://www.jetbrains.com/idea), [Android Studio](https://developer.android.com/studio/) and other IntelliJ IDEA-based IDEs by JetBrains.
It requires Intellij IDEA Ultimate or Community Edition 2023.1 or later,  or Android Studio 2023 or later

#### Technical Support
Support for this plugin is available through the project's home page: [http://intellij-haxe.org](http://intellij-haxe.org), or
the github issues list [http://github.com/HaxeFoundation/intellij-haxe/issues](http://github.com/HaxeFoundation/intellij-haxe/issues).

#### Past Versions Announcement

Very, very few users are continuing to use this plugin with older versions of IDEA. 
Maintaining a code base that can compile to multiple versions is also a lot of work.
its therefore been decided that from now on we will only target one version in develop branch but make maintenance branches for older versions so that  important fixes can be backported.

If you for some reason need to compile one of the old versions that was part of the multi-version code base you can still find it on the `legacy/develop` branch.  

Install
-------

*JetBrains' official plugin installation documentation is at [https://www.jetbrains.com/idea/plugins/](https://www.jetbrains.com/idea/plugins/).
The Haxe plugin page is [https://plugins.jetbrains.com/plugin/6873?pr=idea](https://plugins.jetbrains.com/plugin/6873?pr=idea).*

### To install using IDEA (from Intellij plugin repository):

Install and start IDEA.  It is found at [https://www.jetbrains.com/idea](https://www.jetbrains.com/idea)

If you do not have a project open in IDEA (and after first-time setup):
- On the IDEA welcome screen, select "Configure(dropdown)->Plugins"
- Click on the "Browse Repositories..." button.
- Type 'haxe' to see the description for the plugin.
- Select 'Install' to install it.
- Allow IDEA to restart and initialize the plugin.

If you already have a project open in IDEA:
- Open the Settings dialog (File->Settings...)
- Highlight "Plugins" in the leftmost column
- Click on the "Browse Repositories..." button.
- Type 'haxe' to see the description for the plugin.
- Select 'Install' to install it.
- Allow IDEA to restart and initialize the plugin.

### To manually install the latest or a previous Github release

Download the `intellij-haxe.jar` file from the release you want from [Github releases](https://github.com/HaxeFoundation/intellij-haxe/releases).
More recent releases have begun to be named `intellij-haxe-<release>.jar`, where &lt;release&gt; is the version of Idea for which the Jar is built.  (e.g. `intellij-haxe-2022.3.jar`)
Make sure that you pick the proper one for your release.  A message should pop up and warn you if a release is incompatible.

If you do not yet have a project open in IDEA (and after first-time setup):
- On the IDEA welcome screen, select "Configure(dropdown)->Plugins"
- Click “Install plugin from disk...”
- Select the “intellij-haxe.jar” file you downloaded
- Allow IDEA to restart and initialize the plugin.

If you already have a project open IDEA:
- Open the Settings dialog (File->Settings...)
- Highlight "Plugins" in the leftmost column
- Click “Install plugin from disk...”.  On 2019.x versions or later, click on the settings (gear) icon to see the "Install from disk..." menu item.
- Select the `intellij-haxe-<version>.jar` file you downloaded
- Allow IDEA to restart and initialize the plugin.

Build
-----
Note that installation as described above installs a fully built version of the plugin (the .jar file).  Most users do not have to
build the product for themselves.  This section is for those who like to dig a little deeper.

This describes the command line build.  To build from within Intellij IDEA itself, see the [contributing](CONTRIBUTING.md) document to setup
your development environment.  Much more detail is provided there for command line build options as well.

### Dependencies
- OpenJDK 17
- A windows command prompt or bash compatible shell

### Build command
Windows
```
gradlew.bat clean build verifyPlugin 
```
Mac/Linux
```
./gradlew clean build verifyPlugin 
```

>NOTE: You can run the build without tests by substituting `build` with `buildPlugin` on the lines above

This will generate a `intelllij-haxe-<release>.jar` file at the root of the project that you can then install from disk
(see “Install the latest or a previous Github release).


Note that the first time you build the project Gradle will download the requested version of IntelliJ Ultimate and 
any other other dependencies. This can be quite slow at times and prone to failure.  For repeated building and testing,
we recommended that you set up your machine as described in the [contributing document](CONTRIBUTING.md). 

Test
----

### Dependencies
Same as for build.

### Test command
Windows
```
gradlew.bat test
```
Mac/Linux
```
./gradlew test
```
This will build and run the tests and display the JUnit report.   

Use the hxcpp debugger
----------------------

*NOTE: IDEA Community Edition currently will not start an IDE-based debugging session.  For that,
IDEA Ultimate is required.  Command-line debugging is available because that is a feature
of the Haxe language itself.  See the hxcpp-debugger project for more information.*

The hxcpp debugger functionality conforms to the
Haxe v3.0 debugger.  In order to use this, you must:

- Install the *VERSION 1.1* debugger haxelib from [https://github.com/HaxeFoundation/hxcpp-debugger/tree/protocol_v1.1](https://github.com/HaxeFoundation/hxcpp-debugger/tree/protocol_v1.1).
  The hxcpp-debugger that is installed via 'haxelib install' is generally
  not the latest or best working version. (The Haxe Foundation maintainers
  do not release regular updates for it). Instead, get the current sources
  locally: Install git and clone the repository from
  [http://github.com/HaxeFoundation/hxcpp-debugger](http://github.com/HaxeFoundation/hxcpp-debugger)
  and install via
  ```
  haxelib git hxcpp-debugger <your_local_clone> protocol_v1.1
  ```
  Then, you'll have the version
  that matches the plugin. Whenever you need to update the debugger to
  the latest sources, do a 'git pull' and then rebuild your app.
- Re-build your project using this newest debugger haxelib.
- Configure your haxe program to start the debugger when the following
  command line option is provided:

  ```
  -start_debugger
  ```
  If you expect to do remote debugging, you'll also have to support:

  ```
  -debugger_host=[host]:[port]
  ```

  Most likely you'll just want to add the following in your main() when
  `-start_debugger` is set:
    
  ```haxe
  new debugger.HaxeRemote(true, host, port);
  ```

  Generally speaking, the build/debug configuration (Run->Edit Configurations)
   is set up to use port 6972, so you can probably cheat and use:

  ```haxe
  new debugger.HaxeRemote(true, "localhost", 6972);
  ```

  However, the line has to match your debug settings. Fortunately, they are
  passed to your program on the command line. Notice the
  "-start_debugger -debugger_host=localhost:6972" passed to haxelib:
  ```
  C:/HaxeToolkit/haxe/haxelib.exe run lime run C:/temp/issue349/openfl_cpp_debug/openfl_cpp_debug/project.xml
    windows -verbose -Ddebug -debug -args  -start_debugger -debugger_host=localhost:6972
  ```
Your program should now:
1) Look for the '-start_debugger' parameter before doing anything. It won't
 be there if the program is being started via the "Run" command from IDEA.
2) Parse the '-debugger_host=' parameter.  If it exists,
 then a remote controller (e.g. IDEA) will be trying to connect on that port.
 If it doesn't exist, then the user (you) probably want to start the command
 line debugger:
  ```haxe
  new debugger.Local(true);
  ```

Here's a snippet you can use: (Thanks to @isBatak)
  ```haxe
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
        var args:Array<String> = debuggerHost.split(":");
        new debugger.HaxeRemote(true, args[0], Std.parseInt(args[1]));
      }
      else {
        new debugger.Local(true);
      }
    }
  #end
  ```

Contribute
----------
See the [contributing document](CONTRIBUTING.md).
