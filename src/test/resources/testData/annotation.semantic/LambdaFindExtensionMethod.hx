using Lambda;
using extensions.FindTools;

class Entry {
  public var id:String;
  public function new() {}
}

class Bucket {
  public var entries:Array<Entry>;
  public var kind:String;
  public var expires:Int = 0;
  public function new() {}
}

class Registry {
  public var buckets:Array<Bucket>;
  public function new() {}
}

class Holder<T> {
  public var value:T;
  public function new(value:T) { this.value = value; }
}

class Test {
  final _registry:Holder<Null<Registry>> = new Holder<Null<Registry>>(null);

  // find on a plain local array, expression-body lambda
  static function simpleFind(buckets:Array<Bucket>):Bool {
    return buckets.find(bucket -> bucket.kind == "plain") != null;
  }

  // block-body lambda with multiple returns
  static function blockBodyFind(buckets:Array<Bucket>):Bool {
    return buckets.find(bucket -> {
      if (bucket.kind != "plain") return false;
      if (bucket.entries == null || bucket.entries.length == 0) return false;
      return bucket.expires == 0;
    }) != null;
  }

  // receiver reached through a generic field with Null<T>
  function chainedFind(kind:String):Bool {
    if (_registry.value == null) return false;
    return _registry.value.buckets.find(bucket -> {
      if (bucket.kind != kind) return false;
      return bucket.expires == 0;
    }) != null;
  }

  // the whole thing nested inside another lambda
  function nestedLambdaFind(kind:String):Bool {
    return auto(() -> {
      if (_registry.value == null) return false;
      return _registry.value.buckets.find(bucket -> {
        if (bucket.kind != kind) return false;
        return bucket.expires == 0;
      }) != null;
    });
  }

  // the result of find keeps the element type
  static function findResult(buckets:Array<Bucket>):Null<String> {
    var match = buckets.find(bucket -> bucket.kind == "plain");
    if (match != null) return match.kind;
    return null;
  }

  // extension declared against Array<T> directly (nominal match with the receiver)
  static function arrayDeclaredExtension(buckets:Array<Bucket>):Bool {
    return buckets.findInArray(bucket -> bucket.kind == "plain") != null;
  }

  // extension declared against Iterable<T> (structural match with the receiver)
  static function iterableDeclaredExtension(buckets:Array<Bucket>):Bool {
    return buckets.findInIterable(bucket -> bucket.kind == "plain") != null;
  }

  static function auto<T>(f:() -> T):T return f();

  static function main() {}
}
