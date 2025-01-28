  typedef Haxe3Format =
  {
     @:optional var name:String;
     @:optional var surname:String;
  };

  typedef Haxe4Format =
  {
     var ?name:String;
     var ?surname:String;
  };

class OptionalFieldsSyntax {
 public function new(?optionalArgument:String) {
     var <error descr="<local var declaration> expected, got '?'">?</error><error descr="Missing semicolon.">o</error>ptionalLocalVarNotAllowed<error descr="Missing semicolon.">:</error>String;
 }
}