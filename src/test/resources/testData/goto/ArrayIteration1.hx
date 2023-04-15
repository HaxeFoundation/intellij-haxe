class ArrayIteration1 {
  public function main(){
    var arr = new Array<String>();
    var total = 0;
    for(text in arr){
      total += text.leng<caret>th;
    }
  }
}