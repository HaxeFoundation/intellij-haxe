@:final @:hack @:build
@:native("my.real.Path")
@:ns("namespace")
@:meta(Event(name="test",type="Foo"))
@:bitmap("myfile.png")
@superClass("user name", {name:"user"})
class FullOfMacro {
  @customUser
  var _value;
  @:getter(_value) function getValue() {
    return _value;
  }
  @:setter(_value) function setValue(value) {
    _value = value;
  }
}