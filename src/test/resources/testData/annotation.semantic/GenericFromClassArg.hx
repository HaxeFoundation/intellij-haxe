package ;

class GenericFromClassArg {

    function test() {

        //CORRECT
        var x:Class<Array<String>> = classToClass(Array);
        var x:Array<String> = classToInstance(Array);
        var x:Array<Array<String>> = classToArrayOfInstance(Array);
        var x:Array<Class<Array>> = classToArrayOfClass(Array);

        //WRONG
        var <error descr="Incompatible type: Class<Array<T>> should be Array<String>">x:Array<String> = classToClass(Array)</error>;
        var <error descr="Incompatible type: Array<T> should be Class<String>">x:Class<String> = classToInstance(Array)</error>;
        var <error descr="Incompatible type: Array<Array<T>> should be Array<Class<String>>">x:Array<Class<String>> = classToArrayOfInstance(Array)</error>;
        var <error descr="Incompatible type: Array<Class<Array<T>>> should be Array<Class<String>>">x:Array<Class<String>> = classToArrayOfClass(Array)</error>;
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