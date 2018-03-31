package;
typedef CollectionTypedef = {
    var id : String;
    var title : String;
    var height : Int;
    var width : Int;
}

class Test {
  private static var COLLECTION : Array<CollectionTypedef> = [
          {
              id : "1",
              title : "Collection1",
              height : 150,
              width : 200, // <-- extra comma
          },
          {
              id : "2",
              title : "Collection2",
              height : 150,
              width : 200, // <-- extra comma
          }, // <-- extra comma
      ];
}
