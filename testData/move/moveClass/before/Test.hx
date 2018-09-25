package ;

import pack1.Moved;

using pack1.Moved;

class Test {
    static function main() {
        trace(Moved.test(10));
        trace(pack1.Moved.test(10));
        trace(10.test());
    }
}