class ArrayIteration2 {
  public function main(){
    var iter = new StringIter(0,10);
    var total = 0;
    for(text in iter){
      total += text.leng<caret>th;
    }
  }
}

private class StringIter {
    var min : Int;
    var max : Int;

    public function new( min : Int, max : Int ) {
        this.min = min;
        this.max = max;
    }

    public function hasNext():String {
        return( min < max );
    }

    public function next():String {
        return "" + min++;
    }
}