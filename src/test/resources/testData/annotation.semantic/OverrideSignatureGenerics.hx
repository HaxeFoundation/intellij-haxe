package;

class BaseClassWithConstraint<T:{length:Int}> {
    function functionWithClassTypeParam(x:T) {}
    function functionWithMethodTypeParam<Q>(x:Q) {}
    function functionWithMethodTypeParamConst<W:{length:Int}>(x:W) {}
}

class  ExtendingClassOK extends BaseClassWithConstraint<MyClass> {
    override function functionWithClassTypeParam(x:MyClass) {}
    override function functionWithMethodTypeParam<Q>(x:Q) {}
    override function functionWithMethodTypeParamConst<W:{length:Int}>(x:W) {}
}

class  ExtendingClassError extends BaseClassWithConstraint<MyClass> {
    override function functionWithClassTypeParam(<error descr="Incompatible type: String should be MyClass">x:String</error>) {}

    // TODO should show Error: Field functionWithMethodTypeParam overrides parent class with different or incomplete type
    override function functionWithMethodTypeParam(x:String) {}

    // TODO should show Error : Different number of constraints
    override function functionWithMethodTypeParamConst<W>(x:W) {}
}

class MyClass{public var length:Int;}