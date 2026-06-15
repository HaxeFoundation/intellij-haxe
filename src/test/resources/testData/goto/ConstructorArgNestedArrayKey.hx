class ConstructorArgNestedArrayKey {
    public static function main() {
        new Container({
            items: [
                {item<caret>Id: 1}
            ],
        });
    }
}

class Container {
    public function new(config:ContainerConfig) {}
}

typedef ContainerConfig = {
    var items:Array<ItemConfig>;
}

typedef ItemConfig = {
    var itemId:Int;
}
