package accesscontrol;

@:access(accesscontrol.AccessMetaTestClass)
class MetaAccessTest {
    public function test() {
        var privateInstance:Level3 = null;

        //OK (NOTE: access on class gives access to all classes in class hierarchy)
        privateInstance.Level3;
        privateInstance.Level2;
        privateInstance.Level1;
    }
}