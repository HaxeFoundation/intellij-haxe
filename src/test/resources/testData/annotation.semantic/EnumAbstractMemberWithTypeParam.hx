package;

class AbstractComponent {
  public static inline final TYPE_A = "a";
  public static inline final TYPE_B = "b";
  public static inline final TYPE_C = "c";
}

class ComponentA extends AbstractComponent {
  public function new() {}
}

class ComponentB extends AbstractComponent {
  public function new() {}
}

class ComponentC extends AbstractComponent {
  public function new() {}
}

enum abstract ComponentType<T:AbstractComponent>(String) to String {
  final A:ComponentType<ComponentA> = AbstractComponent.TYPE_A;
  final B:ComponentType<ComponentB> = AbstractComponent.TYPE_B;
  final C:ComponentType<ComponentC> = AbstractComponent.TYPE_C;
}
