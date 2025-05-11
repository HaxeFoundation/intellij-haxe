package ;

class GenericFromClassArg {

    function test() {

        //CORRECT
        var x:Class<Array<String>> = classToClass(Array);
        var x:Array<String> = classToInstance(Array);
        var x:Array<Array<String>> = classToArrayOfInstance(Array);
        var x:Array<Class<Array>> = classToArrayOfClass(Array);

        //WRONG
        var x:Array<String> = <error descr="Incompatible type: Class<Array<T>> should be Array<String>">classToClass(Array)</error>;
        var x:Class<String> = <error descr="Incompatible type: Array<T> should be Class<String>">classToInstance(Array)</error>;
        var x:Array<Class<String>> = <error descr="Incompatible type: Array<Array<T>> should be Array<Class<String>>">classToArrayOfInstance(Array)</error>;
        var x:Array<Class<String>> = <error descr="Incompatible type: Array<Class<Array<T>>> should be Array<Class<String>>">classToArrayOfClass(Array)</error>;
    }

    function classToClass<T>(x:Class<T>):Class<T> {
        return null;
    }
    function classToInstance<T>(x:Class<T>):T {
        return null;
    }
    function classToArrayOfInstance<T>(x:Class<T>):Array<T> {
        return null;
    }
    function classToArrayOfClass<T>(x:Class<T>):Array<Class<T>> {
        return null;
    }

}