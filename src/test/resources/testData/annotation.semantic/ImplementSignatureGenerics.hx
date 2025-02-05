interface  BaseClassWithConstraint<T:{length:Int}> {
  function functionWithClassTypeParam(x:T):Void ;
  function functionWithMethodTypeParam<Q>(x:Q):Void;
  function functionWithMethodTypeParamConst<W:{length:Int}>(x:W):Void ;
}

class  ImplementingClassOK implements BaseClassWithConstraint<MyClass> {
  public function functionWithClassTypeParam(x:MyClass) {}
  public function functionWithMethodTypeParam<Q>(x:Q) {}
  public function functionWithMethodTypeParamConst<W:{length:Int}>(x:W) {}
}

class  ImplementingClassError implements BaseClassWithConstraint<MyClass> {
  function functionWithClassTypeParam(<error descr="Incompatible type: String should be MyClass">x:String</error>) {}

  // TODO should show Error
  function functionWithMethodTypeParam(x:String) {}

  // TODO : Different number of constraints
  function functionWithMethodTypeParamConst<W>(x:W) {}
}

class MyClass{public var length:Int;}