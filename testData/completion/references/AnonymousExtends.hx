typedef BaseType = {
  name:Int,
  ?pack:Array<Int>
}

typedef ClassType = {
  >BaseType,
  ?params:Array<Int>
}

class Test {
  public function new(ct:ClassType) {
    ct.<caret>

  }
}