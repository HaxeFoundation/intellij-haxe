Contributing
============

Reporting errors
----------------
Things that will help us fix your bug:
- A minimal code example. For example if you have some completion issue, you can add the simplest Haxe that
can reproduce the issue.
- We’d like to know your:
    - IDEA version
    - OS and OS version
    - JDK version
- Check if the bug already exists. If is does, add your example to the discussion.

Developing
----------
You will need the release version of Intellij IDEA Ultimate 13 or 14 to develop the plugin.

###Plugins
Install the following plugins [from Intellij IDEA plugin manager](https://www.jetbrains.com/idea/plugins/).

####Required
- Plugin DevKit
- UI Designer

####Testing
- JUnit

####Optional, install if you want to modify lexer/parser:
- Grammar-Kit (for bnf compilation) version 1.2.0.1 or later.
- JFlex (for lexer compilation)
- PsiViewer (for testing grammar)

IntelliJ IDEA uses the Grammar-Kit plugin to generate its lexer and parser for Haxe and HXML.
The grammar file for Haxe is [haxe.bnf](https://github.com/JetBrains/intellij-haxe/blob/master/grammar/haxe.bnf).
The grammar file for HXML is [hxml.bnf](https://github.com/JetBrains/intellij-haxe/blob/master/src/com/intellij/plugins/haxe/hxml/hxml.bnf).

###Steps to configure a IntelliJ Platform Plugin SDK:
- Open Module Settings
- SDKs -> + button -> IntelliJ Platform Plugin SDK -> Choose a folder with IntelliJ Ultimate(!) or *.App on Mac
- Go to the SDK’s settings page -> Classpath tab -> + button(bottom left corner) -> add plugins: flex
- To add a plugin go to IntelliJ IDEA folder/plugins/<plugin-name>/lib and choose all jars
- Add all libraries from <your_IDEA_install_directory>/lib directory.

###Video tutorials from [as3Boyan](https://github.com/as3boyan)

####Installation
- [Setup IntellliJ IDEA for Haxe Plugin development](http://youtu.be/MwrzdBFaZkc)

####How to write plugin code
- [How to develop intention actions.](https://www.youtube.com/watch?v=-mY_DpzVDFs) 
- [How to extend HXML completion using haxelib.](https://www.youtube.com/watch?v=B8zOSEEK7As)
- [How to build a completion contributor for HXML.](https://www.youtube.com/watch?v=UBxuj2ToizY)

###How to build a plugin zip file
From the Build menu:
- Rebuild Project
- Prepare plugin module ‘intellij-haxe’ for deployment.
- The plugin distributable file (‘intellij-haxe.jar’) will be found in the root of the source tree.
Alternately:
- There is a haxe artifact. Check Module Settings -> artifacts. It is build automatically on Make action.
- To get a zip file just archive $PROJECT_ROOT$/out/artifacts/haxe folder

###Updating grammar files.
If you change the haxe.bnf or hxml.bnf files, you must (re)generate the parsing files.

To regenerate, make your local changes and then press Ctrl+G.  You can compile
and test with them.  Do NOT check those files in until you have updated the
comments.  (Otherwise, *every* file will appear updated.)

*(NOTE: To work around a bug in IDEA, it is necessary to change focus to another
application and then back to IDEA before updating the comments.  Otherwise,
the process doesn’t do anything.)*

To fix the comments:
- Open the project window within intellij, select and 
 the right-click on the gen/ tree.  
- Click on the ‘Update Copyright’ item.  (Should be the third from the bottom.  Sometimes you have to re-open the
 context menu.)  
- In the ‘Update copyright scope’ dialog, select the “Directory ‘intellij-haxe...’” item and press OK.  
 In a moment, all of the copyrights will have been updated.  A code comparison (or ‘git status’) will
 then show only those files that really changed.
