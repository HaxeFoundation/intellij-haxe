@:keep
@:final @:hack
@:native("my.real.Path")
@:ns("namespace")
@:meta(Event(name="test",type="Foo"))
@:bitmap("myfile.png")
@:build(MacroGenerator.build())
@:require(dep1, dep2, dep3)
@:require(haxe_ver >= 3.2)
@superClass("user name", {name:"user"})
@:myMeta.HasADot("something")
class FullOfMacro {
  @customUser
  var _value;
  @:getter(_value) function getValue() {
    return _value;
  }
  @:setter(_value) function setValue(value) {
    _value = value;
  }

  @:overload(function(name:String,value:String):js.JQuery{})
  function attr( name : String ) : String {}
}

@:build(MacroGenerator.build([], true))
extern private class Test {}