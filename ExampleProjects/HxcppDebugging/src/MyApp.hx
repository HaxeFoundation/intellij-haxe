package ;

import openfl.system.System;

/**
 * The starting point for our application.
 **/
class MyApp {

    public function new() {
    }

    public static function main() : Void {
        /*
         * Add application initialization here.
         */

        var aNewVar : {a:Int, b:Int} = { a:12, b:100 }

        trace("Hello World!");
        trace("I am alive... alive!");

        System.exit(0);
    }

}
