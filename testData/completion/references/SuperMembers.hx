class Demo {
    private function method1() {
    }
    private function method2() {
    }
    public function method3() {
    }
}

class Demo2 extends Demo {
    override private function method2() {
        super.<caret> // <--- HERE you should see method1, method2 and method3
    }
}