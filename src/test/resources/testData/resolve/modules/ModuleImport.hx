package ;
import modules.* ;
import other.OtherModule;

class <info descr="null">ModuleImportTest</info> {
    public function <info descr="null">new</info>() {
        // should resolve (FQN)
        modules.ModuleWithMainClass.<info descr="null">moduleAFunction</info>();
        modules.ModuleWithoutMainClass.<info descr="null">moduleBFunction</info>();

        // should resolve (wildcard import)
        ModuleWithMainClass.<info descr="null">moduleAFunction</info>(ModuleWithMainClass.<info descr="null">moduleField</info>);
        ModuleWithoutMainClass.<info descr="null">moduleBFunction</info>( ModuleWithoutMainClass.<info descr="null">moduleProperty</info>);

        // should resolve (explicit import)
        <info descr="null">moduleCFunction</info>( <info descr="null">moduleCField</info> );


        ModuleWithMainClass.<info descr="null">moduleField</info>;
        ModuleWithoutMainClass.<info descr="null">moduleProperty</info>;


        // should resolve
        var <info descr="null">instance1</info>:<info descr="null">ModuleWithMainClass.SomeModuleClass</info> =  <info descr="null">new</info> <info descr="null">ModuleWithMainClass.SomeModuleClass</info>();
        var <info descr="null">instance2</info>:<info descr="null">ModuleWithMainClass</info> =  <info descr="null">new</info> <info descr="null">ModuleWithMainClass</info>();

        // should not resolve
        <warning descr="Unresolved symbol">ModuleC</warning>.<warning descr="Unresolved symbol">moduleCFunction</warning><info descr="">()</info>;
        <warning descr="Unresolved symbol">ModuleB</warning>.<warning descr="Unresolved symbol">moduleAFunction</warning><info descr="">()</info>;
        <warning descr="Unresolved symbol">moduleAFunction</warning><info descr="">()</info>;

        var <info descr="null">class1</info>:<error descr="Unresolved type"><warning descr="Unresolved symbol">SomeModuleClass</warning></error> = <error descr="SomeModuleClass does not have a constructor"><info descr="null">new</info> <error descr="Unresolved type"><warning descr="Unresolved symbol">SomeModuleClass</warning></error>()</error>;
    }
}
