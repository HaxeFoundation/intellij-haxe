class Main {
	static function main() {
		switch (kind) {
				// leading block comment line one
				// line two
			case ONE:
				one();

				// trailing after a blank, before the next case
			case TWO:
				two();
			// trailing directly after a statement
			case THREE:
			// the case's only body
			case FOUR:
				four();

			// case FIVE:

			// TODO

			default:
				zero();
		}
	}
}
