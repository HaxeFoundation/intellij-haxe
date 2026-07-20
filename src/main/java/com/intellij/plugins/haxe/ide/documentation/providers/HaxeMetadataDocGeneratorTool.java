package com.intellij.plugins.haxe.ide.documentation.providers;

import org.apache.commons.text.StringEscapeUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

/**
 * Just a utility class to generate the metadata docs file
 */
public class HaxeMetadataDocGeneratorTool {

    record MetadataDoc(String metadata, List<String> arguments, String description, List<String> platforms) {
    }

    public static void main(String[] args) {


        String regex =
                "<tr>" +
                        ".*?(<td>(?<metadata>.*?)</td>).*?" +
                        ".*?(<td>(?<arguments>.*?)</td>).*?" +
                        ".*?(<td>(?<description>.*?)</td>).*?" +
                        ".*?(<td>(?<platforms>.*?)</td>).*?" +
                "</tr>";

        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        String tagReplace = content
                .replaceAll("<code>","`")
                .replaceAll("</code>","`")
                ;
        Matcher matcher = pattern.matcher(tagReplace);

        int count = 0;

        List<MetadataDoc> docList = new ArrayList<>();
        while (matcher.find()) {
            count++;
            String metadata = matcher.group("metadata").replaceAll("`","").trim();
            String arguments = matcher.group("arguments").replaceAll("&lt;","<").replaceAll("&gt;",">").trim();
            String description = matcher.group("description").trim();
            String platforms = matcher.group("platforms").trim();


            String descriptionLinks = description.replaceAll("(?s)<a.*?href=\"(?<url>.*?)\">(?<text>.*?)<\\/a>", "[$2]($1)");
            String descriptionUnescaped = StringEscapeUtils.unescapeHtml4(descriptionLinks);

            System.out.println("Match #" + count);

            System.out.println("Metadata:    " + metadata);
            System.out.println("Arguments:   " + arguments);
            System.out.println("Description: " + description);
            System.out.println("Platforms:   " + platforms);
            System.out.println("------------------------------------");

            List<String> argList = Stream.of(arguments.split(",")).filter(not(String::isEmpty)).toList();
            List<String> platformList = Stream.of(platforms.split(",")).filter(not(String::isEmpty)).toList();
            docList.add(new MetadataDoc(metadata, argList, descriptionUnescaped, platformList));
        }


        if (count == 0) {
            System.out.println("No matches found.");
        }else {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(docList);
            System.out.println(json);
        }
    }

