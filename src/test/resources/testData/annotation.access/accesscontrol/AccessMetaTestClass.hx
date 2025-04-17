package accesscontrol;


class Level3 extends AccessMetaTestClass {
    private var Level3:Int;
}

class AccessMetaTestClass extends Level2 {}

class Level2 extends Level1 {
    private var Level2:Int;
}

class Level1 {
    private var Level1:Int;
}
