// NO_HINTS
class Test {

    // only called by itself: recursive call sites are excluded from
    // inference (the manual documents them as producing over-specialized
    // types), so the parameter stays untyped and shows no hint
    static function spin(value) {
        spin(value);
    }
}