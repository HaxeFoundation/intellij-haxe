/*
*  Testing that untyped fields get unified correctly when init expression is null (Expectes Null<T> types)
*/
class PrimitiveNullUnification {
    //Int
    private var memberA/*<# :|Int #>*/ = 1 ;
    //Null<Int>
    private var memberB/*<# :|Null|<|Int|> #>*/  = null ;
    //Null<String>
    private var memberC/*<# :|Null|<|String|> #>*/ = null ;
    //Null<Array<String>>
    private var memberD/*<# :|Null|<|Array|<|String|>|> #>*/ = null ;


    public function new() {
        memberA = null;
        memberB = 1;
        memberC = "ddd";
        memberD = ["ddd"];
    }
}