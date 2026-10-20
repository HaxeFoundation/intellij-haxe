package;

class CallArgsWrap {
	public function copy():Options {
		return new Options(firstValue, secondValue, thirdValue, fourthValue, fifthValue, sixthValue, seventhValue, eighthValue, ninthValue, tenthValue,
eleventhValue, twelfthValue);
	}

	public function run(first:Array<Int>, second:Array<Int>, third:String, fourth:Float, fifth:Int) {
		var x = someFunctionWithAVeryLongName(firstValue, secondValue, thirdValue, fourthValue, fifthValue, sixthValue, seventhValue, eighthValue, ninthValue,
						tenthValue);
		outer(inner(firstValue, secondValue, thirdValue, fourthValue, fifthValue, sixthValue, seventhValue, eighthValue, ninthValue, tenthValue,
eleventhValue, twelfthValue),
					x);
		if (x != null) {
			deeper(firstValue, secondValue, thirdValue, fourthValue, fifthValue, sixthValue, seventhValue, eighthValue, ninthValue, tenthValue, eleventhValue,
x);
			first.method(firstValue, secondValue, thirdValue, fourthValue, fifthValue, sixthValue, seventhValue, eighthValue, ninthValue, tenthValue,
							eleventhValue)
	.other(secondValue);
		}
	}
}
