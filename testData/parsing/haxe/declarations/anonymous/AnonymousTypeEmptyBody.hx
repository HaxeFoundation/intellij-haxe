class AnonymousTypeWithEmptyBodyTest {
  public var x:{};
  public var y(get,never):{};

  public function new() {}

  function get_y():{} {
    return cast {hello:"hi"};
  }
}

typedef AnonymousWithEmptyBody = {};