package;
class NoModelWarning {

  public function new() {
    var myVarA:UnknownType;
    myVarA = <weak_warning descr="Unable to check compatibility (type 'UnknownType' not found)">1</weak_warning>;
    var <weak_warning descr="Unable to check compatibility (type 'UnknownType' not found)">myVarB:UnknownType = "string"</weak_warning> ;
    var myVarC:String = UnknownType;
    testUnknownArgType(<weak_warning descr="Unable to check compatibility (type 'UnknownType' not found)">"string"</weak_warning>);
    testUnknownParamType(<weak_warning descr="Unable to check compatibility (type 'UnknownType' not found)">myVarA</weak_warning>);
  }
  public function testUnknownArgType(param:UnknownType) {}
  public function testUnknownParamType(param:String) {}

}
