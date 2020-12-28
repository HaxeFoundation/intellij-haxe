class Test
{
    public function new()
    {
        //OK
        var myMap1 = new Map();
        var myMap2 = new Map<String, Int>();
        var myMap3:Map<String, Int> = new Map();
        var myMap4:Map<String, Int> = new Map<String, Int>();

        //Wrong
        var myMap5:<error descr="Invalid number of type parameters for Map">Map<String></error> = new Map();
        var myMap6:<error descr="Invalid number of type parameters for Map">Map</error> = new Map<String, Int>();
    }
}