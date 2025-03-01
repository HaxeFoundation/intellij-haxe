package ;

class InlayHintReifiction {

    // expression reifications

    macro public static function macroArrayReification(inputArray:Array<Expr>)/*<# :|ExprOf|<|Array|<|Expr|>|> #>*/ {
        return macro $a{ inputArray };
    }

    macro public static function macroValueReification(dynamicType:Array<String>)/*<# :|ExprOf|<|Array|<|String|>|> #>*/ {
        return macro $v{ dynamicType }
    }

    macro public static function macroBlockReification(a:Array<Expr>)/*<# :|Expr #>*/ {
        return macro $b{a};
    }

    macro public static function macroIdentifierReification(identifier:String)/*<# :|Expr #>*/ {
        return macro $i{identifier};
    }

    macro public static function macroFieldReification(field:Array<String>)/*<# :|Expr #>*/ {
        return macro ($p{field});
    }

    macro public static function macroExpReification(str:String)/*<# :|ExprOf|<|(|)|->|Dynamic|> #>*/
    return macro (function() return $e{str});

    // type reifications

    macro public static function typeReification(complexType:ComplexType)/*<# :|ComplexType #>*/ {
        return macro : Null<$complexType>;
    }

    // class reification

    macro public static function classReification(funcName:String)/*<# :|TypeDefinition #>*/ {
        return macro class MyClass {
            public function new() {}

            public function $funcName()/*<# :|Void #>*/ {
                trace($v{funcName} + " was called");
            }
        }
    }
}