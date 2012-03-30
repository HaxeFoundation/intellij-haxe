class Gti1 extends Resizable {
}
private class Resizable implements IResizable {
  public function resize() {}
}
private interface <caret>IResizable {
  function resize();
}