package ;
class ArrayUtils {
    public static function delete_if<T>( array: Array<T>, processor: T -> Bool ): Array<T>
    {
        var remove: Array<T> = [];
        for ( item in array )
            if ( processor(item) )
                remove.push( item );
        for ( item in remove )
            array.remove( item );
        return array;
    }

    public static function keep_if<T>( array: Array<T>, processor: T -> Bool ): Array<T>
    {
        var remove: Array<T> = [];
        for ( item in array )
            if ( !processor(item) )
                remove.push( item );
        for ( item in remove )
            array.remove( item );
        return array;
    }
}
