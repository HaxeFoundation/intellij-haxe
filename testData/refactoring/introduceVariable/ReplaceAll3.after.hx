class ReplaceAll3 {
    function main() {
        var tmp = false;
        var a = tmp ? 239 : -239;
        for (i in 0...100) {
            print(a);
        }
        var a = 239 + (a);
    }
}