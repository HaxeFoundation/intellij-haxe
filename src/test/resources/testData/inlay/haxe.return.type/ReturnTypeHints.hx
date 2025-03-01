class Test {
    public function tryCatch()/*<# :|String #>*/ {
        return try getStr() catch(e:String) e;
    }

    public function ifElse()/*<# :|String #>*/ {
        return if (true) "A" else "B";
    }

    public function switchCase()/*<# :|Int #>*/ {
        return switch (true) {
            case true : 1;
            default: 2;
        }
    }

    public function getStr()/*<# :|String #>*/ return "";
}