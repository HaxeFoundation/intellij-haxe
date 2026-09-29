package buddy;

class BuddySuite {
  public function new() {}

  function describe(name:String, body:Void->Void):Void {}

  function it(description:String, ?body:Void->Void):Void {}
}
