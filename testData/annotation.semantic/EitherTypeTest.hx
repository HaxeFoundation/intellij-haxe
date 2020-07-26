// This abstract came from  https://github.com/HaxeFoundation/hxnodejs/blob/3bb5b4d97455318f09cc0f5491e284293b2504c9/src/js/node/Cluster.hx#L107

package ;
class AbstractEnumEitherTypeTest {
    public function new() {
        var leat : Float = ListeningEventAddressType.UDPv4;  // No error because int can be assigned to float.
        var leatInt : Int = ListeningEventAddressType.TCPv4; // No error.
        var leatString : String = ListeningEventAddressType.UDPv4; // No error.

        var <error descr="Incompatible type: ListeningEventAddressType should be Bool">leatBool : Bool = ListeningEventAddressType.Unix</error>; // Should be an error.
    }
}
/**
	Structure emitted by 'listening' event.
**/
typedef ListeningEventAddress = {
    var address:String;
    var port:Int;
    var addressType:ListeningEventAddressType;
}

@:enum abstract ListeningEventAddressType(haxe.extern.EitherType<Int,String>) to haxe.extern.EitherType<Int,String> {
    var TCPv4 = 4;
    var TCPv6 = 6;
    var Unix = -1;
    var UDPv4 = "udp4";
    var UDPv6 = "udp6";
}
