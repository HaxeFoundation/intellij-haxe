package;


class Test {
  public function new() {
    // CORRECT
    var s1:MyStruct = {name:"name", age:30};
    var s2:MyStruct = {name:"name", age:30, height:1.0, address:"address 1"};

    // WRONG
    var <error descr="Incompatible type: missing member(s) age:Int">s3:MyStruct = {name:"name"}</error>; // missing field
    var s4:MyStruct = <error descr="Incompatible type: {...} should be MyStruct">{name:"name", age:30, address:"address 1", extra:"field"}</error>; // to many fields

      // CORRECT (TypeParameter)
    var typeParamA:MyTypeParamStruct<String> = {valueA:"name", valueB:30};
      // WRONG (TypeParameter)
    var typeParamB:MyTypeParamStruct<Int> = {<error descr="have 'valueA:String' wants 'valueA:Int'">valueA:"name"</error>, valueB:30};

      //CORRECT (constructor)
    var NormalConstructor:ConstrcutorStruct<String> = {name:"str",  value:1.0, type: ValueA};
    var diffrentOrder:ConstrcutorStruct<String> = {value:1.0, type: ValueA, name:"str"};
    var usingDefaults:ConstrcutorStruct<String> =  {value:1.0,  name:"str"};

      // WRONG (constructor)
    var wrongType:ConstrcutorStruct<String> =  {<error descr="have 'value:String' wants 'value:Float'"><error descr="have 'value:String' wants 'value:Float'">value</error>:"1.0"</error>,  name:"str"};
    var wrongTypeTypeParameter:ConstrcutorStruct<String> =  {value:1.0,  <error descr="have 'name:Int' wants 'name:String'"><error descr="have 'name:Int' wants 'name:String'">name</error>:1</error>};
    var <error descr="Incompatible type: missing member(s) value">missingField:ConstrcutorStruct<String> =  { name:"str"}</error>;

  }
}

@:structInit class MyStruct {
  //NOTE: should not show warning about final when in structInit class
  final name:String;
  var age:Int;
  var height:Float = 3.0; // init = same as optional
  @:optional var address:String;

}

@:structInit class MyTypeParamStruct<T> {
  var valueA:T;
  var valueB:Int;

}

enum TestNum { ValueA; ValueB; }

@:structInit
class ConstrcutorStruct<T> {
  public var name(default,null) : T;
  public var value(default,null) : Float;
  public var type(default,null) : TestNum;
  public var intenral(default,null) : String;

  public inline function new( name : T, value : Float, type = ValueB ) {
    this.name = name;
    this.type = type;
    this.value = value;
    this.intenral = "internalValue";
  }
}