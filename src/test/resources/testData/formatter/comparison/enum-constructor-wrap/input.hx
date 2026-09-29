package;

enum Shape {
	Circle(radius:Float, filled:Bool, sides:Int);
	Polygon(firstValue:Float, secondValue:Float, thirdValue:Int, fourthValue:Float, fifthValue:Float, sixthValue:Float, seventhValue:Float, eighthValue:Int,
ninthValue:Bool);
	Curve(firstValue:Float, secondValue:Float, firstList:Array<Int>, secondList:Array<Float>, thirdList:Array<Int>, sixthValue:Float, seventhValue:Float,
						eighthValue:Int, ninthValue:String, tenthValue:Bool);
}
