class Test
{
    public function new()
    {
        //OK
        var myMap1 = new Map();
        var myMap2 = new Map<String, Int>();
        var myMap3:Map<String, Int> = new Map();
        var myMap4:Map<String, Int> = new Map<String, Int>();

        //Wrong
        var myMap5:<error descr="Invalid number of type parameters for Map (expected: 2 got: 1)">Map<String></error> = new Map();
        var myMap6:<error descr="Invalid number of type parameters for Map (expected: 2 got: 0)">Map</error> = new Map<String, Int>();

        //OK (default type parameters on all)
        var defaultTypeParam1:Foo;
        var defaultTypeParam2:Foo<String>;
        var defaultTypeParam3:Foo<Int>;
        var defaultTypeParam4:Foo<String, String>;

        // OK
        var defaultTypeParam4:Bar<String, String>;

        // Wrong (since the last typeParameter is not default, 2 typeParameters will be required)
        var defaultTypeParam1:<error descr="Invalid number of type parameters for Bar (expected: 2 got: 0)">Bar</error>;
        var defaultTypeParam2:<error descr="Invalid number of type parameters for Bar (expected: 2 got: 1)">Bar<String></error>;
        var defaultTypeParam3:<error descr="Invalid number of type parameters for Bar (expected: 2 got: 1)">Bar<Int></error>;

        // OK
        var defaultTypeParam5:Baz<String, String>;
        var defaultTypeParam6:Baz<String>; // using default

        // Wrong
        var defaultTypeParam6:<error descr="Invalid number of type parameters for Baz (expected: 1 got: 0)">Baz</error>; //Not enough type parameters for Baz


    }
}
class Foo<T = String, P= Int> {}
class Bar<T = String, P> {}
class Baz<T, P = Int> {}

