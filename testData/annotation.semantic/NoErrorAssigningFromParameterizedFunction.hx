package;

import Type;

class NoErrorAssigningFromParameterizedFunction {
    public function new() {

        var component:Component = new Component();
        var componentClass:Class<Component> = Type.getClass(component);
        var newComponent:Component = Type.createInstance(componentClass, []); // Error was "Incompatible Type: T should be Component"
    }
}

class Component {
    public function new() {}
}