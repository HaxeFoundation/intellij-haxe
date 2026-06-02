// Module-level functions parse as HaxeModuleMethodDeclaration (a subtype of
// HaxeMethodDeclaration) but have no declaring class. The unused-method
// inspection used to call getParentMethod() on them, which dereferenced the
// null declaring class and threw an NPE. This fixture makes sure the
// inspection runs to completion and still flags an unused module-level
// function as a method.
function <warning descr="Method 'unusedModuleLevelFunction' is never used">unusedModuleLevelFunction</warning>() {
    trace("never called");
}
