package;
class Test {
    static inline var enumVar = AnEnum.FIRST;
    static inline var enumVar2 = enumVar;

    static function useEnum(param:AnEnum = AnEnum.SECOND) {}
    static function useEnum2(param:AnEnum = enumVar) {}
    static function useEnum3(param:AnEnum = enumVar2) {}
}

enum AnEnum {
    FIRST;
    SECOND;
}

