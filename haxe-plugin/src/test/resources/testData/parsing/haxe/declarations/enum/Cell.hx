enum Cell<T> {
    empty;
    cons( item : T, next : Cell<T> );
}