class TestingOverloads {
    public function test() {

        var date = new js.lib.Date();
        var date = new js.lib.Date(1.0);
        var date = new js.lib.Date("String");
        var date = new js.lib.Date(1,1);

        var date = new js.lib.Date(1,1 , <error descr="Could not find parameter accepting 'String' ">""</error>); // Wrong
        var date = new js.lib.Date(<error descr="Type mismatch (Expected: 'Int' got: 'Bool')">false</error>,1); // Wrong


        date.toLocaleDateString("", null);
        date.toLocaleDateString([""], null);

        date.toLocaleDateString(false, <error descr="Could not find parameter accepting 'Bool' ">null</error>); // wrong


        var floatVar = testReturnValue(1.0);
        var intVar:Int = testReturnValue(true);
        var StringVar:String = testReturnValue("str");

    }
    // ignore the "Missing return statement", not a real error, should probably be ignored
    @:overload(function<T>(b:T):T {})
    @:overload(function(b:Bool):Int {})
    @:overload(function(b:Bool):String <error descr="Overload must only declare an empty method body {}">{return "";}</error>)
    function testReturnValue(s:String):String;

}