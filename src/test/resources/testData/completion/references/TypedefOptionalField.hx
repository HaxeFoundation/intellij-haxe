typedef NamedObject = {
    @:optional var name : String;
}

class Main {
  function test() {
    var namedObject:NamedObject = {};
    namedObject.<caret>
  }
}