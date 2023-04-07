import <info descr=""><info descr="">haxe.ds</info>.Vector</info>;
class <info descr="">ForLoopVariableResolve</info> {


    public static function <info descr="">main</info>() {
          var <info descr="">items</info>:<info descr="">Vector</info><<info descr="">String</info>> = <info descr=""><info descr="">Vector</info>.fromArrayCopy</info>(["1", "2", "3", "4", "5"]);
          for (<info descr="">item</info> <info descr="">in</info> <info descr="">items</info>) {
          trace(<info descr=""><info descr="">item</info>.charAt</info>(1));
          }
      }
}