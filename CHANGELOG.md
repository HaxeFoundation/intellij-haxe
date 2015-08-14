#Changelog
  <p>
     Warning: This plugin will NOT work correctly using IDEA version 14.1.2. It works well with
     versions 14.1 and 14.1.1. Version 14.1.3 is still being tested.
  </p>
  <p>0.9.5: (community release)</p>
  <ul>
    <li>Add new typing support for type checking and completion. (Thanks to Carlos Ballesteros!) (Issues #288,#291,#308,#317)</li>
    <li>Support static extensions in completion. (Again, Thanks, Carlos!)<li>
    <li>Fix debugger getting stuck "collecting data" for some variables (particularly, "this"). (Issue #325)</li>
    <li>Better compiler error highlighting. (Issue #180 redux.)</li>
    <li>Fix Cut/Copy/Paste buffer inconsistencies (Issue #196)</li>
    <li>Add generics support. (First level only, chained sequences remain incomplete.)</li>
    <li>Allow object literals as return statements. (Issue #278)</li>
    <li>Fix NPE during annotation, causing annotation to stop. (Issue #316)</li>
  </ul>
  <p>0.9.4: (community release)</p>
  <ul>
    <li>Fix compile error highlighting in the output pane and jumping to source location when an error is clicked upon (Issues #129, #160, #180).</li>
    <li>Fix debugger execution under IDEA 14 and 14.1.</li>
    <li>Fix move package</li>
    <li>Fix MoveFile showing "unimplemented" message. (Issues #222, #88)</li>
    <li>Fix copy/paste clipboard functionality.</li>
    <li>Show completion for all static members (Issue #262).</li>
    <li>All unit tests enabled and passing for IDEA versions 13.1, 14.0, and 14.1.1.</li>
    <li>Fix rename not updating all usages (Issue #222)</li>
    <li>Fix parameter info tool tips and code tips.</li>
    <li>Command line ant builds (of the plugin) for automated testing.</li>
    <li>Fix parsing 'new' in ternary expressions (Issue #229).</li>
    <li>Better handling of comments.</li>
    <li>Fix member visibility scoping issues with extern and private keywords.</li>
    <li>Stop generating 'public' and 'private' modifiers when generating getter/setters.</li>
    <li>Stop treating interfaces and extern class declarations identically.</li>
    <li>Disallow multiple variables being declared in one statement for class fields.</li>
    <li>Print compiler commands to the message pane along with command output.</li>
    <li>Fix hang when using the OpenFL compiler for variable and method completion.</li>
    <li>Use correct completion contributor for OpenFL project configurations.</li>
    <li>Fix parsing failures for certain cases of "@meta" and "@:pos" (Issue #81).</li>
    <li>Fix unresolved type error if using full class path without importing the class (Issue #39).</li>
    <li>Resolve extern enum values via qualified name.</li>
    <li>Resolve classes within the same package but defined in a different module (Issue #168).</li>
    <li>Hopefully fix compiler based auto-complete performance problems (Issue #230).</li>
    <li>Fix Plugin wrongly accepting comma separated fields that the compiler wont (Issue #83).</li>
    <li>Fix rare ClassCastException when re-opening projects.</li>
    <li>Fix NotNullExceptions when getting field types for dynamic fields.</li>
  </ul>
  <p>0.9.3: (community release)</p>
  <ul>
    <li>Fix local variable name suggestions to not clash with existing class fields.</li>
    <li>Fix Introduce Variable refactoring to find all occurrences of the selected expression.</li>
    <li>
      No longer block Java (and other) tests from running when Haxe
      plugin is installed. (Issue #166)
    </li>
    <li>
      Resolve static function imports for import with in keyword.
      ("import String.fromCharCode in f;")  (Issue #191)
    </li>
    <li>
      Give extern fields public visibility: 'function a()' will be treated
      as 'public function a()' and will appear in completions.
    </li>
    <li>Fix (un)comment multiple lines of code feature.  (Issue #209)</li>
    <li>Support 'as' keyword in import statements.</li>
    <li>Implemented Refactoring: Pull Members Up/Push Members Down</li>
    <li>Support extern interfaces. (Issue #202)</li>
    <li>Fix visibility determination for methods. (Better completions)</li>
    <li>Check for duplicate imports when copy/pasting.</li>
    <li>
      Fix resolving classes that appear inside of an import file with a
      different name than the class itself.  Fixes goto declaration as well.
    </li>
    <li>Fix colorizing identifiers (variable names) in code.</li>
    <li>Fix Issue 162: "call(new x(), new x());" parse failure.</li>
    <li>(Re)Allow "new" for extern and prototype function declarations.</li>
    <li>Fixed IDEA freeze when XML is edited</li>
    <li>Implemented Refactoring: Extract Superclass</li>
    <li>Implemented Refactoring: Extract Interface</li>
    <li>Implemented Refactoring: Push Members Down</li>
    <li>Fixed OutOfBoundsException when resolving names.</li>
    <li>Fix most unit tests.</li>
  </ul>
  <p>0.9.2: (community release, IDEA 14 only)</p>
  <ul>
    <li>Fixed: HaxeReferenceCopyPasteProcessor issue preventing from using copy paste clipboard functionality</li>
  </ul>
  <p>0.9: (community release)</p>
  <ul>
    <li>Release ID change only</li>
  </ul>
  <p>0.8.1.1.TiVo.4: (community version, TiVo Release 4)</p>
  <ul>
    <li>Class Hierarchy view panels implemented. (Menu->Navigate->Type Hierarchy, et al)</li>
    <li>Better handling of import files.</li>
    <li>Better handling of Haxe language parsing, including many Haxe 3 features.</li>
    <li>Automatic detection and use of installed haxe libraries (using the 'haxelib' command).</li>
    <li>Better completion (Ctrl-space) using the Haxe compiler -- OpenFL projects only.</li>
    <li>Refactorings:
      <ul>
        <li>Pull up members from class to super-class</li>
        <li>Pull up members from class to interface</li>
        <li>Split into declaration and assignment</li>
        <li>Optimize imports</li>
      </ul>
    </li>
    <p> The following sub-releases are included:</p>
    <ul>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.16: (community version, TiVo RC5)</p>
      <ul>
        <li>Refactoring: Pull up members from class to super-class</li>
        <li>Refactoring: Pull up members from class to interface</li>
        <li>Launch Haxe/Neko tests (Patch #131)</li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.15: (community version, TiVo RC4)</p>
      <ul>
        <li>Fixed issue 37 (Parser doesn't recover after new A)</li>
        <li>Fixed issue 95 (Local and class variable names resolving to similar package names)</li>
        <li>Fixed issue 132 (incorrect processing of duplicate imports)</li>
        <li>Fixed issue 134 (incorrect reformat of object and array children)</li>
        <li>Fixed reference resolution for expressions in parenthesis - otherwise, code assist does not work for those.</li>
        <li>Fixed: launching test with neko, overriding haxe build parameters for test run configuration, filtering test result output, compilation path of non test build, line number for ErrorFilter; and removed hard-coded path for ErrorFilter</li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.14: (community version, TiVo RC3)</p>
      <ul>
        <li>Fixed NPE causing the structure view to not populate, resulting from an errant merge.</li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.13: (community version, TiVo RC2)</p>
      <ul>
        <li>Resolve 'convenience' imports that do not export a class named similarly to the file. (TiVo Issue #55)</li>
        <li>Update unbalanced preprocessor token highlighting and detection.</li>
        <li>Improve indentation of comments and preprocessor macros.</li>
        <li>Update for Grammar-Kit 1.2.0.1 </li>
        <li>Fixed syntax rules (BNF) for constructors and external functions.</li>
        <li>Fixed syntax rules (BNF) for code blocks; removed them from being valid syntax everywhere an expression can appear.</li>
        <li>Fixed syntax rules (BNF) to allow meta tags on typedefs.</li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.12: (community version, TiVo RC1+Fixes)</p>
      <ul>
        <li>Auto-indent when adding curly brackets now works correctly. Fixes github tivo/intellij-haxe Issue #119. (Thanks, Jérémy!)</li>
        <li>Fix IDE hang on completion for Haxe compiler completions.</li>
        <li>Fix auto-adding new import statements above package declaration and/or comments.</li>
        <li>Fix NPE when manually adding new import statements.</li>
        <li>Put debugging dialogs on the UI thread.</li>
        <li>Fix ArrayOutOfBounds exception when initializing haxelib cache.</li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.11: (community version, TiVo RC1)</p>
      <ul>
        <li>Fix NPE when colorizing.</li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.10: (community version, TiVo WIP)</p>
      <ul>
        <li> Added timeout to long-running call hierarchy searches. </li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.9: (community version, TiVo WIP)</p>
      <ul>
        <li> Fixed Haxe command-line debugger integration for OpenFL projects that
             are targetting C++ native runtime environments.
        </li>
        <li> Fixed method hierarchy runtime exceptions, and auto-scrolling to source. </li>
        <li> Fixed type hierarchy auto-scrolling to source. </li>
        <li> Enhanced run & debug output to be color-coded for improved readability. </li>
        <li> Fixed find-usages regression. </li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.8: (community version, TiVo WIP)</p>
      <ul>
        <li> More load-time optimizations using new 'haxelib list-path' command. </li>
        <li> Add package and file names to Type hierarchy window.  (File names only
             display if the file name differs from the type name.)
        </li>
        <li> Fixed supertypes list in the combo view of the Type hierarchy window. </li>
        <li> Allow block statements everywhere. </li>
        <li> Allow array literals to have additional comma [1,] </li>
        <li> Moving a file from one package to another no longer displays "Unimplemented"
             and now moves the file, however references are not yet updated.
             Issue #88 -- still unresolved.
        </li>
        <li> Updated unit tests. Issues: #71, #68.</li>
        <li> Fix formatting for ">=", which is used be to reformatted to "> =". Issue </li>
        <li> Fix logic for HaxeIfSurrounder.java /testIf test case/ </li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.7: (community version, TiVo WIP)</p>
      <ul>
        <li>Repaired resolving references to classes and variables.</li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.6: (community version, TiVo WIP)</p>
      <ul>
        <li>Further optimized load time for large projects.</li>
        <li>Run haxelib->Project/SDK/Module library dependency synchronization
            in the background.
        </li>
        <li>HXML completion: add parameters for compiler argument to
            presentable text of completion item
        </li>
        <li>Completion from Haxe compiler: parse function parameters and
            return type to generate completion item with parameters and return
            type
        </li>
        <li>Completion from Haxe compiler: format data from compiler replace
            "&lt;" to "<" and "&gt;" to ">"
        </li>
        <li>HaxeReferenceImpl.java getVariants(completion): Handle case when
            "var d:Array<Int> = []; d.|" when d is not resolved
        </li>
        <li>Add description to completion recived from Haxe compiler:
            HaxeMetaTagsCompletionContributor.java
            HXMLDefineCompletionContributor.java
            HXMLCompilerArgumentsCompletionContributor.java
        </li>
        <li>Preliminary Haxe compiler completion support (OpenFL only)</li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.5: (community version, TiVo WIP)</p>
      <ul>
        <li>Decreased time to load large projects considerably.
            Note that project loading is still on the UI thread, so
            it may appear to lock up for a short period of time.
            For very large projects, 90 seconds is not out of the ordinary.
        </li>
        <li>HXML completion: Provide available libraries list</li>
        <li>HXML completion: show installed haxelibs(also installed libs removed from available haxelibs list)</li>
        <li>Fix meta tag parsing issues</li>
        <li>HaxeMetaTagsCompletionContributor provides completion for meta tags</li>
        <li>Project Xml(NME, OpenFL project project) completion: show available and installed haxelibs</li>
        <li>SplitIntoDeclarationAndAssignment intention action</li>
      </ul>
    </li>
    <li>
      <p>0.8.1.1.TiVo.ClassHierarchy.4: (community version, TiVo WIP)</p>
      <ul>
        <li>Merged with version 0.8.1.1.TiVo.2 from the TiVo/master branch.</li>
        <li>Class Hierarchy partial implementation.</li>
        <li>SuperTypes work.  Sub-types work within the same module.</li>
        <li>All recent changes from github.com/Jetbrains/intellij-haxe/master</li>
        <li>Support typedef optional parameters</li>
        <li>Support optional function types</li>
        <li>Eat compile-time conditional statements only (prevent eating conditional body as it was before)</li>
        <li>Fix multiple metas issue on class</li>
        <li>Highlight compile-time conditional statements if they don't have matching closing statements</li>
        <li>Remove "from" and "to" from keywords, instead highlight them only if they used in abstract declaration</li>
        <li>Prevent suggesting imports for using statements</li>
        <li>Resolve references that have full path to type/field</li>
        <li>Support function types, anonymous types as abstract type</li>
        <li>Automatically add and remove dependencies when project gets opened</li>
        <li>Remove ">=" and ">>=" tokens from lexer, instead parse ('>' '=') to avoid issues(https://github.com/TiVo/intellij-haxe/issues/42)</li>
        <li>Support "inline" declaration attribute on local functions</li>
        <li>Suggest to import class on code paste</li>
        <li>Support macro expressions(including ECheckType)</li>
        <li>Lots more... TODO: Get a complete list of updates.</li>
      </ul>
    </li>
    </ul>
  </ul>
  <p>0.8.1.1.TiVo.2: (TiVo version)</p>
   <ul>
    <li>openFL path can now be retrieved from an .iml file</li>
   </ul>
  <p>0.8.1.1: (community version)</p>
   <ul>
    <li>"Find usages in project" fixed.</li>
    <li>Allowed @:final on methods and fields.</li>
    <li>Re-implemented hxcpp debugger support to work with Haxe v3 built-in debugger</li>
   </ul>
  <p>0.8.1: (community version)</p>
   <ul>
    <li>Remove com.intellij.modules.java from dependencies list to make plugin work in PHPStorm(and other IntelliJ IDEA platform-based IDEs)</li>
   </ul>
  <p>0.8: (community version)</p>
   <ul>
    <li>Migration to new IntelliJ IDEA 13.1 API</li>
    <li>HXML syntax highlighting</li>
    <li>HXML completion</li>
    <li>Parser support for different types of imports</li>
    <li>Parser support for @:jsRequire and more parser fixes</li>
   </ul>
  <p>0.7.2: (community version)</p>
   <ul>
    <li>New version number</li>
    <li>basic hxml support</li>
    <li>@:jsRequire meta support</li>
    <li>Haxe grammar: @:jsRequire and macro support</li>
    <li>templates naming fix ("create new class/enum/interface" issue)</li>
    <li>new/get/set/never keywords, get/set identifiers are valid, jar build</li>
   </ul>
  <p>0.7.1:</p>
   <ul>
    <li>Bug fixes for  13.1.1</li>
   </ul>
  <p>0.7:</p>
   <ul>
    <li>Bug fixes</li>
   </ul>
  <p>0.6.9:</p>
   <ul>
    <li>Neko target for OpenFL</li>
    <li>Bug fixes</li>
   </ul>
  <p>0.6.5:</p>
   <ul>
    <li>OpenFL support</li>
   </ul>
  <p>0.6.4:</p>
   <ul>
    <li>Optimize imports</li>
   </ul>
  <p>0.6.3:</p>
   <ul>
    <li>Parser improvements</li>
   </ul>
  <p>0.6.2:</p>
   <ul>
    <li>Bug fixes</li>
   </ul>
  <p>0.6.1:</p>
   <ul>
    <li>Haxe 3 support</li>
   </ul>
  <p>0.6:</p>
   <ul>
    <li>Folding</li>
   </ul>
  <p>0.5.8:</p>
   <ul>
    <li>Bug fixes</li>
   </ul>
  <p>0.5.6:</p>
   <ul>
    <li>NME support improvements</li>
    <li>HXCPP debugger improvements</li>
   </ul>
  <p>0.5.5:</p>
   <ul>
    <li>Bug fixes</li>
   </ul>
  <p>0.5.4:</p>
   <ul>
    <li>New Compiler Mode</li>
   </ul>
  <p>0.5.2:</p>
   <ul>
    <li>Bug fixes</li>
   </ul>
  <p>0.5.1:</p>
   <ul>
    <li>Bug fixes</li>
   </ul>
  <p>0.5:</p>
   <ul>
    <li>HXCPP Debugging</li>
    <li>Bug fixes</li>
   </ul>
  <p>0.4.7:</p>
   <ul>
    <li>Introduce Variable Refactoring</li>
    <li>Using Completion</li>
    <li>Bug fixes</li>
   </ul>
  <p>0.4.6:</p>
   <ul>
    <li>Conditional Compilation Support</li>
    <li>Bug fixes</li>
   </ul>
  <p>0.4.5:</p>
   <ul>
    <li>Live Templates</li>
    <li>Surround With Action</li>
    <li>Smart completion</li>
    <li>Goto Test Action</li>
   </ul>
  <p>0.4.4:</p>
   <ul>
    <li>Bug fixes</li>
    <li>EReg support</li>
   </ul>
  <p>0.4.3:</p>
   <ul>
    <li>Bug fixes</li>
    <li>Structure view</li>
   </ul>
  <p>0.4.1:</p>
   <ul>
    <li>Bug fixes</li>
    <li>Unresolved type inspection</li>
   </ul>
  <p>0.4:</p>
   <ul>
    <li>NME Support</li>
    <li>Override/Implement method action</li>
    <li>Generate getter/setter action</li>
    <li>Parameter info action</li>
   </ul>
  <p>0.3:</p>
   <ul>
    <li>Type resolving improvements</li>
    <li>Goto Implementation(s) action</li>
    <li>Goto Super Method action</li>
    <li>Move refactoring</li>
   </ul>
  <p>0.2.3:</p>
   <ul>
    <li>Completion fixes</li>
   </ul>
  <p>0.2.2:</p>
   <ul>
    <li>Type resolving improvements</li>
    <li>Rename refactoring</li>
    <li>NMML scheme</li>
    <li>HXML support</li>
   </ul>
  <p>0.2.1:</p>
   <ul>
    <li>Type resolving improvements</li>
    <li>Documentation support</li>
    <li>New color settings</li>
   </ul>
  <p>0.2:</p>
   <ul>
    <li>Jump to declaration of local, std symbol or class</li>
    <li>Reference completion</li>
    <li>Class completion</li>
    <li>Color settings</li>
    <li>Code formatter</li>
    <li>Go to Class</li>
    <li>Icons for Haxe files</li>
    <li>Search for usages</li>
    <li>Highlight symbol occurencies</li>
    <li>Debugger for Flash target ("Flash/Flex Support" plugin required)</li>
   </ul>
  <p>0.1:</p>
   <ul>
    <li>Haxe module and SDK</li>
    <li>Parsing Haxe files</li>
    <li>Keyword completion</li>
    <li>Compile Haxe files and run in Neko VM</li>
   </ul>

