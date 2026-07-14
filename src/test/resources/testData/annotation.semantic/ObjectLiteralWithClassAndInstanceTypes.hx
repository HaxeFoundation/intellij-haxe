class MyHandler {}

typedef EntryA = {
  var handlerClass:Class<MyHandler>;
}
typedef EntryB = {
  var handlerInstance:MyHandler;
}

class Main {
  static function main() {
    var instance:MyHandler;
    var classRef: Class<MyHandler>;

    var e:EntryA = {handlerClass: MyHandler}; // CORRECT
    var e:EntryA = {handlerClass: classRef}; // CORRECT
    var e:EntryB = {handlerInstance: instance}; // CORRECT

    var e:EntryA = {<error descr="have 'handlerClass:MyHandler' wants 'handlerClass:Class<MyHandler>'">handlerClass: instance</error>}; // WRONG
    var e:EntryB = {<error descr="have 'handlerInstance:Class<MyHandler>' wants 'handlerInstance:MyHandler'">handlerInstance: MyHandler</error>}; // WRONG
    var e:EntryB = {<error descr="have 'handlerInstance:Class<MyHandler>' wants 'handlerInstance:MyHandler'">handlerInstance: classRef</error>}; // WRONG
  }
}