class ConstructorArgIntersectionKey {
    public static function main() {
        new Container({
            items: [
                {itemId: 1, fl<caret>ag: true}
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
} & ExtraConfig;

typedef ExtraConfig = {
    var ?flag:Bool;
}
