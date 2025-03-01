package testData.inlay.haxe.local.variable;
class SimpleVarHints<T,Q> {
    public function new() {
        var fromInitExpression/*<# :|String #>*/  = "";
        var fromInitExpressionGenerics/*<# :|Array|<|String|> #>*/  = [""];

        var fromFunctionCall/*<# :|String #>*/ = simpleFunction();
        var fromVarUsage/*<# :|Map|<|String|, |Int|> #>*/ = new Map();

        fromVarUsage.set("Key", 1);

        var fromMethodGenerics/*<# :|Int #>*/ = methodGenricFunction(1);
        var fromClassGenerics/*<# :|String #>*/ = classGenricFunction("str");

        var fromMethodGenericsNoTag/*<# :|Float #>*/ = methodGenricFunctionNoTag(1.0);
        var fromClassGenericsNoTag/*<# :|Float #>*/ = classGenricFunctionNoTag(1.0);

        var fromClassGenericsNoTag/*<# :|Float #>*/ = methodAndClassGenricFunctionNoTag(1.0, "Str");

        var functionPointer1/*<# :|(|)|->|String #>*/ = simpleFunction;
        var functionPointer2/*<# :|(|T|)|->|T #>*/ = methodGenricFunction;
        var functionPointer3/*<# :|(|T|)|->|T #>*/ = classGenricFunction;
        var functionPointer4/*<# :|(|T|)|->|T #>*/ = methodGenricFunctionNoTag;
        var functionPointer5/*<# :|(|T|)|->|T #>*/ = classGenricFunctionNoTag;

        var instnace/*<# :|SimpleVarHints|<|String|, |Int|> #>*/ = new SimpleVarHints<String, Int>();
        var typeFromClassGeneric/*<# :|String #>*/ = instnace.classGenricFunctionNoArg();
        var typeFromMethodGeneric/*<# :|Float #>*/  = instnace.methodGenricFunction(1.0 );
        var typeFromMethodGeneric/*<# :|String #>*/  = instnace.classGenricFunction("str");

        // inlay should stil be String even if parameter does not match Class constraints
        var typeFromMethodGeneric/*<# :|String #>*/  = instnace.classGenricFunction(1);

        var array/*<# :|Array|<|Dynamic|<|String|>|> #>*/ = new Array<Dynamic<String>>();
        var element/*<# :|Dynamic|<|String|> #>*/ = array.iterator().next();

        var vectorCopy/*<# :|Vector|<|String|> #>*/  =  haxe.ds.Vector.fromArrayCopy(["A","B"]);
        var get/*<# :|String #>*/ = vectorCopy.get(1);

        var fromLambda/*<# :|String #>*/ =  methodFunctionTypeArg((z) -> "test");
        var fromLambda/*<# :|String #>*/ =  instnace.methodFunctionTypeArg((z) -> "test");

        var anonymousFn/*<# :|(|)|->|Int #>*/ = function (){1;};
        var anonymousFnResponse/*<# :|Int #>*/ = anonymousFn();

        var anonymousFnArg/*<# :|(|String|)|->|String #>*/ = function (z:String){z;};
        var anonymousFnResponse/*<# :|String #>*/ = anonymousFnArg("");

        function localFnArgCall() {return anonymousFnArg("");}
        var fn/*<# :|(|)|->|String #>*/ = localFnArgCall;
        var fn2/*<# :|(|)|->|String #>*/ = function() {return anonymousFnArg("");}



    }

    public function  simpleFunction() {
        return "some string";
    }

    public function  methodGenricFunction<T>(x:T):T {return x;}
    public function  classGenricFunction(x:T):T {return x;}
    public function  classGenricFunctionNoArg():T {return null;}

    public function  methodGenricFunctionNoTag<T>(x:T) {return x;}
    public function  classGenricFunctionNoTag(x:T) {return x;}

    public function  methodAndClassGenricFunctionNoTag<T>(x:T, y:Q) {return x;}

    public function  methodFunctionTypeArg<T>(x:Q->T) {return x(1);}
}