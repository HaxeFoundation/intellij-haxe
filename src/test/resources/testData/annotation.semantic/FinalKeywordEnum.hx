package;

enum abstract FinalKeyword(String) {
  final MyFinalAssigned = "test1";
  final MyFinalUnassigned;
  var MyNormalAssigned = "test2";
  var MyNormalUnassigned;
}

class Tests {

  public function TestAssign() {
    // allowed
    var a = MyFinalAssigned;
    var b:FinalKeyword = MyFinalUnassigned;
    var c = FinalKeyword.MyFinalUnassigned;

    // not allowed
    var x:String = MyFinalUnassigned; //TODO this should also cause Incompatible type
    var y:String = <error descr="Incompatible type: FinalKeyword should be String">MyFinalAssigned</error>;
    var z:FinalKeyword =<error descr="Incompatible type: String should be FinalKeyword">"test1"</error>;
  }
}