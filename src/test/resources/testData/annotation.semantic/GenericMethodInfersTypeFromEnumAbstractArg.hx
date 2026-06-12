package;

class BaseTrait {
  public function new() {}
}

class CountLimitTrait extends BaseTrait {
  public static inline final ID = "countLimit";
  public var maxCount:Int = 0;
  public function new() { super(); }
}

class RepeatTrait extends BaseTrait {
  public static inline final ID = "repeat";
  public var repeatRate:Int = 0;
  public function new() { super(); }
}

enum abstract TraitKind<T:BaseTrait>(String) {
  final CountLimit:TraitKind<CountLimitTrait> = CountLimitTrait.ID;
  final Repeat:TraitKind<RepeatTrait> = RepeatTrait.ID;
}

class Holder {
  public function new() {}

  public function getTrait<T:BaseTrait>(id:TraitKind<T>, name:String):Null<T> {
    return null;
  }

  public function check():Bool {
    var trait = getTrait(CountLimit, "first");
    if (trait == null) return false;
    return trait.maxCount > 0;
  }

  public function checkRepeat():Int {
    var repeat = getTrait(Repeat, "second");
    if (repeat == null) return 0;
    return repeat.repeatRate;
  }
}

class Test {
  public static function main() {
    new Holder().check();
    new Holder().checkRepeat();
  }
}
