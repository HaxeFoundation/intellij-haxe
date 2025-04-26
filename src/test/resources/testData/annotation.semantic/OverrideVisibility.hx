// test override public to public
class AA extends A {
  override public function a() {
  }
}

class A {
  public function a() {
  }
}
// test override private to public
class BB extends B {
  override public function b() {
  }
}

class B {
  private function b() {
  }
}
// test error and wraning
class CC extends C {
  override private function <error descr="Field c1 has less visibility (public/private) than superclass one.">c1</error>() {
  }
  override function <weak_warning descr="Field c2 has no visibility modifier but overrides parent with 'public'">c2</weak_warning>() {
  }
}

class C {
  public function c1() {
  }
  public function c2() {
  }
}