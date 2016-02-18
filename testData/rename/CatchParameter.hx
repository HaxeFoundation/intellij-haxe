class CatchParameter {
  function bar(e:Int){
    try{}
    catch(e:String) {
      trace(e<caret>);
    }
  }
}