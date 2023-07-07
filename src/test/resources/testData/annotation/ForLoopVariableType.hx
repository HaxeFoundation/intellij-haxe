import <info descr="null"><info descr="null">haxe.ds</info>.Vector</info>;
class <info descr="null">ForLoopVariableResolve</info> {


    public static function <info descr="null">main</info>() {
          var <info descr="null">items</info>:<info descr="null">Vector</info><<info descr="null">String</info>> = <info descr="null">Vector</info>.<info descr="null">fromArrayCopy</info>(["1", "2", "3", "4", "5"]);
          for (<info descr="null">item</info> <info descr="null">in</info> <info descr="null">items</info>) {
          trace(<info descr="null">item</info>.<info descr="null">charAt</info>(1));
          }
      }
}