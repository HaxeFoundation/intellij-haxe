package ;

class Generics {
    public function new() {
        var arr = new Array<String>();
        // OK : different types of functions/lambdas (all types are correct)
        var filtered1 = arr.filter(function(e) {return true;});
        var filtered2 = arr.filter((e) -> {return true;});
        var filtered3 = arr.filter((e) -> { true;});
        var filtered4 = arr.filter((e) -> true);
        filtered3.sort((t1, t2) -> {return 1;});
        filtered3.sort((t1, t2) -> return 1);
        filtered3.sort((t1, t2) -> 1);

        // WRONG: incorrect types in  functions/lambdas
        filtered3.sort(<error descr="Type mismatch (Expected: '(String, String)->Int' got: '(String, String)->String')">(t1, t2) -> {return "";}</error>);
        filtered3.sort(<error descr="Type mismatch (Expected: '(String, String)->Int' got: '(String, Int)->Int')">(t1, t2:Int) -> {return 1;}</error>);
        filtered3.sort(<error descr="Type mismatch (Expected: '(String, String)->Int' got: '(Int, String)->Int')">(t1:Int, t2) -> {return 1;}</error>);
        filtered3.sort(<error descr="Type mismatch (Expected: '(String, String)->Int' got: '(Int, Int)->Int')">(t1:Int, t2:Int) -> {return 1;}</error>);

    }

}
