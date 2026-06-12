package ;

@:autoBuild(<warning descr="Unresolved symbol">AutoBuiltMacro</warning>.<warning descr="Unresolved symbol">build</warning>())
interface AutoBuilt {
  function generatedSetup():Void;
}

interface PlainInterface {
  function plainMethod():Void;
}

// Interface carries @:autoBuild, so the macro is expected to inject
// generatedSetup at compile time. The plugin should downgrade the missing
// method to a weak warning, not flag it as a hard error.
class DirectImpl implements <weak_warning descr="Method implementations might be missing: generatedSetup(compile-time macros used)">AutoBuilt</weak_warning> {
  public function new() {}
}

// Plain interface, no macro involved – this must still be reported as an error.
class MissingPlain implements <error descr="Not implemented methods: plainMethod">PlainInterface</error> {
  public function new() {}
}
