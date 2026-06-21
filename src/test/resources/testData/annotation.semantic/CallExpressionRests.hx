package ;

class CallExpressionTest {

    function restArgs(arg1:String, extra:haxe.Rest<Dynamic>) {}
    function externRestArgs(arg1:String, extra:haxe.extern.Rest<Dynamic>) {}
    function restSyntaxArgs(arg1:String, ...extra:Dynamic) {}

    public function testDynamic<T,Q>(p1:T, p2:Q) {

        // haxe.Rest<Dynamic>
        restArgs("hello");                                              // legal: no extra args
        restArgs("hello", "world");                                     // legal: one extra String
        restArgs("hello", "world", 42);                                 // legal: extra String and Int
        restArgs("hello", 1, 2, 3);                                     // legal: multiple Int extras
        restArgs("hello", p1, p2);                                      // legal: accepts typeParameters
        // illegal: Int as first arg (wrong type)
        restArgs(<error descr="Type mismatch (Expected: 'String' got: 'Int')">42</error>, "world");

        // haxe.extern.Rest<Dynamic>
        externRestArgs("hello");                                        // legal: no extra args
        externRestArgs("hello", "world");                               // legal: one extra String
        externRestArgs("hello", "world", 42);                           // legal: extra String and Int
        externRestArgs("hello", 1, 2, 3);                               // legal: multiple Int extras
        externRestArgs("hello", p1, p2);                                // legal: accepts typeParameters
        // illegal: Int as first arg (wrong type)
        externRestArgs(<error descr="Type mismatch (Expected: 'String' got: 'Int')">42</error>, "world");

        // ...extra:Dynamic
        restSyntaxArgs("hello");                                   // legal: no extra args
        restSyntaxArgs("hello", "world");                          // legal: one extra String
        restSyntaxArgs("hello", "world", 42);                      // legal: extra String and Int
        restSyntaxArgs("hello", 1, 2, 3);                          // legal: multiple Int extras
        restSyntaxArgs("hello", p1, p2);                           // legal: accepts typeParameters
        // illegal: Int as first arg (wrong type)
        restSyntaxArgs(<error descr="Type mismatch (Expected: 'String' got: 'Int')">42</error>, "world");


        var strArray:Array<String> = ["a", "b"];
        var intArray:Array<Int> = [1, 2, 3];

        // passing array and using spread operator
        restArgs("hello", strArray);                              // legal: passing array as type
        restArgs("hello", ...strArray);                           // legal: spread String array
        restArgs("hello", ...intArray);                           // legal: spread Int array (Dynamic accepts all)

        externRestArgs("hello", strArray);                        // legal: passing array as type
        externRestArgs("hello", ...strArray);                     // legal: spread String array
        externRestArgs("hello", ...intArray);                     // legal: spread Int array (Dynamic accepts all)

        restSyntaxArgs("hello", strArray);                         // legal: passing array
        restSyntaxArgs("hello", ...strArray);                      // legal: spread String array
        restSyntaxArgs("hello", ...intArray);                      // legal: spread Int array (Dynamic accepts all)
    }


    function strictRestArgs(arg1:String, extra:haxe.Rest<Int>) {}
    function strictExternRestArgs(arg1:String, extra:haxe.extern.Rest<Int>) {}
    function strictRestSyntaxArgs(arg1:String, ...extra:Int) {}

