class Tmp<T> {
    overload extern public inline function push(value:T):Int
    {
        return this.push(value);
    }

    overload extern public inline function push(args:haxe.Rest<T>):Int
    {
        var ret:Int = this.length;
        for (value in args)
        {
            ret = this.push(value);
        }
        return ret;
    }
}
