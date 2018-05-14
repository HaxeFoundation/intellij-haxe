class AA extends A {
  override public function a() {
  }
}

class A {
  public function a() {
  }
}

class BB extends B {
  override public function b() {
  }
}

class B {
  private function b() {
  }
}

class CC extends C {
  override private function <error descr="<html>Method 'c' has less visibility (<font color='red'><b>private</b></font>) than declared at 'C.c' (was <b>public</b>)</html>">c</error>() {
  }
}

class C {
  public function c() {
  }
}