class Foo {
	function misindentedPlain() {
		/* step one
			step two
				  step three */
		trace("a");
	}

	function interiorOnly() {
		/* first line correct
			step two at margin
			step three deep */
		trace("b");
	}

	function starRail() {
		/*
		 * railed one
		 * railed two
		 */
		trace("c");
	}

	function withGap() {
		/* alpha

			beta */
		trace("d");
	}
}
