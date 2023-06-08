import <text_attr descr="null">helper.Foo</text_attr>;
import <text_attr descr="null">helper.Bar</text_attr>;
import <text_attr descr="null">helper.IFoo</text_attr>;
<warning descr="Unused import statement">import <text_attr descr="null"><text_attr descr="null">helper.Typedefs</text_attr>.AliasTypedef</text_attr>;</warning>

class <text_attr descr="null">UnusedAliasTypedef</text_attr> {
  var <text_attr descr="null">foo</text_attr>:<text_attr descr="null">Foo</text_attr>;
  var <text_attr descr="null">bar</text_attr>:<text_attr descr="null">Bar</text_attr>;
  var <text_attr descr="null">ifoo</text_attr>:<text_attr descr="null">IFoo</text_attr>;
}