Download [intellij-haxe.jar](https://github.com/eliasku/intellij-haxe/blob/master/intellij-haxe.jar?raw=true) file and install plugin using "Install plugin from disk..." button in Plugin dialog

For those who want to learn and maintain this plugin look at this introductory tutorial:
http://confluence.jetbrains.com/display/IntelliJIDEA/Custom+Language+Support

###Required plugins to develop:
- Plugin DevKit (REQUIRED!)
- UI Designer (REQUIRED!)

###Optional plugins, install if you want to modify lexer/parser:
- Grammar-Kit (for bnf compilation)
- JFlex (for lexer compilation)
- JUnit (for unit tests)
- PsiViewer (for testing grammar)

IntelliJ IDEA uses Grammar-Kit plugin to generate lexer/parser, grammar for Haxe is [haXe.bnf](https://github.com/eliasku/intellij-haxe/blob/master/grammar/haXe.bnf)

###Steps to configure a IntelliJ Platform Plugin SDK:
- Open Module Settings
- SDKs -> + button -> IntelliJ Platform Plugin SDK -> Choose a folder with IntelliJ Ultimate(!) or *.App on Mac
- Go to the SDK's settings page -> Classpath tab -> + button(bottom left corner) -> add plugins: flex
- To add a plugin go to IntelliJ IDEA folder/plugins/<plugin-name>/lib and choose all jars

###How to build a plugin zip file
- There is a haxe artifact. Check Module Settings -> artifacts. It is build automatically on Make action.
- To get a zip file just archive $PROJECT_ROOT$/out/artifacts/haxe folder
