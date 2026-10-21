package;

class CallFillRepack {
	function run(surface:Surface, options:Options) {
		surface.moveTo(firstValue, secondValue, thirdValue);
		surface.lineTo(firstValue, secondValue);
		var copy = new Options(firstValue, secondValue);
		surface.curveTo(firstValue, secondValue, thirdValue, fourthValue, fifthValue, sixthValue, seventhValue, eighthValue);
		surface.blend(inner(firstValue, secondValue), thirdValue);
		surface.each(firstValue, secondValue, function(item) {
			return item;
		});
		surface.closeAtMarginVVVVVVVVVVVVVVVVVVVV(firstValue.toString(), secondValue.toString(), options.thirdValue.toString(), options.fourthValue.toString());
		surface.closePastMarginVVVVVVVVVVVVVVVVVVV(firstValue.toString(), secondValue.toString(), options.thirdValue.toString(),
			options.fourthValue.toString());
		if (surface.guardAtMarginVVVVVVVVVVVVVV(firstValue.toString(), secondValue.toString(), options.thirdValue.toString(), options.fourthValue.toString())) {
			return;
		}
		if (surface.guardPastMarginVVVVVVVVVVVVV(firstValue.toString(), secondValue.toString(), options.thirdValue.toString(),
			options.fourthValue.toString())) {
			return;
		}
		var built = new Options(firstValue, secondValue, thirdValue, fourthValue, fifthValue, sixthValue, seventhValue, eighthValue, ninthValue, tenthValue,
			eleventhValue, twelfthValue);
		var longest = surface.name("the first argument of this call reaches the margin on its own, so the count restarts at the continuation column............",
			secondValue,
			thirdValue.toString(), fourthValue);
	}
}
