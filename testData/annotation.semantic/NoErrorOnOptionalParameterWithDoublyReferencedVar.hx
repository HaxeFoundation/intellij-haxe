package;
class Test {
    static inline var baseVar = 1;
    static inline var chasingVar = baseVar;
    static function chase(param:Int = chasingVar) {}
}