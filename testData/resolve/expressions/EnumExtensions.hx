package;

enum <info descr="null">HelperType</info> {
  <info descr="null">A</info>;
  <info descr="null">B</info>;
}

class <info descr="null">TestHelper</info> {
  public function <info descr="null">new</info>() {}
  public function <info descr="null">enumvalueName</info>(<info descr="null">type</info>:<info descr="null">EnumValue</info>) { trace(<info descr="null">type</info>.<info descr="null">getName</info>()); }
  public function <info descr="null">enumName</info>(<info descr="null">type</info>:<info descr="null">Enum</info><<info descr="null">Dynamic</info>>) { trace(<info descr="null">type</info>.<info descr="null">getName</info>()); }
  public function <info descr="null">enumString</info> (<info descr="null">type</info>:<info descr="null">EnumValue</info>) { trace('This enum is named ${<info descr="null">type</info>.<info descr="null">getName</info>()}.'); }
}