class MonomorphTest {

    public function basicMonomorphism():Void {
        var normal;
        normal = "";
        // Wrong already morphed to String
        normal = <error descr="Incompatible type: Int should be String">1</error>;

        var normalNulled = null;
        normalNulled = "";
        // Wrong already morphed to String
        normalNulled = <error descr="Incompatible type: Int should be Null<String>">1</error>;

        var arrayInit = [];
        arrayInit.push("Test");
        // Wrong already morphed to Array<String>
        arrayInit.push(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>);


        var mapInit = new Map();
        mapInit.set("test", 1);
        // Wrong already morphed to Map<String, Int>
        mapInit.set(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>, <error descr="Type mismatch (Expected: 'Int' got: 'String')">"string"</error>);

        var arrayDelayed;
        arrayDelayed = [];
        arrayDelayed.push("Test");
        // Wrong already morphed to Array<String>
        arrayDelayed.push(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>);


        var mapDelayed;
        mapDelayed = new Map();
        mapDelayed.set("test", 1);
        // Wrong already morphed to Map<String, Int>
        mapDelayed.set(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>, <error descr="Type mismatch (Expected: 'Int' got: 'String')">"test"</error>);

        var mapDelayed2 = new Map();
        mapDelayed2.clear();// verify that accessing member without type parameter(s) wont affect monomorph
        mapDelayed2.set("test", 1);
        // Wrong already morphed to Map<String, Int>
        mapDelayed2.set(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>, <error descr="Type mismatch (Expected: 'Int' got: 'String')">"test"</error>);
    }

    var myMap = new Map<String,Int>();

    public function advancemonomorphism(morphA, morphB):Void {
        var arr = [""];
        arr.push(morphA);

        morphA = <error descr="Incompatible type: Int should be String">1</error>;// Wrong already morphed String
        var <error descr="Incompatible type: String should be Int">test1:Int = morphA</error>; // Wrong already morphed String


        var obj:{a:String, b:Int} = {a:morphA, b:morphB};

        morphB = <error descr="Incompatible type: String should be Int">""</error>; // Wrong already morphed Int
        var <error descr="Incompatible type: Int should be String">test2:String = morphB</error>; // Wrong already morphed Int

        var someMap = new Map();
        myMap = someMap;

        someMap.set(<error descr="Type mismatch (Expected: 'String' got: 'Int')">1</error>,1);  // Wrong already morphed to Map<String, Int>

    }

}
