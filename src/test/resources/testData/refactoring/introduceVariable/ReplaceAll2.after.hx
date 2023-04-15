class ReplaceAll2 {
    function main() {
        var tmp = false;
        var foo = tmp ? 239 : -239;
        for (i in 0...100) {
            print(foo);
        }
        var a = 239 + (foo);
    }
}