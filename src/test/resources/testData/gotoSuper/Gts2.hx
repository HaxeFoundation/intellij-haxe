class Gts2 extends Resizable {
}
private class Resizable implements IResizable {
  public function <caret>resize() {}
}
private interface IResizable {
  function resize();
}