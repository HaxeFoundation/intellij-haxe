class StaticVarIssue {
  public static var staticVarInParent:Int = 1;
  public function testMethod() {
    return staticVarInParent;
  }
}
class OtherClass extends StaticVarIssue {
  override public function testMethod() {
    // static access to member of extended type should not reolve (Unknown identifier)
    return <error descr="Unresolved type">staticVarInParent</error>;
  }
}
