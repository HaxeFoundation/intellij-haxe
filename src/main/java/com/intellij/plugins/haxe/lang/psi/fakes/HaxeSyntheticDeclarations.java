package com.intellij.plugins.haxe.lang.psi.fakes;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeModule;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeModuleModel;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.containers.ContainerUtil;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HaxeSyntheticDeclarations {
    @Language("haxe")
    private static final String FEATURE_DECLARATIONS = """
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
      }

      /**
         The untyped target-syntax calls (`__js__`, `__cpp__`, ...): the compiler accepts
         them without a declaration; the target's `Syntax.code` replaces them from Haxe 4.
      **/
      extern class TargetSyntax {
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
      
      enum abstract AnalyzerOptions(String) {
          /** Enables the analyzer's optimizations for the annotated class or field. **/
          var optimize;
      
          /** Disables the analyzer's optimizations for the annotated class or field. **/
          var no_optimize;
      
          /** Replaces reads of variables whose value is a known constant with the constant itself. **/
          var const_propagation;
      
          /** Disables constant propagation. **/
          var no_const_propagation;
      
          /** Replaces reads of variables that merely copy another variable with the original. **/
          var copy_propagation;
      
          /** Disables copy propagation. **/
          var no_copy_propagation;
      
          /** Removes local variables and assignments whose values are never used. **/
          var local_dce;
      
          /** Disables local dead code elimination. **/
          var no_local_dce;
      
          /** Inlines compiler-generated temporary variables into their single use site. **/
          var fusion;
      
          /** Disables variable fusion. **/
          var no_fusion;
      
          /** Extends fusion to user-written variables, not only compiler temporaries. **/
          var user_var_fusion;
      
          /** Keeps user-written variables out of fusion. **/
          var no_user_var_fusion;
      
          /** Skips the analyzer entirely for the annotated class or field; stronger than `no_optimize`. **/
          var ignore;
      
          /** Dumps the analyzer's control-flow graphs as Graphviz dot files. **/
          var dot_debug;
      
          /** Like `dot_debug`, dumping the graph after every transformation step. **/
          var full_debug;
      
          /** Prints the fusion decision for each candidate variable. **/
          var fusion_debug;
      
          /** Obsolete; accepted without effect. Use `@:semantics(variable)` on the type instead. **/
          var as_var;
      }
      """;


    private static final Key<CachedValue<HaxeFile>> LanguageFeatureCache = Key.create("LanguageFeatureFile");

    private static final String LANGUAGE_FEATURE_CLASS = "LanguageFeature";
    private static final String TARGET_SYNTAX_CLASS = "TargetSyntax";
    private static final String ANALYZER_OPTIONS_CLASS = "AnalyzerOptions";

    /** The name of the call the compiler accepts without an import. */
    public static final String TRACE = "trace";

    /** The `trace` the compiler accepts without an import. */
    public static HaxeMethod traceMethod(Project project) {
        return methodOf(classModel(project, LANGUAGE_FEATURE_CLASS).getMethod(TRACE, null));
    }

    /** The untyped target-syntax call declared under `name` (`__js__`, `__cpp__`, ...); null for any other name. */
    public static @Nullable HaxeMethod targetSyntaxMethod(Project project, String name) {
        HaxeMethodModel method = classModel(project, TARGET_SYNTAX_CLASS).getMethod(name, null);
        return method == null ? null : methodOf(method);
    }

    /** Every untyped target-syntax call. */
    public static List<HaxeMethod> targetSyntaxMethods(Project project) {
        return ContainerUtil.map(classModel(project, TARGET_SYNTAX_CLASS).getMethodsSelf(null), HaxeSyntheticDeclarations::methodOf);
    }

    /** The names declared here, which an identifier in user code would shadow. */
    public static Set<String> declaredNames(Project project) {
        Set<String> names = new HashSet<>();
        names.add(TRACE);
        for (HaxeMethod method : targetSyntaxMethods(project)) {
            names.add(method.getName());
        }
        return names;
    }

    /** The values `@:analyzer(...)` accepts. */
    public static HaxeClassModel getAnalyzerOptions(Project project) {
        return classModel(project, ANALYZER_OPTIONS_CLASS);
    }

    private static HaxeMethod methodOf(HaxeMethodModel model) {
        return (HaxeMethod)model.getBasePsi();
    }

    private static HaxeClassModel classModel(Project project, String name) {
        return languageFeaturesModule(project).getClass(name);
    }

    private static HaxeModuleModel languageFeaturesModule(Project project) {
        HaxeModule module = getLanguageFeaturesFile(project).getModule();
        return (HaxeModuleModel)module.getModel();
    }

    private static HaxeFile getLanguageFeaturesFile(Project project) {
        // will ever invalidate, but is stored in project so should unload on project unload
        return CachedValuesManager.getManager(project).getCachedValue(project, LanguageFeatureCache, () -> {
            HaxeFile file = HaxeElementGenerator.createFile(project, "LanguageFeature", FEATURE_DECLARATIONS);
            return CachedValueProvider.Result.create(file, ModificationTracker.NEVER_CHANGED);
        }, false);
    }

}
