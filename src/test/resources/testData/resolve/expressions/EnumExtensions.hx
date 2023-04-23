package;

enum <info descr="">HelperType</info> {
  <info descr="">A</info>;
  <info descr="">B</info>;
}

class <info descr="">TestHelper</info> {
  public function <info descr="">new</info>() {}
  public function <info descr="">enumvalueName</info>(<info descr="">type</info>:<info descr="">EnumValue</info>) { trace(<info descr="">type</info>.<info descr="">getName</info>()); }
  public function <info descr="">enumName</info>(<info descr="">type</info>:<info descr="">Enum</info><<info descr="">Dynamic</info>>) { trace(<info descr="">type</info>.<info descr="">getName</info>()); }
  public function <info descr="">enumString</info> (<info descr="">type</info>:<info descr="">EnumValue</info>) { trace('This enum is named ${<info descr="">type</info>.<info descr="">getName</info>()}.'); }
}