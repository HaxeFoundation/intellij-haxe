package;
class Test {
    static inline var fn:String->Void = (s)->return;
    public function setCallback(callback:String->Void = fn) {}
}