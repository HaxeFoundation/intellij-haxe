Steps to configure a plugin SDK:

* Open Module Settings
* SDKs -> + button -> IntelliJ Platform Plugin SDK -> Choose a folder with IntelliJ Ultimate(!) or *.App on Mac
* Go to the SDK's settings page -> Classpath tab -> + button(bottom left corner) -> add plugins: flex
* To add a plugin go to IntelliJ IDEA folder/plugins/<plugin-name>/lib and choose all jars

How to build a plugin zip file

* There is a haxe artifact. Check Module Settings -> artifacts. It is build automatically on Make action.
* To get a zip file just archive $PROJECT_ROOT$/out/artifacts/haxe folder