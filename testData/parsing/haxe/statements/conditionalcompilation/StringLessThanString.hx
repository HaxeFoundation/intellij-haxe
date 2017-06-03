package;
class Test {
 #if ("this" < "that")
 function bar() {}
 #end
 #if ("that" < "this")
 function baz() {}
 #end
}