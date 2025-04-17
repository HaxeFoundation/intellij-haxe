typedef  MyDef = {
var x:Int;
var y:Float;
}

class Test {

  var member:String;

  public function new() {
    var typeA:{a:String, b:Float, ?extra:String};
    var typeB =  {x:1, y:1.0};

    // CORRECT
    typeA = {a:"1", b:1.0};
    typeA = {a:"1", b:1.0, extra:"field"};
    typeA = {a:"1", b:1.0, extra:this.member};

    var def:MyDef = typeB;

    parameterTestA(typeA);
    parameterTestB(typeB);
    parameterTestC(def);


    var typeZ = {x:"i", y:"j"};

    //WRONG: missing "b"
    typeA = <error descr="Incompatible type: missing member(s) b:Float">{a:"1"}</error>;

    //WRONG: incorrect type for "b"
    typeA = {a:"1", <error descr="have 'b:String' wants 'b:Float'">b:"Str"</error>};
    //WRONG missing members
    parameterTestB(<error descr="Type mismatch, missing member(s) (x:Int, y:Float)">typeA</error>);
    //WRONG: incorrect types
    parameterTestC(<error descr="Type mismatch, Incompatible member type(s) (Incompatible type:  have 'x:String' wants 'x:Int', Incompatible type:  have 'y:String' wants 'y:Float')">typeZ</error>);
    //WRONG: incorrect types
    parameterTestC({<error descr="have 'x:String' wants 'x:Int'">x:"i"</error>, <error descr="have 'y:String' wants 'y:Float'">y:"j"</error>});

  }


  function parameterTestA(x:{a:String, b:Float, ?y:Float}) {
    var i = x.b;
  }
  function parameterTestB(x:{x:Int, y:Float, ?y:Float}) {
    var i = x.x;
  }
  function parameterTestC(x:MyDef) {
    var i = x.x;
  }

}