    //FROM https://haxe.org/manual/cr-metadata.html
    public static String content =
            """
                    <table>
                        <thead>
                        <tr>
                            <th>Metadata</th>
                            <th>Arguments</th>
                            <th>Description</th>
                            <th>Platforms</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr>
                            <td><code>@:abi</code></td>
                            <td></td>
                            <td>Function ABI/calling convention.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:abstract</code></td>
                            <td></td>
                            <td>Sets the underlying class implementation as <code>abstract</code>.</td>
                            <td>java, cs</td>
                        </tr>
                        <tr>
                            <td><code>@:access</code></td>
                            <td>&lt;Target path&gt;</td>
                            <td>Forces private access to package, type or field. See <a href="https://haxe.org/manual/lf-access-control.html">lf-access-control</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:allow</code></td>
                            <td>&lt;Target path&gt;</td>
                            <td>Allows private access from package, type or field. See <a href="https://haxe.org/manual/lf-access-control.html">lf-access-control</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:analyzer</code></td>
                            <td></td>
                            <td>Used to configure the static analyzer.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:annotation</code></td>
                            <td></td>
                            <td>Annotation (<code>@interface</code>) definitions on <code>--java-lib</code> imports will be annotated with
                                this metadata. Has no effect on types compiled by Haxe.
                            </td>
                            <td>java</td>
                        </tr>
                        <tr>
                            <td><code>@:arrayAccess</code></td>
                            <td></td>
                            <td>Allows array access on an abstract. See <a href="https://haxe.org/manual/types-abstract-array-access.html">types-abstract-array-access</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:cs.assemblyMeta</code></td>
                            <td></td>
                            <td>Used to declare a native C# assembly attribute</td>
                            <td>cs</td>
                        </tr>
                        <tr>
                            <td><code>@:cs.assemblyStrict</code></td>
                            <td></td>
                            <td>Used to declare a native C# assembly attribute; is type checked</td>
                            <td>cs</td>
                        </tr>
                        <tr>
                            <td><code>@:astSource</code></td>
                            <td></td>
                            <td>Filled by the compiler with the parsed expression of the field.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:autoBuild</code></td>
                            <td>&lt;Build macro call&gt;</td>
                            <td>Extends <code>@:build</code> metadata to all extending and implementing classes. See <a
                                    href="https://haxe.org/manual/macro-auto-build.html">macro-auto-build</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:bind</code></td>
                            <td></td>
                            <td>Override SWF class declaration.</td>
                            <td>flash</td>
                        </tr>
                        <tr>
                            <td><code>@:bitmap</code></td>
                            <td>&lt;Bitmap file path&gt;</td>
                            <td>Embeds given bitmap data into the class (must extend <code>flash.display.BitmapData</code>). See <a
                                    href="https://haxe.org/manual/target-flash-resources.html">target-flash-resources</a>.
                            </td>
                            <td>flash</td>
                        </tr>
                        <tr>
                            <td><code>@:bridgeProperties</code></td>
                            <td></td>
                            <td>Creates native property bridges for all Haxe properties in this class.</td>
                            <td>cs</td>
                        </tr>
                        <tr>
                            <td><code>@:build</code></td>
                            <td>&lt;Build macro call&gt;</td>
                            <td>Builds a class, enum, or abstract from a macro. See <a href="https://haxe.org/manual/macro-type-building.html">macro-type-building</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:buildXml</code></td>
                            <td></td>
                            <td>Specify XML data to be injected into <code>Build.xml</code>.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:bypassAccessor</code></td>
                            <td></td>
                            <td>Do not call property accessor method and access the field directly. See <a
                                    href="https://haxe.org/manual/class-field-property.html">class-field-property</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:callable</code></td>
                            <td></td>
                            <td>Abstract forwards call to its underlying type.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:classCode</code></td>
                            <td></td>
                            <td>Used to inject platform-native code into a class.</td>
                            <td>java, cs</td>
                        </tr>
                        <tr>
                            <td><code>@:commutative</code></td>
                            <td></td>
                            <td>Declares an abstract operator as commutative. See <a
                                    href="https://haxe.org/manual/types-abstract-operator-overloading.html">types-abstract-operator-overloading</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:const</code></td>
                            <td></td>
                            <td>Allows a type parameter to accept expression values.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:coreApi</code></td>
                            <td></td>
                            <td>Identifies this class as a core API class (forces API check).</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:coreType</code></td>
                            <td></td>
                            <td>Identifies an abstract as core type so that it requires no implementation. See <a
                                    href="https://haxe.org/manual/types-abstract-core-type.html">types-abstract-core-type</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:cppFileCode</code></td>
                            <td></td>
                            <td>Code to be injected into generated cpp file.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:cppInclude</code></td>
                            <td></td>
                            <td>File to be included in generated cpp file.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:cppNamespaceCode</code></td>
                            <td></td>
                            <td></td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:cs.using</code></td>
                            <td></td>
                            <td>Add using directives to your module</td>
                            <td>cs</td>
                        </tr>
                        <tr>
                            <td><code>@:dce</code></td>
                            <td></td>
                            <td>Forces dead code elimination even when <code>--dce full</code> is not specified. See <a
                                    href="https://haxe.org/manual/cr-dce.html">cr-dce</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:debug</code></td>
                            <td></td>
                            <td>Forces debug information to be generated into the SWF even without <code>--debug</code>.</td>
                            <td>flash</td>
                        </tr>
                        <tr>
                            <td><code>@:decl</code></td>
                            <td></td>
                            <td></td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:delegate</code></td>
                            <td></td>
                            <td>Automatically added by <code>--net-lib</code> on delegates.</td>
                            <td>cs</td>
                        </tr>
                        <tr>
                            <td><code>@:depend</code></td>
                            <td></td>
                            <td></td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:deprecated</code></td>
                            <td></td>
                            <td>Mark a type or field as deprecated.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:eager</code></td>
                            <td></td>
                            <td>Forces typedefs to be followed early.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:enum</code></td>
                            <td></td>
                            <td>Defines finite value sets to abstract definitions. See <a href="https://haxe.org/manual/types-abstract-enum.html">types-abstract-enum</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:event</code></td>
                            <td></td>
                            <td>Automatically added by <code>--net-lib</code> on events. Has no effect on types compiled by Haxe.</td>
                            <td>cs</td>
                        </tr>
                        <tr>
                            <td><code>@:expose</code></td>
                            <td>&lt;name&gt;</td>
                            <td>Includes the class or field in Haxe exports (default name is the classpath). See <a
                                    href="https://haxe.org/manual/target-javascript-expose.html">target-javascript-expose</a>.
                            </td>
                            <td>js, lua</td>
                        </tr>
                        <tr>
                            <td><code>@:extern</code></td>
                            <td></td>
                            <td>Marks the field as extern so it is not generated.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:file</code></td>
                            <td>&lt;File path&gt;</td>
                            <td>Includes a given binary file into the target SWF and associates it with the class (must extend <code>flash.utils.ByteArray</code>).
                                See <a href="https://haxe.org/manual/target-flash-resources.html">target-flash-resources</a>.
                            </td>
                            <td>flash</td>
                        </tr>
                        <tr>
                            <td><code>@:fileXml</code></td>
                            <td></td>
                            <td>Include a given XML attribute snippet in the <code>Build.xml</code> entry for the file.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:final</code></td>
                            <td></td>
                            <td>Prevents a class or interface from being extended or a method from being overridden. Deprecated by the
                                keyword <code>final</code>. See <a href="https://haxe.org/manual/class-field-final.html">class-field-final</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:fixed</code></td>
                            <td></td>
                            <td>Declares an anonymous object to have fixed fields.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:flash.property</code></td>
                            <td></td>
                            <td></td>
                            <td>flash</td>
                        </tr>
                        <tr>
                            <td><code>@:font</code></td>
                            <td>&lt;TTF path&gt;, &lt;Range String&gt;</td>
                            <td>Embeds the given TrueType font into the class (must extend <code>flash.text.Font</code>). See <a
                                    href="https://haxe.org/manual/target-flash-resources.html">target-flash-resources</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:forward</code></td>
                            <td>&lt;List of field names&gt;</td>
                            <td>Forwards field access to underlying type. See <a href="https://haxe.org/manual/types-abstract-forward.html">types-abstract-forward</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:forward.new</code></td>
                            <td></td>
                            <td>Forwards constructor call to underlying type.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:forwardStatics</code></td>
                            <td>&lt;List of field names&gt;</td>
                            <td>Forwards static field access to underlying type. See <a href="https://haxe.org/manual/types-abstract-forward.html">types-abstract-forward</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:forward.variance</code></td>
                            <td></td>
                            <td>Forwards variance unification to underlying type.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:from</code></td>
                            <td></td>
                            <td>Specifies that the field of the abstract is a cast operation from the type identified in the function. See
                                <a href="https://haxe.org/manual/types-abstract-implicit-casts.html">types-abstract-implicit-casts</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:functionCode</code></td>
                            <td></td>
                            <td>Used to inject platform-native code into a function.</td>
                            <td>cpp, java, cs</td>
                        </tr>
                        <tr>
                            <td><code>@:functionTailCode</code></td>
                            <td></td>
                            <td></td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:generic</code></td>
                            <td></td>
                            <td>Marks a class or class field as generic so each type parameter combination generates its own type/field. See
                                <a href="https://haxe.org/manual/type-system-generic.html">type-system-generic</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:genericBuild</code></td>
                            <td></td>
                            <td>Builds instances of a type using the specified macro.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:genericClassPerMethod</code></td>
                            <td></td>
                            <td>Makes compiler generate separate class per generic static method specialization</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:getter</code></td>
                            <td>&lt;Class field name&gt;</td>
                            <td>Generates a native getter function on the given field.</td>
                            <td>flash</td>
                        </tr>
                        <tr>
                            <td><code>@:hack</code></td>
                            <td></td>
                            <td>Allows extending classes marked as <code>@:final</code>. Not guaranteed to work on all targets.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:headerClassCode</code></td>
                            <td></td>
                            <td>Code to be injected into the generated class, in the header.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:headerCode</code></td>
                            <td></td>
                            <td>Code to be injected into the generated header file.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:headerInclude</code></td>
                            <td></td>
                            <td>File to be included in generated header file.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:headerNamespaceCode</code></td>
                            <td></td>
                            <td></td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:hlNative</code></td>
                            <td></td>
                            <td>Specifies <code>hdll</code> name and function prefix for native functions.</td>
                            <td>hl</td>
                        </tr>
                        <tr>
                            <td><code>@:hxGen</code></td>
                            <td></td>
                            <td>Annotates that an extern class was generated by Haxe.</td>
                            <td>java, cs</td>
                        </tr>
                        <tr>
                            <td><code>@:ifFeature</code></td>
                            <td>&lt;Feature name&gt;</td>
                            <td>Causes a field to be kept by DCE if the given feature is part of the compilation. See <a
                                    href="https://haxe.org/manual/cr-dce.html">cr-dce</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:pythonImport</code></td>
                            <td></td>
                            <td>Generates python import statement for extern classes.</td>
                            <td>python</td>
                        </tr>
                        <tr>
                            <td><code>@:include</code></td>
                            <td></td>
                            <td></td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:inheritDoc</code></td>
                            <td></td>
                            <td>Append documentation from a parent field or class (if used without an argument) or from a specified class or
                                field (if used like @:inheritDoc(pack.Some.field)).
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:inline</code></td>
                            <td></td>
                            <td>Inserted by the parser in case of <code>inline expr</code> and <code>inline function</code>.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:internal</code></td>
                            <td></td>
                            <td>Generates the annotated field/class with 'internal' access.</td>
                            <td>java, cs</td>
                        </tr>
                        <tr>
                            <td><code>@:isVar</code></td>
                            <td></td>
                            <td>Forces a physical field to be generated for properties that otherwise would not require one. See <a
                                    href="https://haxe.org/manual/class-field-property-rules.html">class-field-property-rules</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:javaCanonical</code></td>
                            <td>&lt;Output type package&gt;, &lt;Output type name&gt;</td>
                            <td>Used by the Java target to annotate the canonical path of the type.</td>
                            <td>java</td>
                        </tr>
                        <tr>
                            <td><code>@:java.default</code></td>
                            <td></td>
                            <td>Equivalent to the default modifier of the Java language</td>
                            <td>java</td>
                        </tr>
                        <tr>
                            <td><code>@:jvm.synthetic</code></td>
                            <td></td>
                            <td>Mark generated class, field or method as synthetic</td>
                            <td>java</td>
                        </tr>
                        <tr>
                            <td><code>@:jsRequire</code></td>
                            <td></td>
                            <td>Generate JavaScript module require expression for given extern. See <a
                                    href="https://haxe.org/manual/target-javascript-require.html">target-javascript-require</a>.
                            </td>
                            <td>js</td>
                        </tr>
                        <tr>
                            <td><code>@:luaRequire</code></td>
                            <td></td>
                            <td>Generate Lua module require expression for given extern.</td>
                            <td>lua</td>
                        </tr>
                        <tr>
                            <td><code>@:luaDotMethod</code></td>
                            <td></td>
                            <td>Indicates that the given extern type instance should have dot-style invocation for methods instead of
                                colon.
                            </td>
                            <td>lua</td>
                        </tr>
                        <tr>
                            <td><code>@:keep</code></td>
                            <td></td>
                            <td>Causes a field or type to be kept by DCE. See <a href="https://haxe.org/manual/cr-dce.html">cr-dce</a>.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:keepInit</code></td>
                            <td></td>
                            <td>Causes a class to be kept by DCE even if all its field are removed. See <a
                                    href="https://haxe.org/manual/cr-dce.html">cr-dce</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:keepSub</code></td>
                            <td></td>
                            <td>Extends <code>@:keep</code> metadata to all implementing and extending classes. See <a
                                    href="https://haxe.org/manual/cr-dce.html">cr-dce</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:markup</code></td>
                            <td></td>
                            <td>Used as a result of inline XML parsing. See <a href="lf-markup">lf-markup</a>.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:macro</code></td>
                            <td></td>
                            <td>(deprecated)</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:mergeBlock</code></td>
                            <td></td>
                            <td>Merge the annotated block into the current scope.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:multiReturn</code></td>
                            <td></td>
                            <td>Annotates an extern class as the result of multi-return function. See <a
                                    href="https://haxe.org/manual/target-lua-multireturns.html">target-lua-multireturns</a>.
                            </td>
                            <td>lua</td>
                        </tr>
                        <tr>
                            <td><code>@:multiType</code></td>
                            <td>&lt;Relevant type parameters&gt;</td>
                            <td>Specifies that an abstract chooses its this-type from its <code>@:to</code> functions.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:native</code></td>
                            <td>&lt;Output path&gt;</td>
                            <td>Rewrites the path of a type or class field during generation. See <a href="https://haxe.org/manual/lf-externs.html">lf-externs</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:java.native</code></td>
                            <td></td>
                            <td>Annotates that a function has implementation in native code through JNI.</td>
                            <td>java</td>
                        </tr>
                        <tr>
                            <td><code>@:nativeChildren</code></td>
                            <td></td>
                            <td>Annotates that all children from a type should be treated as if it were an extern definition - platform
                                native.
                            </td>
                            <td>java, cs</td>
                        </tr>
                        <tr>
                            <td><code>@:nativeGen</code></td>
                            <td></td>
                            <td>Annotates that a type should be treated as if it were an extern definition - platform native.</td>
                            <td>java, cs, python</td>
                        </tr>
                        <tr>
                            <td><code>@:nativeProperty</code></td>
                            <td></td>
                            <td>Use native properties which will execute even with dynamic usage.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:nativeStaticExtension</code></td>
                            <td></td>
                            <td>Converts static function syntax into member call.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:noCompletion</code></td>
                            <td></td>
                            <td>Prevents the compiler from suggesting completion on this field or type. See <a
                                    href="https://haxe.org/manual/cr-completion.html">cr-completion</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:noClosure</code></td>
                            <td></td>
                            <td>Prevents a method or all methods in a class from being used as a value.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:noDebug</code></td>
                            <td></td>
                            <td>Does not generate debug information even if <code>--debug</code> is set.</td>
                            <td>flash, cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:noDoc</code></td>
                            <td></td>
                            <td>Prevents a type or field from being included in documentation generation.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:noImportGlobal</code></td>
                            <td></td>
                            <td>Prevents a static field from being imported with <code>import Class.*</code>.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:nonVirtual</code></td>
                            <td></td>
                            <td>Declares function to be non-virtual in cpp.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:noPrivateAccess</code></td>
                            <td></td>
                            <td>Disallow private access to anything for the annotated expression.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:noStack</code></td>
                            <td></td>
                            <td></td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:notNull</code></td>
                            <td></td>
                            <td>Declares an abstract type as not accepting null values. See <a href="https://haxe.org/manual/types-nullability.html">types-nullability</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:noUsing</code></td>
                            <td></td>
                            <td>Prevents a field from being used with static extension. See <a href="https://haxe.org/manual/lf-static-extension.html">lf-static-extension</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:nullSafety</code></td>
                            <td>&lt;Off | Loose | Strict | StrictThreaded&gt;</td>
                            <td>Enables null safety for classes or fields. Disables null safety for classes, fields or expressions if
                                provided with <code>Off</code> as an argument. See <a href="https://haxe.org/manual/cr-null-safety.html">cr-null-safety</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:objc</code></td>
                            <td></td>
                            <td>Declares a class or interface that is used to interoperate with Objective-C code.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:objcProtocol</code></td>
                            <td></td>
                            <td>Associates an interface with, or describes a function in, a native Objective-C protocol.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:op</code></td>
                            <td>&lt;The operation&gt;</td>
                            <td>Declares an abstract field as being an operator overload. See <a
                                    href="https://haxe.org/manual/types-abstract-operator-overloading.html">types-abstract-operator-overloading</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:optional</code></td>
                            <td></td>
                            <td>Marks the field of a structure as optional. See <a href="https://haxe.org/manual/types-nullability-optional-arguments.html">types-nullability-optional-arguments</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:overload</code></td>
                            <td>&lt;Function specification (no expression)&gt;</td>
                            <td>Allows the field to be called with different argument types. See <a
                                    href="https://haxe.org/manual/target-javascript-external-libraries.html">target-javascript-external-libraries</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:persistent</code></td>
                            <td></td>
                            <td>Keeps the value of static variables in macro context across compilations.</td>
                            <td>eval</td>
                        </tr>
                        <tr>
                            <td><code>@:php.attribute</code></td>
                            <td></td>
                            <td>Adds a PHP attribute to the annotated symbol. Meta argument expects a string constant. E.g. <code>@:php.attribute('\\\\my\\\\Attr(123)')</code>
                                will be generated as <code>#[\\my\\Attr(123)]</code> in the compiled php file.
                            </td>
                            <td>php</td>
                        </tr>
                        <tr>
                            <td><code>@:phpGlobal</code></td>
                            <td></td>
                            <td>Indicates that static fields of an extern class actually are located in the global PHP namespace.</td>
                            <td>php</td>
                        </tr>
                        <tr>
                            <td><code>@:phpClassConst</code></td>
                            <td></td>
                            <td>Indicates that a static var of an extern class is a PHP class constant.</td>
                            <td>php</td>
                        </tr>
                        <tr>
                            <td><code>@:phpMagic</code></td>
                            <td></td>
                            <td>Treat annotated field as special PHP magic field - this meta makes compiler avoid renaming such fields on
                                generating PHP code.
                            </td>
                            <td>php</td>
                        </tr>
                        <tr>
                            <td><code>@:phpNoConstructor</code></td>
                            <td></td>
                            <td>Special meta for extern classes which do not have native constructor in PHP, but need a constructor in Haxe
                                extern.
                            </td>
                            <td>php</td>
                        </tr>
                        <tr>
                            <td><code>@:pos</code></td>
                            <td>&lt;Position&gt;</td>
                            <td>Sets the position of a reified expression. See <a
                                    href="https://haxe.org/manual/macro-reification.html">macro-reification</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:publicFields</code></td>
                            <td></td>
                            <td>Forces all class fields of inheriting classes to be public.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:private</code></td>
                            <td></td>
                            <td>Marks a class field as being private.</td>
                            <td>cs</td>
                        </tr>
                        <tr>
                            <td><code>@:privateAccess</code></td>
                            <td></td>
                            <td>Allow private access to anything for the annotated expression.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:protected</code></td>
                            <td></td>
                            <td>Marks a class field as being protected.</td>
                            <td>cs, java, flash</td>
                        </tr>
                        <tr>
                            <td><code>@:property</code></td>
                            <td></td>
                            <td>Marks a field to be compiled as a native C# property.</td>
                            <td>cs</td>
                        </tr>
                        <tr>
                            <td><code>@:pure</code></td>
                            <td></td>
                            <td>Marks a class field, class or expression as pure (side-effect free).</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:readOnly</code></td>
                            <td></td>
                            <td>Generates a field with the <code>readonly</code> native keyword.</td>
                            <td>cs</td>
                        </tr>
                        <tr>
                            <td><code>@:remove</code></td>
                            <td></td>
                            <td>Causes an interface to be removed from all implementing classes before generation.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:require</code></td>
                            <td>&lt;Compiler flag to check&gt;</td>
                            <td>Allows access to a field only if the specified compiler flag is set. See <a
                                    href="https://haxe.org/manual/lf-condition-compilation.html">lf-condition-compilation</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:resolve</code></td>
                            <td></td>
                            <td>Abstract fields marked with this metadata can be used to resolve unknown fields.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:rtti</code></td>
                            <td></td>
                            <td>Adds runtime type information. See <a href="https://haxe.org/manual/cr-rtti.html">cr-rtti</a>.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:runtimeValue</code></td>
                            <td></td>
                            <td>Marks an abstract as being a runtime value.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:scalar</code></td>
                            <td></td>
                            <td>Used by hxcpp to mark a custom coreType abstract.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:selfCall</code></td>
                            <td></td>
                            <td>Translates method calls into calling object directly. See <a
                                    href="https://haxe.org/manual/target-javascript-external-libraries.html">target-javascript-external-libraries</a>.
                            </td>
                            <td>js, lua</td>
                        </tr>
                        <tr>
                            <td><code>@:semantics</code></td>
                            <td>&lt;value | reference | variable&gt;</td>
                            <td>The native semantics of the type.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:setter</code></td>
                            <td>&lt;Class field name&gt;</td>
                            <td>Generates a native setter function on the given field.</td>
                            <td>flash</td>
                        </tr>
                        <tr>
                            <td><code>@:sound</code></td>
                            <td>&lt;File path&gt;</td>
                            <td>Includes a given .wav or .mp3 file into the target SWF and associates it with the class (must extend <code>flash.media.Sound</code>).
                                See <a href="https://haxe.org/manual/target-flash-resources.html">target-flash-resources</a>.
                            </td>
                            <td>flash</td>
                        </tr>
                        <tr>
                            <td><code>@:sourceFile</code></td>
                            <td></td>
                            <td>Source code filename for external class.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:stackOnly</code></td>
                            <td></td>
                            <td>Instances of this type can only appear on the stack.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:strict</code></td>
                            <td></td>
                            <td>Used to declare a native C# attribute or a native Java metadata; is type checked.</td>
                            <td>java, cs</td>
                        </tr>
                        <tr>
                            <td><code>@:struct</code></td>
                            <td></td>
                            <td>Marks a class definition as a struct.</td>
                            <td>cs, hl</td>
                        </tr>
                        <tr>
                            <td><code>@:structAccess</code></td>
                            <td></td>
                            <td>Marks an extern class as using struct access (<code>.</code>) not pointer (<code>-&gt;</code>).</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:structInit</code></td>
                            <td></td>
                            <td>Allows one to initialize the class with a structure that matches constructor parameters.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:suppressWarnings</code></td>
                            <td></td>
                            <td>Adds a <code>SuppressWarnings</code> annotation for the generated Java class.</td>
                            <td>java</td>
                        </tr>
                        <tr>
                            <td><code>@:templatedCall</code></td>
                            <td></td>
                            <td>Indicates that the first parameter of static call should be treated as a template argument.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:throws</code></td>
                            <td>&lt;Type as String&gt;</td>
                            <td>Adds a <code>throws</code> declaration to the generated function.</td>
                            <td>java</td>
                        </tr>
                        <tr>
                            <td><code>@:to</code></td>
                            <td></td>
                            <td>Specifies that the field of the abstract is a cast operation to the type identified in the function. See <a
                                    href="https://haxe.org/manual/types-abstract-implicit-casts.html">types-abstract-implicit-casts</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:transient</code></td>
                            <td></td>
                            <td>Adds the <code>transient</code> flag to the class field.</td>
                            <td>java</td>
                        </tr>
                        <tr>
                            <td><code>@:transitive</code></td>
                            <td></td>
                            <td>Allows transitive casts with an abstract.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:volatile</code></td>
                            <td></td>
                            <td></td>
                            <td>java, cs</td>
                        </tr>
                        <tr>
                            <td><code>@:unifyMinDynamic</code></td>
                            <td></td>
                            <td>Allows a collection of types to unify to <code>Dynamic</code>.</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:unreflective</code></td>
                            <td></td>
                            <td></td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:unsafe</code></td>
                            <td></td>
                            <td>Declares a class, or a method with the C#'s <code>unsafe</code> flag.</td>
                            <td>cs</td>
                        </tr>
                        <tr>
                            <td><code>@:using</code></td>
                            <td></td>
                            <td>Automatically uses the argument types as static extensions for the annotated type. See <a
                                    href="https://haxe.org/manual/lf-static-extension-metadata.html">lf-static-extension-metadata</a>.
                            </td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:haxe.warning</code></td>
                            <td></td>
                            <td>Modifies warning options, equivalent to the -w CLI argument</td>
                            <td>all</td>
                        </tr>
                        <tr>
                            <td><code>@:void</code></td>
                            <td></td>
                            <td>Use Cpp native <code>void</code> return type.</td>
                            <td>cpp</td>
                        </tr>
                        <tr>
                            <td><code>@:nativeArrayAccess</code></td>
                            <td></td>
                            <td>When used on an extern class which implements haxe.ArrayAccess native array access syntax will be
                                generated
                            </td>
                            <td>cpp</td>
                        </tr>
                        </tbody>
                    </table>
                    """;
}