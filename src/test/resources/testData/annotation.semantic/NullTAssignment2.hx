class TypedefNullTAssignment {
  var a:Null<Int> = <error descr="Incompatible type: String should be Null<Int>">"String"</error>;
}
typedef Null<T> = T;
