enum MyNum<T> {
    StringContainer(x:String);
    DoubleContainer(x:String, i:Int);
    GenericContainer(i:T, x:String);
}

class CaptureVarShadowing {

    var classField = true;

    public function new() {
        var functionVar = true;
        var value = "testString";
        var enumValue = GenericContainer([value], "stringValue");

        switch(enumValue) {
            case StringContainer(_.toLowerCase() => extracted):
                var extracted = extracted.length;
                // verify that case scoped variable is resolved before extracted value
                var trippel =  extracted * 3;

            case DoubleContainer(_.toLowerCase() => _.split(",") => extracted, _):
                var extracted =extracted.pop();
                var length = extracted.length;


            case GenericContainer(array , _.toLowerCase() => _):
                var array = array.pop();
                array.charAt(0);

            case StringContainer(_ => functionVar):
                var len = functionVar.length;

            case StringContainer(classField):
                var len = classField.length;
        }

        switch(getNum(null)) {
            case StringContainer(_.toLowerCase() => extracted):
                var extracted = extracted.length;
                // verify that case scoped variable is resolved before extracted value
                var trippel =  extracted * 3;

            case DoubleContainer(_.toLowerCase() => _.split(",") => extracted, _):
                var extracted =extracted.pop();
                var length = extracted.length;


            case GenericContainer(array , _.toLowerCase() => _):
                var array = array.pop();
                array.charAt(0);
        }
    }


    private function getNum(value:MyNum<Array<String>>) {
        return  value;
    }
}
