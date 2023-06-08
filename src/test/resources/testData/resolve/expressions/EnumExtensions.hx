package;

enum <text_attr descr="null">HelperType</text_attr> {
  <text_attr descr="null">A</text_attr>;
  <text_attr descr="null">B</text_attr>;
}

class <text_attr descr="null">TestHelper</text_attr> {
  public function <text_attr descr="null">new</text_attr>() {}
  public function <text_attr descr="null">enumvalueName</text_attr>(<text_attr descr="null">type</text_attr>:<text_attr descr="null">EnumValue</text_attr>) { trace(<text_attr descr="null">type</text_attr>.<text_attr descr="null">getName</text_attr>()); }
  public function <text_attr descr="null">enumName</text_attr>(<text_attr descr="null">type</text_attr>:<text_attr descr="null">Enum</text_attr><<text_attr descr="null">Dynamic</text_attr>>) { trace(<text_attr descr="null">type</text_attr>.<text_attr descr="null">getName</text_attr>()); }
  public function <text_attr descr="null">enumString</text_attr> (<text_attr descr="null">type</text_attr>:<text_attr descr="null">EnumValue</text_attr>) { trace('This enum is named ${<text_attr descr="null">type</text_attr>.<text_attr descr="null">getName</text_attr>()}.'); }
}