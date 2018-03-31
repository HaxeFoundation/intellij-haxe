class NonOptionalArgumentsAfterOptionalOnes {
  static public function demo(a:Int = 10, <warning descr="Non-optional argument after optional argument">b</warning>, <warning descr="Non-optional argument after optional argument">c:String</warning>, d:Bool = false) {
  }
}