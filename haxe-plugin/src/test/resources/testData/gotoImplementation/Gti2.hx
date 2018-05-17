private class Resizable implements IResizable {
  public function resize() {}
}
private interface IResizable {
  function <caret>resize();
}