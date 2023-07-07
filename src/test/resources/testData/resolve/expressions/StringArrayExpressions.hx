package;
using <info descr="null">StringTools</info>;

class <info descr="null">Demo</info> {
  function <info descr="null">demo</info>() {
    var <info descr="null">lines</info> = "--+--+--".<info descr="null">split</info>("+");
    for(<info descr="null">i</info> <info descr="null">in</info> 0...<info descr="null">lines</info>.<info descr="null">length</info>)
        if((<info descr="null">lines</info>[<info descr="null">i</info>] = <info descr="null">lines</info>[<info descr="null">i</info>].<info descr="null">rtrim</info>()).<info descr="null">length</info> > 0) {  // <-- rtrim and length remain unresolved -- Fixed!!
            trace(<info descr="null">lines</info>[<info descr="null">i</info>]);
            <info descr="null">lines</info>[<info descr="null">i</info>].<info descr="null">rtrim</info>();
            <info descr="null">StringTools</info>.<info descr="null">rtrim</info>(<info descr="null">lines</info>[<info descr="null">i</info>]);  // <--rtrim is unresolved  -- Fixed!!
        }
  }
}