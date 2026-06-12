// This test should show no errors.
package;

interface IHandler {}
class FirstHandler implements IHandler {}
class SecondHandler implements IHandler {}
class SomeModel {}
class SomeView {}

class EventNames {
  public static inline final FIRST:String = "first";
  public static inline final SECOND:String = "second";
}

typedef ConditionPart = {
  var ?condition:Int;
}

typedef ModelEntry = {
  var modelClass:Class<Dynamic>;
  var ?createInstance:Bool;
  var ?mappingClass:Class<Dynamic>;
} & ConditionPart;

typedef HandlerEntry = {
  var eventName:String;
  var handlerClass:Class<IHandler>;
  var ?priority:Int;
}

typedef ViewEntry = {
  var viewClass:Class<Dynamic>;
  var handlerClass:Class<IHandler>;
  var ?lazy:Bool;
}

typedef Config = {
  var models:Array<ModelEntry>;
  var views:Array<ViewEntry>;
  var ?handlers:Array<HandlerEntry>;
}

class Holder {
  final _config:Config;
  public function new(config:Config) {
    _config = config;
  }
}

class Test {
  // literal passed straight to a constructor parameter (static final field initializer)
  public static final holder = new Holder({
    models: [
      {modelClass: SomeModel}
    ],
    handlers: [
      {eventName: EventNames.FIRST, handlerClass: FirstHandler},
      {eventName: EventNames.SECOND, handlerClass: SecondHandler},
    ],
    views: [
      {viewClass: SomeView, handlerClass: FirstHandler}
    ]
  });

  static function main() {
    // simpler variants: array literals of anonymous structures assigned to typed locals
    var a:Array<HandlerEntry> = [{eventName: "x", handlerClass: FirstHandler}];
    var b:Array<ViewEntry> = [{viewClass: SomeView, handlerClass: FirstHandler}];
    var c:Array<ModelEntry> = [{modelClass: SomeModel}];

    // single elements (no array wrapping)
    var d:HandlerEntry = {eventName: "x", handlerClass: FirstHandler};
    var e:ModelEntry = {modelClass: SomeModel};
  }
}
