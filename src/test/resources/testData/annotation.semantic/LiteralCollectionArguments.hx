package;

import haxe.ds.IntMap;

class Test {

  public static function main() {
            // Array<Any> accepts all data types when literal
            arrayAny([new Test()]) ;
            arrayAny([1]) ;
            arrayAny([""]) ;
            arrayAny([new Test(), 1, ""]) ;

            arrayInt([1,2,3]);
            arrayInt(<error descr="Type mismatch (Expected: 'Array<Int>' got: 'Array<String>')">["1","2","3"]</error>); // WRONG

            arrayFloat([1,2,3]);// ints have direct cast to floats so this should be allowed
            arrayFloat(<error descr="Type mismatch (Expected: 'Array<Float>' got: 'Array<String>')">["1","2","3"]</error>);// Wrong

            // Array<Dynamic> parameter accepts all data types
            arrayDynamic([new Test()]) ;
            arrayDynamic([1]) ;
            arrayDynamic([""]) ;
            arrayDynamic([new Test(), 1, ""]) ;

            // Array<Any> accepts all data types when literal
            arrayAny([""]) ;
            arrayAny([new Test(), 1, ""]) ;

            // Map<Any,Any> parameter accepts all data types
            mapAnyAny([""=> ""]) ;
            mapAnyAny([""=> 1]) ;

            mapAnyAny(<error descr="Type mismatch (Expected: 'Map<Any, Any>' got: 'Array<Test>')">[new Test()]</error>) ; //WRONG not map literal
            mapAnyAny(<error descr="Type mismatch (Expected: 'Map<Any, Any>' got: 'Array<Int>')">[1]</error>) ; //WRONG not map literal

            mapStringAny([""=> ""]) ;
            mapStringAny(<error descr="Type mismatch (Expected: 'Map<String, Any>' got: 'haxe.ds.Map<Int, String>')">[1=> ""]</error>) ; // WRONG

            mapAnyString([""=> ""]) ;
            mapAnyString(<error descr="Type mismatch (Expected: 'Map<Any, String>' got: 'haxe.ds.Map<String, Int>')">[""=> 1]</error>) ; // WRONG

            mapStringString(["1" => "1"]);
            mapStringString(<error descr="Type mismatch (Expected: 'Map<String, String>' got: 'haxe.ds.Map<Int, String>')">[1 => "1"]</error>); //WRONG


  }

      public function arrayString(arg:Array<String>){}
      public function arrayInt(arg:Array<Int>){}
      public function arrayFloat(arg:Array<Float>){}
      public function arrayAny(arg:Array<Any>){}
      public function arrayDynamic(arg:Array<Dynamic>){}

      public function mapStringString(arg:Map<String, String>){}
      public function mapAnyString(arg:Map<Any, String>){}
      public function mapStringAny(arg:Map<String, Any>){}
      public function mapAnyAny(arg:Map<Any, Any>){}
}