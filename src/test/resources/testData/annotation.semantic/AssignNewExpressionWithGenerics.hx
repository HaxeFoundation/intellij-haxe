class AssignHintTest {

    // TODO should be assign error
    var a2(default, null):WithConstraints<A>  = new WithConstraints(); // WRONG error: A should be B

    var a1(default, null):WithGenerics<A> = new WithGenerics();
    var a3(default, null):WithDefault<A> = new WithDefault();

    var b2(default, null):WithConstraints<B> = new WithConstraints();
    var b1(default, null):WithGenerics<B> = new WithGenerics();
    var b3(default, null):WithDefault<B> = new WithDefault();

    var c2(default, null):WithConstraints<C> = new WithConstraints();
    var c1(default, null):WithGenerics<C> = new WithGenerics();
    var c3(default, null):WithDefault<C> = new WithDefault();


}

class WithGenerics<T>{public function  new () {}}
class WithConstraints<T:B>{public function  new () {}}
class WithDefault<T = B>{public function  new () {}}

class A {}
class B extends A {}
class C extends B {}