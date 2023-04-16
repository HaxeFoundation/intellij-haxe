extern class ExternDecl
{
    static inline function resolve<T>(deliverable : Deliverable<T>, value : T #if debug , ?pos : PosInfos #end) : Void
    deliverable.deliver(value #if debug , pos #end);
}
