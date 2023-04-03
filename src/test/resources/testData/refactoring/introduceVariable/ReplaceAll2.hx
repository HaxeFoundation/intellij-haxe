class ReplaceAll2 {
    function main() {
        var tmp = false;
        for (i in 0...100) {
            print(tmp ? 239:-239);
        }
        var a = 239 + (<selection>tmp ? 239 : -239</selection>);
    }
}