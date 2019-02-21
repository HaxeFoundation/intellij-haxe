class TypedefNullTAssignment {
  <error descr="Incompatible type: String should be Null<Int>">var a:Null<Int> = "String";</error>
}
typedef Null<T> = T;
