class TryCatch {
    function foo(){
        try{
            bar();
        } catch( e : String ) {
            trace("String");
        } catch( e : Error ) {
            trace("Error");
        } catch( e : Dynamic ) {
            trace("Dynamic");
        }
    }
}