package;

class BaseAbility {
  public function new() {}
}

class CollectingRestrictedAbility extends BaseAbility {
  public static inline final ID = "collectingRestricted";
  public var maxAmount:Int = 0;
  public function new() { super(); }
}

class AutoRefillAbility extends BaseAbility {
  public static inline final ID = "autoRefill";
  public var refillRate:Int = 0;
  public function new() { super(); }
}

enum abstract AbilityType<T:BaseAbility>(String) {
  final CollectingRestricted:AbilityType<CollectingRestrictedAbility> = CollectingRestrictedAbility.ID;
  final AutoRefill:AbilityType<AutoRefillAbility> = AutoRefillAbility.ID;
}

class Holder {
  public function new() {}

  public function getAbility<T:BaseAbility>(id:AbilityType<T>, name:String):Null<T> {
    return null;
  }

  public function check():Bool {
    var ability = getAbility(CollectingRestricted, "strategy");
    if (ability == null) return false;
    return ability.maxAmount > 0;
  }

  public function checkRefill():Int {
    var refill = getAbility(AutoRefill, "auto");
    if (refill == null) return 0;
    return refill.refillRate;
  }
}

class Test {
  public static function main() {
    new Holder().check();
    new Holder().checkRefill();
  }
}
