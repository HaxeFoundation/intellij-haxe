package com.bar;
class Test {
    static function main() {
        new TestSearchOverrides().play();
    }
}

class TestSearchOverrides {
    public function new() {
    }

    public function play() {
        playHighNotes();
        playMidNotes();
        playLowNotes();

        playThis(new Base());
        playThis(new Mid());
        playThis(new Top());
    }

    function playHighNotes() {
        var top = new Top();
        top.play();
    }

    function playMidNotes() {
        var mid = new Mid();
        mid.play();
    }

    function playLowNotes() {
        new Base().play();
    }

    function playThis(player : Base) {
        player.play();
    }
}

class Base {
    public function new() {}
    public function play() {}
}

class Mid extends Base {
    public function new() {super();}
    override function play() {}
}

class Top extends Mid {
    public function new() {super();}
    override function play() {}
}

