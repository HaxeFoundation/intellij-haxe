abstract class AbstractA {
    public abstract function abstractMethodA():Float;
    public abstract function abstractMethodB():String;
}

abstract class AbstractB extends AbstractA  {
    // re-declaring allowed
    public abstract function abstractMethodA():Float;
}

class AbstractOK extends AbstractB  {
    public function abstractMethodA():Int { // Int to Float cast allowed
        return 1;
    }

    public function abstractMethodB():String {
        return "1";
    }
}

class WrongTypes extends AbstractB {

    // Field abstractMethodA overrides parent class with different or incomplete type

    public function abstractMethodA()<error descr="Incompatible return type, expected  'Float' got  'Void'.">:Void</error> {
    }

    //Field abstractMethodB overrides parent class with different or incomplete type

    public function <error descr="Incompatible return type, expected  'String' got  'Void'.">abstractMethodB</error>() {
}

}
class MissingImplementation extends <error descr="Not implemented methods: abstractMethodB">AbstractB</error> {

public function abstractMethodA():Float {
return 1;
}
}
