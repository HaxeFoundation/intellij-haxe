class Constraint<T : (Event, EventDispatcher)> {
    var evt : T;
}

class Constraint<T : EventDispatcher> {
    var evt : T;
}