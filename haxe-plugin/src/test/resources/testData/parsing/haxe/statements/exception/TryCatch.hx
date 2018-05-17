class TryCatch {
    function foo(){
        try{
            throw new Error("invalid foo");
        } catch(e:Error) {
            trace("error");
        }
    }
}