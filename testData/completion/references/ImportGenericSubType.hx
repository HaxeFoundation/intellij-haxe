import generic1.ClassWithGenericSubClass;
class ImportGenericSubType {
  function new(a:GenericSubClass<Int>){
    a.<caret>
  }
}