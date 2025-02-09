interface  RootInterfaceInheretance {
  function  methodInterfaceParamA1(p:A):Void;
  function  methodInterfaceParamA2(p:A):Void;
  function  methodInterfaceParamA3(p:A):Void;

  function  methodInterfaceParamB1(p:B):Void;
  function  methodInterfaceParamB2(p:B):Void;
  function  methodInterfaceParamB3(p:B):Void;
//
  function  methodInterfaceParamC1(p:C):Void;
  function  methodInterfaceParamC2(p:C):Void;
  function  methodInterfaceParamC3(p:C):Void;

}

class ExtendingClassInheretance implements RootInterfaceInheretance {
  //CORRECT
  public function methodInterfaceParamA1(p:A) {}

  // WRONG
  // error: B should be A (Field methodInterfaceParamA has different type than in RootInterfaceInheretance)
  public function methodInterfaceParamA2(<error descr="Incompatible type: B should be A">p:B</error> ) {}

  //WRONG
  //  error: C should be A  Field methodInterfaceParamA3 has different type than in RootInterfaceInheretance)
  public function methodInterfaceParamA3(<error descr="Incompatible type: C should be A">p:C</error> ) {}



  // CORRECT
  public function methodInterfaceParamB1(p:A) {}

  // CORRECT
  public function methodInterfaceParamB2(p:B) {}

  // CORRECT
  public function methodInterfaceParamB3(p:C) {}



  // WRONG
  // error: A should be C (Field methodInterfaceParamC1 has different type than in RootInterfaceInheretance)
  public function methodInterfaceParamC1(<error descr="Incompatible type: A should be C">p:A</error>) {}

  // WRONG
  // error: B should be C (Field methodInterfaceParamC has different type than in RootInterfaceInheretance)
  public function methodInterfaceParamC2(<error descr="Incompatible type: B should be C">p:B</error>) {}

  // CORRECT
  public function methodInterfaceParamC3(p:C) {}

}

class A {}
class  B extends A implements  C {}
interface C {}