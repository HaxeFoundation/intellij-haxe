package ;
class TestIssue704 {
    static function createInstance(type:Class<Dynamic>) {
        return type == null ? throw "Mamma mia" : Type.createInstance(type, []);  //<--- Error was at ?
    }
}