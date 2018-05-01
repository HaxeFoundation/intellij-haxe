class Test {
    static function main() {
     	var update = "blah";
        var u:Dynamic = {"$set":{"events.$":update}}; //<<--- Error was at "$set"
    }
}