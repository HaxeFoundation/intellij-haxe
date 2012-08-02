class ReferenceExpression9 {
  function genType( t : Type ) {
      switch( t ) {
          case TInst(c, _):
              var foo = f<caret>oo.get();
          default:
              return 239;
      }
  }
}