    public function testStrict<T:Int,Q:String>(p1:T, p2:Q) {

        // haxe.Rest<Int>
        strictRestArgs("hello");                                                               // legal: no extra args
        strictRestArgs("hello", 1);                                                            // legal: one Int
        strictRestArgs("hello", 1, 2, 3);                                                      // legal: multiple Ints
        strictRestArgs("hello", p1);                                                           // legal: T:Int satisfies Int constraint

        strictRestArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'String')">"world"</error>);     // illegal: String as rest arg
        strictRestArgs("hello", 1, <error descr="Type mismatch (Expected: 'Int' got: 'String')">"world"</error>);  // illegal: String mixed in rest args
        strictRestArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'String')">p2</error>);          // illegal: Q:String does not satisfy Int constraint

        // haxe.extern.Rest<Int>
        strictExternRestArgs("hello");                                                       // legal: no extra args
        strictExternRestArgs("hello", 1);                                                    // legal: one Int
        strictExternRestArgs("hello", 1, 2, 3);                                              // legal: multiple Ints
        strictExternRestArgs("hello", p1);                                                   // legal: T:Int satisfies Int constraint

        strictExternRestArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'String')">"world"</error>);                   // illegal: String as rest arg
        strictExternRestArgs("hello", 1, <error descr="Type mismatch (Expected: 'Int' got: 'String')">"world"</error>);                // illegal: String mixed in rest args
        strictExternRestArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'String')">p2</error>);                        // illegal: Q:String does not satisfy Int constraint

        // ...extra:Int
        strictRestSyntaxArgs("hello");                                 // legal: no extra args
        strictRestSyntaxArgs("hello", 1);                              // legal: one Int
        strictRestSyntaxArgs("hello", 1, 2, 3);                        // legal: multiple Ints
        strictRestSyntaxArgs("hello", p1);                             // legal: T:Int satisfies Int constraint

        strictRestSyntaxArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'String')">"world"</error>);                        // illegal: String as rest arg
        strictRestSyntaxArgs("hello", 1, <error descr="Type mismatch (Expected: 'Int' got: 'String')">"world"</error>);                     // illegal: String mixed in rest args
        strictRestSyntaxArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'String')">p2</error>);                             // illegal: Q:String does not satisfy Int constraint

        var intArray:Array<Int> = [1, 2, 3];
        var strArray:Array<String> = ["a", "b"];

        // passing array and using spread operator
        strictRestArgs("hello", ...intArray);                                                                             // legal: spread Int array

        strictRestArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'String')">...strArray</error>);        // illegal: spread String array
        strictRestArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'Array<Int>')">intArray</error>);       // illegal: wrong type
        strictRestArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'Array<String>')">strArray</error>);    // illegal: wrong type

        strictExternRestArgs("hello", ...intArray);                                                                             // legal: spread Int array

        strictExternRestArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'String')">...strArray</error>);        // illegal: spread String array
        strictExternRestArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'Array<Int>')">intArray</error>);       // illegal: wrong type
        strictExternRestArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'Array<String>')">strArray</error>);    // illegal: wrong type

        strictRestSyntaxArgs("hello", ...intArray);                                                                             // legal: spread Int array

        strictRestSyntaxArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'String')">...strArray</error>);        // illegal: spread String array
        strictRestSyntaxArgs("hello",<error descr="Type mismatch (Expected: 'Int' got: 'Array<Int>')">intArray</error>);        // illegal: wrong type
        strictRestSyntaxArgs("hello", <error descr="Type mismatch (Expected: 'Int' got: 'Array<String>')">strArray</error>);    // illegal: wrong type
    }

    function genericRestArgs<T,Q>(arg1:T, extra:haxe.Rest<Q>) {}
    function genericExternRestArgs<T,Q>(arg1:T, extra:haxe.extern.Rest<Q>) {}
    function genericRestSyntaxArgs<T,Q>(arg1:T, ...extra:Q) {}

    public function genericStrict() {

        genericRestArgs("hello", 1);                                                           // legal: Q inferred as Int
        genericRestArgs("hello", 1, 2, 3);                                                     // legal: all Ints, Q stays Int
        genericRestArgs("hello", "world");                                                     // legal: Q inferred as String
        genericRestArgs("hello", "world", "again");                                            // legal: all Strings, Q stays String

        genericRestArgs("hello", 1, <error descr="Type mismatch (Expected: 'Int' got: 'String')">"world"</error>);  // illegal: Q monomorphed to Int, String not allowed
        genericRestArgs("hello", "world", <error descr="Type mismatch (Expected: 'String' got: 'Int')">42</error>); // illegal: Q monomorphed to String, Int not allowed

        genericExternRestArgs("hello", 1);                                                          // legal: Q inferred as Int
        genericExternRestArgs("hello", 1, 2, 3);                                                    // legal: all Ints, Q stays Int
        genericExternRestArgs("hello", "world");                                                    // legal: Q inferred as String
        genericExternRestArgs("hello", "world", "again");                                           // legal: all Strings, Q stays String


        genericExternRestArgs("hello", 1, <error descr="Type mismatch (Expected: 'Int' got: 'String')">"world"</error>);   // illegal: Q monomorphed to Int, String not allowed


        genericExternRestArgs("hello", "world", <error descr="Type mismatch (Expected: 'String' got: 'Int')">42</error>);  // illegal: Q monomorphed to String, Int not allowed

        genericRestSyntaxArgs("hello", 1);                                                     // legal: Q inferred as Int
        genericRestSyntaxArgs("hello", 1, 2, 3);                                               // legal: all Ints, Q stays Int
        genericRestSyntaxArgs("hello", "world");                                               // legal: Q inferred as String
        genericRestSyntaxArgs("hello", "world", "again");                                      // legal: all Strings, Q stays String


        genericRestSyntaxArgs("hello", 1, <error descr="Type mismatch (Expected: 'Int' got: 'String')">"world"</error>);    // illegal: Q monomorphed to Int, String not allowed
        genericRestSyntaxArgs("hello", "world", <error descr="Type mismatch (Expected: 'String' got: 'Int')">42</error>);   // illegal: Q monomorphed to String, Int not allowed

    }

}
