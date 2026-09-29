class Main {
	static function main() {
		var offsetX = 1.0;
		var offsetY = 2.0;
		switch (kind) {
			case CUBIC:
				var c = readCurve();
				surface.curveTo(c.firstHandleX
					- offsetX, c.firstHandleY
					- offsetY, c.secondHandleX
					- offsetX, c.secondHandleY
					- offsetY, c.targetX
					- offsetX,
					c.targetY
					- offsetY);
			case SCALED:
				surface.curveTo(scaledFirstHandleX
					- offsetX, scaledFirstHandleY
					- offsetY, scaledSecondHandleX
					- offsetX, scaledSecondHandleY
					- offsetY,
					scaledTargetX
					- offsetX, scaledTargetY
					- offsetY);
			default:
		}
	}
}
