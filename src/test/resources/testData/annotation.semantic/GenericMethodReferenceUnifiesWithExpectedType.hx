package;

class BaseAbility {
  public function new() {}
}

class AutoRefillAbility extends BaseAbility {
  public function new() { super(); }
}

enum abstract AbilityType<T:BaseAbility>(String) {
  final AutoRefill:AbilityType<AutoRefillAbility> = "autoRefill";
}

class Bag {
  public function new(lookup:(AbilityType<AutoRefillAbility>, String) -> AutoRefillAbility) {}
}

class Holder {
  public function new() {}

  public function getAbility<T:BaseAbility>(id:AbilityType<T>, name:String):Null<T> {
    return null;
  }

  public function build():Bag {
    return new Bag(getAbility);
  }
}

class Test {
  public static function main() {
    new Holder().build();
  }
}
