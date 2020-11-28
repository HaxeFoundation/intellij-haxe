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
    <error descr="<statement> expected, got 'var'">var</error> ?optionalLocalVarNotAllowed:String;
 }
}