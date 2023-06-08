import <text_attr descr="null"><text_attr descr="null">haxe.ds</text_attr>.Vector</text_attr>;
class <text_attr descr="null">ForLoopVariableResolve</text_attr> {


    public static function <text_attr descr="null">main</text_attr>() {
          var <text_attr descr="null">items</text_attr>:<text_attr descr="null">Vector</text_attr><<text_attr descr="null">String</text_attr>> = <text_attr descr="null">Vector</text_attr>.<text_attr descr="null">fromArrayCopy</text_attr>(["1", "2", "3", "4", "5"]);
          for (<text_attr descr="null">item</text_attr> <text_attr descr="null">in</text_attr> <text_attr descr="null">items</text_attr>) {
          trace(<text_attr descr="null">item</text_attr>.<text_attr descr="null">charAt</text_attr>(1));
          }
      }
}