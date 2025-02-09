interface  RootInterfaceInheretance2 {
  function  methodInterfacereturnA1():A;
  function  methodInterfacereturnA2():A;
  function  methodInterfacereturnA3():A;

  function  methodInterfacereturnB1():B;
  function  methodInterfacereturnB2():B;
  function  methodInterfacereturnB3():B;

  function  methodInterfacereturnC1():C;
  function  methodInterfacereturnC2():C;
  function  methodInterfacereturnC3():C;
}


class ExtendingClassInheretance2 implements RootInterfaceInheretance2 {

  //CORRECT
  public function methodInterfacereturnA1():A {
    return null;
  }

  // CORRECT
  public function methodInterfacereturnA2():B {
    return null;
  }

  // WRONG
  //error: C should be A (Field methodInterfacereturnA3 has different type than in RootInterfaceInheretance2)
  public function methodInterfacereturnA3()<error descr="Incompatible return type: C should be A">:C</error> {
  return null;
  }

  // WRONG
  //  error: A should be B (Field methodInterfacereturnB has different type than in RootInterfaceInheretance2)
  public function methodInterfacereturnB1()<error descr="Incompatible return type: A should be B">:A</error> {
  return null;
  }

  // CORRECT
  public function methodInterfacereturnB2():B {
    return null;
  }

  //WRONG
  // error: C should be B (Field methodInterfacereturnB3 has different type than in RootInterfaceInheretance2)
  public function methodInterfacereturnB3()<error descr="Incompatible return type: C should be B">:C</error> {
  return null;
  }

  //WRONG
  // error: A should be C
  public function methodInterfacereturnC1()<error descr="Incompatible return type: A should be C">:A</error> {
  return null;
  }

  // CORRECT
  public function methodInterfacereturnC2():B {
    return null;
  }

  // CORRECT
  public function methodInterfacereturnC3():C {
    return null;
  }

}

class A {}
class  B extends A implements  C {}
interface C {}