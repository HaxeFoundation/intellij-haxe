package ;

class AllowUnknownGenerics {

  public function  testArgs() {
    // correct (allowed by the compiler)
    var instanceOK1 = new AllowUnknownGenerics(sourceWithUnknownGenerics());
    var instanceOK2 = createInstance(sourceWithUnknownGenerics());

    // wrong
    var instanceWrong1 = new AllowUnknownGenerics(<error descr="Type mismatch (Expected: 'WithGenerics<LastType>' got: 'WithGenerics<String>')">sourceWithWrongType()</error>); //error: String should be LastType
    var instanceWrong2 = createInstance(<error descr="Type mismatch (Expected: 'WithGenerics<LastType>' got: 'WithGenerics<String>')">sourceWithWrongType()</error>);// error: String should be LastType
  }

  public function new(arg:WithGenerics<LastType>) {}

  public static function createInstance(arg:WithGenerics<LastType>) {
    return new AllowUnknownGenerics(arg);
  }

  function sourceWithUnknownGenerics<T3:MidType>(?arg:Class<T3>){
    return new WithGenerics<T3>();
  }
  function sourceWithWrongType(){
    return new WithGenerics<String>(); //TODO should add check on generic constraint compability, add error
  }
}

class WithGenerics<B:MidType>{function new(){}}

class BaseType {public function new(){}}
class MidType extends BaseType {public function new(){super();}}
class LastType extends MidType {public function new(){super();}}
