package;
using <text_attr descr="null">StringTools</text_attr>;

class <text_attr descr="null">Demo</text_attr> {
  function <text_attr descr="null">demo</text_attr>() {
    var <text_attr descr="null">lines</text_attr> = "--+--+--".<text_attr descr="null">split</text_attr>("+");
    for(<text_attr descr="null">i</text_attr> <text_attr descr="null">in</text_attr> 0...<text_attr descr="null">lines</text_attr>.<text_attr descr="null">length</text_attr>)
        if((<text_attr descr="null">lines</text_attr>[<text_attr descr="null">i</text_attr>] = <text_attr descr="null">lines</text_attr>[<text_attr descr="null">i</text_attr>].<text_attr descr="null">rtrim</text_attr>()).<text_attr descr="null">length</text_attr> > 0) {  // <-- rtrim and length remain unresolved -- Fixed!!
            trace(<text_attr descr="null">lines</text_attr>[<text_attr descr="null">i</text_attr>]);
            <text_attr descr="null">lines</text_attr>[<text_attr descr="null">i</text_attr>].<text_attr descr="null">rtrim</text_attr>();
            <text_attr descr="null">StringTools</text_attr>.<text_attr descr="null">rtrim</text_attr>(<text_attr descr="null">lines</text_attr>[<text_attr descr="null">i</text_attr>]);  // <--rtrim is unresolved  -- Fixed!!
        }
  }
}