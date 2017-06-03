package;
class Test {
 #if !( (((!cpp) && js) && haxe_ver < 3.5 ) || (haxe_ver >= 3.2 && !cpp && "foo" != "bar"))
 function bar() {}
 #end
}