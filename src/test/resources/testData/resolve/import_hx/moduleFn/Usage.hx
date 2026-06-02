package moduleFn;

// Both `_` and `wrap` are brought in globally by import.hx in this directory.
// Neither call should be flagged "Unresolved symbol".
class Usage {
  public function new() {
    _("hello");
    wrap("world");
  }
}
