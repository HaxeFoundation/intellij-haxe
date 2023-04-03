class Simple {
    function foo(){
        var i = 0;
        while( i < 10 ) {
            ++i;
            if( i < 5 )
                continue;
            ++i;
        }
    }
}