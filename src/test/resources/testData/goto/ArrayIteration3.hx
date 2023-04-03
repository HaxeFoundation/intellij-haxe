class ArrayIteration3 {
  public function main(){
    var iter = new StringIter(0,10);
    var total = 0;
    for(iter in iter){
      iter += iter.leng<caret>th;
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