class ThisShadowing {
  public function new(size:Int) {
        super();
        this.s<caret>ize = size;
    }
}