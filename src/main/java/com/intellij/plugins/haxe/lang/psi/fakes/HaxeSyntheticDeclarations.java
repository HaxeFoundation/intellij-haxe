package com.intellij.plugins.haxe.lang.psi.fakes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;

public class HaxeSyntheticDeclarations {
    @Language("haxe")
    private static final String TRACE_DECLARATION = """
            package;
            /**
               This file / class does not exist.
               Its a virtual file containing definitions mimicking language features
               that the compiler handles but that does not exist in the standard library.
            **/
            extern class LanguageFeature {
                /**
                    *Language feature*
            
                    Convenience method that redirects input to `haxe.Log.trace`
            
                    The code `trace("hello", "warning", 123);` will be transformed to call `haxe.Log.trace`
                    with position information and custom parameters.
            
                    If this call was made in a file and class called `Test` in a method named `Main` on line 6
                    it would be compiled to something like this:
            
                    ```haxe
                    haxe.Log.trace("hello", {
                          fileName : "Test.hx",
                          lineNumber : 6,
                          className : "Test",
                          methodName : "main",
                          customParams : ["warning",123]
                      });
                      ```
                   To trace without the default position information `haxe.Log.trace(msg, null)` can be used.
            
            
                   @See https://haxe.org/manual/debugging-trace-log.html
                   @see https://haxe.org/manual/debugging-posinfos.html
                 **/
                static function trace(value:Dynamic, ...args:Dynamic):Void;
            
            
            
                /**
            		Inject `code` directly into generated source.
            
            		`code` must be a string constant.
            
            		Additional `args` are supported to provide code interpolation, for example:
            		```haxe
            		__js__("console.log({0}, {1})", "hi", 42);
            		```
            		will generate
            		```haxe
            		console.log("hi", 42);
            		```
            
            		Emits a compilation error if the count of `args` does not match the count of placeholders in `code`.
            
            		@see js.Syntax.code
            	**/
                static function __js__(code:String, ...args:Dynamic):Dynamic;
            
                /**
            		Embeds plain php code.
            
            		`code` should be a string literal with php code.
            		It can contain placeholders like `{0}`, `{1}` which will be replaced with corresponding arguments from `args`.
            		E.g.:
            		```haxe
            		__php__("var_dump({0}, {1})", a, b);
            		```
            		will generate
            		```haxe
            		var_dump($a, $b);
            		```
            
            		@see php.Syntax.code
            	**/
                static function __php__(code:String, ...args:Dynamic):Dynamic;
            
                /**
                    Inject `code` directly into generated source.
            
                    `code` must be a string constant.
            
                    Additional `args` are supported to provide code interpolation.
            
                    @see python.Syntax.code
                **/
                static function __python__(code:String, ...args:Dynamic):Dynamic;
            
                /**
            		Inject `code` directly into generated source.
            
            		`code` must be a string constant.
            
            		Additional `args` are supported to provide code interpolation.
            
                **/
                static function __cpp__(code:String, ...args:Dynamic):Dynamic;
            
                /**
            		Inject `code` directly into generated source.
            
            		`code` must be a string constant.
            
            		Additional `args` are supported to provide code interpolation.
            
            		 for example:
            		```haxe
            		__cs__("System.Console.WriteLine({0} + {1})", "hi", 42);
            		```
            		will generate
            		```haxe
            		System.Console.WriteLine("hi" + 42);
            		```
            
            		Emits a compilation error if the count of `args` does not match the count of placeholders in `code`.
            
            		@see cs.Syntax.code
            	**/
                static function __cs__(code:String, ...args:Dynamic):Dynamic;
            
                /**
            		Inject `code` directly into generated source.
            
            		`code` must be a string constant.
            
            		Additional `args` are supported to provide code interpolation.
            
                **/
                static function __java__(code:String, ...args:Dynamic):Dynamic;
            
                /**
            		Inject `code` directly into generated source.
            
            		`code` must be a string constant.
            
            		Additional `args` are supported to provide code interpolation.
            
                **/
                static function __lua__(code:String, ...args:Dynamic):Dynamic;
            
            }
            """;


    private static final Key<CachedValue<HaxeFile>> LanguageFeatureCache = Key.create("LanguageFeatureFile");

    public static HaxeMethod getTraceDeclaration(Project project) {
        HaxeBaseMemberModel member = findOrCreateMember(project, "trace");
        return (HaxeMethod)member.getBasePsi();
    }

    public static HaxeMethod getTargetSpecificSyntax(Project project, String name) {
        HaxeBaseMemberModel member = findOrCreateMember(project, name);
        return (HaxeMethod)member.getBasePsi();
    }

    private static @Nullable HaxeBaseMemberModel findOrCreateMember(Project project, String name) {
        HaxeFile LanguageFeaturesFile = getLanguageFeaturesFile(project);
        HaxeModule module = LanguageFeaturesFile.getModule();
        HaxeModuleModel model = (HaxeModuleModel) module.getModel();
        HaxeClassModel aClass = model.getClass("LanguageFeature");
        HaxeBaseMemberModel member = aClass.getMember(name, null);
        return member;
    }

    private static HaxeFile getLanguageFeaturesFile(Project project) {
        // will ever invalidate, but is stored in project so should unload on project unload
        return CachedValuesManager.getManager(project).getCachedValue(project, LanguageFeatureCache, () -> {
            HaxeFile file = HaxeElementGenerator.createFile(project, "LanguageFeature", TRACE_DECLARATION);
            return CachedValueProvider.Result.create(file, ModificationTracker.NEVER_CHANGED);
        }, false);
    }

}
