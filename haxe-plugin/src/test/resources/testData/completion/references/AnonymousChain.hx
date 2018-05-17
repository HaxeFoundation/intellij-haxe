class AnonymousChain {
  public static function test() {
    var user:User;
    user.location.location.<caret>
  }
}

typedef User = {
  name:String,
  id:String,
  location: {
    city:String,
    country:String,
    location: {
      latitude: Float,
      longitude: Float
    }
  }
};
