/* Tests parsing 'new' in a ternary expression */

package test_tivo_229;

class A {
    public function new() {}
}

class B extends A {
    public function new() { super(); }
}

class C extends B {
    public function new( a:A ) { super(); }
}

class Test {
    public function new() {}
    
    public function getA(want_a:Bool):A {
        var qqq = (want_a == true) ? new A() : new B();
        return qqq;
    }

    public function getB(want_a:Bool):A {
        var qqq = null != new B() ? new A() : new B();
        return qqq;
    }

    public function getC(want_a:Bool):A {
        var qqq = null != new B() ? want_a == true ? new A() : new B() : want_a == false ? new A() : new B();
        return qqq;
    }

    public function compareNew() : Bool {
        return new A() == new B();
    }

    public function getChain() : C {
        return new C( new C( new C( new A() )));
    }

}
