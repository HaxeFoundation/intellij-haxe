
extern class EReg {
  function match( s:String ):Bool ;
  function matched( n:Int ):String;
  function matchedLeft():String;
  function matchedRight():String;
  function matchedPos():{ pos:Int, len:Int };
  function split( s:String ):Array<String> ;
  function replace( s:String, by:String ):String ;
  function customReplace( s:String, f:EReg -> String ):String ;
}