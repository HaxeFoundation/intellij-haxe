package ;

typedef Point<T> = { x:T, y:T };

class IsOperator {
    public static function main() {
        var myString  = "testString";
        var priorityTest = 2 + 2 * 2 + 2 & ~0x001;
        var associativityTest = "test" + "this" is String; // Allowed in 4.0. 'Missing `;`' in 3.x

        if (myString is String) {}  // Allowed in 4.2. Syntax error in 4.1: 'characters 22-24 : Expected )'
                                    // 3.4: 'Unexpected `is`', 'Missing `;`', and 'Unexpected )'

        var singleExpression =  myString is String; // allowed in 4.1, 4.2, syntax error in 4.0.5, 'Missing ;' in 3.4.
        var singleExpression2 = myString is String.String; // allowed in 4.2- qualified package name, syntax error in 4.1. 'Missing ;' in 3.4.
        var mulitExpression =  (myString is String); // allowed
        var mulitExpression2 = (myString is String) && false; // allowed since it has parentheses
        var multiExpression3 =  myString is String && false;  // must be wrapped with parentheses in 4.1, allowed in 4.2. 'Missing ;' in 3.4.

        // Incorrect uses
        var incorrectUse = this.stage is 123; //  incorrect use, Expected Type/Class got literal 'Missing ;' in 3.4.
        var incorrectUse2 = this.stage is myString; //  incorrect use, Expected Type/Class got variable. 'Missing ;' in 3.4.
        var incorrectUse3 = myString is {x:1, y:1};  // Syntax error: 'Unexpected `1`.' 'Missing ;' in 3.4.
        var incorrectUse4 = myString is {x:Int, y:Int} || call(); // 'Unsupported type for `is` operator.' 'Missing ;' in 3.4.
        var incorrectUse5 = myString is Point<Int>;     // 'Type parameters are not supported for the `is` operator.' 'Missing ;' in 3.4.
        var incorrectUse6 = myString is {var x:Int; var y:Int;}  // 'Unsupported type for `is` operator.'
        var incorrectUse7 = myString is {function iterator(){}}  // 'Unsupported type for `is` operator.'
    }
}
