package;

class BaseTrait {
  public function new() {}
}

class RepeatTrait extends BaseTrait {
  public function new() { super(); }
}

enum abstract TraitKind<T:BaseTrait>(String) {
  final Repeat:TraitKind<RepeatTrait> = "repeat";
}

class Bag {
  public function new(lookup:(TraitKind<RepeatTrait>, String) -> RepeatTrait) {}
}

class Holder {
  public function new() {}

  public function getTrait<T:BaseTrait>(id:TraitKind<T>, name:String):Null<T> {
    return null;
  }

  public function build():Bag {
    return new Bag(getTrait);
  }
}

class Test {
  public static function main() {
    new Holder().build();
  }
}
