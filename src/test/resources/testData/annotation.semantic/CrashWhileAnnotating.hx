package ;
class StackOverflowInAnnotator {
    public static function registerFont(fontClass:Dynamic) {
        Font.registerFont(fontClass);
        return (Type.createInstance(fontClass, [])).fontName;  // <<-- Stack overflow in fromGenericResolver.
    }
}