class ForDeclaration{
    public static function main(){
        for(foo in 0...10){
          var tmp:Int = foo + 1;
          tmp *= foo;
          trace(fo<caret>o);
        }
    }
}