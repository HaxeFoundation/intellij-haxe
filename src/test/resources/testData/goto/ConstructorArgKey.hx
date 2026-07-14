class ConstructorArgKey {
    public static function main() {
        new Container({
            it<caret>ems: [
                {itemId: 1}
            ],
        });
    }
}

class Container {
    public function new(config:ContainerConfig) {}
}

typedef ContainerConfig = {
    var items:Array<ItemConfig>;
    var ?labels:Array<String>;
}

typedef ItemConfig = {
    var itemId:Int;
}
