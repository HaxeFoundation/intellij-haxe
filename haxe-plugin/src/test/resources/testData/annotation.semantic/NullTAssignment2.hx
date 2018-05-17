class TypedefNullTAssignment {
  <error descr="Incompatible type Null<Int> can't be assigned from String">var a:Null<Int> = "String";</error>
}
typedef Null<T> = T;
