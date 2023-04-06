package;
using <info descr="">StringTools</info>;

class <info descr="">Demo</info> {
  function <info descr="">demo</info>() {
    var <info descr="">lines</info> = <info descr="">"--+--+--".split</info>("+");
    for(<info descr="">i</info> <info descr="">in</info> 0...<info descr=""><info descr="">lines</info>.length</info>)
        if(<info descr="">(<info descr="">lines</info>[<info descr="">i</info>] = <info descr=""><info descr="">lines</info>[<info descr="">i</info>].rtrim</info>()).length</info> > 0) {  // <-- rtrim and length remain unresolved -- Fixed!!
            trace(<info descr="">lines</info>[<info descr="">i</info>]);
            <info descr=""><info descr="">lines</info>[<info descr="">i</info>].rtrim</info>();
            <info descr=""><info descr="">StringTools</info>.rtrim</info>(<info descr="">lines</info>[<info descr="">i</info>]);  // <--rtrim is unresolved  -- Fixed!!
        }
  }
}