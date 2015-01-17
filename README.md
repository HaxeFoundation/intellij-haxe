Download [intellij-haxe.jar](https://github.com/JetBrains/intellij-haxe/blob/master/intellij-haxe.jar?raw=true) file and install plugin using "Install plugin from disk..." button in Plugin dialog

Please report issues at TiVo repository https://github.com/TiVo/intellij-haxe/issues/new

For those who want to learn and maintain this plugin look at this introductory tutorial:
http://confluence.jetbrains.com/display/IntelliJIDEA/Custom+Language+Support


###Important Note

The hxcpp debugger functionality has been rewritten to conform to the
Haxe v3.0 debugger.  In order to use this, you must:

- Install the newest debugger haxelib from https://github.com/TiVo/debugger
- Re-build your project using this newest debugger haxelib
- Configure your haxe program to start the debugger when the following
  command line option is provided:

  -start_debugger
  (and if you expect to do remote debugging, you'll also have to support:

  -debugger_host=[host]:[port]

  Most likely you'll just want to add the following in your main() when
  -start_debugger is set:

  new debugger.Local(true);

###Required plugins to develop:
- Plugin DevKit (REQUIRED!)
- UI Designer (REQUIRED!)

###Optional plugins, install if you want to modify lexer/parser:
- Grammar-Kit (for bnf compilation)
- JFlex (for lexer compilation)
- JUnit (for unit tests)
- PsiViewer (for testing grammar)

IntelliJ IDEA uses Grammar-Kit plugin to generate lexer/parser.  The grammar file for Haxe is [haxe.bnf](https://github.com/JetBrains/intellij-haxe/blob/master/grammar/haxe.bnf)

###Steps to configure a IntelliJ Platform Plugin SDK:
- Open Module Settings
- SDKs -> + button -> IntelliJ Platform Plugin SDK -> Choose a folder with IntelliJ Ultimate(!) or *.App on Mac
- Go to the SDK's settings page -> Classpath tab -> + button(bottom left corner) -> add plugins: flex
- To add a plugin go to IntelliJ IDEA folder/plugins/<plugin-name>/lib and choose all jars
- Add all libraries from <your_IDEA_install_directory>/lib directory.
  (Note: If you get unit test failures from classNotFound errors, 
         it's probably because the netty-all-x.x.x.x.jar is missing.)

###How to build a plugin zip file
Update the release notes in src/META-INF/plugin.xml to let us know what you changed.
From the Build menu:
- Rebuild Project
- Prepare plugin module 'intellij-haxe' for deployment.
- The plugin distributable file ('intellij-haxe.jar') will be found in the root of the source tree.
Alternately:
- There is a haxe artifact. Check Module Settings -> artifacts. It is build automatically on Make action.
- To get a zip file just archive $PROJECT_ROOT$/out/artifacts/haxe folder

###Updating grammar files.
As stated above, the grammar file is located in the source tree at 
grammar/haxe.bnf.  If you change this file, you must (re)generate 
the parsing files.

To regenerate, make your local changes and then press Ctrl+G.  You can compile
and test with them.  Do NOT check those files in until you have updated the 
comments.  (Otherwise, *every* file will appear updated.)

(NOTE: To work around a bug in IDEA, it is necessary to change focus to another
application and then back to IDEA before updating the comments.  Otherwise,
the process doesn't do anything.)

To fix the comments, open the project window within intellij, select and 
the right-click on the gen/ tree.  Click on the 'Update Copyright' item.
(Should be the third from the bottom.  Sometimes you have to re-open the
context menu.)  In the 'Update copyright scope' dialog, select the 
"Directory 'intellij-haxe...'" item and press OK.  In a moment, all of the
copyrights will have been updated.  A code comparison (or 'git status') will
then show only those files that really changed.

