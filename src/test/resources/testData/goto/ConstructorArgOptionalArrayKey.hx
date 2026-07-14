class ConstructorArgOptionalArrayKey {
    public static function main() {
        new Container({
            entries: [
                {ke<caret>y: "a"}
            ],
        });
    }
}

class Container {
    public function new(config:ContainerConfig) {}
}

typedef ContainerConfig = {
    var ?entries:Array<EntryConfig>;
}

typedef EntryConfig = {
    var key:String;
}
